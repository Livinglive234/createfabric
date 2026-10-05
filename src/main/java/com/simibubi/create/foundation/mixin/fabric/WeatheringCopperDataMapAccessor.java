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
    static BiMap<Block, Block> create$getNext() { return create$getNextSupplier().get(); }
    @Mutable
    @Accessor("NEXT_BY_BLOCK")
    static void create$setNext(Supplier<BiMap<Block, Block>> value) { throw new AssertionError(); }
    @Mutable
    @Accessor("PREVIOUS_BY_BLOCK")
    static void create$setPrevious(Supplier<BiMap<Block, Block>> value) { throw new AssertionError(); }
}
