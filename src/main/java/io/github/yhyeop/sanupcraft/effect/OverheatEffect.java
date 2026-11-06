package io.github.yhyeop.sanupcraft.effect;

import java.util.function.Predicate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 과열: {@link #INTERVAL}틱마다 단계당 피해 {@link #DAMAGE_PER_LEVEL} (피해 종류 {@code sanupcraft:overheat}, 불 피해라 화염 저항이 막는다).
 * 물 · 비 · 가루눈에 닿으면 치익 소리와 함께 풀린다.
 */
public class OverheatEffect extends SanupcraftEffect {
	public static final int INTERVAL = 20;
	public static final float DAMAGE_PER_LEVEL = 1.0F;

	public OverheatEffect(int color, Predicate<LivingEntity> resists) {
		super(color, resists);
	}

	/** 물 · 비 · 가루눈에 닿았나. */
	public static boolean cooled(LivingEntity mob) {
		return mob.isInWaterOrRain() || mob.isInPowderSnow || mob.wasInPowderSnow;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
		if (!super.applyEffectTick(level, mob, amplifier)) {
			return false;
		}
		if (cooled(mob)) {
			mob.playSound(SoundEvents.FIRE_EXTINGUISH, 0.7F, 1.6F);
			return false;
		}
		MobEffectInstance instance = mob.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this));
		int clock = instance == null || instance.isInfiniteDuration() ? mob.tickCount : instance.getDuration();
		if (clock % INTERVAL == 0) {
			mob.hurtServer(level, mob.damageSources().source(ModEffects.OVERHEAT_DAMAGE), DAMAGE_PER_LEVEL * (amplifier + 1));
		}
		return true;
	}
}
