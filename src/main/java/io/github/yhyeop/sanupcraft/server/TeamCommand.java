package io.github.yhyeop.sanupcraft.server;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TeamColorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.TeamColor;

import io.github.yhyeop.sanupcraft.server.TeamData.Team;

/**
 * {@code /sanupcraft team ...} (누구나): create · join · leave · list · lock · color · kick. 이름은 한글도 되도록 줄 끝까지 읽는다.
 * 로직은 {@link Teams}.
 */
final class TeamCommand {
	private TeamCommand() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> node() {
		return Commands.literal("team")
				.executes(c -> info(c.getSource()))
				.then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(c -> run(c, "created", (s, p) -> Teams.create(s, p, StringArgumentType.getString(c, "name"))))))
				.then(Commands.literal("join").then(Commands.argument("name", StringArgumentType.greedyString())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(
								Teams.data(c.getSource().getServer()).teams().stream().filter(t -> !t.locked()).map(Team::name).toList(), b))
						.executes(c -> run(c, "joined", (s, p) -> Teams.join(s, p, StringArgumentType.getString(c, "name"))))))
				.then(Commands.literal("leave").executes(c -> run(c, "left", Teams::leave)))
				.then(Commands.literal("list").executes(c -> list(c.getSource())))
				.then(Commands.literal("lock").executes(c -> toggleLock(c)))
				.then(Commands.literal("color").then(Commands.argument("color", TeamColorArgument.teamColor())
						.executes(c -> run(c, "color", (s, p) -> Teams.setColor(s, p, TeamColorArgument.getTeamColor(c, "color"))))))
				.then(Commands.literal("kick").then(Commands.argument("player", StringArgumentType.word())
						.suggests((c, b) -> {
							ServerPlayer self = c.getSource().getPlayer();
							Team team = self == null ? null : Teams.teamOf(c.getSource().getServer(), self.getUUID());
							return SharedSuggestionProvider.suggest(team == null ? List.<String>of() : team.members().values(), b);
						})
						.executes(c -> run(c, "kicked", (s, p) -> Teams.kick(s, p, StringArgumentType.getString(c, "player"))))));
	}

	/** 실행 → 오류 키면 실패 메시지, 아니면 {@code commands.sanupcraft.team.<ok>} 성공 메시지. */
	private static int run(CommandContext<CommandSourceStack> c, String ok, BiFunction<MinecraftServer, ServerPlayer, String> action)
			throws CommandSyntaxException {
		ServerPlayer player = c.getSource().getPlayerOrException();
		String error = action.apply(c.getSource().getServer(), player);
		if (error != null) {
			c.getSource().sendFailure(Component.translatable(error));
			return 0;
		}
		Team team = Teams.teamOf(c.getSource().getServer(), player.getUUID());
		Component name = Component.literal(team == null ? "" : team.name());
		c.getSource().sendSuccess(() -> Component.translatable("commands.sanupcraft.team." + ok, name), false);
		return 1;
	}

	private static int toggleLock(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer player = c.getSource().getPlayerOrException();
		String error = Teams.toggleLock(c.getSource().getServer(), player);
		if (error != null) {
			c.getSource().sendFailure(Component.translatable(error));
			return 0;
		}
		Team team = Teams.teamOf(c.getSource().getServer(), player.getUUID());
		c.getSource().sendSuccess(() -> Component.translatable(team.locked() ? "commands.sanupcraft.team.lock_on" : "commands.sanupcraft.team.lock_off", team.name()), false);
		return 1;
	}

	/** 내 팀 요약 (명령만 쳤을 때). */
	private static int info(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Team team = Teams.teamOf(source.getServer(), player.getUUID());
		if (team == null) {
			source.sendSuccess(() -> Component.translatable("commands.sanupcraft.team.help"), false);
			return 0;
		}
		source.sendSuccess(() -> describe(team), false);
		return 1;
	}

	private static int list(CommandSourceStack source) {
		var teams = Teams.data(source.getServer()).teams();
		if (teams.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("commands.sanupcraft.team.none"), false);
			return 0;
		}
		for (Team team : teams) {
			source.sendSuccess(() -> describe(team), false);
		}
		return teams.size();
	}

	private static Component describe(Team team) {
		String owner = team.members().get(team.owner());
		String members = String.join(", ", team.members().values());
		Function<TeamColor, Component> colored = c -> Component.literal(team.name()).withStyle(style -> style.withColor(c.textColor()));
		return Component.translatable(team.locked() ? "commands.sanupcraft.team.entry_locked" : "commands.sanupcraft.team.entry",
				colored.apply(team.color()), team.members().size(), owner == null ? "?" : owner, members);
	}
}
