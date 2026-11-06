package io.github.yhyeop.sanupcraft.config;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** {@code /sanupcraft config reload} (OP): 설정 파일을 다시 읽고 채팅에 알린다. */
public final class ConfigCommand {
	private ConfigCommand() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("sanupcraft")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("config")
						.then(Commands.literal("reload").executes(c -> reload(c.getSource())))));
	}

	private static int reload(CommandSourceStack source) {
		SanupcraftConfig.Loaded loaded = SanupcraftConfig.load();
		if (loaded.warnings().isEmpty()) {
			source.sendSuccess(() -> Component.translatable("commands.sanupcraft.config.reloaded", SanupcraftConfig.FILE_NAME), true);
		} else {
			source.sendSuccess(() -> Component.translatable("commands.sanupcraft.config.reloaded_warnings",
					SanupcraftConfig.FILE_NAME, loaded.warnings().size()), true);
			for (String warning : loaded.warnings()) {
				source.sendFailure(Component.literal(warning));
			}
		}
		return 1;
	}
}
