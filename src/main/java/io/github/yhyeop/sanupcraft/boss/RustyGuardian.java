package io.github.yhyeop.sanupcraft.boss;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.sound.ModSounds;

/**
 * 녹슨 수호자. 첫 마일스톤 보스. 패턴 3개를 돌아가며 쓴다.
 * <ul>
 *   <li>돌진: 20틱 동안 웅크린다. 12틱에 대상 쪽으로 방향을 잠그고(경로가 입자로 보인다) 8틱 뒤 그 방향으로 돌진. 옆으로 피하면 된다.</li>
 *   <li>내려찍기: 25틱 동안 판정 반경과 같은 경고 고리 → 피해 + 띄우기. 경고를 보고 벗어나면 된다.</li>
 *   <li>졸개 소환: 좀벌레 3마리.</li>
 * </ul>
 * 녹 공격: 강철(또는 내열) 갑옷 한 벌을 다 입지 않은 대상은 피해 1.5배 + 구속. 패턴 타격은 방패를 깨고 들어간다.
 * 강철 검 · 내열 검으로 때리면 피해 1.5배. 거미줄에 걸리지 않고 구속이 안 걸린다.
 * 체력이 절반 아래면 패턴 간격이 짧아진다. 여러 명이면 체력 · 졸개 수가 늘고, 기여자 각자 핵을 받는다 ({@link BossParty}).
 */
public class RustyGuardian extends Monster implements PartyBoss {
	public static final double MAX_HEALTH = 300.0;
	public static final float RUST_MULTIPLIER = 1.5F;
	public static final float STEEL_SWORD_MULTIPLIER = 1.5F;
	public static final float CHARGE_DAMAGE = 12.0F;
	public static final float SLAM_DAMAGE = 10.0F;
	public static final double SLAM_RADIUS = 5.0;
	public static final int MINIONS_PER_SUMMON = 3;
	/** 패턴 타격이 방패를 깨는 시간 (초). 바닐라 워든과 같다. */
	public static final float SHIELD_BREAK_SECONDS = 5.0F;
	private static final int MAX_MINIONS_NEARBY = 8;
	/** 공격 동작 이벤트 id (클라이언트 휘두르기 애니메이션). 파수꾼과 같은 이름. */
	public static final byte EVENT_ATTACK = 4;

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
		NONE, CHARGE, SLAM, SUMMON
	}

	private static final Pattern[] CYCLE = {Pattern.CHARGE, Pattern.SLAM, Pattern.SUMMON, Pattern.SLAM};

	private final ServerBossEvent bossEvent = new ServerBossEvent(
			Mth.createInsecureUUID(this.random), this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
	private Pattern pattern = Pattern.NONE;
	private int patternTick;
	private int cooldown = 60;
	/** 화남(체력 절반 아래)을 알렸는가 (M11 X2). 불러올 때 체력으로 다시 정한다. */
	private boolean phaseAnnounced;
	private int cycleIndex;
	private Vec3 chargeDirection = Vec3.ZERO;
	private final Set<UUID> hitThisCharge = new HashSet<>();
	private int attackAnimationTick;
	/** 플레이어별 녹 경고를 마지막으로 보낸 게임 시간. */
	private final Map<UUID, Long> warnedAt = new HashMap<>();
	private final BossParty party = new BossParty();

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

	@Override
	public BossParty party() {
		return party;
	}

	/** 한 번에 부르는 졸개 수. 3명마다 하나씩 는다 (혼자 3, 10명 6). */
	public int minionsPerSummon() {
		return MINIONS_PER_SUMMON + (party.size() - 1) / 3;
	}

	/** 보스 바에 보이는 제목 (시험용). */
	public Component bossBarName() {
		return bossEvent.getName();
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
		if (!phaseAnnounced && isEnraged()) {
			phaseAnnounced = true;
			ModSounds.bossPhase(level, this, ParticleTypes.CRIT); // M11 X2
		}

		if (pattern == Pattern.NONE) {
			LivingEntity target = getTarget();
			if (cooldown > 0) {
				cooldown--;
			} else if (target != null && target.isAlive()) {
				startPattern(level, CYCLE[cycleIndex++ % CYCLE.length]);
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
			for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.6), EntitySelector.NO_CREATIVE_OR_SPECTATOR)) {
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
		List<Player> players = level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(reach, 2.0, reach), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
		for (Player player : players) {
			if (horizontalDistance(player.position(), position()) > reach) {
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

	/** 녹 공격 (근접). */
	public boolean hitWithRust(ServerLevel level, LivingEntity target, float damage) {
		return hitWithRust(level, target, damage, false);
	}

	/**
	 * 녹 공격: 강철 갑옷 한 벌이 없으면 피해가 커지고 느려진다.
	 * 패턴 타격(pattern)은 맞히기 직전에 대상의 방패를 깨고 그대로 들어간다.
	 */
	public boolean hitWithRust(ServerLevel level, LivingEntity target, float damage, boolean pattern) {
		if (pattern) {
			breakShield(level, target);
			damage *= (float) party.outgoingDamageScale(); // 강화 단계 (근접은 공격력 속성에 이미 들어 있다)
		}
		boolean rustproof = SteelGear.wearsFullSet(target);
		boolean hurt = target.hurtServer(level, damageSources().mobAttack(this), rustproof ? damage : damage * RUST_MULTIPLIER);
		if (hurt && !rustproof) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
			warnRust(level, target);
		}
		return hurt;
	}

	/** 막는 중인 방패를 깨고 쿨다운을 건다. */
	private static void breakShield(ServerLevel level, LivingEntity target) {
		ItemStack shield = target.getItemBlockingWith();
		if (shield == null) {
			return;
		}
		BlocksAttacks blocksAttacks = shield.get(DataComponents.BLOCKS_ATTACKS);
		if (blocksAttacks != null) {
			blocksAttacks.disable(level, target, SHIELD_BREAK_SECONDS, shield);
		}
	}

	/** 세트 없이 녹 공격을 맞은 플레이어에게 안내한다 (플레이어마다 200틱에 한 번). */
	private void warnRust(ServerLevel level, LivingEntity target) {
		if (!(target instanceof ServerPlayer player)) {
			return;
		}
		long now = level.getGameTime();
		warnedAt.values().removeIf(last -> now - last >= WARNING_COOLDOWN);
		if (warnedAt.putIfAbsent(player.getUUID(), now) == null) {
			player.sendOverlayMessage(Component.translatable("entity.sanupcraft.rusty_guardian.rust_warning"));
		}
	}

	private static double horizontalDistance(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** 근접으로 한 번 막히면 방패가 5초 꺼진다 (바닐라 워든과 같다). */
	@Override
	public float getSecondsToDisableBlocking() {
		return SHIELD_BREAK_SECONDS;
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
		level.broadcastEntityEvent(this, EVENT_ATTACK);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING)) {
			return false;
		}
		if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
				&& (attacker.getMainHandItem().is(SteelGear.STEEL_SWORD) || attacker.getMainHandItem().is(HeatproofGear.HEATPROOF_SWORD))) {
			damage *= STEEL_SWORD_MULTIPLIER;
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt) {
			playSound(SoundEvents.IRON_GOLEM_DAMAGE, 1.0F, 0.6F);
		}
		return hurt;
	}

	/** 실제로 깎인 체력을 공격한 플레이어에게 센다 (죽기 전에 불리므로 마지막 일격도 들어간다). */
	@Override
	protected void actuallyHurt(ServerLevel level, DamageSource source, float damage) {
		float before = getHealth();
		super.actuallyHurt(level, source, damage);
		party.record(source.getEntity(), before - getHealth());
	}

	/** 최대 체력이 속성 상한(1024)에 잘렸으면 받는 피해를 그만큼 줄인다 (갑옷 · 마법 계산 뒤, {@link BossParty#incomingDamageScale}). */
	@Override
	protected float getDamageAfterMagicAbsorb(DamageSource source, float damage) {
		return super.getDamageAfterMagicAbsorb(source, damage) * party.incomingDamageScale(this);
	}

	/** 핵은 전리품 표가 아니라 기여자 각자에게 준다. */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		party.giveKeyItem(level, this, ModItems.GUARDIAN_CORE);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		party.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		party.load(input);
		refreshBossBar();
		phaseAnnounced = isEnraged();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.GUARDIAN_DEATH; // M11 X2
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == EVENT_ATTACK) {
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

	/** 이름표로 이름을 바꾸면 보스 바 제목도 바꾼다 (저장된 이름을 불러올 때도 이 경로를 지난다). */
	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		refreshBossBar();
	}

	@Override
	public void refreshBossBar() {
		bossEvent.setName(BossTier.barName(getDisplayName(), party.tier()));
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
