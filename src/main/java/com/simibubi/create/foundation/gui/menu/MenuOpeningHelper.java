package com.simibubi.create.foundation.gui.menu;

import java.util.function.Consumer;

import io.netty.buffer.Unpooled;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

/**
 * fabric: see {@link PendingMenuData} for why this exists. Use this instead of
 * {@code player.openMenu(provider)} for any menu whose client-side reconstruction needs extra data
 * beyond the standard inventory sync (i.e. any menu with a {@code createOnClient(RegistryFriendlyByteBuf)}
 * that reads something).
 */
public class MenuOpeningHelper {

	public static void openWithData(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> dataWriter) {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
		dataWriter.accept(buf);
		byte[] bytes = new byte[buf.readableBytes()];
		buf.readBytes(bytes);
		CatnipServices.NETWORK.sendToClient(player, new MenuOpeningDataPacket(bytes));
		player.openMenu(provider);
	}

}
