package com.simibubi.create.foundation.mixin.fabric.infra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.util.mod.Mod;

/**
 * Mod Menu's own update checker races its mod list: ModMenu.MODS is a plain HashMap, and
 * checkForUpdates() reads it (via Map.values().stream()...toList()) from a background executor
 * while the main thread is still populating it, throwing a ConcurrentModificationException caught
 * and logged on every boot of this (heavily-modded) instance. Reported upstream behavior, not
 * anything introduced by this port.
 *
 * Mixin's @Redirect on a constructor call requires the handler's declared return type to exactly
 * match the original (here, java.util.HashMap itself, not just Map), so a plain swap to
 * ConcurrentHashMap isn't type-compatible at the injection site. Instead, create$ThreadSafeHashMap
 * is a HashMap subclass (satisfying that exact-type requirement) that synchronizes every mutator
 * and returns snapshot copies from values()/keySet()/entrySet() - the actual race is a background
 * thread iterating one of those views while the main thread calls put(), so a stable copy taken
 * under the same lock as put() is what actually prevents the ConcurrentModificationException,
 * rather than merely changing the Map implementation.
 */
@Mixin(ModMenu.class)
public class ModMenuMixin {

	@Redirect(method = "<clinit>", at = @At(value = "NEW", target = "java/util/HashMap", ordinal = 0))
	private static HashMap<String, Mod> create$threadSafeMods() {
		return new Create$ThreadSafeHashMap<>();
	}

	private static final class Create$ThreadSafeHashMap<K, V> extends HashMap<K, V> {
		@Override
		public synchronized V put(K key, V value) {
			return super.put(key, value);
		}

		@Override
		public synchronized void putAll(Map<? extends K, ? extends V> m) {
			super.putAll(m);
		}

		@Override
		public synchronized V remove(Object key) {
			return super.remove(key);
		}

		@Override
		public synchronized void clear() {
			super.clear();
		}

		@Override
		public synchronized V get(Object key) {
			return super.get(key);
		}

		@Override
		public synchronized boolean containsKey(Object key) {
			return super.containsKey(key);
		}

		@Override
		public synchronized boolean containsValue(Object value) {
			return super.containsValue(value);
		}

		@Override
		public synchronized int size() {
			return super.size();
		}

		@Override
		public synchronized boolean isEmpty() {
			return super.isEmpty();
		}

		@Override
		public synchronized V putIfAbsent(K key, V value) {
			return super.putIfAbsent(key, value);
		}

		@Override
		public synchronized Collection<V> values() {
			return new ArrayList<>(super.values());
		}

		@Override
		public synchronized Set<K> keySet() {
			return new LinkedHashSet<>(super.keySet());
		}

		@Override
		public synchronized Set<Map.Entry<K, V>> entrySet() {
			return new LinkedHashSet<>(super.entrySet());
		}
	}
}
