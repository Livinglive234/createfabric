package com.simibubi.create.foundation.mixin.fabric.infra;

import org.spongepowered.asm.mixin.Mixin;

import io.github.fabricators_of_create.porting_lib.extensions.extensions.BlockStateExtensions;

import net.minecraft.world.level.block.state.BlockState;

// fabric: porting_lib's "extensions" module (pinned to beta.54, since it never got a newer 1.21.1
// release - see build.gradle.kts) ships BlockStateExtensions with a default #onTreeGrow(...), and its
// own TrunkPlacerMixin unconditionally calls BlockState#onTreeGrow(...) expecting BlockState to
// implement that interface - but the mixin that was supposed to apply BlockStateExtensions to
// BlockState itself is missing from that release entirely (confirmed: no BlockStateMixin.class exists
// in the jar at all, despite the interface being present and referenced). This caused a
// NoSuchMethodError on BlockState#onTreeGrow during world generation (tree placement), crashing world
// creation outright. Applying the already-shipped interface here (mirroring how porting_lib's own
// BlockMixin applies BlockExtensions to Block) fixes it without needing a patched dependency jar.
@Mixin(BlockState.class)
public class BlockStateExtensionsMixin implements BlockStateExtensions {
}
