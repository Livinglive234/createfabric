package com.simibubi.create.compat.dynamictrees;

import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import com.dtteam.dynamictrees.api.network.BranchDestructionData;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.branch.TrunkShellBlock;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.simibubi.create.foundation.utility.AbstractBlockBreakQueue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LevelEvent;

public class DynamicTree extends AbstractBlockBreakQueue {
	private BlockPos startCutPos;

	public DynamicTree(BlockPos startCutPos) {
		this.startCutPos = startCutPos;
	}

	public static boolean isDynamicBranch(Block block) {
		return TreeHelper.isBranch(block) || block instanceof TrunkShellBlock;
	}

	@Override
	public void destroyBlocks(Level world, ItemStack toDamage, @Nullable Player player,
		BiConsumer<BlockPos, ItemStack> drop) {
		BlockState state = world.getBlockState(startCutPos);
		BranchBlock branch = TreeHelper.getBranch(state);
		if (branch == null && state.getBlock() instanceof TrunkShellBlock shell) {
			TrunkShellBlock.ShellMuse muse = shell.getMuse(world, state, startCutPos);
			if (muse != null) {
				startCutPos = muse.pos();
				branch = TreeHelper.getBranch(muse.state());
			}
		}
		if (branch == null)
			return;
		world.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, startCutPos, Block.getId(world.getBlockState(startCutPos)));
		BranchDestructionData data = branch.destroyBranchFromNode(world, startCutPos, Direction.DOWN, false, player);
		data.leavesDrops.forEach(stackPos -> drop.accept(startCutPos.offset(stackPos.pos), stackPos.stack));
		data.species.getBranchesDrops(world, data.woodVolume, toDamage)
			.forEach(stack -> drop.accept(startCutPos, stack));
	}
}
