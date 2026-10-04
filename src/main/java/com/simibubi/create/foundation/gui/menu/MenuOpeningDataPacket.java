package com.simibubi.create.foundation.gui.menu;

import com.simibubi.create.AllPackets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * fabric: see {@link PendingMenuData} for why this exists. Carries opaque bytes (the same content a
 * menu's {@code createOnClient(RegistryFriendlyByteBuf)} already knows how to read) to the client ahead
 * of the vanilla menu-open packet.
 */
public record MenuOpeningDataPacket(byte[] data) implements ClientboundPacketPayload {

	public static final StreamCodec<ByteBuf, MenuOpeningDataPacket> STREAM_CODEC =
		ByteBufCodecs.BYTE_ARRAY.map(MenuOpeningDataPacket::new, MenuOpeningDataPacket::data);

	@Override
	@Environment(EnvType.CLIENT)
	public void handle(LocalPlayer player) {
		RegistryFriendlyByteBuf buf =
			new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), player.registryAccess());
		PendingMenuData.set(buf);
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.MENU_OPENING_DATA;
	}

}
