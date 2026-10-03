package com.simibubi.create.foundation.recipe;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.Create;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;

public class BlockTagIngredient implements CustomIngredient {
	protected final TagKey<Block> tag;

	protected BlockTagIngredient(TagKey<Block> tag) {
		this.tag = tag;
	}

	public static BlockTagIngredient create(TagKey<Block> tag) {
		return new BlockTagIngredient(tag);
	}

	public TagKey<Block> getTag() {
		return tag;
	}

	@Override
	public boolean requiresTesting() {
		return false;
	}

	@Override
	public List<ItemStack> getMatchingStacks() {
		ImmutableList.Builder<ItemStack> stacks = ImmutableList.builder();
		for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
			stacks.add(new ItemStack(block.value().asItem()));
		}
		return stacks.build();
	}

	@Override
	public boolean test(ItemStack stack) {
		return Block.byItem(stack.getItem()).defaultBlockState().is(tag);
	}

	@Override
	public CustomIngredientSerializer<?> getSerializer() {
		return Serializer.INSTANCE;
	}

	public static class Serializer implements CustomIngredientSerializer<BlockTagIngredient> {
		public static final ResourceLocation ID = Create.asResource("block_tag_ingredient");
		public static final Serializer INSTANCE = new Serializer();

		private static final MapCodec<BlockTagIngredient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			TagKey.codec(Registries.BLOCK).fieldOf("tag").forGetter(BlockTagIngredient::getTag)
		).apply(i, BlockTagIngredient::new));

		private static final StreamCodec<RegistryFriendlyByteBuf, BlockTagIngredient> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, i -> i.tag.location(),
			(location) -> new BlockTagIngredient(TagKey.create(Registries.BLOCK, location))
		);

		@Override
		public ResourceLocation getIdentifier() {
			return ID;
		}

		@Override
		public MapCodec<BlockTagIngredient> getCodec(boolean allowEmpty) {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, BlockTagIngredient> getPacketCodec() {
			return STREAM_CODEC;
		}
	}
}
