package io.github.yhyeop.sanupcraft.industry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 다중 블록 외벽의 이어진 모양 (확장 M15 R5). 모양이 선 구조물 안에서 이 칸이 어디인지를 월드 축 기준으로 둔다:
 * {@link #X} · {@link #Z} = 0 (서 · 북 끝) · 1 (가운데) · 2 (동 · 남 끝), {@link #Y} = 0 (맨 아랫줄) · 1 (가운데 줄) · 2 (맨 윗줄).
 * 블록 모델이 이것으로 바깥 면마다 큰 그림의 한 조각을 고른다 ({@code formed}가 꺼져 있으면 쓰지 않는다).
 * 값은 {@link MultiblockShape}가 외벽 무늬를 켤 때 같이 정한다. 그림 · 모양만 바뀌고 동작은 그대로다.
 */
public final class CasingLook {
	public static final IntegerProperty X = IntegerProperty.create("cx", 0, 2);
	public static final IntegerProperty Y = IntegerProperty.create("cy", 0, 2);
	public static final IntegerProperty Z = IntegerProperty.create("cz", 0, 2);

	private CasingLook() {
	}

	/** 이 속성들이 있는 블록인가. */
	public static boolean has(BlockState state) {
		return state.hasProperty(X) && state.hasProperty(Y) && state.hasProperty(Z);
	}

	/** {@code shape}의 제어기({@code controller}, 보는 쪽 {@code facing}) 구조물 안에서 {@code pos} 칸의 모양을 입힌 상태. */
	public static BlockState of(BlockState state, MultiblockShape shape, BlockPos controller, Direction facing, BlockPos pos) {
		if (!has(state)) {
			return state;
		}
		BlockPos a = MultiblockShape.at(controller, facing, -1, 0, 0);
		BlockPos b = MultiblockShape.at(controller, facing, 1, shape.depth() - 1, shape.height() - 1);
		return state.setValue(X, slot(pos.getX(), Math.min(a.getX(), b.getX()), Math.max(a.getX(), b.getX())))
				.setValue(Y, slot(pos.getY(), Math.min(a.getY(), b.getY()), Math.max(a.getY(), b.getY())))
				.setValue(Z, slot(pos.getZ(), Math.min(a.getZ(), b.getZ()), Math.max(a.getZ(), b.getZ())));
	}

	/** 두 상태의 모양이 같은가. */
	public static boolean same(BlockState a, BlockState b) {
		return !has(a) || !has(b) || a.getValue(X).equals(b.getValue(X)) && a.getValue(Y).equals(b.getValue(Y)) && a.getValue(Z).equals(b.getValue(Z));
	}

	private static int slot(int value, int min, int max) {
		return value <= min ? 0 : value >= max ? 2 : 1;
	}
}
