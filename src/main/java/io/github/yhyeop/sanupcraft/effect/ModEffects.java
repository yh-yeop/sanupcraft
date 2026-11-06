package io.github.yhyeop.sanupcraft.effect;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 상태 이상 3종 (확장 3 C3). 보스 · 몹 · 무기가 {@link #inflict}로 건다.
 * <ul>
 *   <li>녹: 방어 −{@link #RUST_ARMOR_PER_LEVEL}/단계. 강철 한 벌이면 면역.</li>
 *   <li>과열: 1초마다 피해 1/단계 ({@link OverheatEffect}). 물 · 비 · 가루눈에 닿으면 풀린다. 내열 한 벌이면 막는다.</li>
 *   <li>공허 침식: 최대 체력 −{@link #EROSION_HEALTH_PER_LEVEL}/단계. 공허 한 벌이면 막는다.</li>
 * </ul>
 * 막는 장비 판정은 {@link Resistances}.
 */
public final class ModEffects {
	public static final double RUST_ARMOR_PER_LEVEL = 2.0;
	public static final double EROSION_HEALTH_PER_LEVEL = 2.0;
	/** 과열 피해 종류 ({@code data/sanupcraft/damage_type/overheat.json}). */
	public static final ResourceKey<DamageType> OVERHEAT_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Sanupcraft.id("overheat"));

	public static final Holder<MobEffect> RUST = register("rust", new SanupcraftEffect(0xA85628, Resistances::rustImmune)
			.addAttributeModifier(Attributes.ARMOR, Sanupcraft.id("effect.rust"), -RUST_ARMOR_PER_LEVEL, AttributeModifier.Operation.ADD_VALUE));
	public static final Holder<MobEffect> OVERHEAT = register("overheat", new OverheatEffect(0xFF6A1F, Resistances::heatResistant));
	public static final Holder<MobEffect> VOID_EROSION = register("void_erosion", new SanupcraftEffect(0x73479E, Resistances::voidResistant)
			.addAttributeModifier(Attributes.MAX_HEALTH, Sanupcraft.id("effect.void_erosion"), -EROSION_HEALTH_PER_LEVEL, AttributeModifier.Operation.ADD_VALUE));

	private ModEffects() {
	}

	/** 클래스를 불러 등록을 끝낸다. */
	public static void init() {
	}

	/**
	 * 효과를 건다. 막는 장비를 입었거나 단계 · 시간이 0 이하면 걸지 않는다.
	 * @param level 1부터 (2 = II)
	 * @return 걸었으면 true
	 */
	public static boolean inflict(LivingEntity target, Holder<MobEffect> effect, int ticks, int level, @Nullable Entity source) {
		if (ticks <= 0 || level <= 0 || Resistances.resists(target, effect)) {
			return false;
		}
		return target.addEffect(new MobEffectInstance(effect, ticks, level - 1), source);
	}

	private static Holder<MobEffect> register(String name, MobEffect effect) {
		return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Sanupcraft.id(name), effect);
	}
}
