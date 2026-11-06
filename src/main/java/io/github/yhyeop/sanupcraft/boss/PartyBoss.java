package io.github.yhyeop.sanupcraft.boss;

/** 인원 규칙({@link BossParty})을 따르는 보스. 제단이 소환할 때 인원수를 넣는다. */
public interface PartyBoss {
	BossParty party();

	/** 보스 바 제목을 이름 + 강화 단계({@link BossTier#barName})로 다시 쓴다. 이름이 바뀌거나 단계가 바뀔 때 부른다. */
	void refreshBossBar();
}
