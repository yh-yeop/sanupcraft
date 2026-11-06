package io.github.yhyeop.sanupcraft.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 보스 기록 (T2): 보스 종류마다 가장 빨리 잡은 기록 {@link #KEEP}개를 서버 전체에 저장한다 ({@code data/sanupcraft/boss_records.dat}).
 * 기록 = 소환부터 처치까지 시간(틱) · 기여자 이름 · 소환 때 인원 · 강화 단계(없으면 0).
 * 쌓는 곳은 {@code boss/BossParty.giveKeyItem} (보상 지급 자리), 보여 주는 곳은 보스 기록판 블록.
 */
public final class BossRecords extends SavedData {
	/** 기록 하나. 짧을수록 좋다. */
	public record Entry(int ticks, List<String> players, int partySize, int tier) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("ticks").forGetter(Entry::ticks),
				Codec.STRING.listOf().optionalFieldOf("players", List.of()).forGetter(Entry::players),
				Codec.INT.optionalFieldOf("party_size", 1).forGetter(Entry::partySize),
				Codec.INT.optionalFieldOf("tier", 0).forGetter(Entry::tier)).apply(i, Entry::new));

		public int seconds() {
			return ticks / 20;
		}
	}

	/** 보스마다 남기는 기록 수. */
	public static final int KEEP = 10;
	/** 기록판에 보이는 순위 수. */
	public static final int SHOWN = 3;

	public static final Codec<BossRecords> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.unboundedMap(Codec.STRING, Entry.CODEC.listOf()).optionalFieldOf("bosses", Map.of()).forGetter(r -> r.bosses))
			.apply(i, BossRecords::new));

	public static final SavedDataType<BossRecords> TYPE = new SavedDataType<>(Sanupcraft.id("boss_records"), BossRecords::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<String, List<Entry>> bosses = new LinkedHashMap<>();

	public BossRecords() {
		this(Map.of());
	}

	private BossRecords(Map<String, List<Entry>> loaded) {
		loaded.forEach((boss, list) -> bosses.put(boss, new ArrayList<>(list)));
	}

	public static BossRecords get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	/** 기록이 있는 보스 id들 (저장된 순서). */
	public List<String> bossIds() {
		return new ArrayList<>(bosses.keySet());
	}

	/** 그 보스의 상위 {@code n}개 (빠른 순). */
	public List<Entry> top(String bossId, int n) {
		List<Entry> list = bosses.getOrDefault(bossId, List.of());
		return List.copyOf(list.subList(0, Math.min(n, list.size())));
	}

	/**
	 * 기록을 넣는다. 돌려주는 값은 순위(1부터), 상위 {@link #KEEP}에 못 들면 0.
	 * 같은 시간이면 먼저 세운 기록이 위.
	 */
	public int add(String bossId, Entry entry) {
		List<Entry> list = bosses.computeIfAbsent(bossId, id -> new ArrayList<>());
		int at = 0;
		while (at < list.size() && list.get(at).ticks() <= entry.ticks()) {
			at++;
		}
		if (at >= KEEP) {
			return 0;
		}
		list.add(at, entry);
		while (list.size() > KEEP) {
			list.removeLast();
		}
		setDirty();
		return at + 1;
	}

	/**
	 * 보스가 죽어 보상을 줄 때 부른다. 소환 때 시각({@code summonedAt}, 게임 시각)을 모르거나(명령어로 불러냄) 기여자가 없으면 남기지 않는다.
	 * 1위면 모두에게, 아니면 기여자에게만 알린다. 순위를 돌려준다 (기록 안 남기면 0).
	 */
	public static int record(ServerLevel level, Entity boss, long summonedAt, int partySize, int tier, List<ServerPlayer> contributors) {
		if (summonedAt < 0 || contributors.isEmpty()) {
			return 0;
		}
		MinecraftServer server = level.getServer();
		Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
		List<String> names = contributors.stream().map(p -> p.getGameProfile().name()).sorted().collect(Collectors.toList());
		int ticks = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, level.getGameTime() - summonedAt));
		int rank = get(server).add(key.toString(), new Entry(ticks, names, partySize, tier));
		if (rank == 0) {
			return 0;
		}
		Component message = Component.translatable(rank == 1 ? "block.sanupcraft.boss_board.new_best" : "block.sanupcraft.boss_board.ranked",
				boss.getDisplayName(), formatTime(ticks), rank, String.join(", ", names));
		if (rank == 1) {
			server.getPlayerList().broadcastSystemMessage(message, false);
		} else {
			contributors.stream().filter(p -> p.connection != null).forEach(p -> p.sendSystemMessage(message));
		}
		return rank;
	}

	/** 틱을 "분:초"로. */
	public static String formatTime(int ticks) {
		int seconds = ticks / 20;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}
}
