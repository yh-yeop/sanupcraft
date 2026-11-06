package io.github.yhyeop.sanupcraft.client.boss;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import io.github.yhyeop.sanupcraft.boss.PartyBossMonster;

/** 세 보스 렌더 상태의 공통 몫: 공격 동작 남은 틱 · 화남. */
public class BossRenderState extends LivingEntityRenderState {
	/** 공격 동작(휘두르기 · 내려치기) 남은 틱 (10 → 0). 0이면 공격 중이 아님. */
	public float attackTicksRemaining;
	/** 체력 절반 아래: 빛이 더 세게 · 빨리 맥박친다. */
	public boolean enraged;

	/** 공격 진행 (0 → 1). 공격 중일 때만 뜻이 있다. */
	public float attackProgress() {
		return 1.0F - attackTicksRemaining / PartyBossMonster.ATTACK_ANIMATION_TICKS;
	}

	/** 팔 들기 정도: 앞 40% 동안 0 → 1로 들고, 다음 30% 동안 1 → 0으로 내린다. */
	public float attackRaise() {
		float p = attackProgress();
		return p < 0.4F ? p / 0.4F : Math.max(0.0F, 1.0F - (p - 0.4F) / 0.3F);
	}

	/** 렌더러 {@code extractRenderState}의 공통 몫. */
	public static void extractCommon(PartyBossMonster boss, BossRenderState state, float partialTicks) {
		state.attackTicksRemaining = boss.getAttackTick() > 0 ? boss.getAttackTick() - partialTicks : 0.0F;
		state.enraged = boss.isEnraged();
	}
}
