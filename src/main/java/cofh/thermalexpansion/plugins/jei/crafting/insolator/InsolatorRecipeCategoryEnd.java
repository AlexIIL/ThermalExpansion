package cofh.thermalexpansion.plugins.jei.crafting.insolator;

import cofh.lib.util.helpers.StringHelper;
import cofh.thermalexpansion.block.machine.BlockMachine;
import cofh.thermalexpansion.item.ItemAugment;
import cofh.thermalexpansion.plugins.jei.RecipeUidsTE;
import cofh.thermalexpansion.util.managers.machine.InsolatorManager;
import cofh.thermalexpansion.util.managers.machine.InsolatorManager.InsolatorRecipe;
import cofh.thermalexpansion.util.managers.machine.InsolatorManager.Type;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class InsolatorRecipeCategoryEnd extends InsolatorRecipeCategory {

	public static void initialize(IModRegistry registry) {

		IJeiHelpers jeiHelpers = registry.getJeiHelpers();
		IGuiHelper guiHelper = jeiHelpers.getGuiHelper();

		registry.addRecipes(getRecipes(guiHelper), RecipeUidsTE.INSOLATOR_END);
		registry.addRecipeCatalyst(ItemAugment.machineInsolatorEnd, RecipeUidsTE.INSOLATOR_END);
		registry.addRecipeCatalyst(BlockMachine.machineInsolator, RecipeUidsTE.INSOLATOR_END);
	}

	public static List<InsolatorRecipeWrapper> getRecipes(IGuiHelper guiHelper) {

		List<InsolatorRecipeWrapper> recipes = new ArrayList<>();

		for (InsolatorRecipe recipe : InsolatorManager.getRecipeList()) {

			if (recipe.getType() == Type.END) {
				recipes.add(new InsolatorRecipeWrapper(guiHelper, recipe, RecipeUidsTE.INSOLATOR_END));
			}
		}
		return recipes;
	}

	public InsolatorRecipeCategoryEnd(IGuiHelper guiHelper) {

		super(guiHelper);

		localizedName = StringHelper.localize("item.thermalexpansion.augment.machineInsolatorEnd.name");
	}

	@Nonnull
	@Override
	public String getUid() {

		return RecipeUidsTE.INSOLATOR_END;
	}

}
