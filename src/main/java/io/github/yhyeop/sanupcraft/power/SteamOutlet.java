package io.github.yhyeop.sanupcraft.power;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 증기 내보내기 (보일러 · 대형 보일러 · 지열 분출구 · 증기 터빈이 함께 쓴다). 파이프망을 20틱마다 다시 찾고, 이어진 기계에 증기를 고르게 나눈다.
 * 시작 기계를 틱마다 돌려서, 증기가 모자랄 때도 모든 기계가 번갈아 받는다.
 * {@code new SteamOutlet(true)}는 고압 증기 (영혼 화로): 고압 파이프 · 고압 기계만 본다.
 */
public final class SteamOutlet {
	private static final int NETWORK_REFRESH_TICKS = 20;

	private final boolean highPressure;
	private List<?> consumers = List.of();
	private int refresh;
	private int rotation;

	public SteamOutlet() {
		this(false);
	}

	public SteamOutlet(boolean highPressure) {
		this.highPressure = highPressure;
	}

	/** {@code steam}을 나눠 주고 남은 양을 돌려준다. */
	public int distribute(ServerLevel level, BlockPos pos, int steam) {
		return distribute(level, List.of(pos), steam);
	}

	/** 여러 칸에서 파이프망을 찾는다 (대형 보일러: 외벽에 붙은 파이프 · 기계도 받는다). */
	public int distribute(ServerLevel level, Collection<BlockPos> starts, int steam) {
		if (--refresh <= 0) {
			consumers = highPressure ? SteamNetwork.findHighPressureConsumers(level, starts) : SteamNetwork.findConsumers(level, starts);
			refresh = NETWORK_REFRESH_TICKS;
		}
		if (steam <= 0 || consumers.isEmpty()) {
			return steam;
		}
		int count = consumers.size();
		int share = Math.max(1, steam / count);
		int start = rotation % count;
		rotation = (start + 1) % count;
		for (int i = 0; i < count; i++) {
			if (steam <= 0) {
				break;
			}
			Object consumer = consumers.get((start + i) % count);
			if (consumer instanceof BlockEntity be && be.isRemoved()) {
				continue;
			}
			steam -= give(consumer, Math.min(share, steam));
		}
		return steam;
	}

	private int give(Object consumer, int amount) {
		return highPressure ? ((HPSteamConsumer) consumer).acceptHighPressureSteam(amount) : ((SteamConsumer) consumer).acceptSteam(amount);
	}
}
