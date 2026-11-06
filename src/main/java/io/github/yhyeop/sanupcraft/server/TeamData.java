package io.github.yhyeop.sanupcraft.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.scores.TeamColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/** 서버 전체의 팀 목록 (월드 저장 데이터 {@code data/sanupcraft/teams.dat}). 로직은 {@link Teams}. */
public final class TeamData extends SavedData {
	private record Member(UUID id, String name) {
		static final Codec<Member> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(Member::id),
				Codec.STRING.fieldOf("name").forGetter(Member::name)).apply(i, Member::new));
	}

	/** 팀 하나. 바꾸는 것은 {@link Teams}를 거친다 (스코어보드 팀과 같이 맞추려고). */
	public static final class Team {
		static final Codec<Team> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("id").forGetter(Team::id),
				Codec.STRING.fieldOf("name").forGetter(Team::name),
				UUIDUtil.CODEC.fieldOf("owner").forGetter(Team::owner),
				TeamColor.CODEC.optionalFieldOf("color", TeamColor.WHITE).forGetter(Team::color),
				Codec.BOOL.optionalFieldOf("locked", false).forGetter(Team::locked),
				Member.CODEC.listOf().optionalFieldOf("members", List.of()).forGetter(Team::memberList)).apply(i, Team::new));

		private final int id;
		private final String name;
		private UUID owner;
		private TeamColor color;
		private boolean locked;
		private final Map<UUID, String> members = new LinkedHashMap<>();

		Team(int id, String name, UUID owner, TeamColor color, boolean locked, List<Member> members) {
			this.id = id;
			this.name = name;
			this.owner = owner;
			this.color = color;
			this.locked = locked;
			members.forEach(m -> this.members.put(m.id(), m.name()));
		}

		public int id() {
			return id;
		}

		public String name() {
			return name;
		}

		public UUID owner() {
			return owner;
		}

		public TeamColor color() {
			return color;
		}

		public boolean locked() {
			return locked;
		}

		/** 들어온 순서대로의 (id, 이름). */
		public Map<UUID, String> members() {
			return members;
		}

		public boolean has(UUID player) {
			return members.containsKey(player);
		}

		void setOwner(UUID owner) {
			this.owner = owner;
		}

		void setColor(TeamColor color) {
			this.color = color;
		}

		void setLocked(boolean locked) {
			this.locked = locked;
		}

		/** 스코어보드 팀 이름. */
		String scoreboardName() {
			return "sc_" + id;
		}

		private List<Member> memberList() {
			List<Member> list = new ArrayList<>();
			members.forEach((uuid, memberName) -> list.add(new Member(uuid, memberName)));
			return list;
		}
	}

	public static final Codec<TeamData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Team.CODEC.listOf().optionalFieldOf("teams", List.of()).forGetter(d -> d.teams),
			Codec.INT.optionalFieldOf("next_id", 1).forGetter(d -> d.nextId)).apply(i, TeamData::new));

	public static final SavedDataType<TeamData> TYPE = new SavedDataType<>(Sanupcraft.id("teams"), TeamData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final List<Team> teams = new ArrayList<>();
	private int nextId;

	public TeamData() {
		this(List.of(), 1);
	}

	private TeamData(List<Team> teams, int nextId) {
		this.teams.addAll(teams);
		this.nextId = nextId;
	}

	int nextId() {
		return nextId;
	}

	public List<Team> teams() {
		return teams;
	}

	public @Nullable Team byName(String name) {
		for (Team team : teams) {
			if (team.name.equalsIgnoreCase(name)) {
				return team;
			}
		}
		return null;
	}

	public @Nullable Team of(UUID player) {
		for (Team team : teams) {
			if (team.has(player)) {
				return team;
			}
		}
		return null;
	}

	Team add(String name, UUID owner, String ownerName, TeamColor color) {
		Team team = new Team(nextId++, name, owner, color, false, List.of(new Member(owner, ownerName)));
		teams.add(team);
		setDirty();
		return team;
	}

	void remove(Team team) {
		teams.remove(team);
		setDirty();
	}
}
