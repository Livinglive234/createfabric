package com.simibubi.create.infrastructure.data;

import java.util.Set;
import java.nio.charset.StandardCharsets;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.concurrent.CompletableFuture;

import com.simibubi.create.AllDamageTypes;
import com.simibubi.create.AllEnchantments;
import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.equipment.potatoCannon.AllPotatoProjectileTypes;
import com.simibubi.create.infrastructure.worldgen.AllConfiguredFeatures;
import com.simibubi.create.infrastructure.worldgen.AllPlacedFeatures;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;

import io.github.fabricators_of_create.porting_lib.data.DatapackBuiltinEntriesProvider;
import net.minecraft.data.CachedOutput;

public class GeneratedEntriesProvider extends DatapackBuiltinEntriesProvider {
	public GeneratedEntriesProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, addBootstraps(new RegistrySetBuilder()), Set.of(Create.ID));
	}

	private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		return super.run((path, bytes, hash) -> {
			JsonElement json = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
			if (sortEffectCures(json)) {
				byte[] stable = JSON.toJson(json).getBytes(StandardCharsets.UTF_8);
				output.writeIfNeeded(path, stable, Hashing.sha1().hashBytes(stable));
			} else {
				output.writeIfNeeded(path, bytes, hash);
			}
		});
	}

	private static boolean sortEffectCures(JsonElement json) {
		boolean found = false;
		if (json.isJsonObject()) {
			for (var entry : json.getAsJsonObject().entrySet()) {
				if (entry.getKey().equals("porting_lib:cures") && entry.getValue().isJsonArray()) {
					// Porting Lib encodes this set in JVM-dependent iteration order.
					JsonArray sorted = new JsonArray();
					entry.getValue().getAsJsonArray().asList().stream()
						.map(JsonElement::getAsString).sorted().forEach(sorted::add);
					entry.setValue(sorted);
					found = true;
				} else {
					found |= sortEffectCures(entry.getValue());
				}
			}
		} else if (json.isJsonArray()) {
			for (JsonElement element : json.getAsJsonArray())
				found |= sortEffectCures(element);
		}
		return found;
	}

	// fabric: this must be reused in the entrypoint, moved to a method
	public static RegistrySetBuilder addBootstraps(RegistrySetBuilder builder) {
		return builder.add(Registries.ENCHANTMENT, AllEnchantments::bootstrap)
			.add(Registries.DAMAGE_TYPE, AllDamageTypes::bootstrap)
			.add(Registries.CONFIGURED_FEATURE, AllConfiguredFeatures::bootstrap)
			.add(Registries.PLACED_FEATURE, AllPlacedFeatures::bootstrap)
			.add(CreateRegistries.POTATO_PROJECTILE_TYPE, AllPotatoProjectileTypes::bootstrap);
		// fabric: biome modifiers are not a registry, remove
	}

	@Override
	public String getName() {
		return "Create's Generated Registry Entries";
	}
}
