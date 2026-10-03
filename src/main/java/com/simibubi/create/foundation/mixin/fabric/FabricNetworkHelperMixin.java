package com.simibubi.create.foundation.mixin.fabric;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.FabricNetworkHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Ponder-Fabric's FabricNetworkHelper#registerPackets unconditionally calls
 * ClientPlayNetworking.registerGlobalReceiver(...) for every clientbound packet, even though this
 * method is invoked from common init code (CatnipPacketRegistry#registerAllPackets, via
 * AllPackets#register) that runs on a dedicated server too. ClientPlayNetworking and its
 * PlayPayloadHandler functional interface are client-only Fabric API types - referencing them at
 * all (even just to call registerGlobalReceiver) forces the server to classload/verify a
 * client-only type, which Fabric Loader's environment stripper refuses, crashing the entire
 * server boot before Create.<clinit> can even finish (observed via a real runGametestServer launch).
 * <p>
 * Server-side, clientbound packets only need their payload type/codec registered (so the server
 * can serialize and send them) - never a client-side receiver, since the server never receives its
 * own S2C packets. This mixin replaces the whole loop on a dedicated server with a version that
 * registers both S2C and C2S payload types as before, but skips the client-only receiver
 * registration entirely - deliberately avoiding any reference to ClientPlayNetworking/
 * PlayPayloadHandler/LocalPlayer in this file so the mixin class itself loads safely on the server.
 */
@Mixin(FabricNetworkHelper.class)
public class FabricNetworkHelperMixin {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Inject(method = "registerPackets", at = @At("HEAD"), cancellable = true)
	private void create$skipClientOnlyReceiverOnDedicatedServer(CatnipPacketRegistry packetRegistry, CallbackInfo ci) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
			return;

		for (CatnipPacketRegistry.PacketType<?> type : packetRegistry.packetsView) {
			boolean clientbound = ClientboundPacketPayload.class.isAssignableFrom(type.clazz());
			boolean serverbound = ServerboundPacketPayload.class.isAssignableFrom(type.clazz());

			if (clientbound && serverbound) {
				throw new IllegalStateException("Packet class is both clientbound and serverbound: " + type.clazz());
			} else if (clientbound) {
				CatnipPacketRegistry.PacketType<ClientboundPacketPayload> casted =
					(CatnipPacketRegistry.PacketType<ClientboundPacketPayload>) type;
				PayloadTypeRegistry.playS2C()
					.register(casted.type(), casted.codec());
				// deliberately not calling ClientPlayNetworking.registerGlobalReceiver here - see class javadoc
			} else if (serverbound) {
				CatnipPacketRegistry.PacketType<ServerboundPacketPayload> casted =
					(CatnipPacketRegistry.PacketType<ServerboundPacketPayload>) type;
				PayloadTypeRegistry.playC2S()
					.register(casted.type(), casted.codec());
				ServerPlayNetworking.registerGlobalReceiver(casted.type(), (payload, ctx) -> payload.handle(ctx.player()));
			}
		}

		ci.cancel();
	}
}
