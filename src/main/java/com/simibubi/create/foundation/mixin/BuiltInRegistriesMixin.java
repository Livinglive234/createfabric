package com.simibubi.create.foundation.mixin;

import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;

import net.minecraft.Util;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * The previous approach here (@WrapOperation on Registry.forEach, called from "validate") crashed
 * on every real (non-dev) launch: Registry doesn't declare forEach itself - it's inherited all the
 * way from java.lang.Iterable (Registry -> IdMap -> Iterable) - and whatever remaps this project's
 * mixin annotations at build time can't resolve an @At(INVOKE) reference through that interface
 * hierarchy, so it was left in named-mapping form and could never match the real (intermediary)
 * bytecode at runtime. Retargeting @At to Iterable's own forEach directly did NOT fix it either
 * (confirmed against an actual non-dev launch) - Mixin's injector apparently won't match an
 * @At(INVOKE) whose specified owner sits higher in the interface hierarchy than the real
 * instruction's compiled owner (Registry).
 *
 * Sidestepped entirely by not using any @At(INVOKE) string for the forEach call at all: @Inject at
 * HEAD only needs the "method" selector ("validate") to remap, which already works correctly (the
 * crash log's own disassembly confirmed this), and the loop body below is just plain compiled Java -
 * resolved and remapped the completely ordinary way every other method call in this entire mod is,
 * with none of Mixin's string-based @At remapping involved. The loop body reproduces validate()'s
 * original per-entry logic exactly (confirmed via javap/disassembly of the real method and its
 * messages' invokedynamic constants: "Registry '<key>' was empty after loading" and "Missing default
 * of DefaultedMappedRegistry: <key>"), skipping Create's own registries from the checks exactly like
 * the original WrapOperation callback did.
 */
@Mixin(BuiltInRegistries.class)
public class BuiltInRegistriesMixin {
	static {
		CreateBuiltInRegistries.init();
	}

	@Inject(method = "validate", at = @At("HEAD"), cancellable = true)
	private static <T extends Registry<?>> void create$ourRegistriesAreNotEmpty(Registry<T> registry, CallbackInfo ci) {
		for (T t : registry) {
			if (t.key().location().getNamespace().equals(Create.ID))
				continue;

			if (t.keySet().isEmpty())
				Util.logAndPauseIfInIde("Registry '" + registry.getKey(t) + "' was empty after loading");

			if (t instanceof DefaultedRegistry<?> defaulted) {
				ResourceLocation defaultKey = defaulted.getDefaultKey();
				Validate.notNull(t.get(defaultKey), "Missing default of DefaultedMappedRegistry: " + defaultKey);
			}
		}
		ci.cancel();
	}
}
