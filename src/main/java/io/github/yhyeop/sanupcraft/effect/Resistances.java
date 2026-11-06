package io.github.yhyeop.sanupcraft.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import io.github.yhyeop.sanupcraft.end.VoidGear;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;

/**
 * 상태 이상(C3)을 막는 장비 판정. 판정은 여기 한 곳에서만 한다:
 * 효과가 걸릴 때({@link ModEffects#inflict}), 걸린 동안 매 틱({@link SanupcraftEffect}), 보스 패턴의 피해 배율이 모두 이 함수를 본다.
 * 크롬강 한 벌(G2)은 강철 한 벌 판정에 들어 있다. 나중에 방지 모듈(G3)도 여기에 더한다.
 */
public final class Resistances {
	private Resistances() {
	}

	/** 녹 면역: 강철 한 벌 (크롬강 · 내열로 올려 입어도, 신발 자리 증기 부츠도 인정, {@link SteelGear#wearsFullSet}). 크롬강 한 벌(G2)도 여기로 들어온다. */
	public static boolean rustImmune(LivingEntity entity) {
		return SteelGear.wearsFullSet(entity);
	}

	/** 과열 저항: 내열 한 벌. 과열이 걸리지 않고 피해도 없다 (파수꾼의 피해 1.5배도 막는다). */
	public static boolean heatResistant(LivingEntity entity) {
		return HeatproofGear.wearsFullSet(entity);
	}

	/** 공허 침식 저항: 공허 한 벌. 침식이 걸리지 않는다. */
	public static boolean voidResistant(LivingEntity entity) {
		return VoidGear.wearsFullSet(entity);
	}

	/** 이 효과를 막는가. 우리 효과가 아니면 false. */
	public static boolean resists(LivingEntity entity, Holder<MobEffect> effect) {
		return effect.value() instanceof SanupcraftEffect ours && ours.resists(entity);
	}
}
