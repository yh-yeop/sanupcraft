package io.github.yhyeop.sanupcraft.industry;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 대형 보일러의 모양: 3x3 바닥 · 높이 4. 제어기는 앞면(제어기가 보는 쪽) 가운데 맨 아랫줄.
 * 가운데 기둥의 2 · 3층(제어기 기준 위로 1 · 2칸, 뒤로 1칸)은 비워 두고, 나머지 33칸은 전부 외벽.
 * 칸 좌표: 옆 {@code side} -1..1 (제어기 기준 오른쪽이 +), 깊이 {@code depth} 0..2 (앞면이 0), 높이 {@code up} 0..3.
 */
public final class BigBoilerShape {
	public static final int WIDTH = 3;
	public static final int DEPTH = 3;
	public static final int HEIGHT = 4;

	/** 한 칸에 와야 하는 것. */
	public enum Part {
		CONTROLLER, CASING, HOLLOW
	}

	/** 틀린 칸: 위치, 와야 하는 것. */
	public record Problem(BlockPos pos, Part expected) {
	}

	private BigBoilerShape() {
	}

	public static Part partAt(int side, int depth, int up) {
		if (side == 0 && depth == 0 && up == 0) {
			return Part.CONTROLLER;
		}
		if (side == 0 && depth == 1 && (up == 1 || up == 2)) {
			return Part.HOLLOW;
		}
		return Part.CASING;
	}

	/** 제어기 기준 칸 좌표를 월드 위치로. {@code facing}은 제어기가 보는 쪽 (앞). */
	public static BlockPos at(BlockPos controller, Direction facing, int side, int depth, int up) {
		return controller.relative(facing.getClockWise(), side).relative(facing.getOpposite(), depth).above(up);
	}

	/** 모양 안의 모든 칸 (36칸, 빈칸 포함). 증기를 내보낼 시작 칸으로 쓴다. */
	public static List<BlockPos> positions(BlockPos controller, Direction facing) {
		List<BlockPos> list = new ArrayList<>(WIDTH * DEPTH * HEIGHT);
		for (int up = 0; up < HEIGHT; up++) {
			for (int depth = 0; depth < DEPTH; depth++) {
				for (int side = -1; side <= 1; side++) {
					list.add(at(controller, facing, side, depth, up).immutable());
				}
			}
		}
		return list;
	}

	/** 외벽 칸만 (33칸). */
	public static List<BlockPos> casings(BlockPos controller, Direction facing) {
		List<BlockPos> list = new ArrayList<>();
		for (int up = 0; up < HEIGHT; up++) {
			for (int depth = 0; depth < DEPTH; depth++) {
				for (int side = -1; side <= 1; side++) {
					if (partAt(side, depth, up) == Part.CASING) {
						list.add(at(controller, facing, side, depth, up).immutable());
					}
				}
			}
		}
		return list;
	}

	/** 모양을 맞춰 본다. 맞으면 null, 틀리면 아랫줄 · 앞쪽부터 찾은 첫 틀린 칸. */
	public static @Nullable Problem check(Level level, BlockPos controller, Direction facing) {
		for (int up = 0; up < HEIGHT; up++) {
			for (int depth = 0; depth < DEPTH; depth++) {
				for (int side = -1; side <= 1; side++) {
					Part part = partAt(side, depth, up);
					if (part == Part.CONTROLLER) {
						continue;
					}
					BlockPos pos = at(controller, facing, side, depth, up);
					BlockState state = level.getBlockState(pos);
					boolean ok = part == Part.CASING ? state.getBlock() instanceof BigBoilerCasingBlock : state.isAir();
					if (!ok) {
						return new Problem(pos.immutable(), part);
					}
				}
			}
		}
		return null;
	}
}
