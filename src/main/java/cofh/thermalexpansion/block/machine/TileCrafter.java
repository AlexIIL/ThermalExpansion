package cofh.thermalexpansion.block.machine;

import cofh.core.fluid.FluidTankCore;
import cofh.core.network.PacketCoFHBase;
import cofh.lib.inventory.InventoryCraftingFalse;
import cofh.lib.util.helpers.FluidHelper;
import cofh.lib.util.helpers.InventoryHelper;
import cofh.lib.util.helpers.ItemHelper;
import cofh.lib.util.helpers.ServerHelper;
import cofh.thermalexpansion.ThermalExpansion;
import cofh.thermalexpansion.gui.client.machine.GuiCrafter;
import cofh.thermalexpansion.gui.container.machine.ContainerCrafter;
import cofh.thermalexpansion.init.TEProps;
import cofh.thermalfoundation.util.helpers.SchematicHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Arrays;
import java.util.HashSet;

public class TileCrafter extends TileMachineBase {

	private static final int TYPE = BlockMachine.Type.CRAFTER.getMetadata();

	public static void initialize() {

		SIDE_CONFIGS[TYPE] = new SideConfig();
		SIDE_CONFIGS[TYPE].numConfig = 6;
		SIDE_CONFIGS[TYPE].slotGroups = new int[][] { {}, { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 }, { 1 }, { 3, 4, 5, 6, 7, 8, 9, 10, 11 }, { 12, 13, 14, 15, 16, 17, 18, 19, 20 }, { 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 } };
		SIDE_CONFIGS[TYPE].sideTypes = new int[] { 0, 1, 4, 5, 6, 7 };
		SIDE_CONFIGS[TYPE].defaultSides = new byte[] { 1, 1, 2, 2, 2, 2 };

		SLOT_CONFIGS[TYPE] = new SlotConfig();
		SLOT_CONFIGS[TYPE].allowInsertionSlot = new boolean[] { true, true, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true };
		SLOT_CONFIGS[TYPE].allowExtractionSlot = new boolean[] { true, true, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true };

		VALID_AUGMENTS[TYPE] = new HashSet<>();

		GameRegistry.registerTileEntity(TileCrafter.class, "thermalexpansion:machine_crafter");

		config();
	}

	public static void config() {

		String category = "Machine.Crafter";
		BlockMachine.enable[TYPE] = ThermalExpansion.CONFIG.get(category, "Enable", true);

		ENERGY_CONFIGS[TYPE] = new EnergyConfig();
		ENERGY_CONFIGS[TYPE].setDefaultParams(20);
	}

	public static final int PROCESS_ENERGY = 20;

	private boolean needsCache = true;
	private boolean needsCraft = false;

	private int outputTracker;
	private FluidTankCore tank = new FluidTankCore(TEProps.MAX_FLUID_LARGE);
	private InventoryCrafting crafting = new InventoryCraftingFalse(3, 3);
	private ItemStack recipeOutput;

	private FluidStack[] filledContainer = new FluidStack[9];
	private ItemStack[] recipeSlot = new ItemStack[9];
	private String[] recipeOre = new String[9];

	public TileCrafter() {

		super();
		inventory = new ItemStack[1 + 1 + 1 + 18];
		Arrays.fill(inventory, ItemStack.EMPTY);

		createAllSlots(inventory.length);
	}

	@Override
	public int getType() {

		return TYPE;
	}

	@Override
	public void update() {

		if (ServerHelper.isClientWorld(world)) {
			return;
		}
		boolean curActive = isActive;

		if (redstoneControlOrDisable()) {
			if (needsCraft) {
				updateOutput();
			}
			if (timeCheck()) {
				transferOutput();
			}
		} else {
			if (isActive) {
				wasActive = true;
			}
			isActive = false;
		}
		updateIfChanged(curActive);
		chargeEnergy();
	}

	@Override
	protected void transferOutput() {

		if (!enableAutoOutput) {
			return;
		}
		if (inventory[1].isEmpty()) {
			return;
		}
		int side;
		for (int i = outputTracker + 1; i <= outputTracker + 6; i++) {
			side = i % 6;

			if (sideCache[side] == 2) {
				if (transferItem(1, ITEM_TRANSFER[level], EnumFacing.VALUES[side])) {
					outputTracker = side;
					break;
				}
			}
		}
	}

	@Override
	public int getChargeSlot() {

		return 2;
	}

	private boolean canCreate(ItemStack recipe) {

		return recipe != null && (inventory[1].isEmpty() || recipe.isItemEqual(inventory[1]) && inventory[1].getCount() + recipe.getCount() <= recipe.getMaxStackSize());
	}

	private boolean createItem() {

		if (energyStorage.getEnergyStored() < PROCESS_ENERGY) {
			return false;
		}
		ItemStack[] invCopy = InventoryHelper.cloneInventory(inventory);
		FluidStack fluidCopy = null;

		if (tank.getFluid() != null) {
			fluidCopy = tank.getFluid().copy();
		}
		boolean found = false;
		for (int i = 0; i < 9; i++) {
			if (fluidCopy != null) {
				if (fluidCopy.isFluidEqual(filledContainer[i])) {
					if (fluidCopy.amount >= filledContainer[i].amount) {
						fluidCopy.amount -= filledContainer[i].amount;
						crafting.setInventorySlotContents(i, recipeSlot[i].copy());
						continue; // Go to the next item in the schematic
					}
				}
			}
			if (!recipeSlot[i].isEmpty()) {
				for (int j = 2; j < invCopy.length; j++) {
					if (!invCopy[j].isEmpty() && ItemHelper.craftingEquivalent(invCopy[j], recipeSlot[i], recipeOre[i], recipeOutput)) {
						crafting.setInventorySlotContents(i, invCopy[j].copy());
						invCopy[j].shrink(1);

						if (invCopy[j].getItem().hasContainerItem(invCopy[j])) {
							ItemStack containerStack = invCopy[j].getItem().getContainerItem(invCopy[j]);

							if (containerStack.isEmpty()) {
								// this is absolutely stupid and nobody should ever make a container item where this gets called
							} else {
								if (containerStack.isItemStackDamageable() && containerStack.getItemDamage() > containerStack.getMaxDamage()) {
									containerStack = ItemStack.EMPTY;
								}
								if (!containerStack.isEmpty() && (/*!invCopy[j].getItem().doesContainerItemLeaveCraftingGrid(invCopy[j]) ||*/ !InventoryHelper.addItemStackToInventory(invCopy, containerStack, 3))) {
									if (invCopy[j].getCount() <= 0) {
										invCopy[j] = containerStack;
										if (containerStack.getCount() <= 0) {
											invCopy[j].setCount(1);
										}
									} else {
										return false;
									}
								}
							}
						}
						if (invCopy[j].getCount() <= 0) {
							invCopy[j] = ItemStack.EMPTY;
						}
						found = true;
						break;
					}
				}
				if (!found) {
					return false;
				}

				found = false;
			} else {
				crafting.setInventorySlotContents(i, ItemStack.EMPTY);
			}
		}
		// Craftable - Update inventories.
		inventory = invCopy;

		if (fluidCopy == null || fluidCopy.amount <= 0) {
			fluidCopy = null;
		}
		tank.setFluid(fluidCopy);
		energyStorage.modifyEnergyStored(-PROCESS_ENERGY);
		return true;
	}

	private void updateOutput() {

		if (!inventory[0].isEmpty()) {
			if (needsCache) {
				recipeOutput = SchematicHelper.getOutput(inventory[0], world);
				for (int i = 0; i < 9; i++) {
					recipeSlot[i] = SchematicHelper.getSchematicSlot(inventory[0], i);
					filledContainer[i] = FluidHelper.getFluidForFilledItem(recipeSlot[i]);
					recipeOre[i] = SchematicHelper.getSchematicOreSlot(inventory[0], i);
				}
				needsCache = false;
			}
			if (recipeOutput.isEmpty()) {
				isActive = false;
				return;
			}
			if (canCreate(recipeOutput)) {
				if (createItem()) {
					recipeOutput = ItemHelper.findMatchingRecipe(crafting, world);
					if (!recipeOutput.isEmpty()) {
						if (inventory[1].isEmpty()) {
							inventory[1] = recipeOutput.copy();
						} else {
							inventory[1].grow(recipeOutput.getCount());
						}
						transferOutput();
						isActive = true;
					}
				} else {
					if (energyStorage.getEnergyStored() >= PROCESS_ENERGY) {
						needsCraft = false;
					}
					wasActive = true;
					isActive = false;
					return;
				}
			} else {
				if (isActive) {
					wasActive = true;
				}
				isActive = false;
			}
		}
	}

	/* GUI METHODS */
	@Override
	public Object getGuiClient(InventoryPlayer inventory) {

		return new GuiCrafter(inventory, this);
	}

	@Override
	public Object getGuiServer(InventoryPlayer inventory) {

		return new ContainerCrafter(inventory, this);
	}

	@Override
	public FluidTankCore getTank() {

		return tank;
	}

	@Override
	public FluidStack getTankFluid() {

		return tank.getFluid();
	}

	/* NBT METHODS */
	@Override
	public void readFromNBT(NBTTagCompound nbt) {

		super.readFromNBT(nbt);
		outputTracker = nbt.getInteger("Output");
		needsCraft = true;
		needsCache = true;
		tank.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {

		super.writeToNBT(nbt);
		nbt.setInteger("Output", outputTracker);
		tank.writeToNBT(nbt);
		return nbt;
	}

	/* NETWORK METHODS */

	/* SERVER -> CLIENT */
	@Override
	public PacketCoFHBase getGuiPacket() {

		PacketCoFHBase payload = super.getGuiPacket();

		payload.addFluidStack(getTankFluid());

		return payload;
	}

	@Override
	protected void handleGuiPacket(PacketCoFHBase payload) {

		super.handleGuiPacket(payload);

		tank.setFluid(payload.getFluidStack());
	}

	/* IInventory */
	@Override
	public void markDirty() {

		needsCraft = true;
		super.markDirty();
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount) {

		needsCraft = true;
		needsCache = needsCache || slot == 0;
		return super.decrStackSize(slot, amount);
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {

		needsCraft = true;
		needsCache = needsCache || slot == 0;

		inventory[slot] = stack;

		if (!stack.isEmpty() && stack.getCount() > getInventoryStackLimit()) {
			stack.setCount(getInventoryStackLimit());
		}
	}

	/* CAPABILITIES */
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing from) {

		return super.hasCapability(capability, from) || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
	}

	//	@Override
	//	public <T> T getCapability(Capability<T> capability, final EnumFacing from) {
	//
	//		if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
	//			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new IFluidHandler() {
	//				@Override
	//				public IFluidTankProperties[] getTankProperties() {
	//
	//					return FluidTankProperties.convert(new FluidTankInfo[] { tank.getInfo() });
	//				}
	//
	//				@Override
	//				public int fill(FluidStack resource, boolean doFill) {
	//
	//					if (from != null && !sideConfig.allowInsertionSide[sideCache[from.ordinal()]]) {
	//						return 0;
	//					}
	//					int filled = tank.fill(resource, doFill);
	//
	//					if (doFill && filled > 0) {
	//						needsCraft = true;
	//					}
	//					return filled;
	//				}
	//
	//				@Nullable
	//				@Override
	//				public FluidStack drain(FluidStack resource, boolean doDrain) {
	//
	//					if (from != null && !sideConfig.allowExtractionSide[sideCache[from.ordinal()]]) {
	//						return null;
	//					}
	//					if (resource == null || !resource.isFluidEqual(tank.getFluid())) {
	//						return null;
	//					}
	//					return tank.drain(resource.amount, doDrain);
	//				}
	//
	//				@Nullable
	//				@Override
	//				public FluidStack drain(int maxDrain, boolean doDrain) {
	//
	//					if (from != null && !sideConfig.allowExtractionSide[sideCache[from.ordinal()]]) {
	//						return null;
	//					}
	//					return tank.drain(maxDrain, doDrain);
	//				}
	//			});
	//		}
	//		return super.getCapability(capability, from);
	//	}

}
