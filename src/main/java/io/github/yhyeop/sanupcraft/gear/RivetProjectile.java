package io.github.yhyeop.sanupcraft.gear;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/** 리벳총이 쏘는 리벳. 눈덩이처럼 날지만 훨씬 빠르고 거의 곧게 간다. 맞은 것에 피해를 주고 사라진다. */
public class RivetProjectile extends ThrowableItemProjectile {
	/** 이 틱이 지나면 (맞지 않아도) 사라진다. */
	public static final int MAX_AGE = 40;
	/** 쏘는 속도 (틱당 칸). 화살(최대 3)보다 조금 빠르다. */
	public static final float SPEED = 3.5F;

	public RivetProjectile(EntityType<? extends RivetProjectile> type, Level level) {
		super(type, level);
	}

	public RivetProjectile(Level level, LivingEntity owner) {
		super(SteamGear.RIVET_ENTITY, owner, level, new ItemStack(SteamGear.RIVET));
	}

	@Override
	protected Item getDefaultItem() {
		return SteamGear.RIVET;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.01;
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && tickCount > MAX_AGE) {
			discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		Entity target = hitResult.getEntity();
		if (level() instanceof ServerLevel serverLevel) {
			target.hurtServer(serverLevel, damageSources().thrown(this, getOwner()), (float) SanupcraftConfig.current().rivetGunDamage);
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

	/** 맞은 자리에 쇠 부스러기. */
	@Override
	public void handleEntityEvent(@EntityEvent.Value byte id) {
		if (id == EntityEvent.DEATH) {
			ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(getItem()));
			for (int i = 0; i < 6; i++) {
				level().addParticle(particle, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
			}
		}
	}
}
