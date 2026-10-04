package com.simibubi.create.foundation.gui.menu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * fabric: Registrate-fabric's {@code ExtendedScreenHandlerType} wiring for menus that need extra
 * server->client opening data is broken (confirmed by live testing - even the menus previously believed
 * to correctly implement {@code ExtendedScreenHandlerFactory} disconnect the client with "found N bytes
 * extra" when opened, since the packet codec Registrate stores for them doesn't actually round-trip).
 * <p>
 * Instead of relying on that, {@link MenuOpeningHelper} sends the opening data as a plain, ordinary
 * server-to-client packet ({@link MenuOpeningDataPacket}) immediately before calling
 * {@code player.openMenu(...)}. Packets on one connection are always processed in the order they were
 * sent, so this data is guaranteed to arrive and be stashed here before the vanilla
 * "open screen" packet triggers the client to actually construct the {@link MenuBase} subclass - whose
 * plain (type, id, inv) constructor consumes it synchronously, on the same (client main) thread, with no
 * window where the menu exists without its data.
 */
public class PendingMenuData {

	@Environment(EnvType.CLIENT)
	private static RegistryFriendlyByteBuf pending;

	@Environment(EnvType.CLIENT)
	public static void set(RegistryFriendlyByteBuf buf) {
		pending = buf;
	}

	@Environment(EnvType.CLIENT)
	public static RegistryFriendlyByteBuf consume() {
		RegistryFriendlyByteBuf buf = pending;
		pending = null;
		return buf;
	}

}
