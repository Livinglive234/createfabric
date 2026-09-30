package com.simibubi.create.content.processing.sequenced;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import com.google.gson.JsonParseException;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SequencedRecipe<T extends ProcessingRecipe<?, ?>> {
	public static final Codec<SequencedRecipe<?>> CODEC = AllRecipeTypes.CODEC
		.<ProcessingRecipe<?, ?>>dispatch(ProcessingRecipe::getRecipeType, AllRecipeTypes::processingCodec)
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

	void initFromSequencedAssembly(SequencedAssemblyRecipe parent, boolean isFirst) {
		if (getAsAssemblyRecipe().supportsAssembly()) {
			Ingredient transit = Ingredient.of(parent.getTransitionalItem());
			wrapped.getIngredients()
					.set(0, isFirst ? CompoundIngredient.of(transit, parent.getIngredient()) : transit);
		}
	}
}
