package io.github.yhyeop.sanupcraft.power;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 증기 계기판의 블록 엔티티. 붙은 블록({@link SteamStorage})의 저장률을 읽고,
 * 신호가 바뀌면 이웃 비교기에 알린다 (저장량은 블록 갱신 없이 바뀌므로 2틱마다 확인).
 */
public class SteamGaugeBlockEntity extends BlockEntity {
	private static final int CHECK_TICKS = 2;

	/** 마지막으로 이웃에 알린 신호 (저장하지 않는다). */
	private int lastSignal = -1;

	public SteamGaugeBlockEntity(BlockPos pos, BlockState state) {
		super(ModPower.STEAM_GAUGE_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SteamGaugeBlockEntity gauge) {
		if (level.getGameTime() % CHECK_TICKS != 0) {
			return;
		}
		int signal = gauge.signal();
		if (signal != gauge.lastSignal) {
			// 처음 도는 틱에는 알릴 필요가 없다 (놓을 때 이웃이 이미 읽는다)
			boolean first = gauge.lastSignal < 0;
			gauge.lastSignal = signal;
			if (!first) {
				level.updateNeighbourForOutputSignal(pos, state.getBlock());
			}
		}
	}

	/** 눈금판 반대쪽에 붙은 증기 저장 블록. 없으면 null. */
	public @Nullable SteamStorage target() {
		if (level == null) {
			return null;
		}
		Direction back = getBlockState().getValue(SteamGaugeBlock.FACING).getOpposite();
		return level.getBlockEntity(getBlockPos().relative(back)) instanceof SteamStorage storage ? storage : null;
	}

	/** 비교기 신호 0~15: 비면 0, 조금이라도 있으면 1 이상, 가득이면 15. */
	public int signal() {
		SteamStorage target = target();
		return target == null ? 0 : signalFor(target.steamStored(), target.steamCapacity());
	}

	public static int signalFor(int stored, int capacity) {
		if (stored <= 0 || capacity <= 0) {
			return 0;
		}
		return Math.min(15, (int) Math.floor(14.0 * stored / capacity) + 1);
	}
}
