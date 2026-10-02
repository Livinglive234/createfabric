package com.simibubi.create.infrastructure.data;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.compat.archEx.ArchExCompat;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.data.DamageTypeTagGen;
import com.simibubi.create.foundation.data.recipe.CreateMechanicalCraftingRecipeGen;
import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider;
import com.simibubi.create.foundation.data.recipe.CreateSequencedAssemblyRecipeGen;
import com.simibubi.create.foundation.data.recipe.CreateStandardRecipeGen;
import com.simibubi.create.foundation.ponder.CreatePonderPlugin;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;

import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import com.simibubi.create.foundation.data.TagLangGen;
import com.tterrag.registrate.providers.RegistrateDataProvider;

public class CreateDatagen implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		ExistingFileHelper helper = ExistingFileHelper.withResourcesFromArg();
		FabricDataGenerator.Pack pack = generator.createPack();
		// fabric: must run before setupDatagen() - that call constructs Registrate's root data
		// generator, after which further addDataGenerator() calls (as addExtraRegistrateData() makes)
		// throw "Cannot add data generator after construction of root generator". NeoForge avoids this
		// naturally via its separate gatherDataHighPriority/gatherData event priorities; Fabric's single
		// entrypoint method has to replicate that ordering explicitly.
		addExtraRegistrateData();
		// fabric: tag lang - also calls addDataGenerator(), so it has to run before setupDatagen() too
		TagLangGen.datagen();
		// fabric: archex compat - its LangProvider also lazily calls addDataGenerator() via
		// Registrate#addRawLang(), so it has to run before setupDatagen() too
		ArchExCompat.init(pack);
		Create.registrate().setupDatagen(pack, helper);
		gatherData(generator, pack, helper);
	}

	public static void gatherData(FabricDataGenerator generator, FabricDataGenerator.Pack pack, ExistingFileHelper existingFileHelper) {
		pack.addProvider((output, registries) -> new CreateRecipeSerializerTagsProvider(output, registries));
		pack.addProvider((output, registries) -> new CreateContraptionTypeTagsProvider(output, registries, existingFileHelper));
		pack.addProvider((output, registries) -> new CreateMountedItemStorageTypeTagsProvider(output, registries, existingFileHelper));
		pack.addProvider(DamageTypeTagGen::new);
		pack.addProvider(AllAdvancements::new);
		pack.addProvider(CreateStandardRecipeGen::new);
		pack.addProvider(CreateMechanicalCraftingRecipeGen::new);
		pack.addProvider(CreateSequencedAssemblyRecipeGen::new);
		// TODO fabric: NeoForge's data-map system (oxidizable/waxable copper) has no fabric port
		pack.addProvider(VanillaHatOffsetGenerator::new);
		pack.addProvider((output, registries) -> new CreateEnchantmentTagsProvider(output, registries, existingFileHelper));
		pack.addProvider((FabricDataGenerator.Pack.Factory<CreateWikiBlockInfoProvider>) CreateWikiBlockInfoProvider::new);

		CreateRecipeProvider.registerAllProcessing(pack);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		GeneratedEntriesProvider.addBootstraps(registryBuilder);
	}

	private static void addExtraRegistrateData() {
		CreateRegistrateTags.addGenerators();

		Create.registrate().addDataGenerator(ProviderType.LANG, provider -> {
			BiConsumer<String, String> langConsumer = provider::add;

			provideDefaultLang("interface", langConsumer);
			provideDefaultLang("tooltips", langConsumer);
			AllAdvancements.provideLang(langConsumer);
			AllSoundEvents.provideLang(langConsumer);
			AllKeys.provideLang(langConsumer);
			providePonderLang(langConsumer);
			new TagLangGenerator(langConsumer).generate();
		});
	}

	private static void provideDefaultLang(String fileName, BiConsumer<String, String> consumer) {
		String path = "assets/create/lang/default/" + fileName + ".json";
		JsonElement jsonElement = FilesHelper.loadJsonResource(path);
		if (jsonElement == null) {
			throw new IllegalStateException(String.format("Could not find default lang file: %s", path));
		}
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue().getAsString();
			consumer.accept(key, value);
		}
	}

	private static void providePonderLang(BiConsumer<String, String> consumer) {
		// Register this since FMLClientSetupEvent does not run during datagen
		PonderIndex.addPlugin(new CreatePonderPlugin());

		PonderIndex.getLangAccess().provideLang(Create.ID, consumer);
	}
}
