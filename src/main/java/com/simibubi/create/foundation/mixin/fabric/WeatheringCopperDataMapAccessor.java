package com.simibubi.create.foundation.mixin.fabric;

import java.util.function.Supplier;
import com.google.common.collect.BiMap;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.world.level.block.WeatheringCopper.class)
public interface WeatheringCopperDataMapAccessor {
    @Accessor("NEXT_BY_BLOCK")
    static Supplier<BiMap<Block, Block>> create$getNextSupplier() { throw new AssertionError(); }
}
