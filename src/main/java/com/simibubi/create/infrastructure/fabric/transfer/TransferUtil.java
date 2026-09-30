package com.simibubi.create.infrastructure.fabric.transfer;

import com.simibubi.create.infrastructure.fabric.transfer.fluid.FluidStack;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Predicate;

public class TransferUtil {
	/**
	 * fabric: {@code Storage#exactView} was removed from the Transfer API; this replicates its old default-method
	 * behaviour by scanning the non-empty views for one whose resource matches exactly.
	 */
	@Nullable
	public static <T extends TransferVariant<?>> StorageView<T> exactView(Storage<T> storage, T resource) {
		for (StorageView<T> view : storage.nonEmptyViews()) {
			if (view.getResource().equals(resource)) {
				return view;
			}
		}
		return null;
	}

	public static long insert(Storage<FluidVariant> storage, FluidStack stack) {
		try (Transaction t = Transaction.openOuter()) {
			long inserted = insert(storage, stack, t);
			t.commit();
			return inserted;
		}
	}

	public static long insert(Storage<ItemVariant> storage, ItemStack stack) {
		try (Transaction t = Transaction.openOuter()) {
			long inserted = insert(storage, stack, t);
			t.commit();
			return inserted;
		}
	}

	public static long insert(Storage<FluidVariant> storage, FluidStack stack, TransactionContext ctx) {
		return storage.insert(stack.getVariant(), stack.getAmount(), ctx);
	}

	public static long insert(Storage<ItemVariant> storage, ItemStack stack, TransactionContext ctx) {
		return storage.insert(ItemVariant.of(stack), stack.getCount(), ctx);
	}

	@Nullable
	public static <T extends TransferVariant<?>> ResourceAmount<T> extractAny(Storage<T> storage, long maxAmount) {
		return commit(t -> StorageUtil.extractAny(storage, maxAmount, t));
	}

	@Nullable
	public static <T extends TransferVariant<?>> ResourceAmount<T> extractMatching(Storage<T> storage, Predicate<T> predicate, long maxAmount, TransactionContext ctx) {
		T resourceExtracting = null;
		long extracted = 0;

		for (StorageView<T> view : storage.nonEmptyViews()) {
			T resource = view.getResource();

			// see if a resource has already been chosen
			if (resourceExtracting != null && !resourceExtracting.equals(resource))
				continue;

			// if one hasn't, see if this one matches
			if (resourceExtracting == null && predicate.test(resource)) {
				resourceExtracting = resource;
			} else {
				// nope, skip
				continue;
			}

			extracted += view.extract(resource, maxAmount - extracted, ctx);
			if (extracted >= maxAmount) {
				return new ResourceAmount<>(resource, extracted);
			}
		}

		return resourceExtracting != null ? new ResourceAmount<>(resourceExtracting, extracted) : null;
	}

	@Nullable
	public static Storage<ItemVariant> getItemStorage(BlockEntity be) {
		return ItemStorage.SIDED.find(be.getLevel(), be.getBlockPos(), be.getBlockState(), be, null);
	}

	@Nullable
	public static Storage<ItemVariant> getItemStorage(Level level, BlockPos pos) {
		return ItemStorage.SIDED.find(level, pos, null);
	}

	@Nullable
	public static Storage<ItemVariant> getItemStorage(Level level, BlockPos pos, Direction side) {
		return ItemStorage.SIDED.find(level, pos, side);
	}

	@Nullable
	public static Storage<FluidVariant> getFluidStorage(Level level, BlockPos pos) {
		return FluidStorage.SIDED.find(level, pos, null);
	}

	@Nullable
	public static Storage<FluidVariant> getFluidStorage(Level level, BlockPos pos, Direction side) {
		return FluidStorage.SIDED.find(level, pos, side);
	}

	public static FluidStack firstOrEmpty(Storage<FluidVariant> storage) {
		for (StorageView<FluidVariant> view : storage.nonEmptyViews()) {
			return new FluidStack(view);
		}
		return FluidStack.EMPTY;
	}

	public static long totalCapacity(Storage<?> storage) {
		long total = 0;
		for (StorageView<?> view : storage)
			total += view.getCapacity();
		return total;
	}

	public static List<ItemStack> extractAllAsStacks(Storage<ItemVariant> storage) {
		List<ItemStack> stacks = new ArrayList<>();
		try (Transaction t = Transaction.openOuter()) {
			for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
				ItemVariant resource = view.getResource();
				long extracted = view.extract(resource, view.getAmount(), t);
				if (extracted > 0)
					stacks.add(resource.toStack((int) extracted));
			}
			t.commit();
		}
		return stacks;
	}

	public static OptionalLong firstCapacity(Storage<?> storage) {
		for (StorageView<?> view : storage) {
			return OptionalLong.of(view.getCapacity());
		}
		return OptionalLong.empty();
	}

	public static <T> void clear(Storage<T> storage) {
		try (Transaction t = Transaction.openOuter()) {
			for (StorageView<T> view : storage.nonEmptyViews()) {
				view.extract(view.getResource(), view.getAmount(), t);
			}
			t.commit();
		}
	}

	public static <T> T commit(Function<TransactionContext, T> function) {
		try (Transaction t = Transaction.openOuter()) {
			T value = function.apply(t);
			t.commit();
			return value;
		}
	}

	public static <T> T simulate(Function<TransactionContext, T> function) {
		try (Transaction t = Transaction.openOuter()) {
			return function.apply(t);
		}
	}

	public static ItemStack insertItemStacked(Storage<ItemVariant> storage, ItemStack stack, boolean simulate) {
		if (stack.isEmpty())
			return ItemStack.EMPTY;
		ItemVariant variant = ItemVariant.of(stack);
		long toInsert = stack.getCount();
		long inserted = simulate ? simulate(t -> storage.insert(variant, toInsert, t)) : commit(t -> storage.insert(variant, toInsert, t));
		long remaining = toInsert - inserted;
		return remaining <= 0 ? ItemStack.EMPTY : variant.toStack((int) remaining);
	}

	public static Optional<FluidStack> getFluidContained(ItemStack stack) {
		ContainerItemContext ctx = ContainerItemContext.withConstant(stack.copyWithCount(1));
		Storage<FluidVariant> storage = FluidStorage.ITEM.find(stack, ctx);
		if (storage == null)
			return Optional.empty();
		ResourceAmount<FluidVariant> extracted = extractAny(storage, Long.MAX_VALUE);
		return extracted == null ? Optional.empty() : Optional.of(FluidStack.of(extracted));
	}

	public static FluidStack extractAnyFluid(Storage<FluidVariant> storage, long maxAmount) {
		ResourceAmount<FluidVariant> extracted = extractAny(storage, maxAmount);
		return extracted == null ? FluidStack.EMPTY : FluidStack.of(extracted);
	}
}
