package com.simibubi.create.foundation.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import com.mojang.datafixers.util.Either;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import com.simibubi.create.foundation.data.SimpleDatagenIngredient;

import net.minecraft.world.item.crafting.Ingredient;

import net.minecraft.world.item.crafting.Ingredient.Value;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Ingredient.Value.class)
public interface Ingredient$ValueMixin {
	// TODO fabric: NeoForge's DatagenModLoader.isRunningDataGen() has no fabric equivalent (fabric-api's
	// datagen "is running" state is only exposed through an internal impl class, not a public API) —
	// always wiring in the datagen-only codec shortcut unconditionally is harmless outside datagen since
	// the extra mapEither branch is a strict superset of what `original` alone accepts.
	@ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/MapCodec;xmap(Ljava/util/function/Function;Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;"))
	private static MapCodec<Value> create$recipeWithoutCompound(MapCodec<Value> original) {
		return Codec.mapEither(SimpleDatagenIngredient.MAP_CODEC, original).xmap((thing) -> thing.map(dv -> dv, v -> v), value -> {
			if (value instanceof SimpleDatagenIngredient datagenValue) {
				return Either.left(datagenValue);
			}
			return Either.right(value);
		});
	}
}
