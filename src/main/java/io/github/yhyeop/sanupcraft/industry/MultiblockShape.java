package io.github.yhyeop.sanupcraft.industry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 폭 3 다중 블록 모양 도우미 (M8 묶음 B: 대형 보일러 · 용광로 탑). 제어기는 앞면 가운데 맨 아랫줄.
 * 칸 좌표: 옆 {@code side} -1..1 (제어기 기준 오른쪽이 +), 깊이 {@code depth} 0..depth-1 (앞면이 0), 높이 {@code up} 0..height-1.
 * 칸마다 와야 하는 것은 {@link Layout}이 정하고, 외벽인지는 {@code isCasing}이 본다. 빈칸은 공기여야 한다.
 * 외벽 무늬({@code formed} 속성) 켜기 · 끄기, 외벽이 부서질 때 제어기 알림, 틀린 칸 문구도 여기서 한다.
 * 무늬를 켤 때 외벽의 이어진 모양({@link CasingLook}, 확장 M15 R5)도 같이 정한다.
 * <p>
 * 외벽 공유 (REFACTOR.md 7-10): 두 구조물이 외벽을 같이 써도 된다. 한쪽이 풀릴 때({@link #unform}) 모양이 선 이웃 제어기가 가진 칸은
 * 무늬를 남기고 그 이웃에 잇는다. 나머지 칸만 끄고, 벽돌 링크는 나를 가리킬 때만 푼다.
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

	/** 이 모양을 쓰는 제어기 블록 엔티티. */
	public interface Controller {
		MultiblockShape shape();

		BlockPos getBlockPos();

		/** 제어기가 보는 쪽 (앞). */
		Direction facing();

		boolean isFormed();

		/** 다음 틱에 모양을 다시 확인한다. */
		void requestCheck();
	}

	/** 자기를 쓰는 제어기 위치를 기억하는 외벽 블록 엔티티 (내열 벽돌). */
	public interface LinkedCasing {
		@Nullable BlockPos linkedController();

		void link(@Nullable BlockPos controller);
	}

	private final int depth;
	private final int height;
	private final Layout layout;
	private final Predicate<BlockState> isCasing;
	private final BooleanProperty formed;

	public MultiblockShape(int depth, int height, Layout layout, Predicate<BlockState> isCasing, BooleanProperty formed) {
		this.depth = depth;
		this.height = height;
		this.layout = layout;
		this.isCasing = isCasing;
		this.formed = formed;
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

	/** 틀린 칸 문구: {@code keyPrefix.need_casing} / {@code keyPrefix.need_hollow} (좌표 · 지금 블록 이름). */
	public static Component problemMessage(Level level, Problem problem, String keyPrefix) {
		BlockPos at = problem.pos();
		String key = keyPrefix + (problem.expected() == Part.CASING ? ".need_casing" : ".need_hollow");
		return Component.translatable(key, at.getX(), at.getY(), at.getZ(), level.getBlockState(at).getBlock().getName());
	}

	/** 모양이 섰다: 외벽 무늬를 켜고 벽돌을 나에게 잇는다 (확인할 때마다 — 다른 제어기가 끈 공유 외벽도 다시 켠다). 이웃 갱신 없이. */
	public void form(ServerLevel level, BlockPos controller, Direction facing) {
		BlockPos self = controller.immutable();
		for (BlockPos casing : casings(controller, facing)) {
			setCasing(level, casing, true, self, null, facing, self);
		}
	}

	/**
	 * 모양이 풀렸다 (또는 제어기가 부서졌다). 모양이 선 이웃 제어기가 가진 외벽은 무늬를 남기고 그 이웃에 잇는다.
	 * 나머지 외벽은 무늬를 끄고, 벽돌 링크는 나를 가리킬 때만 푼다. 이웃 갱신 없이, 안 불러온 청크는 건드리지 않는다.
	 */
	public void unform(ServerLevel level, BlockPos controller, Direction facing) {
		List<BlockPos> mine = casings(controller, facing);
		Map<BlockPos, Controller> heldBy = new HashMap<>();
		for (Controller other : controllersNear(level, mine, controller)) {
			if (other.isFormed()) {
				BlockPos otherPos = other.getBlockPos().immutable();
				for (BlockPos casing : casings(otherPos, other.facing())) {
					heldBy.putIfAbsent(casing, other);
				}
			}
		}
		for (BlockPos casing : mine) {
			Controller holder = heldBy.get(casing);
			BlockPos holderPos = holder == null ? null : holder.getBlockPos().immutable();
			setCasing(level, casing, holder != null, holderPos, controller, holder == null ? facing : holder.facing(), controller);
		}
	}

	/**
	 * 외벽 한 칸: 무늬를 {@code on}으로, 벽돌이면 {@code linkTo}에 잇거나 ({@code linkTo}가 null이면) {@code unlinkFrom}을 가리킬 때만 푼다.
	 * 켤 때는 이어진 모양({@link CasingLook})도 {@code linkTo} 구조물({@code linkFacing}) 기준으로 맞춘다 ({@code except} 제어기는 빼고 본다).
	 */
	private void setCasing(ServerLevel level, BlockPos casing, boolean on, @Nullable BlockPos linkTo, @Nullable BlockPos unlinkFrom,
			Direction linkFacing, BlockPos except) {
		BlockState state = level.getBlockState(casing);
		if (!isCasing.test(state)) {
			return;
		}
		BlockState next = state.setValue(formed, on);
		if (on && linkTo != null) {
			next = look(level, state, next, casing, linkTo, linkFacing, except);
		}
		if (next != state) {
			level.setBlock(casing, next, Block.UPDATE_CLIENTS);
		}
		if (level.getBlockEntity(casing) instanceof LinkedCasing linked) {
			if (linkTo != null) {
				linked.link(linkTo);
			} else if (unlinkFrom != null && unlinkFrom.equals(linked.linkedController())) {
				linked.link(null);
			}
		}
	}

	/**
	 * 확장 M15 R5: 외벽 칸의 이어진 모양을 {@code owner} 구조물 기준으로. 이미 무늬가 켜져 있고 다른 모양 선 구조물 기준 모양이면
	 * 그대로 둔다 (공유 외벽이 확인할 때마다 번갈아 바뀌지 않게). 그런 구조물이 없으면 (예전 세이브 등) 내 기준으로 고친다.
	 */
	private BlockState look(ServerLevel level, BlockState before, BlockState next, BlockPos casing, BlockPos owner, Direction ownerFacing, BlockPos except) {
		BlockState mine = CasingLook.of(next, this, owner, ownerFacing, casing);
		if (!before.getValue(formed) || CasingLook.same(mine, before)) {
			return mine;
		}
		for (Controller other : controllersNear(level, List.of(casing), except)) {
			BlockPos otherPos = other.getBlockPos();
			if (other.isFormed() && !otherPos.equals(owner) && casings(otherPos, other.facing()).contains(casing)
					&& CasingLook.same(CasingLook.of(next, this, otherPos, other.facing(), casing), before)) {
				return next;
			}
		}
		return mine;
	}

	/** 외벽 한 칸이 바뀌었다 (부서짐): 이 칸을 쓸 수 있는 이 모양의 제어기에 다시 확인하라고 알린다. */
	public void notifyControllers(ServerLevel level, BlockPos casing) {
		for (Controller controller : controllersNear(level, List.of(casing), null)) {
			controller.requestCheck();
		}
	}

	/**
	 * {@code casings} 중 한 칸이라도 자기 외벽으로 쓸 수 있는 자리의 이 모양 제어기들 ({@code except} 자리는 뺀다).
	 * 제어기에서 외벽까지 가로는 깊이 방향 0..depth-1 · 옆 ±1, 세로는 0..height-1 위다.
	 */
	private List<Controller> controllersNear(ServerLevel level, List<BlockPos> casings, @Nullable BlockPos except) {
		List<Controller> found = new ArrayList<>();
		if (casings.isEmpty()) {
			return found;
		}
		int reach = Math.max(1, depth - 1);
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (BlockPos pos : casings) {
			minX = Math.min(minX, pos.getX());
			minY = Math.min(minY, pos.getY());
			minZ = Math.min(minZ, pos.getZ());
			maxX = Math.max(maxX, pos.getX());
			maxY = Math.max(maxY, pos.getY());
			maxZ = Math.max(maxZ, pos.getZ());
		}
		for (BlockPos near : BlockPos.betweenClosed(minX - reach, minY - (height - 1), minZ - reach, maxX + reach, maxY, maxZ + reach)) {
			if (near.equals(except) || !level.isLoaded(near)) {
				continue;
			}
			if (level.getBlockEntity(near) instanceof Controller controller && controller.shape() == this) {
				found.add(controller);
			}
		}
		return found;
	}
}
