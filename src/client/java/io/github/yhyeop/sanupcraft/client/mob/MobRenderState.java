package io.github.yhyeop.sanupcraft.client.mob;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** 새 몹 셋(확장 3 C2)의 렌더 상태: 정예 여부 · 능력 색, 공격 자세, 휘두르기 진행. */
public class MobRenderState extends LivingEntityRenderState {
	public boolean elite;
	/** 정예 능력 색 (RGB, 보통이면 0). 정예 빛 테두리에 쓴다. */
	public int eliteColor;
	/** 대상을 노리는 중 (자동인형은 팔을 앞으로 든다). */
	public boolean aggressive;
	/** 휘두르기 진행 0 → 1 (때릴 때만). */
	public float swing;
}
