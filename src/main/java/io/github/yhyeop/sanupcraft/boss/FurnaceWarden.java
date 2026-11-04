package io.github.yhyeop.sanupcraft.boss;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.gear.HeatproofGear;

/**
 * 용광로 파수꾼. 두 번째 보스. 패턴 3개를 돌아가며 쓴다.
 * <ul>
 *   <li>증기 분출: 20틱 동안 포효하며 입에 증기가 모인다 → 앞쪽 부채꼴(7칸, 좌우 35도) 피해. 옆이나 뒤로 돌면 된다.</li>
 *   <li>화염 장판: 주변 플레이어 발밑(최대 3곳)에 25틱 동안 불꽃 고리 → 반경 2.5칸 폭발. 고리 밖으로 나가면 된다.</li>
 *   <li>도약 내려찍기: 15틱 웅크렸다가 대상에게 뛰어올라 착지하면 반경 4칸 피해 + 띄우기.</li>
 * </ul>
 * 과열: 내열 갑옷 한 벌을 다 입지 않은 대상은 피해 1.5배 + 불 붙음. 내열 검으로 때리면 피해 1.5배.
 * 불 · 낙하 피해를 받지 않는다. 체력이 절반 아래면 패턴 간격이 짧아진다.
 */
public class FurnaceWarden extends Monster {
	public static final double MAX_HEALTH = 450.0;
	public static final float HEAT_MULTIPLIER = 1.5F;
	public static final float HEATPROOF_SWORD_MULTIPLIER = 1.5F;
	public static final int HEAT_FIRE_TICKS = 100;
	public static final float BLAST_DAMAGE = 10.0F;
	public static final double BLAST_RANGE = 7.0;
	public static final double BLAST_HALF_ANGLE = Math.toRadians(35.0);
	public static final float RING_DAMAGE = 8.0F;
	public static final double RING_RADIUS = 2.5;
	public static final float LEAP_DAMAGE = 11.0F;
	public static final double LEAP_RADIUS = 4.0;
	public static final byte EVENT_ATTACK = 4;
	public static final byte EVENT_ROAR = 61;

	private static final int BLAST_WINDUP = 20;
	private static final int BLAST_END = 30;
	private static final int RING_WINDUP = 25;
	private static final int RING_END = 35;
	private static final int MAX_RINGS = 3;
	private static final int LEAP_CROUCH = 15;
	private static final int LEAP_TIMEOUT = 70;
	private static final int COOLDOWN = 70;
	private static final int COOLDOWN_ENRAGED = 40;

	public enum Pattern {
		NONE, STEAM_BLAST, FIRE_RINGS, LEAP
	}

	private static final Pattern[] CYCLE = {Pattern.STEAM_BLAST, Pattern.LEAP, Pattern.FIRE_RINGS, Pattern.STEAM_BLAST, Pattern.FIRE_RINGS, Pattern.LEAP};

	private final ServerBossEvent bossEvent = new ServerBossEvent(
			Mth.createInsecureUUID(this.random), this.getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
	private Pattern pattern = Pattern.NONE;
	private int patternTick;
	private int cooldown = 60;
	private int cycleIndex;
	private final List<Vec3> rings = new ArrayList<>();
	private boolean airborne;
	private int attackTick;
	private int roarTick;

	public FurnaceWarden(EntityType<? extends FurnaceWarden> type, Level level) {
		super(type, level);
		this.xpReward = 80;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, MAX_HEALTH)
				.add(Attributes.ATTACK_DAMAGE, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ARMOR, 10.0)
				.add(Attributes.ARMOR_TOUGHNESS, 2.0)
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
			case STEAM_BLAST -> {
				level.broadcastEntityEvent(this, EVENT_ROAR);
				playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.6F);
			}
			case FIRE_RINGS -> {
				rings.clear();
				for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(20.0), p -> !p.isSpectator() && !p.isCreative())) {
					if (rings.size() < MAX_RINGS) {
						rings.add(player.position());
					}
				}
				if (rings.isEmpty() && getTarget() != null) {
					rings.add(getTarget().position());
				}
				playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.6F);
			}
			case LEAP -> {
				airborne = false;
				playSound(SoundEvents.RAVAGER_STEP, 2.0F, 0.5F);
			}
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
			case STEAM_BLAST -> tickBlast(level);
			case FIRE_RINGS -> tickRings(level);
			case LEAP -> tickLeap(level);
			case NONE -> {
			}
		}
	}

	private void endPattern() {
		pattern = Pattern.NONE;
		patternTick = 0;
		cooldown = isEnraged() ? COOLDOWN_ENRAGED : COOLDOWN;
	}

	private void tickBlast(ServerLevel level) {
		LivingEntity target = getTarget();
		if (patternTick < BLAST_WINDUP) {
			if (target != null) {
				getLookControl().setLookAt(target, 20.0F, 20.0F);
			}
			Vec3 mouth = mouth();
			level.sendParticles(ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z, 3, 0.3, 0.2, 0.3, 0.01);
			return;
		}
		if (patternTick == BLAST_WINDUP) {
			steamBlast(level);
			return;
		}
		if (patternTick >= BLAST_END) {
			endPattern();
		}
	}

	/** 증기 분출 판정: 바라보는 방향 부채꼴 안 플레이어. 맞은 수를 돌려준다. */
	public int steamBlast(ServerLevel level) {
		setYBodyRot(getYHeadRot());
		setYRot(getYHeadRot());
		Vec3 forward = Vec3.directionFromRotation(0.0F, getYHeadRot());
		Vec3 mouth = mouth();
		for (int i = 1; i <= 7; i++) {
			Vec3 p = mouth.add(forward.scale(i));
			level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 6, i * 0.12, 0.25, i * 0.12, 0.02);
			level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 3, i * 0.1, 0.2, i * 0.1, 0.01);
		}
		playSound(SoundEvents.FIRE_EXTINGUISH, 2.0F, 0.5F);
		int hits = 0;
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(BLAST_RANGE, 2.0, BLAST_RANGE))) {
			Vec3 to = player.position().subtract(position());
			Vec3 flat = new Vec3(to.x, 0.0, to.z);
			if (flat.length() > BLAST_RANGE + getBbWidth() / 2 || flat.lengthSqr() < 1.0E-4) {
				continue;
			}
			double angle = Math.acos(Mth.clamp(flat.normalize().dot(forward), -1.0, 1.0));
			if (angle <= BLAST_HALF_ANGLE) {
				hitWithHeat(level, player, BLAST_DAMAGE);
				player.push(forward.x * 0.8, 0.3, forward.z * 0.8);
				hits++;
			}
		}
		return hits;
	}

	private void tickRings(ServerLevel level) {
		if (patternTick < RING_WINDUP) {
			if (patternTick % 4 == 0) {
				for (Vec3 center : rings) {
					for (int i = 0; i < 16; i++) {
						double a = i * Math.PI * 2.0 / 16.0;
						level.sendParticles(ParticleTypes.FLAME, center.x + Math.cos(a) * RING_RADIUS, center.y + 0.1,
								center.z + Math.sin(a) * RING_RADIUS, 1, 0.0, 0.0, 0.0, 0.0);
					}
				}
			}
			return;
		}
		if (patternTick == RING_WINDUP) {
			explodeRings(level);
			return;
		}
		if (patternTick >= RING_END) {
			endPattern();
		}
	}

	/** 화염 장판 폭발. 고리 안 플레이어에게 피해. 맞은 수를 돌려준다 (한 사람은 한 번만). */
	public int explodeRings(ServerLevel level) {
		List<Player> hit = new ArrayList<>();
		for (Vec3 center : rings) {
			level.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.2, center.z, 12, RING_RADIUS / 2, 0.1, RING_RADIUS / 2, 0.0);
			level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.3, center.z, 30, RING_RADIUS / 2, 0.3, RING_RADIUS / 2, 0.05);
			for (Player player : level.getEntitiesOfClass(Player.class, new AABB(center, center).inflate(RING_RADIUS, 2.0, RING_RADIUS))) {
				if (!hit.contains(player) && horizontalDistance(player.position(), center) <= RING_RADIUS) {
					hitWithHeat(level, player, RING_DAMAGE);
					player.push(0.0, 0.5, 0.0);
					hit.add(player);
				}
			}
		}
		playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.8F);
		return hit.size();
	}

	/** 화염 장판 위치를 직접 정한다 (테스트용). */
	public void setRings(List<Vec3> centers) {
		rings.clear();
		rings.addAll(centers);
	}

	private void tickLeap(ServerLevel level) {
		LivingEntity target = getTarget();
		if (patternTick < LEAP_CROUCH) {
			if (target != null) {
				getLookControl().setLookAt(target, 30.0F, 30.0F);
			}
			level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 4, 0.8, 0.1, 0.8, 0.01);
			return;
		}
		if (patternTick == LEAP_CROUCH) {
			Vec3 to = target != null ? target.position().subtract(position()) : getLookAngle().scale(6.0);
			Vec3 flat = new Vec3(to.x, 0.0, to.z);
			double distance = Math.min(flat.length(), 12.0);
			Vec3 push = distance > 0.01 ? flat.normalize().scale(distance * 0.075) : Vec3.ZERO;
			setDeltaMovement(push.x, 1.0, push.z);
			airborne = true;
			playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 1.2F);
			return;
		}
		if (airborne && patternTick > LEAP_CROUCH + 5 && onGround()) {
			airborne = false;
			leapSlam(level);
			endPattern();
			return;
		}
		if (patternTick >= LEAP_TIMEOUT) {
			endPattern();
		}
	}

	/** 착지 판정: 반경 안 플레이어에게 피해 + 띄우기. 맞은 수를 돌려준다. */
	public int leapSlam(ServerLevel level) {
		swingHead(level);
		playSound(SoundEvents.ANVIL_LAND, 2.0F, 0.4F);
		level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 6, LEAP_RADIUS / 2, 0.1, LEAP_RADIUS / 2, 0.0);
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.2, getZ(), 40, LEAP_RADIUS / 2, 0.2, LEAP_RADIUS / 2, 0.05);
		int hits = 0;
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(LEAP_RADIUS, 2.0, LEAP_RADIUS))) {
			if (horizontalDistance(player.position(), position()) > LEAP_RADIUS + getBbWidth() / 2) {
				continue;
			}
			hitWithHeat(level, player, LEAP_DAMAGE);
			Vec3 away = player.position().subtract(position());
			Vec3 push = new Vec3(away.x, 0.0, away.z).normalize().scale(0.9);
			player.push(push.x, 0.7, push.z);
			hits++;
		}
		return hits;
	}

	/** 과열: 내열 갑옷 한 벌이 없으면 더 아프고 불이 붙는다. */
	public boolean hitWithHeat(ServerLevel level, LivingEntity target, float damage) {
		boolean heatproof = HeatproofGear.wearsFullSet(target);
		boolean hurt = target.hurtServer(level, damageSources().mobAttack(this), heatproof ? damage : damage * HEAT_MULTIPLIER);
		if (hurt && !heatproof) {
			target.igniteForTicks(HEAT_FIRE_TICKS);
		}
		return hurt;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		swingHead(level);
		float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
		boolean hurt = target instanceof LivingEntity living
				? hitWithHeat(level, living, damage)
				: target.hurtServer(level, damageSources().mobAttack(this), damage);
		playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 0.7F);
		return hurt;
	}

	private void swingHead(ServerLevel level) {
		attackTick = 10;
		level.broadcastEntityEvent(this, EVENT_ATTACK);
	}

	private Vec3 mouth() {
		Vec3 forward = Vec3.directionFromRotation(0.0F, getYHeadRot());
		return position().add(forward.scale(getBbWidth() * 0.7)).add(0.0, getBbHeight() * 0.55, 0.0);
	}

	private static double horizontalDistance(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_DROWNING)) {
			return false;
		}
		if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
				&& attacker.getMainHandItem().is(HeatproofGear.HEATPROOF_SWORD)) {
			damage *= HEATPROOF_SWORD_MULTIPLIER;
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt) {
			playSound(SoundEvents.RAVAGER_HURT, 1.0F, 0.6F);
		}
		return hurt;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == EVENT_ATTACK) {
			attackTick = 10;
		} else if (id == EVENT_ROAR) {
			roarTick = BLAST_WINDUP;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (attackTick > 0) {
			attackTick--;
		}
		if (roarTick > 0) {
			roarTick--;
		}
		if (level().isClientSide() && random.nextInt(3) == 0) {
			level().addParticle(random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SMOKE,
					getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0.0, 0.02, 0.0);
		}
	}

	public int getAttackTick() {
		return attackTick;
	}

	public int getRoarTick() {
		return roarTick;
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
