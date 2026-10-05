package com.simibubi.create.foundation.mixin.fabric;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Apply vanilla's missing/extra-table validation to Registrate's blocks, not all loaded mods. */
@Mixin(RegistrateBlockLootTables.class)
public abstract class RegistrateBlockLootTablesMixin extends VanillaBlockLoot {
	@Shadow(remap = false)
	@Final
	private AbstractRegistrate<?> parent;

	protected RegistrateBlockLootTablesMixin(HolderLookup.Provider registries) {
		super(registries);
	}

	@Shadow
	public abstract void generate();

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		generate();
		Set<ResourceKey<LootTable>> emitted = new HashSet<>();
		for (var entry : parent.getAll(Registries.BLOCK)) {
			Block block = (Block) entry.get();
			ResourceKey<LootTable> key = block.getLootTable();
			if (!block.isEnabled(enabledFeatures) || key.equals(BuiltInLootTables.EMPTY) || !emitted.add(key))
				continue;
			LootTable.Builder table = map.remove(key);
			if (table == null)
				throw new IllegalStateException(String.format(Locale.ROOT, "Missing loottable '%s' for '%s'",
					key.location(), BuiltInRegistries.BLOCK.getKey(block)));
			output.accept(key, table);
		}
		if (!map.isEmpty())
			throw new IllegalStateException("Created block loot tables for non-blocks: " + map.keySet());
	}
}
