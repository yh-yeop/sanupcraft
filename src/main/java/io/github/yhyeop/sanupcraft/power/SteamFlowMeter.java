package io.github.yhyeop.sanupcraft.power;

/** 최근 1초(20틱) 동안 들어온 증기를 센다. 증기를 받는 블록 엔티티가 하나씩 들고 있는다 (저장하지 않음). */
public final class SteamFlowMeter {
	public static final int WINDOW_TICKS = 20;

	private final int[] buckets = new int[WINDOW_TICKS];
	private long lastTick = Long.MIN_VALUE;

	/** {@code now} 시각(게임 틱)에 증기 {@code amount}가 들어왔다. */
	public void add(long now, int amount) {
		if (amount <= 0) {
			return;
		}
		advance(now);
		buckets[(int) Math.floorMod(now, (long) WINDOW_TICKS)] += amount;
	}

	/** {@code now} 시각 기준 최근 1초 합계. */
	public int lastSecond(long now) {
		advance(now);
		int sum = 0;
		for (int bucket : buckets) {
			sum += bucket;
		}
		return sum;
	}

	/** 지난 칸을 비운다 (시각이 거꾸로 가도 처음부터 다시 센다). */
	private void advance(long now) {
		if (lastTick == Long.MIN_VALUE || now < lastTick || now - lastTick >= WINDOW_TICKS) {
			java.util.Arrays.fill(buckets, 0);
		} else {
			for (long t = lastTick + 1; t <= now; t++) {
				buckets[(int) Math.floorMod(t, (long) WINDOW_TICKS)] = 0;
			}
		}
		lastTick = now;
	}
}
