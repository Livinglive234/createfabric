package com.simibubi.create.foundation.mixin.accessor;

import java.util.List;

import net.minecraft.data.loot.LootTableProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootTableProvider.class)
public interface LootTableProviderAccessor {
	@Mutable
	@Accessor("subProviders")
	void create$setSubProviders(List<LootTableProvider.SubProviderEntry> providers);
}
