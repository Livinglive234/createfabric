package com.simibubi.create.infrastructure.data;

import java.util.concurrent.CompletableFuture;

import com.simibubi.create.AllContraptionTypes;
import com.simibubi.create.AllTags.AllContraptionTypeTags;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateRegistries;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;

import org.jetbrains.annotations.Nullable;

import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;

public class CreateContraptionTypeTagsProvider extends TagsProvider<ContraptionType> {
	public CreateContraptionTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
		super(output, CreateRegistries.CONTRAPTION_TYPE, lookupProvider);
	}

	@Override
	protected void addTags(Provider pProvider) {
		tag(AllContraptionTypeTags.OPENS_CONTROLS.tag)
			.add(AllContraptionTypes.CARRIAGE.key());
		tag(AllContraptionTypeTags.REQUIRES_VEHICLE_FOR_RENDER.tag)
			.add(AllContraptionTypes.MOUNTED.key());
	}

	@Override
	public String getName() {
		return "Create's Contraption Type Tags";
	}
}
