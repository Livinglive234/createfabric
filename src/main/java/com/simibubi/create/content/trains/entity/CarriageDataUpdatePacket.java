package com.simibubi.create.content.trains.entity;

import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public record CarriageDataUpdatePacket(int entityId, CarriageSyncData data) implements ClientboundPacketPayload {
	private static final StreamCodec<FriendlyByteBuf, CarriageSyncData> DATA_STREAM_CODEC =
		StreamCodec.of((buf, data) -> data.write(buf), CarriageSyncData::new);

	public static final StreamCodec<FriendlyByteBuf, CarriageDataUpdatePacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, CarriageDataUpdatePacket::entityId,
		DATA_STREAM_CODEC, CarriageDataUpdatePacket::data,
		CarriageDataUpdatePacket::new
	);

	public CarriageDataUpdatePacket(CarriageContraptionEntity entity) {
		this(entity.getId(), entity.carriageData);
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.CARRIAGE_DATA_UPDATE;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handle(LocalPlayer player) {
		Minecraft mc = Minecraft.getInstance();
		Entity entity = mc.level.getEntity(this.entityId);
		if (entity instanceof CarriageContraptionEntity carriage) {
			carriage.onCarriageDataUpdate(this.data);
		} else {
			Create.LOGGER.error("Invalid CarriageDataUpdatePacket for non-carriage entity: " + entity);
		}
	}
}
