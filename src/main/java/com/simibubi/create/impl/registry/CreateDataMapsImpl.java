package com.simibubi.create.impl.registry;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.Create;
import com.simibubi.create.api.data.datamaps.BlazeBurnerFuel;
import com.simibubi.create.api.registry.CreateDataMaps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

/**
 * fabric: loads {@code data/<namespace>/data_maps/item/*.json} files in the same shape NeoForge's data maps use,
 * since that generic registry-attached-data system isn't available on Fabric.
 */
public class CreateDataMapsImpl implements SimpleSynchronousResourceReloadListener {
	public static final ResourceLocation ID = Create.asResource("data_maps");

	@Override
	public ResourceLocation getFabricId() {
		return ID;
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		CreateDataMaps.REGULAR_BLAZE_BURNER_FUELS.clear();
		CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS.clear();
		load(manager, "regular_blaze_burner_fuels", CreateDataMaps.REGULAR_BLAZE_BURNER_FUELS);
		load(manager, "superheated_blaze_burner_fuels", CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS);
	}

	private static void load(ResourceManager manager, String path, Map<Item, BlazeBurnerFuel> target) {
		for (Map.Entry<ResourceLocation, Resource> entry : manager
			.listResources("data_maps/item", loc -> loc.getPath().endsWith("/" + path + ".json"))
			.entrySet()) {
			try (var reader = entry.getValue().openAsReader()) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				JsonObject values = json.getAsJsonObject("values");
				if (values == null)
					continue;
				for (Map.Entry<String, JsonElement> value : values.entrySet()) {
					Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(value.getKey()));
					BlazeBurnerFuel.CODEC.parse(JsonOps.INSTANCE, value.getValue())
						.result()
						.ifPresent(fuel -> target.put(item, fuel));
				}
			} catch (Exception e) {
				Create.LOGGER.error("Couldn't load data map {}", entry.getKey(), e);
			}
		}
	}
}
