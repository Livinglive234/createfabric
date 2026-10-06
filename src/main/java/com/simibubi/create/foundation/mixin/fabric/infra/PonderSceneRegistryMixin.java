package com.simibubi.create.foundation.mixin.fabric.infra;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.foundation.ponder.FabricStructureProcessing;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PonderSceneRegistry.class)
public class PonderSceneRegistryMixin {
	@ModifyExpressionValue(method = "loadSchematic(Ljava/io/InputStream;)Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;",
		at = @At(value = "INVOKE", remap = true,
			target = "Lnet/minecraft/nbt/NbtIo;read(Ljava/io/DataInput;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/CompoundTag;"))
	private static CompoundTag create$normalizeTemplate(CompoundTag template) {
		return FabricStructureProcessing.normalizePonderTemplate(template);
	}
}
