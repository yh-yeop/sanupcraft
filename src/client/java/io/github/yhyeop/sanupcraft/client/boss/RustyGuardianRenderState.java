package io.github.yhyeop.sanupcraft.client.boss;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class RustyGuardianRenderState extends LivingEntityRenderState {
	/** 휘두르기 남은 틱 (10 → 0). */
	public float attackTicksRemaining;
	/** 체력 절반 아래: 눈이 더 빨리 깜박인다. */
	public boolean enraged;
}
