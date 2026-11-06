package io.github.yhyeop.sanupcraft.gear.special;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 광부 헬멧의 머리 조명. 어두운 곳에서 머리 칸(빈 공기)에 바닐라 {@code light} 블록(보이지 않음)을 하나 놓고,
 * 움직이면 옛 칸을 지우고 새 칸에 놓는다. 사람마다 한 칸만 기억하고, 지울 때는 그 칸이 아직 빛 블록일 때만 지운다.
 * 이웃 갱신 · 모양 갱신 없이 놓아서(관찰자 · 레드스톤이 반응하지 않게) 빛만 바뀐다.
 * 헬멧을 벗거나 · 죽거나 · 나가거나 · 차원을 옮기거나 · 서버가 멈추면 지운다
 * (불러온 청크에서만 지운다. 같은 차원 안 먼 순간이동 직후에는 드물게 빛 블록이 남을 수 있다).
 * "어둡다" = 하늘 빛과 (우리 조명이 아닌) 블록 빛이 둘 다 {@link #DARK_BELOW} 아래.
 */
public final class HeadLamp {
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json (빛 세기, 0이면 끔). */
	public static final int LAMP_LEVEL = 13;
	/** 이 밝기 아래면 켠다. */
	static final int DARK_BELOW = 8;
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

	private record Spot(ResourceKey<Level> dimension, BlockPos pos) {
	}

	private static final Map<UUID, Spot> LAMPS = new ConcurrentHashMap<>();

	private HeadLamp() {
	}

	/** 지금 이 사람의 조명 칸 (없으면 null). 시험용. */
	public static @Nullable BlockPos lampOf(LivingEntity entity) {
		Spot spot = LAMPS.get(entity.getUUID());
		return spot == null ? null : spot.pos();
	}

	/** 한 번 (2틱마다): 켜야 할 칸을 정해 옛 칸과 다르면 옮긴다. 지금 조명 칸을 돌려준다. */
	public static @Nullable BlockPos tick(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return null;
		}
		Spot old = LAMPS.get(entity.getUUID());
		BlockPos want = null;
		int lampLevel = SanupcraftConfig.current().minerLampLevel;
		if (lampLevel > 0 && entity.isAlive() && !entity.isSpectator() && MinerGear.wearsHelmet(entity)) {
			BlockPos head = BlockPos.containing(entity.getEyePosition());
			Spot here = old != null && old.dimension() == level.dimension() ? old : null;
			if (canPlace(level, head, here) && isDark(level, head, here, lampLevel)) {
				want = head;
			}
		}
		if (old != null && !(want != null && old.dimension() == level.dimension() && old.pos().equals(want))) {
			remove(level.getServer(), old);
			LAMPS.remove(entity.getUUID());
			old = null;
		}
		if (want != null && old == null) {
			level.setBlock(want, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lampLevel), FLAGS);
			LAMPS.put(entity.getUUID(), new Spot(level.dimension(), want));
		}
		return want;
	}

	/** 이 사람의 조명을 지운다 (나감 · 죽음). */
	public static void clear(LivingEntity entity) {
		Spot old = LAMPS.remove(entity.getUUID());
		if (old != null && entity.level().getServer() != null) {
			remove(entity.level().getServer(), old);
		}
	}

	/** 서버가 멈출 때 전부 지운다. */
	public static void clearAll(MinecraftServer server) {
		for (Spot spot : LAMPS.values()) {
			remove(server, spot);
		}
		LAMPS.clear();
	}

	private static boolean canPlace(ServerLevel level, BlockPos pos, @Nullable Spot ours) {
		if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		return state.isAir() || ours != null && ours.pos().equals(pos) && state.is(Blocks.LIGHT);
	}

	private static boolean isDark(ServerLevel level, BlockPos pos, @Nullable Spot ours, int lampLevel) {
		int sky = Math.max(0, level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken());
		int block = level.getBrightness(LightLayer.BLOCK, pos);
		// 우리 조명이 비추는 몫은 빼고 본다 (그 몫보다 밝으면 다른 빛이 있는 것)
		int mine = ours == null ? 0 : Math.max(0, lampLevel - ours.pos().distManhattan(pos));
		int other = block > mine ? block : 0;
		return Math.max(sky, other) < DARK_BELOW;
	}

	private static void remove(MinecraftServer server, Spot spot) {
		ServerLevel level = server.getLevel(spot.dimension());
		if (level != null && level.isLoaded(spot.pos()) && level.getBlockState(spot.pos()).is(Blocks.LIGHT)) {
			level.setBlock(spot.pos(), Blocks.AIR.defaultBlockState(), FLAGS);
		}
	}
}
