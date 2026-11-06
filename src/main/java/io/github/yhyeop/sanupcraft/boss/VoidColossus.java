package io.github.yhyeop.sanupcraft.boss;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.end.ModEnd;
import io.github.yhyeop.sanupcraft.sound.ModSounds;

/**
 * 공허 거상 (M6, 세 번째 보스). 엔드에서, 원점에서 가로로 {@link #MIN_DISTANCE}칸 이상 떨어진 제단에서만 깨어난다.
 * <ul>
 *   <li>셜커 탄 일제 사격: 32칸 안 플레이어마다 바닐라 셜커 탄 하나 (맞으면 공중 부양).</li>
 *   <li>공허 끌어당기기: 20틱 동안 둘레에 보라 입자 고리 + 소리로 예고 → 20틱 동안 16칸 안 플레이어를 거상 쪽으로 끌어당긴다.</li>
 *   <li>공허 수정: 처음 깨어날 때와 체력이 절반 아래로 내려갈 때 수정 {@link #CRYSTALS}개를 둘레에 띄운다.
 *       수정이 하나라도 살아 있으면 거상은 피해를 받지 않는다 (수정에서 거상으로 빛줄기). 수정은 리벳 · 화살 · 근접으로 부순다.</li>
 * </ul>
 * 여러 명이면 체력이 늘고 (바닐라 상한 1024를 넘는 몫은 받는 피해를 줄여 맞춘다, {@link BossParty#incomingDamageScale}), 기여자 각자 공허 핵을 받는다 ({@link BossParty}).
 * 강화 소환석(C3)이면 체력 · 근접 공격력 · 셜커 탄 피해가 단계만큼 오른다. 끌어당기기는 피해가 없어서 그대로.
 * 불 · 낙하 피해를 받지 않는다. 체력이 절반 아래로 처음 내려가면 화남 소리 + 입자 (M11 X2).
 */
public class VoidColossus extends Monster implements PartyBoss {
	public static final double MAX_HEALTH = 600.0;
	/** 원점에서 가로로 이만큼은 떨어져야 깨어난다 (중앙 섬 밖). */
	public static final int MIN_DISTANCE = 500;
	public static final int CRYSTALS = 4;
	public static final double CRYSTAL_RADIUS = 6.0;
	public static final double VOLLEY_RANGE = 32.0;
	public static final double PULL_RANGE = 16.0;
	/** 끌어당기는 동안 틱마다 더하는 속도 (칸/틱). */
	public static final double PULL_STRENGTH = 0.16;
	/** 셜커 탄 피해 (바닐라 값). 강화 단계면 배율을 곱한다. */
	public static final float BULLET_DAMAGE = 4.0F;
	public static final byte EVENT_ATTACK = 4;
	/** 클라이언트 동작용 상태 비트: 수정이 지키는 중 · 끌어당기기 예고 · 끌어당기는 중. */
	public static final int FLAG_SHIELDED = 1;
	public static final int FLAG_PULL_WINDUP = 2;
	public static final int FLAG_PULLING = 4;
	private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(VoidColossus.class, EntityDataSerializers.BYTE);

	private static final int PULL_WINDUP = 20;
	private static final int PULL_END = 40;
	private static final int VOLLEY_END = 20;
	private static final int COOLDOWN = 80;
	private static final int COOLDOWN_ENRAGED = 50;
	private static final int SHIELD_MESSAGE_COOLDOWN = 60;
	/** 수정이 이보다 멀면 (거상이 멀리 끌려감) 사라진 것으로 친다. */
	private static final double CRYSTAL_LEASH = 64.0;

	public enum Pattern {
		NONE, VOLLEY, PULL
	}

	/** 깨어날 수 없는 이유. */
	public enum WakeProblem {
		NOT_END, TOO_CLOSE
	}

	private final ServerBossEvent bossEvent = new ServerBossEvent(
			Mth.createInsecureUUID(this.random), this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
	private final BossParty party = new BossParty();
	private Pattern pattern = Pattern.NONE;
	private int patternTick;
	private int cooldown = 60;
	private int cycleIndex;
	private int attackTick;
	/** 클라이언트 동작 (0 → 1로 천천히): 끌어당기기 자세 · 수정 보호막 일렁임. 이전 틱 값은 보간용. */
	private float pullAnim;
	private float pullAnimO;
	private float shieldAnim;
	private float shieldAnimO;
	/** 화남(체력 절반 아래)을 알렸는가 (M11 X2). 불러올 때 체력으로 다시 정한다. */
	private boolean phaseAnnounced;
	/** 0 = 아직 수정 안 띄움, 1 = 처음 수정 띄움, 2 = 절반에서 다시 띄움. */
	private int shieldWave;
	private final List<UUID> crystals = new ArrayList<>();
	private final Map<UUID, Long> shieldMessageAt = new HashMap<>();

	public VoidColossus(EntityType<? extends VoidColossus> type, Level level) {
		super(type, level);
		this.xpReward = 120;
		this.setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_FLAGS, (byte) 0);
	}

	/** 클라이언트에서도 보이는 상태 비트 ({@link #FLAG_SHIELDED} 등). */
	public int flags() {
		return entityData.get(DATA_FLAGS);
	}

	/** 클라이언트에 보낼 상태 비트를 고친다 (AI 틱마다, 시험에서는 직접). */
	public void updateFlags(ServerLevel level) {
		int flags = 0;
		if (isShielded(level)) {
			flags |= FLAG_SHIELDED;
		}
		if (pattern == Pattern.PULL) {
			flags |= patternTick < PULL_WINDUP ? FLAG_PULL_WINDUP : FLAG_PULLING;
		}
		if (flags != flags()) {
			entityData.set(DATA_FLAGS, (byte) flags);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, MAX_HEALTH)
				.add(Attributes.ATTACK_DAMAGE, 14.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.ARMOR, 12.0)
				.add(Attributes.ARMOR_TOUGHNESS, 4.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.STEP_HEIGHT, 1.5);
	}

	/** 이 차원 · 자리에서 깨어날 수 없는 이유. 깨어나면 null. */
	public static @Nullable WakeProblem wakeProblem(ResourceKey<Level> dimension, BlockPos pos) {
		if (dimension != Level.END) {
			return WakeProblem.NOT_END;
		}
		long x = pos.getX();
		long z = pos.getZ();
		return x * x + z * z < (long) MIN_DISTANCE * MIN_DISTANCE ? WakeProblem.TOO_CLOSE : null;
	}

	/** 제단이 보여 줄 문구. 거상이 아니거나 깨어나면 null. */
	public static @Nullable Component wakeMessage(Level level, BlockPos pos, EntityType<?> type) {
		if (type != ModEnd.VOID_COLOSSUS) {
			return null;
		}
		WakeProblem problem = wakeProblem(level.dimension(), pos);
		if (problem == null) {
			return null;
		}
		return problem == WakeProblem.NOT_END
				? Component.translatable("entity.sanupcraft.void_colossus.not_end")
				: Component.translatable("entity.sanupcraft.void_colossus.too_close", MIN_DISTANCE,
						(int) Math.sqrt((double) pos.getX() * pos.getX() + (double) pos.getZ() * pos.getZ()));
	}

	/** 제단의 소환 검사: 거상이 아니면 늘 참. */
	public static boolean canWakeAt(Level level, BlockPos pos, EntityType<?> type) {
		return type != ModEnd.VOID_COLOSSUS || wakeProblem(level.dimension(), pos) == null;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PatternGoal());
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public BossParty party() {
		return party;
	}

	public Pattern pattern() {
		return pattern;
	}

	public boolean isEnraged() {
		return getHealth() < getMaxHealth() / 2.0F;
	}

	public Component bossBarName() {
		return bossEvent.getName();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		bossEvent.setProgress(getHealth() / getMaxHealth());
		if (!phaseAnnounced && isEnraged()) {
			phaseAnnounced = true;
			ModSounds.bossPhase(level, this, ModSounds.COLOSSUS_RAGE, ParticleTypes.REVERSE_PORTAL); // M11 X2
		}
		tickShields(level);
		if (pattern == Pattern.NONE) {
			LivingEntity target = getTarget();
			if (cooldown > 0) {
				cooldown--;
			} else if (target != null && target.isAlive()) {
				startPattern(level, cycleIndex++ % 2 == 0 ? Pattern.VOLLEY : Pattern.PULL);
			}
		} else {
			tickPattern(level);
		}
		updateFlags(level);
	}

	/** 처음 깨어날 때 · 절반 아래로 내려갈 때 수정을 띄우고, 살아 있는 수정에서 빛줄기 입자를 뿌린다. */
	private void tickShields(ServerLevel level) {
		if (shieldWave == 0 || shieldWave == 1 && isEnraged()) {
			spawnCrystals(level);
		}
		if (tickCount % 4 == 0) {
			for (VoidCrystal crystal : livingCrystals(level)) {
				Vec3 from = crystal.position();
				Vec3 to = position().add(0.0, getBbHeight() * 0.6, 0.0);
				Vec3 step = to.subtract(from).scale(1.0 / 8.0);
				for (int i = 1; i < 8; i++) {
					Vec3 p = from.add(step.scale(i));
					level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
				}
			}
		}
	}

	/** 수정 {@link #CRYSTALS}개를 둘레(반경 6, 3칸 위)에 띄운다. 띄운 수정을 돌려준다 (시험용). */
	public List<VoidCrystal> spawnCrystals(ServerLevel level) {
		shieldWave = Math.min(2, shieldWave + 1);
		List<VoidCrystal> spawned = new ArrayList<>();
		for (int i = 0; i < CRYSTALS; i++) {
			double angle = Math.PI * 2.0 * i / CRYSTALS + random.nextDouble() * 0.4;
			VoidCrystal crystal = ModEnd.VOID_CRYSTAL.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			if (crystal == null) {
				continue;
			}
			crystal.setPos(getX() + Math.cos(angle) * CRYSTAL_RADIUS, getY() + 3.0, getZ() + Math.sin(angle) * CRYSTAL_RADIUS);
			crystal.setOwner(this);
			level.addFreshEntity(crystal);
			crystals.add(crystal.getUUID());
			spawned.add(crystal);
			level.sendParticles(ParticleTypes.PORTAL, crystal.getX(), crystal.getY(), crystal.getZ(), 20, 0.4, 0.4, 0.4, 0.4);
		}
		playSound(ModSounds.COLOSSUS_CRYSTALS, 2.0F, 1.0F); // M11 X2
		return spawned;
	}

	/** 살아 있는 내 수정들. 죽었거나 너무 먼 수정은 목록에서 뺀다. */
	public List<VoidCrystal> livingCrystals(ServerLevel level) {
		List<VoidCrystal> alive = new ArrayList<>();
		crystals.removeIf(uuid -> {
			Entity entity = level.getEntity(uuid);
			if (entity instanceof VoidCrystal crystal && crystal.isAlive()) {
				if (crystal.distanceTo(this) > CRYSTAL_LEASH) {
					crystal.discard();
					return true;
				}
				alive.add(crystal);
				return false;
			}
			return true;
		});
		return alive;
	}

	/** 수정이 하나라도 살아 있으면 무적. */
	public boolean isShielded(ServerLevel level) {
		return !livingCrystals(level).isEmpty();
	}

	/** 다음 패턴을 바로 시작한다 (시험 · 디버그용). */
	public void startPattern(ServerLevel level, Pattern next) {
		pattern = next;
		patternTick = 0;
		getNavigation().stop();
		if (next == Pattern.VOLLEY) {
			playSound(SoundEvents.SHULKER_SHOOT, 2.0F, 0.6F);
		} else if (next == Pattern.PULL) {
			playSound(SoundEvents.PORTAL_TRIGGER, 1.5F, 0.6F);
		}
	}

	/** 패턴을 한 틱 진행한다. */
	public void tickPattern(ServerLevel level) {
		patternTick++;
		switch (pattern) {
			case VOLLEY -> {
				if (patternTick == 1) {
					volley(level);
				}
				if (patternTick >= VOLLEY_END) {
					endPattern();
				}
			}
			case PULL -> tickPull(level);
			case NONE -> {
			}
		}
	}

	private void endPattern() {
		pattern = Pattern.NONE;
		patternTick = 0;
		cooldown = isEnraged() ? COOLDOWN_ENRAGED : COOLDOWN;
	}

	/** 셜커 탄 일제 사격: 32칸 안 플레이어마다 하나. 쏜 수를 돌려준다. */
	public int volley(ServerLevel level) {
		int shots = 0;
		for (Player player : targets(level, VOLLEY_RANGE)) {
			level.addFreshEntity(bullet(level, player));
			shots++;
		}
		if (shots == 0 && getTarget() != null) {
			level.addFreshEntity(bullet(level, getTarget()));
			shots++;
		}
		return shots;
	}

	/** {@code target}을 쫓는 셜커 탄 하나 (피해 = 바닐라 4 × 강화 배율). */
	public VoidBullet bullet(ServerLevel level, Entity target) {
		return new VoidBullet(level, this, target, BULLET_DAMAGE * (float) party.outgoingDamageScale());
	}

	private void tickPull(ServerLevel level) {
		if (patternTick < PULL_WINDUP) {
			if (patternTick % 2 == 0) {
				double r = PULL_RANGE * (1.0 - patternTick / (double) PULL_WINDUP);
				for (int i = 0; i < 24; i++) {
					double a = i * Math.PI * 2.0 / 24.0;
					level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX() + Math.cos(a) * r, getY() + 0.2, getZ() + Math.sin(a) * r, 1, 0.0, 0.1, 0.0, 0.0);
				}
			}
			if (patternTick == PULL_WINDUP - 10) {
				playSound(ModSounds.COLOSSUS_PULL, 2.0F, 1.0F); // M11 X2 끌어당기기 예고
			}
			return;
		}
		pull(level);
		if (patternTick >= PULL_END) {
			endPattern();
		}
	}

	/** 끌어당기기 한 틱: 16칸 안 플레이어를 거상 쪽으로. 당긴 수를 돌려준다. */
	public int pull(ServerLevel level) {
		int pulled = 0;
		for (Player player : targets(level, PULL_RANGE)) {
			Vec3 to = position().add(0.0, 1.0, 0.0).subtract(player.position());
			if (to.lengthSqr() < 4.0) {
				continue;
			}
			Vec3 push = to.normalize().scale(PULL_STRENGTH);
			player.push(push.x, push.y * 0.5, push.z);
			pulled++;
		}
		if (patternTick % 3 == 0) {
			level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.0, getZ(), 30, PULL_RANGE / 3, 1.0, PULL_RANGE / 3, -1.0);
		}
		return pulled;
	}

	private List<Player> targets(ServerLevel level, double range) {
		return level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(range),
				p -> !p.isSpectator() && !p.isCreative() && p.isAlive() && p.distanceTo(this) <= range);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		attackTick = 10;
		level.broadcastEntityEvent(this, EVENT_ATTACK);
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt) {
			target.push(0.0, 0.4, 0.0);
		}
		playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.6F);
		return hurt;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_DROWNING)) {
			return false;
		}
		if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && isShielded(level)) {
			level.sendParticles(ParticleTypes.WITCH, getX(), getY() + getBbHeight() * 0.6, getZ(), 12, 0.8, 1.0, 0.8, 0.0);
			playSound(SoundEvents.SHULKER_HURT_CLOSED, 1.0F, 0.6F);
			if (source.getEntity() instanceof ServerPlayer player) {
				long now = level.getGameTime();
				Long last = shieldMessageAt.get(player.getUUID());
				if (last == null || now - last >= SHIELD_MESSAGE_COOLDOWN) {
					shieldMessageAt.put(player.getUUID(), now);
					player.sendOverlayMessage(Component.translatable("entity.sanupcraft.void_colossus.shielded"));
				}
			}
			return false;
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt) {
			playSound(SoundEvents.IRON_GOLEM_HURT, 1.0F, 0.5F);
		}
		return hurt;
	}

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

	/** 공허 핵은 기여자 각자에게. 남은 수정은 같이 사라진다. */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		party.giveKeyItem(level, this, ModEnd.VOID_CORE);
		for (VoidCrystal crystal : livingCrystals(level)) {
			crystal.discard();
		}
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		return !effect.is(MobEffects.SLOWNESS) && !effect.is(MobEffects.LEVITATION) && super.canBeAffected(effect);
	}

	@Override
	public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		party.save(output);
		output.putInt("shield_wave", shieldWave);
		output.store("crystals", UUIDUtil.CODEC.listOf(), List.copyOf(crystals));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		party.load(input);
		shieldWave = input.getIntOr("shield_wave", 0);
		crystals.clear();
		input.read("crystals", UUIDUtil.CODEC.listOf()).ifPresent(crystals::addAll);
		refreshBossBar();
		phaseAnnounced = isEnraged();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.COLOSSUS_DEATH; // M11 X2
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == EVENT_ATTACK) {
			attackTick = 10;
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
		if (level().isClientSide()) {
			int flags = flags();
			pullAnimO = pullAnim;
			shieldAnimO = shieldAnim;
			pullAnim = Mth.approach(pullAnim, (flags & (FLAG_PULL_WINDUP | FLAG_PULLING)) != 0 ? 1.0F : 0.0F, 0.1F);
			shieldAnim = Mth.approach(shieldAnim, (flags & FLAG_SHIELDED) != 0 ? 1.0F : 0.0F, 0.05F);
			if (random.nextInt(2) == 0) {
				level().addParticle(ParticleTypes.PORTAL, getRandomX(0.8), getRandomY(), getRandomZ(0.8),
						(random.nextDouble() - 0.5) * 2.0, -random.nextDouble(), (random.nextDouble() - 0.5) * 2.0);
			}
		}
	}

	public int getAttackTick() {
		return attackTick;
	}

	/** 끌어당기기 자세 (클라이언트, 0 ~ 1). */
	public float pullAnimation(float partialTicks) {
		return Mth.lerp(partialTicks, pullAnimO, pullAnim);
	}

	/** 수정 보호막 일렁임 (클라이언트, 0 ~ 1). */
	public float shieldAnimation(float partialTicks) {
		return Mth.lerp(partialTicks, shieldAnimO, shieldAnim);
	}

	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		refreshBossBar();
	}

	@Override
	public void refreshBossBar() {
		bossEvent.setName(BossTier.barName(getDisplayName(), party().tier()));
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

	/**
	 * 거상의 셜커 탄: 바닐라 탄과 같은데 피해만 강화 배율을 곱한 값 (바닐라는 4 고정). 엔티티 종류는 바닐라 그대로라
	 * 클라이언트 · 수정의 셜커 탄 면역은 같고, 저장했다 불러오면 바닐라 탄(피해 4)으로 돌아온다 (탄은 몇 초면 사라진다).
	 */
	public static final class VoidBullet extends ShulkerBullet {
		private final float damage;

		public VoidBullet(Level level, LivingEntity owner, Entity target, float damage) {
			super(level, owner, target, Direction.Axis.Y);
			this.damage = damage;
		}

		public float damage() {
			return damage;
		}

		/** 바닐라 {@code ShulkerBullet.onHitEntity}와 같고 피해만 다르다 (위 단계 {@code Projectile.onHitEntity}는 빈 메서드). */
		@Override
		protected void onHitEntity(EntityHitResult hitResult) {
			Entity target = hitResult.getEntity();
			Entity owner = getOwner();
			DamageSource source = damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null);
			if (target.hurtOrSimulate(source, damage)) {
				if (level() instanceof ServerLevel serverLevel) {
					EnchantmentHelper.doPostAttackEffects(serverLevel, target, source);
				}
				if (target instanceof LivingEntity living) {
					living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 200), owner != null ? owner : this);
				}
			}
		}
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
