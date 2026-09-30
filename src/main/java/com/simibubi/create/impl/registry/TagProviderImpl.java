package com.simibubi.create.impl.registry;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.registry.SimpleRegistry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.simibubi.create.Create;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

public class TagProviderImpl<K, V> implements SimpleRegistry.Provider<K, V> {
	private final TagKey<K> tag;
	private final Function<K, Holder<K>> holderGetter;
	private final V value;

	public TagProviderImpl(TagKey<K> tag, Function<K, Holder<K>> holderGetter, V value) {
		this.tag = tag;
		this.holderGetter = holderGetter;
		this.value = value;
	}

	@Override
	@Nullable
	public V get(K object) {
		Holder<K> holder = this.holderGetter.apply(object);
		return holder.is(this.tag) ? this.value : null;
	}

	@Override
	public void onRegister(Runnable invalidate) {
		net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA)
			.registerReloadListener(new SimpleSynchronousResourceReloadListener() {
				@Override
				public ResourceLocation getFabricId() {
					return Create.asResource("tag_provider_" + tag.location().toString().replace(':', '_').replace('/', '_'));
				}

				@Override
				public void onResourceManagerReload(ResourceManager manager) {
					invalidate.run();
				}
			});
	}

	// eye of the beholder? check the nametag, buddy
	public static Holder<BlockEntityType<?>> getBeHolder(BlockEntityType<?> type) {
		return BuiltInRegistries.BLOCK_ENTITY_TYPE.wrapAsHolder(type);
	}
}
