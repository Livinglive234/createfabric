package com.simibubi.create.compat.rei;

import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Helper recipe type for displaying an item relationship in JEI
 */
@ParametersAreNonnullByDefault
public class ConversionRecipe extends StandardProcessingRecipe<SingleRecipeInput> {

	static int counter = 0;

	public static ConversionRecipe create(ItemStack from, ItemStack to) {
		ResourceLocation recipeId = Create.asResource("conversion_" + counter++);
		return new StandardProcessingRecipe.Builder<>(ConversionRecipe::new, recipeId)
			.withItemIngredients(Ingredient.of(from))
			.withSingleItemOutput(to)
			.build();
	}

	public ConversionRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.CONVERSION, params);
	}

	@Override
	public boolean matches(SingleRecipeInput inv, Level worldIn) {
		return false;
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

}
