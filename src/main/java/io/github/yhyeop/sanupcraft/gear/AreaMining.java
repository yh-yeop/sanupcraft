package io.github.yhyeop.sanupcraft.gear;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 강철 · 크롬강 해머 · 굴삭 삽: 캔 블록을 가운데로, 바라본 면에 평평한 3x3을 함께 캔다.
 * 웅크리면 한 칸만. 도구가 맞지 않거나 가운데보다 훨씬 단단한 블록(흑요석 등)은 남긴다. 블록마다 내구도를 쓴다.
 */
public final class AreaMining {
	/** 가운데보다 이만큼 더 단단하면 같이 캐지 않는다. */
	private static final float HARDNESS_MARGIN = 1.5F;

	/** 캐기 직전에 본 면 (캐고 나면 블록이 없어 다시 볼 수 없다). */
	private static final Map<UUID, Direction> FACES = new ConcurrentHashMap<>();
	private static boolean breaking;

	private AreaMining() {
	}

	public static void init() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!breaking && isAreaTool(player.getMainHandItem())) {
				FACES.put(player.getUUID(), hitFace(player, pos));
			}
			return true;
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			Direction face = FACES.remove(player.getUUID());
			if (!breaking && face != null && player instanceof ServerPlayer serverPlayer && !player.isShiftKeyDown()) {
				breakAround(level, serverPlayer, pos, state, face);
			}
		});
	}

	/** 3x3으로 캐는가: 해머 · 굴삭 삽, 또는 범위 모듈(확장 3 G3)을 끼운 곡괭이 · 삽 · 도끼. */
	public static boolean isAreaTool(ItemStack stack) {
		return isAreaToolItem(stack) || io.github.yhyeop.sanupcraft.module.GearModules.has(stack, io.github.yhyeop.sanupcraft.module.GearModule.AREA);
	}

	/** 원래 3x3인 도구 (해머 · 굴삭 삽). */
	public static boolean isAreaToolItem(ItemStack stack) {
		return stack.is(SteelGear.STEEL_HAMMER) || stack.is(SteelGear.STEEL_EXCAVATOR)
				|| stack.is(ChromeSteelGear.CHROME_STEEL_HAMMER) || stack.is(ChromeSteelGear.CHROME_STEEL_EXCAVATOR); // 확장 3 G2
	}

	private static Direction hitFace(Player player, BlockPos pos) {
		HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
		if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK && blockHit.getBlockPos().equals(pos)) {
			return blockHit.getDirection();
		}
		return Direction.orderedByNearest(player)[0].getOpposite();
	}

	/** 가운데를 뺀 3x3 판 (face에 수직인 평면). */
	public static List<BlockPos> plane(BlockPos center, Direction face) {
		List<BlockPos> result = new ArrayList<>();
		for (int a = -1; a <= 1; a++) {
			for (int b = -1; b <= 1; b++) {
				if (a == 0 && b == 0) {
					continue;
				}
				result.add(switch (face.getAxis()) {
					case X -> center.offset(0, a, b);
					case Y -> center.offset(a, 0, b);
					case Z -> center.offset(a, b, 0);
				});
			}
		}
		return result;
	}

	/** 가운데 둘레를 캔다. 캔 블록 수를 돌려준다. */
	public static int breakAround(Level level, ServerPlayer player, BlockPos center, BlockState centerState, Direction face) {
		float centerHardness = centerState.getDestroySpeed(level, center);
		int broken = 0;
		breaking = true;
		try {
			for (BlockPos pos : plane(center, face)) {
				ItemStack tool = player.getMainHandItem();
				if (!isAreaTool(tool)) {
					break;
				}
				BlockState state = level.getBlockState(pos);
				float hardness = state.getDestroySpeed(level, pos);
				if (state.isAir() || hardness < 0 || hardness > centerHardness + HARDNESS_MARGIN || !tool.isCorrectToolForDrops(state)) {
					continue;
				}
				if (player.gameMode.destroyBlock(pos)) {
					broken++;
				}
			}
		} finally {
			breaking = false;
		}
		return broken;
	}
}
