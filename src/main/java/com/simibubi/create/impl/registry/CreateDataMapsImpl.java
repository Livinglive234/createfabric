package com.simibubi.create.impl.registry;

import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import com.simibubi.create.Create;
import com.simibubi.create.api.data.datamaps.BlazeBurnerFuel;
import com.simibubi.create.api.registry.CreateDataMaps;
import com.simibubi.create.foundation.mixin.fabric.HoneycombDataMapAccessor;
import com.simibubi.create.foundation.mixin.fabric.WeatheringCopperDataMapAccessor;
import io.github.fabricators_of_create.porting_lib.resources.data_maps.DataMapType;
import io.github.fabricators_of_create.porting_lib.resources.data_maps.PortingLibDataMaps;
import io.github.fabricators_of_create.porting_lib.resources.events.DataMapsUpdatedEvent;
import io.github.fabricators_of_create.porting_lib.resources.injections.RegistryInjection;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Uses Porting Lib's registry data maps, including pack priority, tags, removal and synchronization. */
public class CreateDataMapsImpl {
    public static final DataMapType<Item, BlazeBurnerFuel> REGULAR = fuel("create", "regular_blaze_burner_fuels");
    public static final DataMapType<Item, BlazeBurnerFuel> SUPERHEATED = fuel("create", "superheated_blaze_burner_fuels");
    public static final DataMapType<Item, BlazeBurnerFuel> FURNACE = fuel("neoforge", "furnace_fuels");
    public static final DataMapType<Block, Block> OXIDIZABLES = blockMap("oxidizables", "next_oxidation_stage");
    public static final DataMapType<Block, Block> WAXABLES = blockMap("waxables", "waxed");
    private static final Map<Item, Integer> previousFuel = new HashMap<>();
    private static BiMap<Block, Block> baseOxidation, baseWaxing;
    private static volatile BiMap<Block, Block> oxidation, waxing;

    private static DataMapType<Item, BlazeBurnerFuel> fuel(String namespace, String path) {
        return DataMapType.builder(ResourceLocation.fromNamespaceAndPath(namespace, path), Registries.ITEM,
            BlazeBurnerFuel.CODEC).synced(BlazeBurnerFuel.CODEC, false).build();
    }

    private static DataMapType<Block, Block> blockMap(String path, String field) {
        Codec<Block> codec = BuiltInRegistries.BLOCK.byNameCodec().fieldOf(field).codec();
        return DataMapType.builder(ResourceLocation.fromNamespaceAndPath("neoforge", path), Registries.BLOCK,
            codec).synced(codec, false).build();
    }

    @SuppressWarnings("unchecked")
    private static <T, V> Map<ResourceKey<T>, V> values(Registry<T> registry, DataMapType<T, V> type) {
        return ((RegistryInjection<T>) (Object) registry).getDataMap(type);
    }

    public static BiMap<Block, Block> getOxidationMap() { return oxidation; }

    public static void register() {
        PortingLibDataMaps.registerDataMap(REGULAR);
        PortingLibDataMaps.registerDataMap(SUPERHEATED);
        PortingLibDataMaps.registerDataMap(FURNACE);
        PortingLibDataMaps.registerDataMap(OXIDIZABLES);
        PortingLibDataMaps.registerDataMap(WAXABLES);
        DataMapsUpdatedEvent.EVENT.register(event -> {
            event.ifRegistry(Registries.ITEM, registry -> {
                CreateDataMaps.REGULAR_BLAZE_BURNER_FUELS.clear();
                CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS.clear();
                values(registry, REGULAR).forEach((key, value) -> CreateDataMaps.REGULAR_BLAZE_BURNER_FUELS.put(registry.get(key), value));
                values(registry, SUPERHEATED).forEach((key, value) -> CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS.put(registry.get(key), value));
                previousFuel.forEach((item, burnTime) -> {
                    if (burnTime == null) FuelRegistry.INSTANCE.remove(item);
                    else FuelRegistry.INSTANCE.add(item, burnTime);
                });
                previousFuel.clear();
                values(registry, FURNACE).forEach((key, value) -> {
                    Item item = registry.get(key);
                    previousFuel.put(item, FuelRegistry.INSTANCE.get(item));
                    FuelRegistry.INSTANCE.add(item, Math.min(value.burnTime(), Short.MAX_VALUE));
                });
            });
            event.ifRegistry(Registries.BLOCK, registry -> {
                boolean install = baseOxidation == null;
                if (install) {
                    // Capture the completed vanilla/Fabric registrations on the first data load.
                    baseOxidation = HashBiMap.create(WeatheringCopperDataMapAccessor.create$getNextSupplier().get());
                    baseWaxing = HashBiMap.create(HoneycombDataMapAccessor.create$getWaxablesSupplier().get());

                }
                BiMap<Block, Block> next = HashBiMap.create(baseOxidation);
                values(registry, OXIDIZABLES).forEach((key, value) -> next.forcePut(registry.get(key), value));
                BiMap<Block, Block> waxed = HashBiMap.create(baseWaxing);
                values(registry, WAXABLES).forEach((key, value) -> waxed.forcePut(registry.get(key), value));
                oxidation = next;
                waxing = waxed;
                if (install) {
                    HoneycombDataMapAccessor.create$setWaxables(() -> waxing);
                    HoneycombDataMapAccessor.create$setWaxOff(() -> waxing.inverse());
                }
            });
        });
    }
}
