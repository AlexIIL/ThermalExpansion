package cofh.thermalexpansion.plugins.jei.crafting.insolator;

import cofh.lib.util.helpers.ItemHelper;
import cofh.lib.util.helpers.StringHelper;
import cofh.thermalexpansion.block.machine.TileInsolator;
import cofh.thermalexpansion.plugins.jei.Drawables;
import cofh.thermalexpansion.plugins.jei.JEIPluginTE;
import cofh.thermalexpansion.plugins.jei.RecipeUidsTE;
import cofh.thermalexpansion.plugins.jei.crafting.BaseRecipeWrapper;
import cofh.thermalexpansion.util.managers.machine.InsolatorManager.ComparableItemStackInsolator;
import cofh.thermalexpansion.util.managers.machine.InsolatorManager.InsolatorRecipe;
import cofh.thermalexpansion.util.managers.machine.InsolatorManager.Type;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableAnimated.StartDirection;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InsolatorRecipeWrapper extends BaseRecipeWrapper {

	/* Recipe */
	final List<List<ItemStack>> inputs;
	final List<List<FluidStack>> inputFluids;
	final List<ItemStack> outputs;

	final int chance;

	final Type substrate;

	/* Animation */
	final IDrawableAnimated fluid;
	final IDrawableAnimated progress;
	final IDrawableAnimated speed;

	public InsolatorRecipeWrapper(IGuiHelper guiHelper, InsolatorRecipe recipe) {

		this(guiHelper, recipe, RecipeUidsTE.INSOLATOR);
	}

	public InsolatorRecipeWrapper(IGuiHelper guiHelper, InsolatorRecipe recipe, String uIdIn) {

		uId = uIdIn;

		List<List<ItemStack>> recipeInputs = new ArrayList<>();
		List<FluidStack> recipeInputFluids = new ArrayList<>();
		List<ItemStack> recipeInputsPrimary = new ArrayList<>();
		List<ItemStack> recipeInputsSecondary = new ArrayList<>();

		if (ComparableItemStackInsolator.getOreID(recipe.getPrimaryInput()) != -1) {
			for (ItemStack ore : OreDictionary.getOres(ItemHelper.getOreName(recipe.getPrimaryInput()), false)) {
				recipeInputsPrimary.add(ItemHelper.cloneStack(ore, recipe.getPrimaryInput().getCount()));
			}
		} else {
			recipeInputsPrimary.add(recipe.getPrimaryInput());
		}
		if (ComparableItemStackInsolator.getOreID(recipe.getSecondaryInput()) != -1) {
			for (ItemStack ore : OreDictionary.getOres(ItemHelper.getOreName(recipe.getSecondaryInput()), false)) {
				recipeInputsSecondary.add(ItemHelper.cloneStack(ore, recipe.getSecondaryInput().getCount()));
			}
		} else {
			recipeInputsSecondary.add(recipe.getSecondaryInput());
		}
		recipeInputs.add(recipeInputsPrimary);
		recipeInputs.add(recipeInputsSecondary);
		recipeInputFluids.add(new FluidStack(FluidRegistry.WATER, recipe.getEnergy() / 10));

		List<ItemStack> recipeOutputs = new ArrayList<>();
		recipeOutputs.add(recipe.getPrimaryOutput());

		if (recipe.getSecondaryOutput() != null) {
			recipeOutputs.add(recipe.getSecondaryOutput());
		}
		inputs = recipeInputs;
		inputFluids = Collections.singletonList(recipeInputFluids);
		outputs = recipeOutputs;

		energy = recipe.getEnergy();
		chance = recipe.getSecondaryOutputChance();

		substrate = recipe.getType();

		IDrawableStatic fluidDrawable = Drawables.getDrawables(guiHelper).getProgress(Drawables.PROGRESS_ARROW_FLUID);
		IDrawableStatic progressDrawable = Drawables.getDrawables(guiHelper).getProgressFill(Drawables.PROGRESS_ARROW_FLUID);
		IDrawableStatic speedDrawable = Drawables.getDrawables(guiHelper).getScaleFill(Drawables.SCALE_SUN);
		IDrawableStatic energyDrawable = Drawables.getDrawables(guiHelper).getEnergyFill();

		fluid = guiHelper.createAnimatedDrawable(fluidDrawable, energy / TileInsolator.basePower, StartDirection.LEFT, true);
		progress = guiHelper.createAnimatedDrawable(progressDrawable, energy / TileInsolator.basePower, StartDirection.LEFT, false);
		speed = guiHelper.createAnimatedDrawable(speedDrawable, 1000, StartDirection.TOP, true);
		energyMeter = guiHelper.createAnimatedDrawable(energyDrawable, 1000, StartDirection.TOP, true);
	}

	@Override
	public void getIngredients(IIngredients ingredients) {

		ingredients.setInputLists(ItemStack.class, inputs);
		ingredients.setInputLists(FluidStack.class, inputFluids);
		ingredients.setOutputs(ItemStack.class, outputs);
	}

	@Override
	public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {

		JEIPluginTE.drawFluid(69, 23, inputFluids.get(0).get(0), 24, 16);

		fluid.draw(minecraft, 69, 23);
		progress.draw(minecraft, 69, 23);
		speed.draw(minecraft, 34, 34);
		energyMeter.draw(minecraft, 2, 8);

		if (chance > 0) {
			String dispChance = StringHelper.formatNumber(chance) + "%";
			minecraft.fontRendererObj.drawString(dispChance, 102 - 6 * dispChance.length(), 48, 0x808080);
		}
	}

}
