package com.simibubi.create.content.contraptions.wrench;

import dev.engine_room.flywheel.api.visualization.VisualizationLevel;
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class NonVisualizationLevel extends WrappedLevel implements VisualizationLevel {
	public NonVisualizationLevel(Level level) {
		super(level);
	}

	@Override
	public boolean supportsVisualization() {
		return false;
	}

	// fabric: porting-lib version-skew diamond conflict between two "extensions" module
	// snapshotParticipant() default methods, see ContraptionWorld.java for details
	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant snapshotParticipant() {
		throw new UnsupportedOperationException();
	}

	// fabric: same version-skew diamond conflict, this time between LevelReaderInjection and
	// LevelReaderExtensions' isAreaLoaded(BlockPos, int) defaults - delegate to the wrapped level.
	@Override
	public boolean isAreaLoaded(BlockPos pos, int range) {
		return level.isAreaLoaded(pos, range);
	}
}
