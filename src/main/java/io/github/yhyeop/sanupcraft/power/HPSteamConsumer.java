package io.github.yhyeop.sanupcraft.power;

/**
 * 고압 파이프로 고압 증기를 받는 블록 엔티티 (정밀 조립기 · 증기 터빈). 보통 증기망({@link SteamConsumer})과는 따로 돈다:
 * 영혼 화로가 고압 파이프를 따라 이 블록들만 찾고, 보통 보일러 · 분출구는 이 블록에 증기를 주지 않는다.
 */
public interface HPSteamConsumer {
	/** 고압 증기를 최대 {@code amount}만큼 받고, 실제로 받은 양을 돌려준다. */
	int acceptHighPressureSteam(int amount);

	/** 지금 고압 증기를 받는가. 한 클래스가 기계 종류에 따라 다를 때 (기계 블록 엔티티) 거짓을 돌려준다. */
	default boolean takesHighPressureSteam() {
		return true;
	}
}
