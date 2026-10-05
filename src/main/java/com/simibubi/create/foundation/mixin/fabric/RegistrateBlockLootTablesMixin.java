package com.simibubi.create.foundation.mixin.fabric;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Registrate's callbacks generate only its own blocks, not every loaded mod's blocks. */
@Mixin(RegistrateBlockLootTables.class)
public abstract class RegistrateBlockLootTablesMixin extends VanillaBlockLoot {
	@Shadow(remap = false)
	@Final
	private AbstractRegistrate<?> parent;

	protected RegistrateBlockLootTablesMixin(HolderLookup.Provider registries) {
		super(registries);
	}

	@Override
	protected Iterable<Block> getKnownBlocks() {
		return parent.getAll(Registries.BLOCK).stream().map(entry -> (Block) entry.get()).toList();
	}
}
