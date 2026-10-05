package com.simibubi.create.foundation.mixin.fabric;

import java.util.Optional;
import com.simibubi.create.impl.registry.CreateDataMapsImpl;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WeatheringCopper.class)
public interface WeatheringCopperDataMapMixin {
    @Inject(method = "getNext(Lnet/minecraft/world/level/block/Block;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private static void create$dataMapNext(Block block, CallbackInfoReturnable<Optional<Block>> cir) {
        var map = CreateDataMapsImpl.getOxidationMap();
        if (map != null) cir.setReturnValue(Optional.ofNullable(map.get(block)));
    }

    @Inject(method = "getPrevious(Lnet/minecraft/world/level/block/Block;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private static void create$dataMapPrevious(Block block, CallbackInfoReturnable<Optional<Block>> cir) {
        var map = CreateDataMapsImpl.getOxidationMap();
        if (map != null) cir.setReturnValue(Optional.ofNullable(map.inverse().get(block)));
    }
}
