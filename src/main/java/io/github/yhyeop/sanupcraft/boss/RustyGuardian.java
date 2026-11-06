package io.github.yhyeop.sanupcraft.boss;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.boss.gear.ModBossGear;
import io.github.yhyeop.sanupcraft.boss.phase.BossHazard;
import io.github.yhyeop.sanupcraft.effect.ModEffects;
import io.github.yhyeop.sanupcraft.effect.Resistances;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.sound.ModSounds;

/**
 * 녹슨 수호자. 첫 마일스톤 보스. 패턴 3개를 돌아가며 쓴다.
 * <ul>
 *   <li>돌진: 20틱 동안 웅크린다. 12틱에 대상 쪽으로 방향을 잠그고(경로가 입자로 보인다) 8틱 뒤 그 방향으로 돌진. 옆으로 피하면 된다.</li>
 *   <li>내려찍기: 25틱 동안 판정 반경과 같은 경고 고리 → 피해 + 띄우기. 경고를 보고 벗어나면 된다.</li>
 *   <li>졸개 소환: 좀벌레 3마리.</li>
 *   <li>녹 폭풍 (2페이즈, 확장 3 C4): 30틱 동안 장판 {@value #RUST_STORM_ZONES}곳(주변 플레이어 발밑부터, 모자라면 둘레 아무 데나)에 녹가루 고리 →
 *       8초 동안 녹 장판 ({@link BossHazard}, 안에 서 있으면 녹 II). 고리 밖으로 나가면 된다.</li>
 * </ul>
 * 녹 공격: 강철(또는 내열) 갑옷 한 벌을 다 입지 않은 대상은 피해 1.5배 + 구속 + 녹 II(방어 −4, 5초, 확장 3 C3). 패턴 타격은 방패를 깨고 들어간다.
 * 강철 검 · 내열 검으로 때리면 피해 1.5배. 거미줄에 걸리지 않고 구속이 안 걸린다.
 * 체력이 절반 아래면 패턴 간격이 짧아진다. 여러 명이면 체력 · 졸개 수가 늘고, 기여자 각자 핵을 받는다 ({@link BossParty}).
 */
public class RustyGuardian extends PartyBossMonster {
	public static final double MAX_HEALTH = 300.0;
	public static final float RUST_MULTIPLIER = 1.5F;
	public static final float STEEL_SWORD_MULTIPLIER = 1.5F;
	public static final float CHARGE_DAMAGE = 12.0F;
	public static final float SLAM_DAMAGE = 10.0F;
	public static final double SLAM_RADIUS = 5.0;
	public static final int MINIONS_PER_SUMMON = 3;
	/** 녹 효과 기본값 (설정 {@code statusEffect.guardianRust*}). */
	public static final int RUST_TICKS = 100;
	public static final int RUST_LEVEL = 2;
	private static final int MAX_MINIONS_NEARBY = 8;
	/** 녹 폭풍 장판 수. */
	public static final int RUST_STORM_ZONES = 5;
	public static final int RUST_STORM_WINDUP = 30;
	private static final int RUST_STORM_END = 40;

	private static final int CHARGE_LOCK = 12;
	private static final int CHARGE_WINDUP = 20;
	private static final int CHARGE_END = 36;
	private static final int SLAM_WINDUP = 25;
	private static final int SLAM_END = 35;
	private static final int SUMMON_AT = 10;
	private static final int SUMMON_END = 20;
	private static final int COOLDOWN = 80;
	private static final int COOLDOWN_ENRAGED = 45;
	private static final int WARNING_COOLDOWN = 200;

	public enum Pattern {
		NONE, CHARGE, SLAM, SUMMON, RUST_STORM
	}

	private static final Pattern[] CYCLE = {Pattern.CHARGE, Pattern.SLAM, Pattern.SUMMON, Pattern.SLAM};
	/** 2페이즈: 녹 폭풍으로 시작해 두 패턴마다 한 번. */
	private static final Pattern[] CYCLE_TWO = {Pattern.RUST_STORM, Pattern.CHARGE, Pattern.SLAM, Pattern.RUST_STORM, Pattern.SUMMON, Pattern.SLAM};

	private Pattern pattern = Pattern.NONE;
	private int patternTick;
	private int cooldown = 60;
	private int cycleIndex;
	private Vec3 chargeDirection = Vec3.ZERO;
	private final Set<UUID> hitThisCharge = new HashSet<>();
	private final List<Vec3> rustZones = new ArrayList<>();
	/** 플레이어별 녹 경고 (200틱에 한 번). */
	private final BossCombat.PlayerCooldown rustWarning = new BossCombat.PlayerCooldown(WARNING_COOLDOWN);

	public RustyGuardian(EntityType<? extends RustyGuardian> type, Level level) {
		super(type, level, BossEvent.BossBarColor.RED, 50);
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

	@Override
	protected Item keyItem() {
		return ModItems.GUARDIAN_CORE;
	}

	@Override
	protected void announcePhase(ServerLevel level) {
		ModSounds.bossPhase(level, this, ParticleTypes.CRIT); // M11 X2
	}

	@Override
	protected Item gearItem() {
		return ModBossGear.RUSTY_HAMMER;
	}

	/** 2페이즈: 다음 패턴은 녹 폭풍부터 (2페이즈 순서의 처음). */
	@Override
	protected void onPhaseTwo(ServerLevel level) {
		cycleIndex = 0;
	}

	/** 쿨다운이 끝나면 쓸 패턴 (시험용). */
	public Pattern upcomingPattern() {
		Pattern[] cycle = isPhaseTwo() ? CYCLE_TWO : CYCLE;
		return cycle[cycleIndex % cycle.length];
	}

	/** 한 번에 부르는 졸개 수. 3명마다 하나씩 는다 (혼자 3, 10명 6). */
	public int minionsPerSummon() {
		return MINIONS_PER_SUMMON + (party().size() - 1) / 3;
	}

	@Override
	protected boolean inPattern() {
		return pattern != Pattern.NONE;
	}

	public Pattern pattern() {
		return pattern;
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
			case RUST_STORM -> {
				pickRustZones(level);
				playSound(ModSounds.GUARDIAN_RUST_STORM, 2.0F, 1.0F);
			}
			case NONE -> {
			}
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (pattern == Pattern.NONE) {
			LivingEntity target = getTarget();
			if (cooldown > 0) {
				cooldown--;
			} else if (target != null && target.isAlive()) {
				Pattern next = upcomingPattern();
				cycleIndex++;
				startPattern(level, next);
			}
			return;
		}

		tickPattern(level);
	}

	/** 패턴을 한 틱 진행한다. AI 틱이 부르고, 시험이 직접 불러 결정적으로 돌릴 수도 있다. */
	public void tickPattern(ServerLevel level) {
		patternTick++;
		switch (pattern) {
			case CHARGE -> tickCharge(level);
			case SLAM -> tickSlam(level);
			case SUMMON -> tickSummon(level);
			case RUST_STORM -> tickRustStorm(level);
			case NONE -> {
			}
		}
	}

	/** 잠긴 돌진 방향 (잠그기 전에는 0). */
	public Vec3 chargeDirection() {
		return chargeDirection;
	}

	private void endPattern() {
		pattern = Pattern.NONE;
		patternTick = 0;
		cooldown = isEnraged() ? COOLDOWN_ENRAGED : COOLDOWN;
	}

	private void tickCharge(ServerLevel level) {
		LivingEntity target = getTarget();
		if (patternTick < CHARGE_LOCK) {
			if (target != null) {
				getLookControl().setLookAt(target, 30.0F, 30.0F);
			}
			level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 4, 0.6, 0.1, 0.6, 0.02);
			return;
		}
		if (patternTick == CHARGE_LOCK) {
			playSound(ModSounds.GUARDIAN_CHARGE, 1.5F, 0.8F); // M11 X2 돌진 방향을 잠금
			Vec3 aim = target != null ? target.position().subtract(position()) : getLookAngle();
			chargeDirection = new Vec3(aim.x, 0.0, aim.z).normalize();
			hitThisCharge.clear();
		}
		if (patternTick < CHARGE_WINDUP) {
			faceChargeDirection();
			if (patternTick % 2 == 0) {
				for (int i = 1; i <= 14; i++) {
					level.sendParticles(ParticleTypes.CRIT, getX() + chargeDirection.x * i, getY() + 0.1, getZ() + chargeDirection.z * i, 1, 0.0, 0.0, 0.0, 0.0);
				}
			}
			return;
		}
		if (patternTick < CHARGE_END) {
			setDeltaMovement(chargeDirection.x * 0.9, getDeltaMovement().y, chargeDirection.z * 0.9);
			faceChargeDirection();
			for (Player player : BossCombat.targets(level, getBoundingBox().inflate(0.6))) {
				if (hitThisCharge.add(player.getUUID())) {
					hitWithRust(level, player, CHARGE_DAMAGE, true);
					player.push(chargeDirection.x * 1.2, 0.4, chargeDirection.z * 1.2);
				}
			}
			return;
		}
		endPattern();
	}

	/** 잠긴 돌진 방향으로 몸 · 머리를 돌려 둔다. */
	private void faceChargeDirection() {
		float yaw = (float) (Mth.atan2(chargeDirection.z, chargeDirection.x) * Mth.RAD_TO_DEG) - 90.0F;
		setYRot(yaw);
		setYHeadRot(yaw);
		setYBodyRot(yaw);
	}

	private void tickSlam(ServerLevel level) {
		if (patternTick < SLAM_WINDUP) {
			if (patternTick % 3 == 0) {
				double reach = slamReach();
				for (int i = 0; i < 32; i++) {
					double angle = i * Math.PI * 2.0 / 32.0;
					level.sendParticles(ParticleTypes.CRIT, getX() + Math.cos(angle) * reach, getY() + 0.1,
							getZ() + Math.sin(angle) * reach, 1, 0.0, 0.0, 0.0, 0.0);
				}
			}
			if (patternTick == SLAM_WINDUP - 10) {
				playAttackAnimation(level);
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

	/** 내려찍기가 닿는 수평 거리. 경고 고리와 판정이 같은 반경을 쓴다. */
	public double slamReach() {
		return SLAM_RADIUS + getBbWidth() / 2;
	}

	/** 내려찍기 판정. 반경 안 플레이어에게 피해 + 띄우기. 맞은 수를 돌려준다. */
	public int slam(ServerLevel level) {
		playSound(SoundEvents.ANVIL_LAND, 2.0F, 0.5F);
		level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 6, SLAM_RADIUS / 2, 0.1, SLAM_RADIUS / 2, 0.0);
		int hits = 0;
		double reach = slamReach();
		List<Player> players = BossCombat.targets(level, getBoundingBox().inflate(reach, 2.0, reach));
		for (Player player : players) {
			if (BossCombat.horizontalDistance(player.position(), position()) > reach) {
				continue;
			}
			hitWithRust(level, player, SLAM_DAMAGE, true);
			Vec3 away = player.position().subtract(position());
			Vec3 push = new Vec3(away.x, 0.0, away.z).normalize().scale(0.8);
			player.push(push.x, 0.7, push.z);
			hits++;
		}
		return hits;
	}

	private void tickSummon(ServerLevel level) {
		if (patternTick < SUMMON_AT && patternTick % 2 == 0) {
			// M11 X2 예고: 졸개가 나올 둘레(2칸)에 마법 입자
			for (int i = 0; i < 12; i++) {
				double angle = i * Math.PI * 2.0 / 12.0;
				level.sendParticles(ParticleTypes.WITCH, getX() + Math.cos(angle) * 2.0, getY() + 0.2, getZ() + Math.sin(angle) * 2.0, 1, 0.0, 0.1, 0.0, 0.0);
			}
		}
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
		int count = Math.min(minionsPerSummon(), MAX_MINIONS_NEARBY - nearby);
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

	/** 녹 폭풍 장판 자리: 20칸 상자 안 플레이어 발밑부터, 모자라면 둘레 3 ~ 7칸 아무 데나. */
	private void pickRustZones(ServerLevel level) {
		rustZones.clear();
		for (Player player : BossCombat.targets(level, getBoundingBox().inflate(20.0))) {
			if (rustZones.size() < RUST_STORM_ZONES) {
				rustZones.add(player.position());
			}
		}
		while (rustZones.size() < RUST_STORM_ZONES) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = 3.0 + random.nextDouble() * 4.0;
			rustZones.add(position().add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance));
		}
	}

	/** 녹 폭풍 장판 자리 (시험용). */
	public List<Vec3> rustZones() {
		return List.copyOf(rustZones);
	}

	private void tickRustStorm(ServerLevel level) {
		if (patternTick < RUST_STORM_WINDUP) {
			if (patternTick % 3 == 0) {
				DustParticleOptions dust = new DustParticleOptions(0xDE8442, 1.2F);
				for (Vec3 center : rustZones) {
					for (int i = 0; i < 16; i++) {
						double a = i * Math.PI * 2.0 / 16.0;
						level.sendParticles(dust, center.x + Math.cos(a) * BossHazard.PUDDLE_RADIUS, center.y + 0.1,
								center.z + Math.sin(a) * BossHazard.PUDDLE_RADIUS, 1, 0.0, 0.0, 0.0, 0.0);
					}
				}
				// 몸에서 녹가루가 휘몰아친다
				level.sendParticles(new DustParticleOptions(0xA85628, 1.6F), getX(), getY() + getBbHeight() * 0.6, getZ(), 10, 1.0, 1.0, 1.0, 0.0);
			}
			if (patternTick == RUST_STORM_WINDUP - 10) {
				playAttackAnimation(level);
			}
			return;
		}
		if (patternTick == RUST_STORM_WINDUP) {
			spawnRustPuddles(level);
			return;
		}
		if (patternTick >= RUST_STORM_END) {
			endPattern();
		}
	}

	/** 장판 자리마다 녹 장판을 깐다. 깐 장판을 돌려준다. */
	public List<BossHazard> spawnRustPuddles(ServerLevel level) {
		List<BossHazard> puddles = new ArrayList<>();
		for (Vec3 center : rustZones) {
			puddles.add(BossHazard.spawn(level, BossHazard.Kind.RUST_PUDDLE, center, BossHazard.PUDDLE_TICKS, this));
		}
		playSound(SoundEvents.GRINDSTONE_USE, 2.0F, 0.5F);
		return puddles;
	}

	/** 녹 공격 (근접). */
	public boolean hitWithRust(ServerLevel level, LivingEntity target, float damage) {
		return hitWithRust(level, target, damage, false);
	}

	/**
	 * 녹 공격: 강철 갑옷 한 벌이 없으면 피해가 커지고 느려지고 녹이 슨다 (방어가 깎인다).
	 * 패턴 타격(pattern)은 맞히기 직전에 대상의 방패를 깨고 그대로 들어간다.
	 */
	public boolean hitWithRust(ServerLevel level, LivingEntity target, float damage, boolean pattern) {
		if (pattern) {
			BossCombat.breakShield(level, target);
			damage *= (float) party().outgoingDamageScale(); // 강화 단계 (근접은 공격력 속성에 이미 들어 있다)
		}
		boolean rustproof = Resistances.rustImmune(target);
		boolean hurt = target.hurtServer(level, damageSources().mobAttack(this), rustproof ? damage : damage * RUST_MULTIPLIER);
		if (hurt && !rustproof) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
			SanupcraftConfig config = SanupcraftConfig.current();
			ModEffects.inflict(target, ModEffects.RUST, config.guardianRustTicks, config.guardianRustLevel, this);
			BossCombat.warn(level, target, rustWarning, "entity.sanupcraft.rusty_guardian.rust_warning"); // 세트 없이 맞은 플레이어에게 안내
		}
		return hurt;
	}

	/** 근접으로 한 번 막히면 방패가 5초 꺼진다 (바닐라 워든과 같다). */
	@Override
	public float getSecondsToDisableBlocking() {
		return BossCombat.SHIELD_BREAK_SECONDS;
	}

	/** 거미줄 같은 블록에 느려지지 않는다 (돌진이 막히지 않게). */
	@Override
	public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {
	}

	@Override
	public boolean canBeAffected(MobEffectInstance newEffect) {
		return newEffect.is(MobEffects.SLOWNESS) ? false : super.canBeAffected(newEffect);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		playAttackAnimation(level);
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

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING)) {
			return false;
		}
		if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
				&& (attacker.getMainHandItem().is(SteelGear.STEEL_SWORD) || attacker.getMainHandItem().is(HeatproofGear.HEATPROOF_SWORD)
						|| attacker.getMainHandItem().is(io.github.yhyeop.sanupcraft.gear.ChromeSteelGear.CHROME_STEEL_SWORD))) {
			damage *= STEEL_SWORD_MULTIPLIER;
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt) {
			playSound(SoundEvents.IRON_GOLEM_DAMAGE, 1.0F, 0.6F);
		}
		return hurt;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.GUARDIAN_DEATH; // M11 X2
	}

}
