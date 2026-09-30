package com.simibubi.create.foundation.item;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import com.simibubi.create.infrastructure.fabric.item.ItemUtils;
import com.simibubi.create.infrastructure.fabric.transfer.item.ItemStackHandler;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class SmartInventory extends ItemStackHandler {

	protected boolean extractionAllowed;
	protected boolean insertionAllowed;
	protected boolean stackNonStackables;
	protected int stackSize;

	private SyncedBlockEntity blockEntity;
	private Consumer<Integer> updateCallback;
	private BiPredicate<Integer, ItemStack> isValid = super::isItemValid;

	public SmartInventory(int slots, SyncedBlockEntity be) {
		this(slots, be, 64, false);
	}

	public SmartInventory(int slots, SyncedBlockEntity be, BiPredicate<Integer, ItemStack> isValid) {
		this(slots, be, 64, false);
		this.isValid = isValid;
	}

	public SmartInventory(int slots, SyncedBlockEntity be, int stackSize, boolean stackNonStackables) {
		super(slots);
		this.stackNonStackables = stackNonStackables;
		insertionAllowed = true;
		extractionAllowed = true;
		this.stackSize = stackSize;
		this.blockEntity = be;
	}

	public SmartInventory withMaxStackSize(int maxStackSize) {
		stackSize = maxStackSize;
		return this;
	}

	public SmartInventory whenContentsChanged(Consumer<Integer> updateCallback) {
		this.updateCallback = updateCallback;
		return this;
	}

	public SmartInventory allowInsertion() {
		insertionAllowed = true;
		return this;
	}

	public SmartInventory allowExtraction() {
		extractionAllowed = true;
		return this;
	}

	public SmartInventory forbidInsertion() {
		insertionAllowed = false;
		return this;
	}

	public SmartInventory forbidExtraction() {
		extractionAllowed = false;
		return this;
	}

	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		if (!insertionAllowed)
			return 0;
		return super.insert(resource, maxAmount, transaction);
	}

	@Override
	public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		if (!extractionAllowed)
			return 0;
		if (stackNonStackables) {
			try (Transaction t = transaction.openNested()) {
				long extracted = super.extract(resource, maxAmount, t);
				t.abort();
				int maxStackSize = ItemUtils.getMaxStackSize(resource);
				if (extracted != 0 && maxStackSize < extracted)
					maxAmount = maxStackSize;
			}
		}
		return super.extract(resource, maxAmount, transaction);
	}

	@Override
	protected void onContentsChanged(int slot) {
		super.onContentsChanged(slot);
		if (updateCallback != null)
			updateCallback.accept(slot);
		blockEntity.notifyUpdate();
	}

	@Override
	public int getSlotLimit(int slot) {
		return Math.min(stackNonStackables ? 64 : super.getSlotLimit(slot), stackSize);
	}

	@Override
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return isValid.test(slot, stack);
	}

	public int getStackLimit(int slot, @NotNull ItemStack stack) {
		return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider registries) {
		return super.serializeNBT(registries);
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt) {
		super.deserializeNBT(registries, nbt);
	}

}
