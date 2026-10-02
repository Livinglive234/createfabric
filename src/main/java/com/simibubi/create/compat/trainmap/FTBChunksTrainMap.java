package com.simibubi.create.compat.trainmap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

// TODO fabric: no working FTB Chunks/FTB Library fabric port could be resolved (see build.gradle.kts, the
// dev.ftb.mods:*-fabric coordinates are commented out there) - this integration is stubbed out until a real
// fabric artifact is available. Everything here is gated behind Mods.FTBCHUNKS checks at the call sites
// (TrainMapEvents.java), so these no-ops are never actually reached.
public class FTBChunksTrainMap {

	public static void tick() {
	}

	public static boolean cancelTooltips() {
		return false;
	}

	public static boolean mouseClick(Screen screen, int mouseX, int mouseY) {
		return false;
	}

	public static void renderGui(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
	}

}
