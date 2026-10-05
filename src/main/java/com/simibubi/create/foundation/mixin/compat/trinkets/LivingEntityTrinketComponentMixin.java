package com.simibubi.create.foundation.mixin.compat.trinkets;

import dev.emi.trinkets.api.LivingEntityTrinketComponent;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntityTrinketComponent.class, remap = false)
public abstract class LivingEntityTrinketComponentMixin {
	@Shadow
	public LivingEntity entity;

	@Inject(method = "update()V", at = @At("HEAD"), cancellable = true)
	private void create$deferSlotsWithoutLevel(CallbackInfo ci) {
		// Ponder constructs worldless entities while compiling scene translations.
		// Trinkets initializes this component in Entity's constructor, before those
		// dummy entities can have a level. Keep the initially empty slot state until
		// a level is available; ordinary entities continue through Trinkets unchanged.
		if (entity.level() == null)
			ci.cancel();
	}
}
