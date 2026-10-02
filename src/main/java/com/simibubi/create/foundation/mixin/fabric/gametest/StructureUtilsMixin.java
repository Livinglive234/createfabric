package com.simibubi.create.foundation.mixin.fabric.gametest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.simibubi.create.foundation.utility.fabric.StructureBlockEntityExtensions;

import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.world.level.block.entity.StructureBlockEntity;

@Mixin(value = StructureUtils.class, priority = 900) // apply before FAPI, run earlier
public class StructureUtilsMixin {
	// TODO fabric: StructureUtils#getStructureTemplate(String, ServerLevel) - the method this mixin used to
	// redirect to the vanilla/forge structure-manager-based lookup instead of FAPI's resource-pack-based one -
	// no longer exists in 1.21.1; Mojang folded structure loading into prepareTestStructure/createStructureBlock
	// directly. Dropped the injection rather than guess at the new internal flow; gametest structure loading may
	// still use FAPI's resource-pack-based lookup as a result, but this only affects running gametests, not
	// normal gameplay.

	@ModifyReceiver(
		method = "createStructureBlock",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/entity/StructureBlockEntity;setIgnoreEntities(Z)V"
		)
	)
	private static StructureBlockEntity markGameTest(StructureBlockEntity instance, boolean ignoreEntities) {
		((StructureBlockEntityExtensions) instance).create$markGameTest();
		return instance;
	}
}
