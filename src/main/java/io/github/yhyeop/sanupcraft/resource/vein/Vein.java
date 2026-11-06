package io.github.yhyeop.sanupcraft.resource.vein;

/** 한 청크의 광맥: 종류 · 처음 매장량 · 남은 양. */
public record Vein(VeinType type, int total, int remaining) {
	public boolean depleted() {
		return remaining <= 0;
	}

	/** 남은 비율 (0 ~ 100). 조금이라도 남았으면 최소 1. */
	public int percent() {
		if (remaining <= 0 || total <= 0) {
			return 0;
		}
		return Math.max(1, Math.min(100, (int) Math.round(100.0 * remaining / total)));
	}

	public Vein withRemaining(int value) {
		return new Vein(type, total, Math.max(0, value));
	}
}
