package io.github.yhyeop.sanupcraft.mob;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * 잿불 정령 (확장 3 C2): 네더 현무암 삼각주 · 황무지에 떠다니는 불꽃 핵. 블레이즈처럼 천천히 떨어지고 땅에서 조금 떠 있으며,
 * 대상이 높으면 따라 오른다. 불씨({@link EmberBolt}) 셋을 연달아 쏜다 (과열 I). 불에 안 타고, 물 · 비에 닿으면 다치며,
 * 눈덩이는 {@link #SNOWBALL_DAMAGE} 피해를 준다.
 */
public class EmberWisp extends EliteMonster {
	public static final int OVERHEAT_TICKS = 100;
	public static final int OVERHEAT_LEVEL = 1;
	public static final float SNOWBALL_DAMAGE = 4.0F;
	/** 땅에서 이만큼(칸) 떠 있으려 한다. */
	private static final double HOVER_HEIGHT = 1.4;

	public EmberWisp(EntityType<? extends EmberWisp> type, Level level) {
		super(type, level);
		setPathfindingMalus(PathType.WATER, -1.0F);
		setPathfindingMalus(PathType.LAVA, 8.0F);
		setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0F);
		setPathfindingMalus(PathType.FIRE, 0.0F);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 14.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(4, new BoltAttackGoal(this));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0F));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** 대상 쪽으로 불씨 하나를 쏜다 (조금 흩어진다). */
	public EmberBolt shootAt(LivingEntity target) {
		double dx = target.getX() - getX();
		double dy = target.getY(0.5) - getY(0.5);
		double dz = target.getZ() - getZ();
		double spread = Math.sqrt(Math.sqrt(distanceToSqr(target))) * 0.3;
		Vec3 direction = new Vec3(getRandom().triangle(dx, spread), dy, getRandom().triangle(dz, spread));
		EmberBolt bolt = new EmberBolt(level(), this, direction.normalize());
		bolt.setPos(bolt.getX(), getY(0.5) + 0.3, bolt.getZ());
		level().addFreshEntity(bolt);
		playSound(MobSounds.WISP_SHOOT, 1.0F, 1.2F + random.nextFloat() * 0.2F);
		return bolt;
	}

	@Override
	public void aiStep() {
		// 천천히 떨어지고, 땅 가까이면 떠오른다
		Vec3 motion = getDeltaMovement();
		if (!onGround() && motion.y < 0.0) {
			setDeltaMovement(motion.multiply(1.0, 0.6, 1.0));
		}
		if (!level().isClientSide() && !isNoAi() && heightAboveGround() < HOVER_HEIGHT) {
			setDeltaMovement(getDeltaMovement().add(0.0, (0.12 - getDeltaMovement().y) * 0.25, 0.0));
		}
		if (level().isClientSide()) {
			if (random.nextInt(3) == 0) {
				level().addParticle(ParticleTypes.FLAME, getRandomX(0.4), getY() + 0.1 + random.nextDouble() * 0.6, getRandomZ(0.4), 0.0, 0.01, 0.0);
			}
			if (random.nextInt(4) == 0) {
				level().addParticle(ParticleTypes.SMOKE, getRandomX(0.3), getY() + 1.0, getRandomZ(0.3), 0.0, 0.02, 0.0);
			}
		}
		super.aiStep();
	}

	/** 바로 아래 단단한 블록까지 거리 (3칸까지만 본다). */
	private double heightAboveGround() {
		BlockPos.MutableBlockPos pos = blockPosition().mutable();
		for (int i = 0; i <= 3; i++) {
			if (!level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()) {
				return getY() - (pos.getY() + 1.0);
			}
			pos.move(0, -1, 0);
		}
		return 4.0;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		LivingEntity target = getTarget();
		if (target != null && target.getEyeY() > getEyeY() + 1.0 && canAttack(target)) {
			Vec3 motion = getDeltaMovement();
			setDeltaMovement(motion.add(0.0, (0.3 - motion.y) * 0.3, 0.0));
			needsSync = true;
		}
		super.customServerAiStep(level);
	}

	/** 눈덩이에 약하다 (바닐라 눈덩이는 블레이즈가 아니면 피해 0). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.getDirectEntity() instanceof Snowball) {
			damage = Math.max(damage, SNOWBALL_DAMAGE);
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean isSensitiveToWater() {
		return true;
	}

	@Override
	public float getLightLevelDependentMagicValue() {
		return 1.0F;
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return MobSounds.WISP_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return MobSounds.WISP_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return MobSounds.WISP_DEATH;
	}

	/** 블레이즈 공격과 같은 흐름: 모으기 40틱 → 불씨 셋 (8틱 간격) → 쉬기. 아주 가까우면 몸으로 부딪친다. */
	static class BoltAttackGoal extends Goal {
		private final EmberWisp wisp;
		private int attackStep;
		private int attackTime;
		private int lastSeen;

		BoltAttackGoal(EmberWisp wisp) {
			this.wisp = wisp;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = wisp.getTarget();
			return target != null && target.isAlive() && wisp.canAttack(target);
		}

		@Override
		public void start() {
			attackStep = 0;
		}

		@Override
		public void stop() {
			lastSeen = 0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			attackTime--;
			LivingEntity target = wisp.getTarget();
			if (target == null) {
				return;
			}
			boolean seen = wisp.getSensing().hasLineOfSight(target);
			lastSeen = seen ? 0 : lastSeen + 1;
			double distance = wisp.distanceToSqr(target);
			double follow = wisp.getAttributeValue(Attributes.FOLLOW_RANGE);
			if (distance < 4.0) {
				if (seen && attackTime <= 0) {
					attackTime = 20;
					wisp.doHurtTarget(getServerLevel(wisp), target);
				}
				wisp.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
			} else if (distance < follow * follow && seen) {
				if (attackTime <= 0) {
					attackStep++;
					if (attackStep == 1) {
						attackTime = 40;
					} else if (attackStep <= 4) {
						attackTime = 8;
						wisp.shootAt(target);
					} else {
						attackTime = 70;
						attackStep = 0;
					}
				}
				wisp.getLookControl().setLookAt(target, 10.0F, 10.0F);
				// 너무 붙지 않게 6칸 밖에서 맴돈다
				if (distance > 64.0) {
					wisp.getMoveControl().setWantedPosition(target.getX(), target.getY() + 1.0, target.getZ(), 1.0);
				}
			} else if (lastSeen < 5) {
				wisp.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
			}
		}
	}
}
