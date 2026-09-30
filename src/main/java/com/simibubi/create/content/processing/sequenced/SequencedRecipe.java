package com.simibubi.create.content.processing.sequenced;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import com.simibubi.create.AllRecipeTypes;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SequencedRecipe<T extends ProcessingRecipe<?, ?>> {
	public static final Codec<SequencedRecipe<?>> CODEC = AllRecipeTypes.CODEC
		.<ProcessingRecipe<?, ?>>dispatch(r -> (AllRecipeTypes) r.getTypeInfo(), AllRecipeTypes::processingCodec)
		.validate(r -> r instanceof IAssemblyRecipe ? DataResult.success(r) :
			DataResult.error(() -> r.getType() + " is not a supported recipe type"))
		.xmap(SequencedRecipe::new, SequencedRecipe::getRecipe);

	public static final StreamCodec<RegistryFriendlyByteBuf, SequencedRecipe<?>> STREAM_CODEC = StreamCodec.of(
			(b, v) -> v.writeToBuffer(b), SequencedRecipe::readFromBuffer
	);

	private final T wrapped;

	public SequencedRecipe(T wrapped) {
		this.wrapped = wrapped;
	}

	public IAssemblyRecipe getAsAssemblyRecipe() {
		return (IAssemblyRecipe) wrapped;
	}

	public T getRecipe() {
		return wrapped;
	}

	@SuppressWarnings("unchecked")
	void writeToBuffer(RegistryFriendlyByteBuf buffer) {
		AllRecipeTypes type = (AllRecipeTypes) wrapped.getTypeInfo();
		buffer.writeEnum(type);
		RecipeSerializer<T> serializer = type.getSerializer();
		serializer.streamCodec().encode(buffer, wrapped);
	}

	@SuppressWarnings("unchecked")
	static SequencedRecipe<?> readFromBuffer(RegistryFriendlyByteBuf buffer) {
		AllRecipeTypes type = buffer.readEnum(AllRecipeTypes.class);
		RecipeSerializer<ProcessingRecipe<?, ?>> serializer = type.getSerializer();
		return new SequencedRecipe<>(serializer.streamCodec().decode(buffer));
	}

	void initFromSequencedAssembly(SequencedAssemblyRecipe parent, boolean isFirst) {
		if (getAsAssemblyRecipe().supportsAssembly()) {
			Ingredient transit = Ingredient.of(parent.getTransitionalItem());
			wrapped.getIngredients()
					.set(0, isFirst ? DefaultCustomIngredients.any(transit, parent.getIngredient()) : transit);
		}
	}
}
