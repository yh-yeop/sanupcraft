package io.github.yhyeop.sanupcraft.boss;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.gear.SteelGear;

/**
 * 녹슨 수호자. 첫 마일스톤 보스. 패턴 3개를 돌아가며 쓴다.
 * <ul>
 *   <li>돌진: 20틱 동안 웅크렸다가 바라본 방향으로 돌진. 옆으로 피하면 된다.</li>
 *   <li>내려찍기: 25틱 동안 주변에 경고 입자 → 반경 5칸 피해 + 띄우기. 경고를 보고 벗어나면 된다.</li>
 *   <li>졸개 소환: 좀벌레 3마리.</li>
 * </ul>
 * 녹 공격: 강철 갑옷 한 벌을 다 입지 않은 대상은 피해 1.5배 + 구속. 강철 검으로 때리면 피해 1.5배.
 * 체력이 절반 아래면 패턴 간격이 짧아진다.
 */
public class RustyGuardian extends Monster {
	public static final double MAX_HEALTH = 300.0;
	public static final float RUST_MULTIPLIER = 1.5F;
	public static final float STEEL_SWORD_MULTIPLIER = 1.5F;
	public static final float CHARGE_DAMAGE = 12.0F;
	public static final float SLAM_DAMAGE = 10.0F;
	public static final double SLAM_RADIUS = 5.0;
	public static final int MINIONS_PER_SUMMON = 3;
	private static final int MAX_MINIONS_NEARBY = 8;

	private static final int CHARGE_WINDUP = 20;
	private static final int CHARGE_END = 36;
	private static final int SLAM_WINDUP = 25;
	private static final int SLAM_END = 35;
	private static final int SUMMON_AT = 10;
	private static final int SUMMON_END = 20;
	private static final int COOLDOWN = 80;
	private static final int COOLDOWN_ENRAGED = 45;

	public enum Pattern {
		NONE, CHARGE, SLAM, SUMMON
	}

	private static final Pattern[] CYCLE = {Pattern.CHARGE, Pattern.SLAM, Pattern.SUMMON, Pattern.SLAM};

	private final ServerBossEvent bossEvent = new ServerBossEvent(
			Mth.createInsecureUUID(this.random), this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
	private Pattern pattern = Pattern.NONE;
	private int patternTick;
	private int cooldown = 60;
	private int cycleIndex;
	private Vec3 chargeDirection = Vec3.ZERO;
	private final Set<UUID> hitThisCharge = new HashSet<>();
	private int attackAnimationTick;

	public RustyGuardian(EntityType<? extends RustyGuardian> type, Level level) {
		super(type, level);
		this.xpReward = 50;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, MAX_HEALTH)
				.add(Attributes.ATTACK_DAMAGE, 9.0)
				.add(Attributes.MOVEMENT_SPEED, 0.26)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 40.0)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PatternGoal());
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public Pattern pattern() {
		return pattern;
	}

	public boolean isEnraged() {
		return getHealth() < getMaxHealth() / 2.0F;
	}

	/** 다음 패턴을 바로 시작한다 (테스트 · 디버그용). */
	public void startPattern(ServerLevel level, Pattern next) {
		pattern = next;
		patternTick = 0;
		getNavigation().stop();
		switch (next) {
			case CHARGE -> playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 0.7F);
			case SLAM -> playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.5F, 0.5F);
			case SUMMON -> playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.8F);
			case NONE -> {
			}
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		bossEvent.setProgress(getHealth() / getMaxHealth());

		if (pattern == Pattern.NONE) {
			LivingEntity target = getTarget();
			if (cooldown > 0) {
				cooldown--;
			} else if (target != null && target.isAlive()) {
				startPattern(level, CYCLE[cycleIndex++ % CYCLE.length]);
			}
			return;
		}

		patternTick++;
		switch (pattern) {
			case CHARGE -> tickCharge(level);
			case SLAM -> tickSlam(level);
			case SUMMON -> tickSummon(level);
			case NONE -> {
			}
		}
	}

	private void endPattern() {
		pattern = Pattern.NONE;
		patternTick = 0;
		cooldown = isEnraged() ? COOLDOWN_ENRAGED : COOLDOWN;
	}

	private void tickCharge(ServerLevel level) {
		LivingEntity target = getTarget();
		if (patternTick < CHARGE_WINDUP) {
			if (target != null) {
				getLookControl().setLookAt(target, 30.0F, 30.0F);
			}
			level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 4, 0.6, 0.1, 0.6, 0.02);
			return;
		}
		if (patternTick == CHARGE_WINDUP) {
			Vec3 aim = target != null ? target.position().subtract(position()) : getLookAngle();
			chargeDirection = new Vec3(aim.x, 0.0, aim.z).normalize();
			hitThisCharge.clear();
		}
		if (patternTick < CHARGE_END) {
			setDeltaMovement(chargeDirection.x * 0.9, getDeltaMovement().y, chargeDirection.z * 0.9);
			setYRot((float) (Mth.atan2(chargeDirection.z, chargeDirection.x) * Mth.RAD_TO_DEG) - 90.0F);
			for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.6))) {
				if (hitThisCharge.add(player.getUUID())) {
					hitWithRust(level, player, CHARGE_DAMAGE);
					player.push(chargeDirection.x * 1.2, 0.4, chargeDirection.z * 1.2);
				}
			}
			return;
		}
		endPattern();
	}

	private void tickSlam(ServerLevel level) {
		if (patternTick < SLAM_WINDUP) {
			if (patternTick % 5 == 0) {
				for (int i = 0; i < 24; i++) {
					double angle = i * Math.PI * 2.0 / 24.0;
					level.sendParticles(ParticleTypes.CRIT, getX() + Math.cos(angle) * SLAM_RADIUS, getY() + 0.1,
							getZ() + Math.sin(angle) * SLAM_RADIUS, 1, 0.0, 0.0, 0.0, 0.0);
				}
			}
			if (patternTick == SLAM_WINDUP - 10) {
				swingArms(level);
			}
			return;
		}
		if (patternTick == SLAM_WINDUP) {
			slam(level);
			return;
		}
		if (patternTick >= SLAM_END) {
			endPattern();
		}
	}

	/** 내려찍기 판정. 반경 안 플레이어에게 피해 + 띄우기. 맞은 수를 돌려준다. */
	public int slam(ServerLevel level) {
		playSound(SoundEvents.ANVIL_LAND, 2.0F, 0.5F);
		level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 6, SLAM_RADIUS / 2, 0.1, SLAM_RADIUS / 2, 0.0);
		int hits = 0;
		List<Player> players = level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(SLAM_RADIUS, 2.0, SLAM_RADIUS));
		for (Player player : players) {
			if (player.distanceTo(this) > SLAM_RADIUS + getBbWidth() / 2) {
				continue;
			}
			hitWithRust(level, player, SLAM_DAMAGE);
			Vec3 away = player.position().subtract(position());
			Vec3 push = new Vec3(away.x, 0.0, away.z).normalize().scale(0.8);
			player.push(push.x, 0.7, push.z);
			hits++;
		}
		return hits;
	}

	private void tickSummon(ServerLevel level) {
		if (patternTick == SUMMON_AT) {
			summonMinions(level);
		}
		if (patternTick >= SUMMON_END) {
			endPattern();
		}
	}

	/** 졸개 소환. 주변에 이미 많으면 덜 부른다. 부른 수를 돌려준다. */
	public int summonMinions(ServerLevel level) {
		int nearby = level.getEntitiesOfClass(Silverfish.class, getBoundingBox().inflate(16.0)).size();
		int count = Math.min(MINIONS_PER_SUMMON, MAX_MINIONS_NEARBY - nearby);
		int summoned = 0;
		for (int i = 0; i < count; i++) {
			Silverfish minion = EntityTypes.SILVERFISH.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (minion == null) {
				continue;
			}
			double angle = random.nextDouble() * Math.PI * 2.0;
			minion.snapTo(getX() + Math.cos(angle) * 2.0, getY(), getZ() + Math.sin(angle) * 2.0, random.nextFloat() * 360.0F, 0.0F);
			if (getTarget() != null) {
				minion.setTarget(getTarget());
			}
			level.addFreshEntity(minion);
			level.sendParticles(ParticleTypes.POOF, minion.getX(), minion.getY() + 0.3, minion.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
			summoned++;
		}
		return summoned;
	}

	/** 녹 공격: 강철 갑옷 한 벌이 없으면 피해가 커지고 느려진다. */
	public boolean hitWithRust(ServerLevel level, LivingEntity target, float damage) {
		boolean rustproof = SteelGear.wearsFullSet(target);
		boolean hurt = target.hurtServer(level, damageSources().mobAttack(this), rustproof ? damage : damage * RUST_MULTIPLIER);
		if (hurt && !rustproof) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
		}
		return hurt;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		swingArms(level);
		float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
		boolean hurt = target instanceof LivingEntity living
				? hitWithRust(level, living, damage)
				: target.hurtServer(level, damageSources().mobAttack(this), damage);
		if (hurt) {
			target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.4, 0.0));
		}
		playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.7F);
		return hurt;
	}

	private void swingArms(ServerLevel level) {
		attackAnimationTick = 10;
		level.broadcastEntityEvent(this, (byte) 4);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING)) {
			return false;
		}
		if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
				&& attacker.getMainHandItem().is(SteelGear.STEEL_SWORD)) {
			damage *= STEEL_SWORD_MULTIPLIER;
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt) {
			playSound(SoundEvents.IRON_GOLEM_DAMAGE, 1.0F, 0.6F);
		}
		return hurt;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 4) {
			attackAnimationTick = 10;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (attackAnimationTick > 0) {
			attackAnimationTick--;
		}
	}

	public int getAttackAnimationTick() {
		return attackAnimationTick;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	/** 패턴 중에는 걷기 · 근접 공격을 멈춘다. */
	private class PatternGoal extends Goal {
		PatternGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return pattern != Pattern.NONE;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}
	}
}
