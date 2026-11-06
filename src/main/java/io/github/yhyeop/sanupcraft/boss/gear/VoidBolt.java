package io.github.yhyeop.sanupcraft.boss.gear;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import io.github.yhyeop.sanupcraft.effect.ModEffects;

/** 공허 지팡이의 탄: 중력 없이 곧게 날아 맞은 것에 피해 {@link #DAMAGE} + 공허 침식 I (10초). {@link #MAX_AGE}틱이 지나면 사라진다. */
public class VoidBolt extends ThrowableItemProjectile {
	public static final float DAMAGE = 6.0F;
	public static final float SPEED = 2.0F;
	public static final int MAX_AGE = 40;
	public static final int EROSION_TICKS = 200;
	public static final int EROSION_LEVEL = 1;

	public VoidBolt(EntityType<? extends VoidBolt> type, Level level) {
		super(type, level);
	}

	public VoidBolt(Level level, LivingEntity owner) {
		super(ModBossGear.VOID_BOLT_ENTITY, owner, level, new ItemStack(ModBossGear.VOID_BOLT));
	}

	@Override
	protected Item getDefaultItem() {
		return ModBossGear.VOID_BOLT;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			level().addParticle(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.1, getZ(), 0.0, 0.0, 0.0);
		} else if (tickCount > MAX_AGE) {
			discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		Entity target = hitResult.getEntity();
		if (level() instanceof ServerLevel serverLevel
				&& target.hurtServer(serverLevel, damageSources().thrown(this, getOwner()), DAMAGE)
				&& target instanceof LivingEntity living) {
			ModEffects.inflict(living, ModEffects.VOID_EROSION, EROSION_TICKS, EROSION_LEVEL, getOwner());
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!level().isClientSide()) {
			level().broadcastEntityEvent(this, EntityEvent.DEATH);
			discard();
		}
	}

	/** 맞은 자리에 공허 입자. */
	@Override
	public void handleEntityEvent(@EntityEvent.Value byte id) {
		if (id == EntityEvent.DEATH) {
			for (int i = 0; i < 12; i++) {
				level().addParticle(ParticleTypes.PORTAL, getX(), getY(), getZ(),
						(random.nextDouble() - 0.5) * 1.5, random.nextDouble() - 0.5, (random.nextDouble() - 0.5) * 1.5);
			}
		}
	}
}
