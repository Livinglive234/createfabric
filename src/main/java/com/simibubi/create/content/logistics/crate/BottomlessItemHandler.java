package com.simibubi.create.content.logistics.crate;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandlerSlot;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BottomlessItemHandler extends ItemStackHandler implements SingleSlotStorage<ItemVariant> { // must extend ItemStackHandler for mounted storages

	private Supplier<ItemStack> suppliedItemStack;

	public BottomlessItemHandler(Supplier<ItemStack> suppliedItemStack) {
		super(0);
		this.suppliedItemStack = suppliedItemStack;
		setSize(1); // create slot after setting supplier
	}

	@Override
	protected ItemStackHandlerSlot makeSlot(int index, ItemStack stack) {
		return new BottomlessSlot();
	}

	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		return maxAmount;
	}

	@Override
	public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		ItemStack stack = getStack();
		if (stack.isEmpty() || !resource.matches(stack))
			return 0;
		return Math.min(stack.getMaxStackSize(), maxAmount);
	}

	protected ItemStack getStack() {
		ItemStack stack = suppliedItemStack.get();
		if (stack == null)
			return ItemStack.EMPTY;
		if (!stack.isEmpty())
			return stack.copyWithCount(stack.getMaxStackSize());
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isResourceBlank() {
		return getStack().isEmpty();
	}

	@Override
	public ItemVariant getResource() {
		return ItemVariant.of(getStack());
	}

	@Override
	public long getAmount() {
		return Long.MAX_VALUE;
	}

	@Override
	public long getCapacity() {
		return Long.MAX_VALUE;
	}

	// shortcuts

	@Override
	public Iterator<StorageView<ItemVariant>> iterator() {
		return SingleSlotStorage.super.iterator(); // singleton iterator on this
	}

	@Override
	public Iterable<StorageView<ItemVariant>> nonEmptyViews() {
		return this::nonEmptyIterator;
	}

	@Override
	public Iterator<StorageView<ItemVariant>> nonEmptyIterator() {
		return isResourceBlank() ? Collections.emptyIterator() : iterator();
	}

	private class BottomlessSlot extends ItemStackHandlerSlot {
		public BottomlessSlot() {
			super(0, BottomlessItemHandler.this, ItemStack.EMPTY);
		}

		@Override
		public ItemStack getStack() {
			return BottomlessItemHandler.this.getStack();
		}

		@Override
		public ItemVariant getResource() {
			return BottomlessItemHandler.this.getResource();
		}

		@Override
		public long getAmount() {
			return BottomlessItemHandler.this.getAmount();
		}

		@Override
		protected void setStack(ItemStack stack) {
		}

		@Override
		@Nullable
		public Tag save(HolderLookup.Provider provider, Tag tag) {
			return null;
		}

		@Override
		protected void onFinalCommit() {
		}
	}
}
