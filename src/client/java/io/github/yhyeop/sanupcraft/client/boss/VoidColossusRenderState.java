package io.github.yhyeop.sanupcraft.client.boss;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class VoidColossusRenderState extends LivingEntityRenderState {
	/** 내려치기 남은 틱 (10 → 0). */
	public float attackTicksRemaining;
	/** 끌어당기기 자세 (0 → 1). */
	public float pullAnimation;
	/** 예고가 끝나고 실제로 끌어당기는 중 (팔을 몸 쪽으로 당긴다). */
	public boolean pulling;
	/** 공허 수정 보호막 일렁임 (0 → 1). */
	public float shieldAnimation;
	/** 체력 절반 아래: 핵이 빨리 맥박친다. */
	public boolean enraged;
}
