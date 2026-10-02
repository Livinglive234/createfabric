package com.simibubi.create.foundation.mixin.fabric.gametest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.foundation.ponder.FabricStructureProcessing;
import com.simibubi.create.foundation.utility.fabric.StructureBlockEntityExtensions;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

@Mixin(StructureBlockEntity.class)
public abstract class StructureBlockEntityMixin implements StructureBlockEntityExtensions {
	@Shadow
	public abstract String getStructureName();

	@Unique
	private boolean isGameTest;

	@Override
	public void create$markGameTest() {
		this.isGameTest = true;
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void saveIsGameTest(CompoundTag tag, CallbackInfo ci) {
		if (this.isGameTest) {
			NBTHelper.putMarker(tag, "create:is_game_test");
		}
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void loadIsGameTest(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, CallbackInfo ci) {
		this.isGameTest = tag.contains("create:is_game_test");
	}

	@ModifyArg(
		method = "loadStructure(Lnet/minecraft/server/level/ServerLevel;ZLnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;placeInWorld(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Lnet/minecraft/util/RandomSource;I)Z"
		)
	)
	private StructurePlaceSettings addProcessorToGameTests(StructurePlaceSettings settings) {
		if (this.isGameTest) {
			String name = this.getStructureName();
			ResourceLocation id = ResourceLocation.tryParse(name);
			if (id != null) {
				settings.addProcessor(new FabricStructureProcessing.Processor(id));
			}
		}

		return settings;
	}
}
