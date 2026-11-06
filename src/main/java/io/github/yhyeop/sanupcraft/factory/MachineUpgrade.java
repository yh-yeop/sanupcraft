package io.github.yhyeop.sanupcraft.factory;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 기계 업그레이드 종류. 기계의 업그레이드 칸 2개에 끼우고, 같은 것 둘이면 효과가 두 번 (곱) 걸린다.
 * 수치 기본값은 여기 상수, 실제로 쓰는 값은 config/sanupcraft.json ({@code machineUpgrade}).
 */
public enum MachineUpgrade {
	/** 처리 1.5배 빠르게. 틱당 증기 · 연료도 1.5배 (한 개를 만드는 데 드는 양은 같다). */
	SPEED,
	/** 증기 · 연료 30% 절약. */
	EFFICIENCY,
	/** 결과가 25% 확률로 하나 더. 둘이면 따로 두 번 굴린다 (최대 +2). */
	DOUBLE;

	public static final double SPEED_MULTIPLIER = 1.5;
	public static final double EFFICIENCY_MULTIPLIER = 0.7;
	public static final double DOUBLE_CHANCE = 0.25;

	/** 끼운 업그레이드로 정해지는 배율. {@code speed}: 틱당 진행, {@code use}: 틱당 증기 · 연료, {@code doubles}: 두 배 굴림 수. */
	public record Effects(double speed, double use, int doubles) {
		public static final Effects NONE = new Effects(1.0, 1.0, 0);

		public static Effects of(int speedCount, int efficiencyCount, int doubleCount) {
			SanupcraftConfig config = SanupcraftConfig.current();
			double speed = Math.pow(config.upgradeSpeedMultiplier, speedCount);
			double use = speed * Math.pow(config.upgradeEfficiencyMultiplier, efficiencyCount);
			return new Effects(speed, use, doubleCount);
		}
	}
}
