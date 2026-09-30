package com.simibubi.create.compat.trainmap;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.Mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

public class TrainMapEvents {

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(TrainMapEvents::tick);
		// TODO fabric-port: mouseClick/cancelTooltips/renderGui are not yet wired to
		// ScreenMouseEvents/tooltip-render/screen-render hooks
	}

	public static void tick(Minecraft mc) {
		if (mc.level == null)
			return;

		if (Mods.FTBCHUNKS.isLoaded())
			FTBChunksTrainMap.tick();
		if (Mods.JOURNEYMAP.isLoaded())
			JourneyTrainMap.tick();
		if (Mods.XAEROWORLDMAP.isLoaded())
			XaeroTrainMap.tick();
	}

	// TODO fabric-port: not yet wired up to ScreenMouseEvents.beforeMouseClick anywhere
	public static boolean mouseClick(Screen screen, double mouseX, double mouseY, int button) {
		if (Mods.FTBCHUNKS.isLoaded() && FTBChunksTrainMap.mouseClick(screen, (int) mouseX, (int) mouseY))
			return true;
		if (Mods.JOURNEYMAP.isLoaded() && JourneyTrainMap.mouseClick(screen, (int) mouseX, (int) mouseY))
			return true;
		if (Mods.XAEROWORLDMAP.isLoaded() && XaeroTrainMap.mouseClick(screen, (int) mouseX, (int) mouseY))
			return true;
		return false;
	}

	public static boolean cancelTooltips(ItemStack stack, PoseStack matrices, int x, int y, int width, int height, Font font, List<ClientTooltipComponent> tooltip) {
		if (Mods.FTBCHUNKS.isLoaded()) {
			return FTBChunksTrainMap.cancelTooltips();
		}

		return false;
	}

	public static void renderGui(Screen screen, GuiGraphics graphics, double mouseX, double mouseY, float partialTicks) {
		if (Mods.FTBCHUNKS.isLoaded()) {
			FTBChunksTrainMap.renderGui(screen, graphics, (int) mouseX, (int) mouseY, partialTicks);
		}
	}
}
