package com.simibubi.create.foundation.mixin.fabric;

import java.util.function.Supplier;
import com.google.common.collect.BiMap;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.world.item.HoneycombItem.class)
public interface HoneycombDataMapAccessor {
    @Accessor("WAXABLES")
    static Supplier<BiMap<Block, Block>> create$getWaxablesSupplier() { throw new AssertionError(); }
    static BiMap<Block, Block> create$getWaxables() { return create$getWaxablesSupplier().get(); }
    @Mutable
    @Accessor("WAXABLES")
    static void create$setWaxables(Supplier<BiMap<Block, Block>> value) { throw new AssertionError(); }
    @Mutable
    @Accessor("WAX_OFF_BY_BLOCK")
    static void create$setWaxOff(Supplier<BiMap<Block, Block>> value) { throw new AssertionError(); }
}
