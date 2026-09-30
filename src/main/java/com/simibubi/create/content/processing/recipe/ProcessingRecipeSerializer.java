package com.simibubi.create.content.processing.recipe;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import com.simibubi.create.infrastructure.fabric.transfer.fluid.FluidStack;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ProcessingRecipeSerializer {

	public static <T extends ProcessingRecipe<?, ?>> MapCodec<T> codec(AllRecipeTypes recipeTypes) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.either(Ingredient.CODEC, FluidIngredient.CODEC).listOf().fieldOf("ingredients").forGetter(i -> {
					List<Either<Ingredient, FluidIngredient>> list = new ArrayList<>();
					i.getIngredients().forEach(o -> list.add(Either.left(o)));
					i.getFluidIngredients().forEach(o -> list.add(Either.right(o)));
					return list;
				}),
				Codec.either(ProcessingOutput.CODEC, FluidStack.CODEC).listOf().fieldOf("results").forGetter(i -> {
					List<Either<ProcessingOutput, FluidStack>> list = new ArrayList<>();
					i.getRollableResults().forEach(o -> list.add(Either.left(o)));
					i.getFluidResults().forEach(o -> list.add(Either.right(o)));
					return list;
				}),
			ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("processing_time", 0).forGetter(i -> i.getProcessingDuration()),
			HeatCondition.CODEC.optionalFieldOf("heat_requirement", HeatCondition.NONE).forGetter(i -> i.getRequiredHeat())
		).apply(instance, (ingredients, results, processingTime, heatRequirement) -> {
			RecipeSerializer<?> serializer = recipeTypes.getSerializer();
			if (!(serializer instanceof StandardProcessingRecipe.Serializer<?> standardSerializer))
				throw new RuntimeException("Not a standard processing recipe serializer " + serializer);

			@SuppressWarnings({"unchecked", "rawtypes"})
			StandardProcessingRecipe.Builder<T> builder =
				new StandardProcessingRecipe.Builder<T>((StandardProcessingRecipe.Factory) standardSerializer.factory(), recipeTypes.getId());

			NonNullList<Ingredient> ingredientList = NonNullList.create();
			NonNullList<FluidIngredient> fluidIngredientList = NonNullList.create();

			NonNullList<ProcessingOutput> processingOutputList = NonNullList.create();
			NonNullList<FluidStack> fluidStackOutputList = NonNullList.create();

			for (Either<Ingredient, FluidIngredient> either : ingredients) {
				either.left().ifPresent(ingredientList::add);
				either.right().ifPresent(fluidIngredientList::add);
			}

			for (Either<ProcessingOutput, FluidStack> either : results) {
				either.left().ifPresent(processingOutputList::add);
				either.right().ifPresent(fluidStackOutputList::add);
			}

			builder.withItemIngredients(ingredientList)
					.withItemOutputs(processingOutputList)
					.withFluidIngredients(fluidIngredientList)
					.withFluidOutputs(fluidStackOutputList)
					.duration(processingTime)
					.requiresHeat(heatRequirement);

			return builder.build();
		}));
	}
}
