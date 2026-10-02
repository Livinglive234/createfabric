package com.simibubi.create.compat.trainmap;

import net.minecraft.client.gui.screens.Screen;

// TODO fabric: this file targeted JourneyMap's old `journeymap.api.v2.*` plugin API (and NeoForge's
// InputEvent.MouseButton.Pre for click handling) and was never finished for fabric. The journeymap-api
// dependency pinned in build.gradle.kts (info.journeymap:journeymap-api:1.20-1.9-SNAPSHOT) is for
// JourneyMap 5.x/MC 1.20 and only ships the old `journeymap.client.api.*` package; the resolved JourneyMap
// 6.x mod jar for 1.21.1 (maven.modrinth:journeymap:1.21.1-6.0.0-beta.39+fabric) has moved to a completely
// different `journeymap.api.*` plugin API and doesn't bundle a compile-time API jar at all. Properly wiring
// this up needs the correct journeymap-api coordinates/version for JourneyMap 6.x plus a real rewrite of the
// plugin registration and render/click hooks against that new API - stubbed out until that's done.
public class JourneyTrainMap {

	public static void tick() {
	}

	public static boolean mouseClick(Screen screen, int mouseX, int mouseY) {
		return false;
	}

}
