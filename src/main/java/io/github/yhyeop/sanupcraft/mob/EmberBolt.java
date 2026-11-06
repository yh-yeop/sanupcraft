package io.github.yhyeop.sanupcraft.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.effect.ModEffects;

/**
 * 잿불 정령의 불씨 (작은 화염구 모양). 맞으면 피해 {@link #DAMAGE} (화염구 피해), 피해가 들어가면 과열 I ({@code mobs.wispOverheatTicks}),
 * 불은 붙이지 않고 블록에도 불을 놓지 않는다. 과열을 막는 장비는 {@link ModEffects#inflict}가 본다.
 */
public class EmberBolt extends Fireball {
	public static final float DAMAGE = 4.0F;

	public EmberBolt(EntityType<? extends EmberBolt> type, Level level) {
		super(type, level);
	}

	public EmberBolt(Level level, LivingEntity owner, Vec3 direction) {
		super(ModMobs.EMBER_BOLT, owner, direction, level);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (level() instanceof ServerLevel serverLevel) {
			Entity target = hitResult.getEntity();
			DamageSource source = damageSources().fireball(this, getOwner());
			if (target.hurtServer(serverLevel, source, DAMAGE)) {
				EnchantmentHelper.doPostAttackEffects(serverLevel, target, source);
				if (target instanceof LivingEntity living) {
					ModEffects.inflict(living, ModEffects.OVERHEAT, SanupcraftConfig.current().mobWispOverheatTicks, EmberWisp.OVERHEAT_LEVEL, getOwner());
				}
			}
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!level().isClientSide()) {
			discard();
		}
	}
}
