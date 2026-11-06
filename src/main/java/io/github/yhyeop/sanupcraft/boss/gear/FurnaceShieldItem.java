package io.github.yhyeop.sanupcraft.boss.gear;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShieldItem;

import io.github.yhyeop.sanupcraft.effect.ModEffects;

/**
 * 화구 방패 (용광로 파수꾼 전용 장비, 확장 3 C4): 바닐라 방패와 같이 막고 (내구도 2배 · 불에 안 탐),
 * 근접 공격을 막으면 때린 쪽이 과열 I (5초)에 걸린다. 화살 같은 투사체를 막을 때는 아무 일 없다.
 */
public class FurnaceShieldItem extends ShieldItem {
	public static final int DURABILITY = 672;
	public static final int OVERHEAT_TICKS = 100;
	public static final int OVERHEAT_LEVEL = 1;

	public FurnaceShieldItem(Item.Properties properties) {
		super(properties);
	}

	/** 피해 뒤 (Fabric {@code AFTER_DAMAGE}): 이 방패로 근접 공격을 막았으면 때린 쪽에 과열. */
	static void afterDamage(LivingEntity defender, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		if (blocked && defender.getUseItem().is(ModBossGear.FURNACE_SHIELD)) {
			burnAttacker(defender, source);
		}
	}

	/** 근접(때린 쪽이 곧 맞힌 것)이면 때린 쪽에 과열을 건다. 걸었으면 true. */
	public static boolean burnAttacker(LivingEntity defender, DamageSource source) {
		if (!(source.getDirectEntity() instanceof LivingEntity attacker) || source.getEntity() != attacker || attacker == defender) {
			return false;
		}
		boolean burned = ModEffects.inflict(attacker, ModEffects.OVERHEAT, OVERHEAT_TICKS, OVERHEAT_LEVEL, defender);
		if (burned && defender.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.FLAME, attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.6, attacker.getZ(),
					12, 0.3, 0.4, 0.3, 0.02);
		}
		return burned;
	}
}
