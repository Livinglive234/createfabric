package com.simibubi.create.foundation.mixin.fabric.infra;

import java.util.Locale;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.createmod.catnip.platform.FabricClientHooksHelper;
import net.minecraft.client.resources.language.LanguageManager;

/**
 * Ponder/Catnip's FabricClientHooksHelper#getCurrentLocale() calls
 * LanguageManager#getJavaLocale(), a method 1.21.1 no longer has (removed upstream, confirmed via
 * javap - LanguageManager now only exposes getSelected(), a raw language code like "en_us"). That
 * throws a NoSuchMethodError on every resource reload (caught asynchronously by Catnip's own
 * LangNumberFormat#update, called from its ClientResourceReloadListener), which cascades into
 * Minecraft's own reload-error handling logging "Caught error loading resourcepacks, removing all
 * selected resourcepacks" - a real functional bug, not just log noise: any resource pack the
 * player has selected gets silently dropped on every reload.
 *
 * Redirects the broken call to build an equivalent java.util.Locale from getSelected()'s code
 * instead (e.g. "en_us" -> Locale.forLanguageTag("en-us")), falling back to the JVM default locale
 * if that ever fails to parse.
 */
@Mixin(FabricClientHooksHelper.class)
public class FabricClientHooksHelperMixin {

	@Redirect(method = "getCurrentLocale", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/resources/language/LanguageManager;getJavaLocale()Ljava/util/Locale;"))
	private Locale create$getJavaLocale(LanguageManager languageManager) {
		try {
			return Locale.forLanguageTag(languageManager.getSelected().replace('_', '-'));
		} catch (Exception e) {
			return Locale.getDefault();
		}
	}
}
