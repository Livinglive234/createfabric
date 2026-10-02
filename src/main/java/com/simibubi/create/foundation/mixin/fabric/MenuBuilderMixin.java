package com.simibubi.create.foundation.mixin.fabric;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.tterrag.registrate.builders.MenuBuilder;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * registrate-fabric's MenuBuilder#createEntry passes a literal {@code null} packet codec to
 * ExtendedScreenHandlerType's constructor (its own source even has a "FIXME: pass packet codec
 * here" comment), which throws immediately via Objects.requireNonNull. Create's own menus rely on
 * the NeoForge-style pattern of reading their extra constructor data straight off the raw
 * RegistryFriendlyByteBuf (see AllMenuTypes, which casts the factory's "data" argument back to
 * RegistryFriendlyByteBuf), so the fix here is a codec that passes the buffer through unchanged:
 * encode copies the pre-written opening-data buffer's bytes onto the outgoing packet, and decode
 * just hands back the incoming buffer so the menu's own constructor can keep reading from it.
 */
@Mixin(MenuBuilder.class)
public class MenuBuilderMixin {

	private static final StreamCodec<RegistryFriendlyByteBuf, RegistryFriendlyByteBuf> create$PASSTHROUGH_CODEC =
		StreamCodec.of(
			(buf, data) -> buf.writeBytes(data),
			buf -> buf
		);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Redirect(method = "createEntry", at = @At(value = "NEW", target = "net/fabricmc/fabric/api/screenhandler/v1/ExtendedScreenHandlerType"))
	private <T extends AbstractContainerMenu> ExtendedScreenHandlerType<T, ?> create$fixNullPacketCodec(
			ExtendedScreenHandlerType.ExtendedFactory<T, ?> factory, StreamCodec<? super RegistryFriendlyByteBuf, ?> codec) {
		return new ExtendedScreenHandlerType<>((ExtendedScreenHandlerType.ExtendedFactory) factory, create$PASSTHROUGH_CODEC);
	}
}
