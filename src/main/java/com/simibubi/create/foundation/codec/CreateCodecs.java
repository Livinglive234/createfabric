package com.simibubi.create.foundation.codec;

import java.util.function.Function;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.item.ItemSlots;
import com.simibubi.create.infrastructure.fabric.transfer.item.ItemStackHandler;

public class CreateCodecs {
	public static final Codec<Long> NON_NEGATIVE_LONG = Codec.LONG.validate(
		value -> value < 0 ? DataResult.error(() -> "Value is negative: " + value) : DataResult.success(value)
	);

	public static final Codec<Integer> INT_STR = Codec.STRING.comapFlatMap(
		string -> {
			try {
				return DataResult.success(Integer.parseInt(string));
			} catch (NumberFormatException ignored) {
				return DataResult.error(() -> "Not an integer: " + string);
			}
		},
		String::valueOf
	);

	public static final Codec<ItemStackHandler> ITEM_STACK_HANDLER = Codec.lazyInitialized(() -> ItemSlots.CODEC.xmap(
		slots -> slots.toHandler(ItemStackHandler::new), ItemSlots::fromHandler
	));

	public static Codec<Integer> boundedIntStr(int min) {
		return INT_STR.validate(i -> i >= min ? DataResult.success(i) : DataResult.error(() -> "Value under minimum of " + min));
	}

	public static final Codec<Double> NON_NEGATIVE_DOUBLE = doubleRangeWithMessage(0, Double.MAX_VALUE,
		i -> "Value must be non-negative: " + i);
	public static final Codec<Double> POSITIVE_DOUBLE = doubleRangeWithMessage(1, Double.MAX_VALUE,
		i -> "Value must be positive: " + i);

	private static Codec<Double> doubleRangeWithMessage(double min, double max, Function<Double, String> errorMessage) {
		return Codec.DOUBLE.validate(i ->
			i.compareTo(min) >= 0 && i.compareTo(max) <= 0 ? DataResult.success(i) : DataResult.error(() ->
				errorMessage.apply(i)
			)
		);
	}

	// Older integrations encode sized fluid tags with NeoForge's millibucket schema.
	private static final Codec<FluidIngredient> LEGACY_FLUID_TAG = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.validate(type -> type.equals("neoforge:tag") || type.equals("forge:tag")
			? DataResult.success(type) : DataResult.error(() -> "Not a legacy fluid tag"))
			.fieldOf("type").forGetter(ingredient -> "neoforge:tag"),
		TagKey.codec(Registries.FLUID).fieldOf("tag").forGetter(ingredient -> { throw new UnsupportedOperationException(); }),
		NON_NEGATIVE_LONG.fieldOf("amount").forGetter(FluidIngredient::getRequiredAmount)
	).apply(instance, (type, tag, amount) ->
		FluidIngredient.fromTag(tag, Math.multiplyExact(amount, FluidConstants.BUCKET / 1000))));

	public static final Codec<FluidIngredient> SIZED_FLUID_INGREDIENT =
		Codec.either(FluidIngredient.CODEC, LEGACY_FLUID_TAG)
			.xmap(value -> value.map(Function.identity(), Function.identity()), Either::left);
}
