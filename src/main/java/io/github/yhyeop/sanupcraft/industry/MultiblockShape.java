package io.github.yhyeop.sanupcraft.industry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 폭 3 다중 블록 모양 도우미 (M8 묶음 B, 대형 보일러 {@link BigBoilerShape}와 같은 방식). 제어기는 앞면 가운데 맨 아랫줄.
 * 칸 좌표: 옆 {@code side} -1..1 (제어기 기준 오른쪽이 +), 깊이 {@code depth} 0..depth-1 (앞면이 0), 높이 {@code up} 0..height-1.
 * 칸마다 와야 하는 것은 {@link Layout}이 정하고, 외벽인지는 {@code isCasing}이 본다. 빈칸은 공기여야 한다.
 */
public final class MultiblockShape {
	/** 한 칸에 와야 하는 것. */
	public enum Part {
		CONTROLLER, CASING, HOLLOW
	}

	/** 칸 좌표 → 와야 하는 것. */
	@FunctionalInterface
	public interface Layout {
		Part partAt(int side, int depth, int up);
	}

	/** 틀린 칸: 위치, 와야 하는 것. */
	public record Problem(BlockPos pos, Part expected) {
	}

	private final int depth;
	private final int height;
	private final Layout layout;
	private final Predicate<BlockState> isCasing;

	public MultiblockShape(int depth, int height, Layout layout, Predicate<BlockState> isCasing) {
		this.depth = depth;
		this.height = height;
		this.layout = layout;
		this.isCasing = isCasing;
	}

	public int depth() {
		return depth;
	}

	public int height() {
		return height;
	}

	public Part partAt(int side, int depth, int up) {
		return layout.partAt(side, depth, up);
	}

	/** 제어기 기준 칸 좌표를 월드 위치로. {@code facing}은 제어기가 보는 쪽 (앞). */
	public static BlockPos at(BlockPos controller, Direction facing, int side, int depth, int up) {
		return controller.relative(facing.getClockWise(), side).relative(facing.getOpposite(), depth).above(up);
	}

	/** 모양 안의 모든 칸 (빈칸 포함). */
	public List<BlockPos> positions(BlockPos controller, Direction facing) {
		return collect(controller, facing, null);
	}

	/** 외벽 칸만. */
	public List<BlockPos> casings(BlockPos controller, Direction facing) {
		return collect(controller, facing, Part.CASING);
	}

	private List<BlockPos> collect(BlockPos controller, Direction facing, @Nullable Part only) {
		List<BlockPos> list = new ArrayList<>();
		for (int up = 0; up < height; up++) {
			for (int d = 0; d < depth; d++) {
				for (int side = -1; side <= 1; side++) {
					if (only == null || layout.partAt(side, d, up) == only) {
						list.add(at(controller, facing, side, d, up).immutable());
					}
				}
			}
		}
		return list;
	}

	/** 모양을 맞춰 본다. 맞으면 null, 틀리면 아랫줄 · 앞쪽부터 찾은 첫 틀린 칸. */
	public @Nullable Problem check(Level level, BlockPos controller, Direction facing) {
		for (int up = 0; up < height; up++) {
			for (int d = 0; d < depth; d++) {
				for (int side = -1; side <= 1; side++) {
					Part part = layout.partAt(side, d, up);
					if (part == Part.CONTROLLER) {
						continue;
					}
					BlockPos pos = at(controller, facing, side, d, up);
					BlockState state = level.getBlockState(pos);
					boolean ok = part == Part.CASING ? isCasing.test(state) : state.isAir();
					if (!ok) {
						return new Problem(pos.immutable(), part);
					}
				}
			}
		}
		return null;
	}
}
