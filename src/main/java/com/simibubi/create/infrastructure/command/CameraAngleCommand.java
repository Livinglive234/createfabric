package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.simibubi.create.foundation.utility.CameraAngleAnimationService;
import com.simibubi.create.foundation.utility.CameraAngleAnimationService.Mode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import com.mojang.brigadier.context.CommandContext;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.server.level.ServerPlayer;

public class CameraAngleCommand {
	private static final DynamicCommandExceptionType INVALID_MODE = new DynamicCommandExceptionType(
		arg -> Component.literal("Unknown camera angle mode: " + arg));

	private static Mode getMode(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		String name = StringArgumentType.getString(ctx, "mode");
		try {
			return Mode.valueOf(name.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw INVALID_MODE.create(name);
		}
	}

	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("angle")
			.requires(cs -> cs.hasPermission(2))
			.then(Commands.argument("players", EntityArgument.players())
				.then(Commands.literal("yaw")
					.then(Commands.argument("degrees", FloatArgumentType.floatArg())
						.executes(ctx -> {
							float angleTarget = FloatArgumentType.getFloat(ctx, "degrees");
							CameraAngleAnimationService.setYawTarget(angleTarget);

							return Command.SINGLE_SUCCESS;
						})
					)
				).then(Commands.literal("pitch")
					.then(Commands.argument("degrees", FloatArgumentType.floatArg())
						.executes(ctx -> {
							float angleTarget = FloatArgumentType.getFloat(ctx, "degrees");
							CameraAngleAnimationService.setPitchTarget(angleTarget);

							return Command.SINGLE_SUCCESS;
						})
					)
				).then(Commands.literal("mode")
					.then(Commands.argument("mode", StringArgumentType.word())
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
							java.util.Arrays.stream(Mode.values()).map(Mode::name), builder))
						.executes(ctx -> {
							Mode mode = getMode(ctx);

							CameraAngleAnimationService.setAnimationMode(mode);

							return Command.SINGLE_SUCCESS;
						})
						.then(Commands.argument("speed", FloatArgumentType.floatArg(0))
							.executes(ctx -> {
								Mode mode = getMode(ctx);
								float speed = FloatArgumentType.getFloat(ctx, "speed");

								CameraAngleAnimationService.setAnimationMode(mode);
								CameraAngleAnimationService.setAnimationSpeed(speed);

								return Command.SINGLE_SUCCESS;
							})
						))
				)
			);
	}
}
