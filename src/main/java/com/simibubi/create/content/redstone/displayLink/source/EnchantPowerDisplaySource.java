package com.simibubi.create.content.redstone.displayLink.source;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

public class EnchantPowerDisplaySource extends NumericSingleLineDisplaySource {

	protected static final RandomSource random = RandomSource.create();
	protected static final ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);

	@Override
	protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
		BlockPos pos = context.getSourcePos();
		Level level = context.level();
		if (!(level.getBlockState(pos).getBlock() instanceof EnchantingTableBlock))
			return ZERO.copy();

		float enchantPower = 0;

		// TODO fabric: NeoForge's BlockState#getEnchantPowerBonus / porting-lib's EnchantmentBonusBlock have no
		// fabric port, so only plain bookshelves contribute bonus enchanting power here
		for(BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			if (!EnchantingTableBlock.isValidBookShelf(level, pos, offset))
				continue;
			BlockPos bookPos = pos.offset(offset);
			BlockState state = level.getBlockState(bookPos);
			enchantPower += state.is(Blocks.BOOKSHELF) ? 1 : 0;
		}


		int cost = EnchantmentHelper.getEnchantmentCost(random, 2, (int) enchantPower, stack);

		return Component.literal(String.valueOf(cost));
	}

	@Override
	protected String getTranslationKey() {
		return "max_enchant_level";
	}

	@Override
	protected boolean allowsLabeling(DisplayLinkContext context) {
		return true;
	}
}
