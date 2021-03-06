package cofh.thermalexpansion.gui.container.machine;

import cofh.lib.gui.slot.ISlotValidator;
import cofh.lib.gui.slot.SlotEnergy;
import cofh.lib.gui.slot.SlotValidated;
import cofh.thermalexpansion.block.machine.TileFurnace;
import cofh.thermalexpansion.gui.container.ContainerTEBase;
import cofh.thermalexpansion.util.managers.machine.FurnaceManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.SlotFurnaceOutput;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ContainerFurnace extends ContainerTEBase implements ISlotValidator {

	protected TileFurnace myTile;

	public ContainerFurnace(InventoryPlayer inventory, TileEntity tile) {

		super(inventory, tile);

		myTile = (TileFurnace) tile;
		addSlotToContainer(new SlotValidated(this, myTile, 0, 53, 26));
		addSlotToContainer(new SlotFurnaceOutput(inventory.player, myTile, 1, 116, 35));
		addSlotToContainer(new SlotEnergy(myTile, myTile.getChargeSlot(), 8, 53));
	}

	@Override
	public boolean isItemValid(ItemStack stack) {

		if (myTile.augmentFood() && !FurnaceManager.isFood(stack)) {
			return false;
		}
		if (myTile.augmentOre() && !FurnaceManager.isOre(stack)) {
			return false;
		}
		if (myTile.augmentPyrolysis()) {
			return FurnaceManager.recipeExistsPyrolysis(stack);
		}
		return FurnaceManager.recipeExists(stack);
	}

}
