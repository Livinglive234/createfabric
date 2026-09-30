package com.simibubi.create.infrastructure.command;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.CameraAngleAnimationService;
import com.simibubi.create.content.equipment.goggles.GoggleConfigScreen;
import com.simibubi.create.content.trains.CameraDistanceModifier;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class SimpleCreateActions {
	public static void camAngleTarget(String value, boolean yaw) {
		try {
			float v = Float.parseFloat(value);

			if (yaw) {
				CameraAngleAnimationService.setYawTarget(v);
			} else {
				CameraAngleAnimationService.setPitchTarget(v);
			}

		} catch (NumberFormatException ignored) {
			Create.LOGGER.debug("Received non-float value {} in camAngle packet, ignoring", value);
		}
	}
}
