package com.simibubi.create.foundation.data;

import java.util.Collection;
import java.util.List;

import net.minecraft.world.item.crafting.Ingredient;

import net.minecraft.world.item.crafting.Ingredient.Value;

import org.jetbrains.annotations.ApiStatus.Internal;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.data.recipe.DatagenMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import com.simibubi.create.foundation.mixin.accessor.MappedRegistryAccessor;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

@Internal
public record SimpleDatagenIngredient(DatagenMod mod, String id) implements Ingredient.Value {
	// fabric: this codec is write-only by design (datagen emits recipe JSON referencing other mods'
	// items via it, and is never supposed to read any of it back - see getItems() below), but
	// Ingredient$ValueMixin wraps it in a Codec.either(..., original) so every real ingredient in the
	// game tries decoding through this codec first, falling back to vanilla's own ingredient codec on
	// failure. Decode must therefore always fail, unconditionally, with a graceful DataResult.error -
	// not just for namespaces Create's own Mods enum doesn't recognize. The original NeoForge version
	// threw a raw AssertionError for unrecognized namespaces specifically (succeeding otherwise), which
	// is doubly wrong here: an uncaught Throwable isn't something Codec.either can catch and fall
	// through on (crashes immediately for any namespace Create doesn't know, e.g. "betterendforge"),
	// AND for any namespace it DOES recognize (e.g. "farmersdelight", actually installed) it would
	// successfully decode and shadow an entirely ordinary vanilla {"item": "farmersdelight:xyz"}
	// ingredient - which looks identical on the wire - only to crash later anyway the first time
	// something calls getItems() on it. There is no namespace for which decoding through this codec is
	// ever actually correct, so it unconditionally fails instead of attempting a match.
	public static final MapCodec<SimpleDatagenIngredient> MAP_CODEC = ResourceLocation.CODEC.fieldOf("item")
		.flatXmap(
			location -> DataResult.error(() -> "SimpleDatagenIngredient is not meant for deserialization"),
			i -> DataResult.success(i.mod.asResource(i.id)));


	@Override
	public Collection<ItemStack> getItems() {
		throw new AssertionError("Only for datagen output");
	}

	public static Ingredient of(DatagenMod mod, String id) {
		return new Ingredient(Stream.of(new SimpleDatagenIngredient(mod, id)));
	}
}
