package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.CreateClient;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.createmod.ponder.PonderClient;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.createmod.catnip.platform.CatnipServices;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class ClearBufferCacheCommand {
	static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("clearRenderBuffers")
			.executes(ctx -> {
				PonderClient.invalidateRenderers();
				CreateClient.invalidateRenderers();

				ctx.getSource().sendSuccess(() -> Component.literal("Cleared rendering buffers."), true);
				return Command.SINGLE_SUCCESS;
			});
	}
}
