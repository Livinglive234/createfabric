package com.simibubi.create.foundation.utility.fabric;

import com.simibubi.create.infrastructure.fabric.transfer.TransactionSuccessCallback;

import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * A {@link StorageView} wrapper that fires a listener whenever a transaction extracting from it
 * successfully commits.
 */
public class ListeningStorageView<T> implements StorageView<T> {
	private final StorageView<T> view;
	private final Runnable listener;

	public ListeningStorageView(StorageView<T> view, Runnable listener) {
		this.view = view;
		this.listener = listener;
	}

	@Override
	public long extract(T resource, long maxAmount, TransactionContext transaction) {
		long extracted = view.extract(resource, maxAmount, transaction);
		if (extracted > 0)
			TransactionSuccessCallback.register(transaction, listener);
		return extracted;
	}

	@Override
	public boolean isResourceBlank() {
		return view.isResourceBlank();
	}

	@Override
	public T getResource() {
		return view.getResource();
	}

	@Override
	public long getAmount() {
		return view.getAmount();
	}

	@Override
	public long getCapacity() {
		return view.getCapacity();
	}

	@Override
	public StorageView<T> getUnderlyingView() {
		return view.getUnderlyingView();
	}
}
