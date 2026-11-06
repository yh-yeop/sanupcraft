package io.github.yhyeop.sanupcraft.power;

/** 증기 파이프로 증기를 받는 블록 엔티티 (1단계 기계, 벨트 모터). */
public interface SteamConsumer {
	/** 증기를 최대 {@code amount}만큼 받고, 실제로 받은 양을 돌려준다. */
	int acceptSteam(int amount);

	/** 지금 보통 증기를 받는가. 고압 전용 기계(정밀 조립기)는 거짓 → 보통 파이프가 잇지도, 증기를 주지도 않는다 (M5). */
	default boolean takesNormalSteam() {
		return true;
	}
}
