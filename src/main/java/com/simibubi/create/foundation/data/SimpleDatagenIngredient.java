package com.simibubi.create.foundation.data;

import java.util.Collection;
import java.util.List;

import net.minecraft.world.item.crafting.Ingredient;

import net.minecraft.world.item.crafting.Ingredient.Value;

import org.jetbrains.annotations.ApiStatus.Internal;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.data.recipe.DatagenMod;
import com.simibubi.create.foundation.data.recipe.Mods;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import com.simibubi.create.foundation.mixin.accessor.MappedRegistryAccessor;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

@Internal
public record SimpleDatagenIngredient(DatagenMod mod, String id) implements Ingredient.Value {
	public static final MapCodec<SimpleDatagenIngredient> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) ->
		instance.group(ResourceLocation.CODEC.fieldOf("item").forGetter((i) -> i.mod.asResource(i.id)))
			.apply(instance, (location) -> {
				for (Mods mod : Mods.values()) {
					if (mod.getId().equals(location.getNamespace())) {
						return new SimpleDatagenIngredient(mod, location.getPath());
					}
				}
				throw new AssertionError("ID " + location.getNamespace() + " doesn't correspond to any compat mod." +
					" SimpleDatagenIngredient is not meant for deserialization anyway");
			}));


	@Override
	public Collection<ItemStack> getItems() {
		throw new AssertionError("Only for datagen output");
	}

	public static Ingredient of(DatagenMod mod, String id) {
		return new Ingredient(Stream.of(new SimpleDatagenIngredient(mod, id)));
	}
}
