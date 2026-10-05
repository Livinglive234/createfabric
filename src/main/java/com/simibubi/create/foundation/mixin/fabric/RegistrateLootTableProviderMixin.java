package com.simibubi.create.foundation.mixin.fabric;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.simibubi.create.foundation.mixin.accessor.LootTableProviderAccessor;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registrate 1.3.77 seeds its superclass with vanilla loot generators, but never replaces them
 * with getTables(), so Create's registered loot callbacks never run. Install its actual generators
 * at execution time, after Registrate's root provider has finished construction.
 */
@Mixin(RegistrateLootTableProvider.class)
public abstract class RegistrateLootTableProviderMixin extends LootTableProvider {
	@Unique
	private FabricDataOutput create$output;

	protected RegistrateLootTableProviderMixin(PackOutput output, Set<net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>> requiredTables,
			List<SubProviderEntry> subProviders, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, requiredTables, subProviders, registries);
	}

	@Inject(method = "<init>", at = @At("TAIL"), remap = false)
	private void create$rememberOutput(AbstractRegistrate<?> parent, PackOutput output,
			CompletableFuture<HolderLookup.Provider> registries, CallbackInfo ci) {
		create$output = (FabricDataOutput) output;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		RegistrateLootTableProvider provider = (RegistrateLootTableProvider) (Object) this;
		((LootTableProviderAccessor) this).create$setSubProviders(provider.getTables(create$output));
		return super.run(cache);
	}
}
