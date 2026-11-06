package io.github.yhyeop.sanupcraft.gear.special;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 특수 장비(G5)가 거는 효과 · 속성 수정자의 도우미. 효과는 "입자 없음 · 아이콘 보임"으로 걸고,
 * 거둘 때는 그 모양(은은함 여부 · 입자 없음 · 단계 · 우리 시간 이하)이 맞는 것만 거둬서 물약 · 신호기 효과는 건드리지 않는다.
 */
final class SpecialEffects {
	private SpecialEffects() {
	}

	static MobEffectInstance effect(Holder<MobEffect> effect, int amplifier, int ticks, boolean ambient) {
		return new MobEffectInstance(effect, ticks, amplifier, ambient, false, true);
	}

	/** 효과를 건다. 이미 우리 것이 넉넉히 남았으면 그대로 둔다 (남은 시간이 {@code refreshBelow} 아래일 때만 다시). */
	static void give(LivingEntity entity, Holder<MobEffect> effect, int amplifier, int ticks, int refreshBelow, boolean ambient) {
		MobEffectInstance have = entity.getEffect(effect);
		if (have == null || isOurs(have, amplifier, ticks, ambient) && have.getDuration() < refreshBelow) {
			entity.addEffect(effect(effect, amplifier, ticks, ambient));
		}
	}

	/** 우리가 건 효과면 거둔다. */
	static void take(LivingEntity entity, Holder<MobEffect> effect, int amplifier, int ticks, boolean ambient) {
		MobEffectInstance have = entity.getEffect(effect);
		if (have != null && isOurs(have, amplifier, ticks, ambient)) {
			entity.removeEffect(effect);
		}
	}

	static boolean isOurs(MobEffectInstance have, int amplifier, int ticks, boolean ambient) {
		return have.isAmbient() == ambient && !have.isVisible() && have.showIcon() && have.getAmplifier() == amplifier
				&& have.getDuration() <= ticks;
	}

	/** 속성 수정자를 붙이거나 뗀다. 바뀔 때만 건드린다 (속성 패킷이 매 틱 나가지 않게). */
	static void modifier(LivingEntity entity, Holder<Attribute> attribute, Identifier id, boolean on, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (!on || amount == 0.0) {
			if (instance.hasModifier(id)) {
				instance.removeModifier(id);
			}
			return;
		}
		AttributeModifier modifier = new AttributeModifier(id, amount, operation);
		if (!modifier.equals(instance.getModifier(id))) {
			instance.removeModifier(id);
			instance.addTransientModifier(modifier);
		}
	}
}
