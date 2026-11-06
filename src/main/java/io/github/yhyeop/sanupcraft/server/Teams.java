package io.github.yhyeop.sanupcraft.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

import io.github.yhyeop.sanupcraft.server.TeamData.Team;

/**
 * 팀 (T1). 이 모드의 팀이 정본이고 ({@link TeamData}), 바닐라 스코어보드 팀({@code sc_<번호>})에 거울처럼 맞춘다.
 * 그래서 이름표 · 채팅 · 사망 메시지에 팀 색과 [팀 이름]이 붙는다.
 * <ul>
 *   <li>한 사람은 한 팀. 만든 사람이 주인. 기본은 열려 있어서 누구나 들어올 수 있고, 주인이 잠그면(lock) 새로 못 들어온다.</li>
 *   <li>주인이 나가면 다음 사람이 주인. 마지막 사람이 나가면 팀이 사라진다.</li>
 *   <li>{@link #sameTeam}: 잠금(엔더 운반기 등)이 주인 대신 주인의 팀원에게도 열리게 하는 도우미.</li>
 * </ul>
 * 바꾸는 함수는 실패하면 번역 키를, 성공하면 null을 돌려준다.
 */
public final class Teams {
	public static final int MAX_NAME_LENGTH = 12;
	/** 새 팀의 색을 번호 순으로 돌려 쓴다 (검정 · 흰색 · 회색 계열은 이름표에서 안 보이거나 밋밋해서 뺀다). */
	private static final List<TeamColor> PALETTE = List.of(TeamColor.RED, TeamColor.GOLD, TeamColor.GREEN, TeamColor.AQUA,
			TeamColor.LIGHT_PURPLE, TeamColor.YELLOW, TeamColor.BLUE, TeamColor.DARK_GREEN, TeamColor.DARK_AQUA, TeamColor.DARK_PURPLE);

	private Teams() {
	}

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(server, handler.getPlayer()));
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			for (Team team : data(server).teams()) {
				sync(server, team);
			}
		});
	}

	public static TeamData data(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TeamData.TYPE);
	}

	public static @Nullable Team teamOf(MinecraftServer server, UUID player) {
		return data(server).of(player);
	}

	/** {@code owner}가 있고 {@code player}가 그 사람과 같은 팀이면 참. 잠금 검사에서 "주인 아니어도 팀원이면 연다"에 쓴다. */
	public static boolean sameTeam(@Nullable Level level, @Nullable UUID owner, Player player) {
		if (owner == null || level == null || level.getServer() == null) {
			return false;
		}
		Team team = teamOf(level.getServer(), player.getUUID());
		return team != null && team.has(owner);
	}

	// ---- 바꾸기 ----

	public static @Nullable String create(MinecraftServer server, ServerPlayer player, String rawName) {
		String name = rawName.strip();
		if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
			return "commands.sanupcraft.team.bad_name";
		}
		TeamData data = data(server);
		if (data.of(player.getUUID()) != null) {
			return "commands.sanupcraft.team.already_in";
		}
		if (data.byName(name) != null) {
			return "commands.sanupcraft.team.name_taken";
		}
		Team team = data.add(name, player.getUUID(), player.getScoreboardName(), PALETTE.get(data.nextId() % PALETTE.size()));
		sync(server, team);
		return null;
	}

	public static @Nullable String join(MinecraftServer server, ServerPlayer player, String name) {
		TeamData data = data(server);
		Team team = data.byName(name.strip());
		if (team == null) {
			return "commands.sanupcraft.team.not_found";
		}
		if (data.of(player.getUUID()) != null) {
			return "commands.sanupcraft.team.already_in";
		}
		if (team.locked()) {
			return "commands.sanupcraft.team.locked";
		}
		team.members().put(player.getUUID(), player.getScoreboardName());
		data.setDirty();
		sync(server, team);
		tell(server, team, Component.translatable("commands.sanupcraft.team.joined_notice", player.getDisplayName(), team.name()));
		return null;
	}

	public static @Nullable String leave(MinecraftServer server, ServerPlayer player) {
		TeamData data = data(server);
		Team team = data.of(player.getUUID());
		if (team == null) {
			return "commands.sanupcraft.team.not_in";
		}
		removeMember(server, data, team, player.getUUID(), player.getScoreboardName());
		return null;
	}

	/** 주인만: 팀원을 내보낸다. */
	public static @Nullable String kick(MinecraftServer server, ServerPlayer owner, String targetName) {
		TeamData data = data(server);
		Team team = data.of(owner.getUUID());
		if (team == null) {
			return "commands.sanupcraft.team.not_in";
		}
		if (!team.owner().equals(owner.getUUID())) {
			return "commands.sanupcraft.team.owner_only";
		}
		UUID target = null;
		for (Map.Entry<UUID, String> member : team.members().entrySet()) {
			if (member.getValue().equalsIgnoreCase(targetName)) {
				target = member.getKey();
			}
		}
		if (target == null) {
			return "commands.sanupcraft.team.no_member";
		}
		if (target.equals(owner.getUUID())) {
			return "commands.sanupcraft.team.kick_self";
		}
		removeMember(server, data, team, target, team.members().get(target));
		return null;
	}

	/** 주인만: 잠그기 / 풀기. 잠그면 새로 못 들어온다. */
	public static @Nullable String toggleLock(MinecraftServer server, ServerPlayer owner) {
		Team team = ownedTeam(server, owner);
		if (team == null) {
			return notOwnerError(server, owner);
		}
		team.setLocked(!team.locked());
		data(server).setDirty();
		return null;
	}

	public static @Nullable String setColor(MinecraftServer server, ServerPlayer owner, TeamColor color) {
		Team team = ownedTeam(server, owner);
		if (team == null) {
			return notOwnerError(server, owner);
		}
		team.setColor(color);
		data(server).setDirty();
		sync(server, team);
		return null;
	}

	private static String notOwnerError(MinecraftServer server, ServerPlayer player) {
		return teamOf(server, player.getUUID()) == null ? "commands.sanupcraft.team.not_in" : "commands.sanupcraft.team.owner_only";
	}

	private static @Nullable Team ownedTeam(MinecraftServer server, ServerPlayer player) {
		Team team = teamOf(server, player.getUUID());
		return team != null && team.owner().equals(player.getUUID()) ? team : null;
	}

	private static void removeMember(MinecraftServer server, TeamData data, Team team, UUID member, String memberName) {
		Scoreboard scoreboard = server.getScoreboard();
		PlayerTeam board = scoreboard.getPlayerTeam(team.scoreboardName());
		if (board != null && scoreboard.getPlayersTeam(memberName) == board) {
			scoreboard.removePlayerFromTeam(memberName, board);
		}
		team.members().remove(member);
		if (team.members().isEmpty()) {
			data.remove(team);
			if (board != null) {
				scoreboard.removePlayerTeam(board);
			}
			return;
		}
		data.setDirty();
		if (team.owner().equals(member)) {
			team.setOwner(team.members().keySet().iterator().next());
			tell(server, team, Component.translatable("commands.sanupcraft.team.new_owner", team.members().get(team.owner())));
		}
		sync(server, team);
		tell(server, team, Component.translatable("commands.sanupcraft.team.left_notice", memberName));
	}

	/** 팀을 통째로 없앤다 (시험 정리용). */
	public static void disband(MinecraftServer server, Team team) {
		Scoreboard scoreboard = server.getScoreboard();
		PlayerTeam board = scoreboard.getPlayerTeam(team.scoreboardName());
		if (board != null) {
			scoreboard.removePlayerTeam(board);
		}
		data(server).remove(team);
	}

	// ---- 스코어보드와 맞추기 ----

	private static void onJoin(MinecraftServer server, ServerPlayer player) {
		Team team = teamOf(server, player.getUUID());
		if (team != null) {
			// 이름이 바뀌었을 수 있다
			if (!player.getScoreboardName().equals(team.members().get(player.getUUID()))) {
				team.members().put(player.getUUID(), player.getScoreboardName());
				data(server).setDirty();
			}
			sync(server, team);
		}
		GuideBook.giveFirstJoin(server, player);
	}

	/** 스코어보드 팀의 이름 · 색 · [접두]와 구성원을 이 모드의 팀과 똑같이 맞춘다. */
	static void sync(MinecraftServer server, Team team) {
		Scoreboard scoreboard = server.getScoreboard();
		PlayerTeam board = scoreboard.getPlayerTeam(team.scoreboardName());
		if (board == null) {
			board = scoreboard.addPlayerTeam(team.scoreboardName());
		}
		board.setDisplayName(Component.literal(team.name()));
		board.setColor(Optional.of(team.color()));
		board.setPlayerPrefix(Component.literal("[" + team.name() + "] ").withStyle(style -> style.withColor(team.color().textColor())));
		for (String stale : new ArrayList<>(board.getPlayers())) {
			if (!team.members().containsValue(stale)) {
				scoreboard.removePlayerFromTeam(stale, board);
			}
		}
		for (String name : team.members().values()) {
			if (scoreboard.getPlayersTeam(name) != board) {
				scoreboard.addPlayerToTeam(name, board);
			}
		}
	}

	/** 접속 중인 팀원에게 알린다. */
	private static void tell(MinecraftServer server, Team team, Component message) {
		for (UUID id : team.members().keySet()) {
			ServerPlayer member = server.getPlayerList().getPlayer(id);
			if (member != null) {
				member.sendSystemMessage(message);
			}
		}
	}
}
