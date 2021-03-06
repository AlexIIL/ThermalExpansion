package cofh.thermalexpansion.plugins.jei.crafting.transposer;

import cofh.lib.util.helpers.StringHelper;
import cofh.thermalexpansion.block.machine.BlockMachine;
import cofh.thermalexpansion.gui.client.machine.GuiTransposer;
import cofh.thermalexpansion.plugins.jei.RecipeUidsTE;
import cofh.thermalexpansion.util.managers.machine.TransposerManager;
import cofh.thermalexpansion.util.managers.machine.TransposerManager.TransposerRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TransposerRecipeCategoryFill extends TransposerRecipeCategory {

	public static void initialize(IModRegistry registry) {

		IJeiHelpers jeiHelpers = registry.getJeiHelpers();
		IGuiHelper guiHelper = jeiHelpers.getGuiHelper();

		registry.addRecipes(getRecipes(guiHelper, registry.getIngredientRegistry()), RecipeUidsTE.TRANSPOSER_FILL);
		registry.addRecipeCatalyst(BlockMachine.machineTransposer, RecipeUidsTE.TRANSPOSER_FILL);
	}

	public static List<TransposerRecipeWrapper> getRecipes(IGuiHelper guiHelper, IIngredientRegistry ingredientRegistry) {

		List<TransposerRecipeWrapper> recipes = new ArrayList<>();

		for (TransposerRecipe recipe : TransposerManager.getFillRecipeList()) {
			recipes.add(new TransposerRecipeWrapper(guiHelper, recipe, RecipeUidsTE.TRANSPOSER_FILL));
		}
		List<ItemStack> ingredients = ingredientRegistry.getIngredients(ItemStack.class);

		for (ItemStack ingredient : ingredients) {
			if (ingredient.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
				for (Fluid fluid : FluidRegistry.getRegisteredFluids().values()) {
					addFillRecipe(ingredient, fluid, recipes, guiHelper);
				}
			}
		}
		return recipes;
	}

	private static void addFillRecipe(ItemStack baseStack, Fluid fluid, List<TransposerRecipeWrapper> recipes, IGuiHelper guiHelper) {

		ItemStack filledStack = baseStack.copy();
		IFluidHandlerItem handler = filledStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
		int fill = handler.fill(new FluidStack(fluid, Fluid.BUCKET_VOLUME), true);

		if (fill > 0) {
			FluidStack filledFluid = new FluidStack(fluid, fill);
			filledStack = handler.getContainer();
			TransposerRecipe recipe = new TransposerRecipe(baseStack, filledStack, filledFluid, TransposerManager.DEFAULT_ENERGY, 100);
			recipes.add(new TransposerRecipeWrapper(guiHelper, recipe, RecipeUidsTE.TRANSPOSER_FILL));
		}
	}

	public TransposerRecipeCategoryFill(IGuiHelper guiHelper) {

		super(guiHelper);

		localizedName += " - " + StringHelper.localize("gui.thermalexpansion.jei.transposer.modeFill");

		icon = guiHelper.createDrawable(GuiTransposer.TEXTURE, 176, 48, 16, 16);
	}

	@Nonnull
	@Override
	public String getUid() {

		return RecipeUidsTE.TRANSPOSER_FILL;
	}

	@Override
	public void setRecipe(IRecipeLayout recipeLayout, TransposerRecipeWrapper recipeWrapper, IIngredients ingredients) {

		List<List<ItemStack>> inputs = ingredients.getInputs(ItemStack.class);
		List<List<ItemStack>> outputs = ingredients.getOutputs(ItemStack.class);
		List<List<FluidStack>> fluids = ingredients.getInputs(FluidStack.class);

		IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
		IGuiFluidStackGroup guiFluidStacks = recipeLayout.getFluidStacks();

		guiItemStacks.init(0, true, 30, 10);
		guiItemStacks.init(1, false, 30, 41);
		guiFluidStacks.init(0, true, 103, 1, 16, 60, 2000, false, tankOverlay);

		guiItemStacks.set(0, inputs.get(0));
		guiItemStacks.set(1, outputs.get(0));
		guiFluidStacks.set(0, fluids.get(0));
	}

}
