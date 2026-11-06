package io.github.yhyeop.sanupcraft.client.boss;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class FurnaceWardenRenderState extends LivingEntityRenderState {
	/** 내려치기 남은 틱 (10 → 0). */
	public float attackTicksRemaining;
	/** 증기 분출 예고 동작 (0 → 1). 0이면 예고 중이 아님. */
	public float roarAnimation;
	/** 체력 절반 아래: 화구가 더 세게 일렁인다. */
	public boolean enraged;
}
