package io.github.yhyeop.sanupcraft.combat;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 이 패키지의 효과 바탕 (바닐라 생성자가 protected라서). */
public class CombatEffect extends MobEffect {
	public CombatEffect(MobEffectCategory category, int color) {
		super(category, color);
	}
}
