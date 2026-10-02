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
	// fabric: lazily computed to break a circular static-init dependency - constructing
	// AllRecipeTypes.SEQUENCED_ASSEMBLY (an enum constant, initialized before any static fields declared after
	// it, like AllRecipeTypes.CODEC) loads this class, which would otherwise dereference AllRecipeTypes.CODEC
	// while it's still null. This datafixerupper version has no Codec.lazyInitialized, so the delay is done by
	// hand via a memoizing holder instead.
	private static Codec<SequencedRecipe<?>> lazyCodec;

	private static Codec<SequencedRecipe<?>> lazyCodec() {
		Codec<SequencedRecipe<?>> result = lazyCodec;
		if (result == null) {
			result = AllRecipeTypes.CODEC
				.<ProcessingRecipe<?, ?>>dispatch(r -> (AllRecipeTypes) r.getTypeInfo(), AllRecipeTypes::processingCodec)
				.validate(r -> r instanceof IAssemblyRecipe ? DataResult.success(r) :
					DataResult.error(() -> r.getType() + " is not a supported recipe type"))
				.xmap(SequencedRecipe::new, SequencedRecipe::getRecipe);
			lazyCodec = result;
		}
		return result;
	}

	public static final Codec<SequencedRecipe<?>> CODEC = Codec.of(
		new com.mojang.serialization.Encoder<SequencedRecipe<?>>() {
			@Override
			public <T> DataResult<T> encode(SequencedRecipe<?> input, com.mojang.serialization.DynamicOps<T> ops, T prefix) {
				return lazyCodec().encode(input, ops, prefix);
			}
		},
		new com.mojang.serialization.Decoder<SequencedRecipe<?>>() {
			@Override
			public <T> DataResult<com.mojang.datafixers.util.Pair<SequencedRecipe<?>, T>> decode(com.mojang.serialization.DynamicOps<T> ops, T input) {
				return lazyCodec().decode(ops, input);
			}
		}
	);

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
