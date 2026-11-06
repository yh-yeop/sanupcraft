package io.github.yhyeop.sanupcraft.boss;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 공허 수정 (M6). 공허 거상이 둘레에 띄우는 작은 수정. 체력 {@link #MAX_HEALTH}, 리벳 · 화살 · 근접으로 부순다.
 * 살아 있는 동안 거상은 무적이고, 수정에서 거상으로 빛줄기(엔드 수정과 같은 것)가 보인다.
 * 거상 자신 · 셜커 탄의 피해는 받지 않는다. 거상이 사라지면 곧 같이 사라진다.
 */
public class VoidCrystal extends Entity {
	public static final float MAX_HEALTH = 20.0F;
	private static final int ORPHAN_TICKS = 100;
	private static final EntityDataAccessor<Optional<BlockPos>> DATA_BEAM_TARGET = SynchedEntityData.defineId(VoidCrystal.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

	/** 회전 · 출렁임 (그리기용). */
	public int time;
	private float health = MAX_HEALTH;
	private @Nullable UUID owner;
	private int orphanTicks;

	public VoidCrystal(EntityType<? extends VoidCrystal> type, Level level) {
		super(type, level);
		this.blocksBuilding = true;
		this.noPhysics = true;
		this.time = this.random.nextInt(100000);
		setNoGravity(true);
	}

	public void setOwner(VoidColossus colossus) {
		owner = colossus.getUUID();
		setBeamTarget(colossus.blockPosition().above((int) (colossus.getBbHeight() * 0.6)));
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public float health() {
		return health;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_BEAM_TARGET, Optional.empty());
	}

	public void setBeamTarget(@Nullable BlockPos target) {
		getEntityData().set(DATA_BEAM_TARGET, Optional.ofNullable(target));
	}

	public @Nullable BlockPos getBeamTarget() {
		return getEntityData().get(DATA_BEAM_TARGET).orElse(null);
	}

	@Override
	public void tick() {
		super.tick();
		time++;
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		Entity colossus = owner == null ? null : level.getEntity(owner);
		if (colossus instanceof VoidColossus boss && boss.isAlive()) {
			orphanTicks = 0;
			if (tickCount % 5 == 0) {
				setBeamTarget(boss.blockPosition().above((int) (boss.getBbHeight() * 0.6)));
			}
		} else if (++orphanTicks > ORPHAN_TICKS) {
			discard();
		}
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean isAttackable() {
		return true;
	}

	@Override
	public boolean hurtClient(DamageSource source) {
		return !isInvulnerableToBase(source) && !(source.getEntity() instanceof VoidColossus);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (isInvulnerableToBase(source) || isRemoved()) {
			return false;
		}
		if (source.getEntity() instanceof VoidColossus || source.getDirectEntity() instanceof ShulkerBullet) {
			return false;
		}
		health -= damage;
		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 6, 0.3, 0.3, 0.3, 0.05);
		if (health <= 0.0F) {
			level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 0.5, getZ(), 30, 0.5, 0.5, 0.5, 0.5);
			playSound(SoundEvents.GLASS_BREAK, 1.5F, 0.6F);
			remove(Entity.RemovalReason.KILLED);
		} else {
			playSound(SoundEvents.AMETHYST_BLOCK_HIT, 1.5F, 0.8F);
		}
		return true;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return super.shouldRenderAtSqrDistance(distance) || getBeamTarget() != null;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("health", health);
		output.storeNullable("owner", UUIDUtil.CODEC, owner);
		output.storeNullable("beam_target", BlockPos.CODEC, getBeamTarget());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		health = input.getFloatOr("health", MAX_HEALTH);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		setBeamTarget(input.read("beam_target", BlockPos.CODEC).orElse(null));
	}
}
