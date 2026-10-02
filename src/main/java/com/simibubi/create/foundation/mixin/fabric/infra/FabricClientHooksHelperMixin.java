package com.simibubi.create.foundation.mixin.fabric.infra;

import java.util.Locale;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.createmod.catnip.platform.FabricClientHooksHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageManager;

/**
 * Ponder/Catnip's FabricClientHooksHelper#getCurrentLocale() calls
 * LanguageManager#getJavaLocale(), a method 1.21.1 no longer has at all (removed upstream, confirmed
 * via javap - LanguageManager now only exposes getSelected(), a raw language code like "en_us").
 * That throws a NoSuchMethodError on every resource reload, cascading into Minecraft logging
 * "Caught error loading resourcepacks, removing all selected resourcepacks" - a real functional bug,
 * not just log noise.
 *
 * The first fix here used @Redirect targeting that getJavaLocale() call directly via @At(INVOKE).
 * That's the same category of bug BuiltInRegistriesMixin hit (see its own comment): since
 * getJavaLocale() doesn't exist anywhere in vanilla 1.21.1 under any name, there is no possible
 * remap target for that string at all, so it's left exactly as-written and can never match the real
 * (intermediary) bytecode - confirmed by an actual non-dev crash with the identical "Scanned 0
 * target(s). No refMap loaded." failure, this time for
 * "Redirector create$getJavaLocale(...)" in this class.
 *
 * Fixed the same way as BuiltInRegistriesMixin: inject at HEAD of getCurrentLocale() itself instead
 * (whose "method" selector needs no remapping at all, since FabricClientHooksHelper is a third-party
 * Ponder class, never subject to Minecraft's own mapping scheme) and compute the Locale entirely in
 * plain Java, with zero @At(INVOKE) string anywhere referencing the broken/nonexistent method.
 */
@Mixin(FabricClientHooksHelper.class)
public class FabricClientHooksHelperMixin {

	@Inject(method = "getCurrentLocale", at = @At("HEAD"), cancellable = true)
	private void create$getJavaLocale(CallbackInfoReturnable<Locale> cir) {
		LanguageManager languageManager = Minecraft.getInstance().getLanguageManager();
		try {
			cir.setReturnValue(Locale.forLanguageTag(languageManager.getSelected().replace('_', '-')));
		} catch (Exception e) {
			cir.setReturnValue(Locale.getDefault());
		}
	}
}
