package com.simibubi.create.compat.sodium;

import java.util.function.Function;

import com.simibubi.create.Create;
import com.simibubi.create.compat.Mods;

// fabric: net.caffeinemc.mods.sodium.api.texture.SpriteUtil (an interface with a static INSTANCE)
// is a newer public API only present starting some version after 0.6.7 - confirmed via the actual
// installed Sodium 0.6.7 jar, which only has the older static-method class below. That older class
// is still present even in newer Sodium versions (0.6.9, which this project compiles against, ships
// both), so using it directly maximizes compatibility across the Sodium versions actually in use
// instead of requiring whatever newer version first introduced the api.texture package.
import net.caffeinemc.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/**
 * Fixes the Mechanical Saw's sprite and Factory Gauge's sprite
 */
public class SodiumCompat {
	public static final ResourceLocation SAW_TEXTURE = Create.asResource("block/saw_reversed");
	public static final ResourceLocation FACTORY_PANEL_TEXTURE = Create.asResource("block/factory_panel_connections_animated");

	public static void init() {
		WorldRenderEvents.START.register(ctx -> {
			Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
			TextureAtlasSprite sawSprite = atlas.apply(SAW_TEXTURE);
			SpriteUtil.markSpriteActive(sawSprite);

			TextureAtlasSprite factoryPanelSprite = atlas.apply(FACTORY_PANEL_TEXTURE);
			SpriteUtil.markSpriteActive(factoryPanelSprite);
		});
	}
}
