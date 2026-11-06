package io.github.yhyeop.sanupcraft.mob;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.effect.ModEffects;

/**
 * 공허 그림자 (확장 3 C2): 엔드 바깥 섬의 키 크고 마른 그림자. 대상이 멀면 ({@link #AMBUSH_MIN_DISTANCE}칸 밖)
 * {@link #AMBUSH_COOLDOWN}틱마다 대상 등 뒤로 순간이동해 기습하고, 화살 같은 발사체에 맞으면 쏜 쪽 등 뒤로 간다.
 * 엔더맨과 달리 블록은 줍지 않는다. 때리면 공허 침식 I ({@code mobs.shadeErosionTicks}). 공허 한 벌이면 막는다.
 */
public class VoidShade extends EliteMonster {
	public static final int EROSION_TICKS = 200;
	public static final int EROSION_LEVEL = 1;
	public static final int AMBUSH_COOLDOWN = 100;
	public static final double AMBUSH_MIN_DISTANCE = 5.0;
	public static final double AMBUSH_MAX_DISTANCE = 24.0;
	private int ambushCooldown = 40;

	public VoidShade(EntityType<? extends VoidShade> type, Level level) {
		super(type, level);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 24.0)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8, 0.0F));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		return hurtWithEffect(level, target, ModEffects.VOID_EROSION, SanupcraftConfig.current().mobShadeErosionTicks, EROSION_LEVEL);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (ambushCooldown > 0) {
			ambushCooldown--;
		}
		LivingEntity target = getTarget();
		if (ambushCooldown <= 0 && target != null && target.isAlive()) {
			double distance = distanceTo(target);
			if (distance > AMBUSH_MIN_DISTANCE && distance < AMBUSH_MAX_DISTANCE) {
				// 실패해도 쿨다운을 걸어 매 틱 순간이동을 되풀이하지 않는다 (좁은 굴 · 대상 뒤가 막힌 경우)
				ambushCooldown = teleportBehind(target) ? AMBUSH_COOLDOWN + random.nextInt(40) : 20;
			} else if (distance <= AMBUSH_MIN_DISTANCE) {
				ambushCooldown = 20;
			}
		}
	}

	/** 발사체에 맞으면 쏜 쪽 등 뒤로 (쿨다운이 끝났을 때). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && isAlive() && ambushCooldown <= 0 && source.getDirectEntity() instanceof Projectile
				&& source.getEntity() instanceof LivingEntity shooter && teleportBehind(shooter)) {
			ambushCooldown = AMBUSH_COOLDOWN;
		}
		return hurt;
	}

	/**
	 * 대상이 보는 방향의 뒤 2칸 (안 되면 1.5 · 3칸, 옆 비스듬히) 땅 위로 순간이동하고 대상을 바라본다.
	 * @return 옮겼으면 true
	 */
	public boolean teleportBehind(LivingEntity target) {
		if (isPassenger() || !isAlive()) {
			return false;
		}
		float yaw = target.getYRot() * Mth.DEG_TO_RAD;
		Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
		Vec3 old = position();
		double[] distances = {2.0, 1.5, 3.0};
		double[] sideAngles = {0.0, 0.5, -0.5};
		for (double side : sideAngles) {
			Vec3 dir = forward.yRot((float) side);
			for (double d : distances) {
				double x = target.getX() - dir.x * d;
				double z = target.getZ() - dir.z * d;
				if (randomTeleport(x, target.getY() + 1.0, z, false, BlockTags.ENDERMAN_DOES_NOT_TELEPORT_TO)) {
					afterTeleport(old, target);
					return true;
				}
			}
		}
		return false;
	}

	private void afterTeleport(Vec3 old, @Nullable LivingEntity target) {
		Level level = level();
		if (target != null) {
			lookAt(target, 180.0F, 90.0F);
			setYHeadRot(getYRot());
			yBodyRot = getYRot();
		}
		BlockPos oldBlock = BlockPos.containing(old);
		level.levelEvent(2018, oldBlock, BlockUtil.clampedPackDifferenceInPosition(oldBlock, blockPosition(), 127, 127, 127));
		if (!isSilent()) {
			level.playSound(null, old.x, old.y, old.z, MobSounds.SHADE_TELEPORT, getSoundSource(), 1.0F, 1.0F);
			playSound(MobSounds.SHADE_TELEPORT, 1.0F, 1.0F);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// 그림자가 흩날린다: 발치에 보라 입자, 몸에서 옅은 연기
		if (level().isClientSide()) {
			if (random.nextInt(2) == 0) {
				level().addParticle(ParticleTypes.REVERSE_PORTAL, getRandomX(0.4), getY() + random.nextDouble() * 0.6, getRandomZ(0.4), 0.0, 0.02, 0.0);
			}
			if (random.nextInt(4) == 0) {
				level().addParticle(ParticleTypes.SMOKE, getRandomX(0.35), getY() + 0.2 + random.nextDouble() * 1.8, getRandomZ(0.35), 0.0, 0.01, 0.0);
			}
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return MobSounds.SHADE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return MobSounds.SHADE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return MobSounds.SHADE_DEATH;
	}
}
