package com.simibubi.create.foundation.data.recipe;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags.AllItemTags;

import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

import io.github.fabricators_of_create.porting_lib.tags.Tags;
import com.simibubi.create.AllTags;

public abstract class CreateRecipeProvider {

	public static void registerAllProcessing(FabricDataGenerator.Pack pack) {
		pack.addProvider((o, r) -> new CreateCrushingRecipeGen(o, r));
		pack.addProvider((o, r) -> new CreateMillingRecipeGen(o, r));
		pack.addProvider((o, r) -> new CreateCuttingRecipeGen(o, r));
		pack.addProvider((o, r) -> new CreateWashingRecipeGen(o, r));
		pack.addProvider((o, r) -> new CreatePolishingRecipeGen(o, r));
		pack.addProvider((o, r) -> new CreateDeployingRecipeGen(o, r));
		pack.addProvider((o, r) -> new CreateMixingRecipeGen(o, r));
		pack.addProvider((o, r) -> new CreateCompactingRecipeGen(o, r));
		pack.addProvider((o, r) -> new CreatePressingRecipeGen(o, r));
		pack.addProvider((o, r) -> new CreateFillingRecipeGen(o, r));
		pack.addProvider((o, r) -> new CreateEmptyingRecipeGen(o, r));
		pack.addProvider((o, r) -> new CreateHauntingRecipeGen(o, r));
		pack.addProvider((o, r) -> new CreateItemApplicationRecipeGen(o, r));
	}

	protected static class I {

		static TagKey<Item> redstone() {
			return Tags.Items.DUSTS_REDSTONE;
		}

		static TagKey<Item> planks() {
			return ItemTags.PLANKS;
		}

		static TagKey<Item> woodSlab() {
			return ItemTags.WOODEN_SLABS;
		}

		static TagKey<Item> gold() {
			return Tags.Items.INGOTS_GOLD;
		}

		static TagKey<Item> goldSheet() {
			return AllTags.commonItemTag("gold_plates");
		}

		static TagKey<Item> stone() {
			return Tags.Items.STONES;
		}

		static ItemLike andesiteAlloy() {
			return AllItems.ANDESITE_ALLOY.get();
		}

		static ItemLike shaft() {
			return AllBlocks.SHAFT.get();
		}

		static ItemLike cog() {
			return AllBlocks.COGWHEEL.get();
		}

		static ItemLike largeCog() {
			return AllBlocks.LARGE_COGWHEEL.get();
		}

		static ItemLike andesiteCasing() {
			return AllBlocks.ANDESITE_CASING.get();
		}

		static ItemLike vault() {
			return AllBlocks.ITEM_VAULT.get();
		}

		static ItemLike stockLink() {
			return AllBlocks.STOCK_LINK.get();
		}

		static TagKey<Item> brass() {
			return AllTags.commonItemTag("brass_ingots");
		}

		static TagKey<Item> brassSheet() {
			return AllTags.commonItemTag("brass_plates");
		}

		static TagKey<Item> iron() {
			return Tags.Items.INGOTS_IRON;
		}

		static TagKey<Item> ironNugget() {
			return Tags.Items.NUGGETS_IRON;
		}

		static TagKey<Item> zinc() {
			return AllTags.commonItemTag("zinc_ingots");
		}

		static TagKey<Item> ironSheet() {
			return AllTags.commonItemTag("iron_plates");
		}

		static TagKey<Item> sturdySheet() {
			return AllTags.commonItemTag("obsidian_plates");
		}

		static ItemLike brassCasing() {
			return AllBlocks.BRASS_CASING.get();
		}

		static ItemLike cardboard() {
			return AllItems.CARDBOARD.get();
		}

		static ItemLike railwayCasing() {
			return AllBlocks.RAILWAY_CASING.get();
		}

		static ItemLike electronTube() {
			return AllItems.ELECTRON_TUBE.get();
		}

		static ItemLike precisionMechanism() {
			return AllItems.PRECISION_MECHANISM.get();
		}

		static TagKey<Item> brassBlock() {
			return AllTags.commonItemTag("brass_blocks");
		}

		static TagKey<Item> zincBlock() {
			return AllTags.commonItemTag("zinc_blocks");
		}

		static TagKey<Item> wheatFlour() {
			return AllItemTags.WHEAT_FLOURS.tag;
		}

		static TagKey<Item> copper() {
			return Tags.Items.INGOTS_COPPER;
		}

		static TagKey<Item> copperNugget() {
			return AllTags.commonItemTag("copper_nuggets");
		}

		static TagKey<Item> copperBlock() {
			return Tags.Items.STORAGE_BLOCKS_COPPER;
		}

		static TagKey<Item> copperSheet() {
			return AllTags.commonItemTag("copper_plates");
		}

		static TagKey<Item> brassNugget() {
			return AllTags.commonItemTag("brass_nuggets");
		}

		static TagKey<Item> zincNugget() {
			return AllTags.commonItemTag("zinc_nuggets");
		}

		static ItemLike copperCasing() {
			return AllBlocks.COPPER_CASING.get();
		}

		static ItemLike refinedRadiance() {
			return AllItems.REFINED_RADIANCE.get();
		}

		static ItemLike shadowSteel() {
			return AllItems.SHADOW_STEEL.get();
		}

		static Ingredient netherite() {
			return Ingredient.of(Tags.Items.INGOTS_NETHERITE);
		}

	}
}
