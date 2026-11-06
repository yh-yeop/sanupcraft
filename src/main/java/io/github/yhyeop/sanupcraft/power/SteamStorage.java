package io.github.yhyeop.sanupcraft.power;

/** 증기를 모아 두는 블록 엔티티. 증기 계기판이 읽는다. */
public interface SteamStorage {
	/** 지금 모아 둔 증기. */
	int steamStored();

	/** 모아 둘 수 있는 최대 증기. */
	int steamCapacity();

	/** 최근 1초(20틱) 동안 들어온 (만들어진) 증기 양. */
	int steamReceivedLastSecond();
}
