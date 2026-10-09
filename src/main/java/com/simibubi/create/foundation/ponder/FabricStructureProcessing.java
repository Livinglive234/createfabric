package com.simibubi.create.foundation.ponder;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;


/**
 * Processing for structures exported on Forge to allow using the same ones on Forge and Fabric.
 */
public class FabricStructureProcessing {
	public static final MapCodec<Processor> PROCESSOR_CODEC = ResourceLocation.CODEC
			.fieldOf("structureId")
			.xmap(Processor::new, processor -> processor.structureId);

	public static final StructureProcessorType<Processor> PROCESSOR_TYPE = Registry.register(
			BuiltInRegistries.STRUCTURE_PROCESSOR,
			Create.asResource("fabric_structure_processor"),
			() -> PROCESSOR_CODEC
	);

	/**
	 * A predicate that makes all processes apply to all schematics.
	 */
	public static final ProcessingPredicate ALWAYS = (id, process) -> true;

	private static final Map<String, ProcessingPredicate> predicates = new HashMap<>();

	/**
	 * Register a {@link ProcessingPredicate} for a mod.
	 * Only one predicate may be registered for each mod.
	 * The predicate determines which {@link Process}es will be applied to which schematics.
	 */
	public static ProcessingPredicate register(String modId, ProcessingPredicate predicate) {
		ProcessingPredicate existing = predicates.get(modId);
		if (existing != null) {
			throw new IllegalStateException(
					"Tried to register ProcessingPredicate [%s] for mod '%s', while one already exists: [%s]"
							.formatted(predicate, modId, existing)
			);
		}
	    predicates.put(modId, predicate);
		return predicate;
	}

	@Internal
	public static void init() {
		register(Create.ID, ALWAYS);
	}

	public enum Process {
		FLUID_AMOUNTS
	}

	@FunctionalInterface
	public interface ProcessingPredicate {
		boolean shouldApplyProcess(ResourceLocation schematicId, Process process);
	}

	public static class Processor extends StructureProcessor {
		public final ResourceLocation structureId;

		public Processor(ResourceLocation structureId) {
			this.structureId = structureId;
		}

		@Nullable
		@Override
		public StructureTemplate.StructureBlockInfo processBlock(
				@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockPos pivot,
				@NotNull StructureBlockInfo blockInfo, @NotNull StructureBlockInfo relativeBlockInfo,
				@NotNull StructurePlaceSettings settings) {
			ProcessingPredicate predicate = predicates.get(structureId.getNamespace());
			if (predicate == null) // do nothing
				return relativeBlockInfo;

			CompoundTag nbt = relativeBlockInfo.nbt();
			if (nbt == null)
				return relativeBlockInfo;

			if (!predicate.shouldApplyProcess(structureId, Process.FLUID_AMOUNTS))
				return relativeBlockInfo;

			if (AllBlocks.FLUID_TANK.has(relativeBlockInfo.state()) && nbt.contains("TankContent", Tag.TAG_COMPOUND)) {
				CompoundTag copy = nbt.copy();
				fixTankContent(copy.getCompound("TankContent"));
				return new StructureBlockInfo(relativeBlockInfo.pos(), relativeBlockInfo.state(), copy);
			} else if (AllBlocks.BASIN.has(relativeBlockInfo.state())) {
				CompoundTag copy = nbt.copy();
				ListTag inputTanks = copy.getList("InputTanks", Tag.TAG_COMPOUND);
				if (!inputTanks.isEmpty()) {
					for (int i = 0; i < inputTanks.size(); i++) {
						CompoundTag compound = inputTanks.getCompound(i);
						CompoundTag content = compound.getCompound("TankContent");
						fixTankContent(content);
					}
				}
				ListTag outputTanks = copy.getList("OutputTanks", Tag.TAG_COMPOUND);
				if (!outputTanks.isEmpty()) {
					for (int i = 0; i < outputTanks.size(); i++) {
						CompoundTag compound = outputTanks.getCompound(i);
						CompoundTag content = compound.getCompound("TankContent");
						fixTankContent(content);
					}
				}

				return new StructureBlockInfo(relativeBlockInfo.pos(), relativeBlockInfo.state(), copy);
			}

			// no processes were applied
			return relativeBlockInfo;
		}

		@Override
		@NotNull
		protected StructureProcessorType<?> getType() {
			return PROCESSOR_TYPE;
		}
	}

	/** Convert only Create block entities in imported Ponder templates, not saved worlds. */
	public static CompoundTag normalizePonderTemplate(CompoundTag template) {
		CompoundTag copy = template.copy();
		ListTag blocks = copy.getList("blocks", Tag.TAG_COMPOUND);
		for (int i = 0; i < blocks.size(); i++) {
			CompoundTag block = blocks.getCompound(i);
			CompoundTag nbt = block.getCompound("nbt");
			if (nbt.getString("id").startsWith(Create.ID + ":"))
				block.put("nbt", normalizePonderCompound(nbt));
		}
		return copy;
	}

	private static boolean isEmptyItem(CompoundTag tag) {
		return tag.contains("id", Tag.TAG_STRING) && ("minecraft:air".equals(tag.getString("id"))
			|| (tag.contains("Count", Tag.TAG_ANY_NUMERIC) && tag.getLong("Count") <= 0)
			|| (tag.contains("count", Tag.TAG_ANY_NUMERIC) && tag.getLong("count") <= 0));
	}

	private static CompoundTag normalizePonderCompound(CompoundTag tag) {
		if (isEmptyItem(tag))
			return new CompoundTag();
		CompoundTag result = tag.copy();
		for (String key : tag.getAllKeys()) {
			Tag value = tag.get(key);
			if (value instanceof CompoundTag compound)
				result.put(key, normalizePonderCompound(compound));
			else if (value instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND) {
				ListTag normalized = new ListTag();
				for (int i = 0; i < list.size(); i++) {
					CompoundTag entry = list.getCompound(i);
					if (!isEmptyItem(entry))
						normalized.add(normalizePonderCompound(entry));
				}
				result.put(key, normalized);
			}
		}
		boolean legacy = tag.contains("FluidName", Tag.TAG_STRING) && tag.contains("Amount", Tag.TAG_ANY_NUMERIC);
		boolean neo = tag.contains("id", Tag.TAG_STRING) && tag.contains("amount", Tag.TAG_ANY_NUMERIC);
		if (!legacy && !neo)
			return result;
		String name = tag.getString(legacy ? "FluidName" : "id");
		long amount = tag.getLong(legacy ? "Amount" : "amount");
		if (amount <= 0 || "minecraft:empty".equals(name))
			return new CompoundTag();
		if ("minecraft:milk".equals(name) || "create:milk".equals(name))
			name = "milk:still_milk";
		CompoundTag variant = new CompoundTag();
		variant.putString("fluid", name);
		if (tag.contains("components", Tag.TAG_COMPOUND))
			variant.put("components", tag.getCompound("components").copy());
		result.put("id", variant);
		result.putLong("amount", ProcessingRecipeBuilder.millibuckets(amount));
		result.remove("FluidName");
		result.remove("Amount");
		result.remove("components");
		return result;
	}

	private static void fixTankContent(CompoundTag content) {
		if (content.contains("FluidName", Tag.TAG_STRING) && content.getString("FluidName").equals("minecraft:milk")) {
			content.putString("FluidName", "milk:still_milk");
		}
		// fabric: old schematics store the amount in the classic Forge milli-bucket unit (1000/bucket);
		// convert in place to this project's fabric-native bucket unit, keeping the rest of the tag as-is
		long amount = content.getInt("Amount");
		double buckets = amount / 1000d;
		long fixedAmount = Math.round(buckets * FluidConstants.BUCKET);
		content.putInt("Amount", (int) fixedAmount);
	}
}
