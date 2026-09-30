package io.github.fabricators_of_create.porting_lib.core.util;

import io.github.fabricators_of_create.porting_lib.core.PortingLib;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

/**
 * Polyfill for a class missing from the published porting-lib "core" module at the version this
 * project is pinned to (3.1.0-beta.91+1.21.1): {@code entity} and {@code items} modules at the same
 * version compile their {@code Entity}/{@code ItemStack} mixins against this interface (confirmed via
 * {@code javap} on the merged Minecraft jar, which lists
 * {@code io.github.fabricators_of_create.porting_lib.core.util.DefaultNbtSerializable} as an
 * implemented interface of both), but the "core" module jar itself does not ship the class file.
 * Reconstructed to match the real interface's shape (from an older "core" module build,
 * 3.1.0-beta.47+1.21.1, where it still shipped) — a marker interface whose default methods are always
 * overridden by the actual mixin injected into Entity/ItemStack, so the throwing bodies here are never
 * actually invoked at runtime.
 */
public interface DefaultNbtSerializable<T extends Tag> extends INBTSerializable<T> {
	@Override
	default T serializeNBT(HolderLookup.Provider provider) {
		throw PortingLib.createMixinException("NBT serialization");
	}

	@Override
	default void deserializeNBT(HolderLookup.Provider provider, T nbt) {
		throw PortingLib.createMixinException("NBT serialization");
	}
}
