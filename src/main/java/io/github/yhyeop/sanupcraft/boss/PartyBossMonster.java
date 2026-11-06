package io.github.yhyeop.sanupcraft.boss;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 인원 규칙({@link BossParty})을 따르는 보스 셋(수호자 · 파수꾼 · 거상)의 공통 부모.
 * 보스 바(이름 + 강화 단계, 체력 비율), 인원 기록 저장 · 읽기, 받은 피해 기록 · 상한 보정, 필수품 나눠 주기,
 * 체력 절반 아래 첫 알림을 맡는다. 패턴 · 받는 피해 규칙({@code hurtServer}) · 효과 면역은 보스마다 다르다.
 * 저장 키는 {@link BossParty#save}의 {@code party_*} 그대로 (부모로 옮겨도 이름이 같다).
 * <p>2페이즈 (확장 3 C4): 체력이 절반 이하로 처음 내려가면 한 번 들어간다 ({@link #checkPhaseTwo}). 동기화 데이터라 클라이언트가 금 간 모습을 그리고,
 * 저장 키 {@code phase_two}로 남는다 (예전 저장에 없으면 체력이 절반 아래인지로 정한다). 들어갈 때 화남 소리 · 입자({@link #announcePhase}),
 * 둘레 플레이어 안내 · 발전 과제({@link io.github.yhyeop.sanupcraft.boss.phase.BossPhaseTwo}), 보스마다 새 패턴({@link #onPhaseTwo}).
 * 기여자는 필수품과 함께 전용 장비를 확률로 받는다 ({@link #gearItem}, {@link io.github.yhyeop.sanupcraft.boss.gear.BossGearDrops}).
 */
public abstract class PartyBossMonster extends Monster implements PartyBoss {
	/** 공격 동작 이벤트 id (클라이언트 휘두르기 · 내려치기 애니메이션). */
	public static final byte EVENT_ATTACK = 4;
	/** 공격 동작 길이 (틱). 렌더러가 남은 틱으로 자세를 잡는다. */
	public static final int ATTACK_ANIMATION_TICKS = 10;
	/** 이 체력 비율 이하로 내려가면 2페이즈 (확장 3 C4). */
	public static final float PHASE_TWO_HEALTH = 0.5F;
	private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO = SynchedEntityData.defineId(PartyBossMonster.class, EntityDataSerializers.BOOLEAN);

	private final ServerBossEvent bossEvent;
	private final BossParty party = new BossParty();
	/** 이번 세션에 2페이즈로 들어간 횟수 (시험용, 저장하지 않음 — 한 번만 들어가야 한다). */
	private int phaseTwoEntries;
	/** 공격 동작 남은 틱 (서버 · 클라이언트 둘 다, 저장하지 않음). */
	private int attackTick;

	/** 보스 바는 {@code Monster} 생성자 뒤에 만든다 (지금 이름 · 무작위 UUID가 필요하다). 자식 필드는 아직 없으니 여기서 읽지 않는다. */
	protected PartyBossMonster(EntityType<? extends PartyBossMonster> type, Level level, BossEvent.BossBarColor color, int xpReward) {
		super(type, level);
		this.bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
		this.xpReward = xpReward;
		this.setPersistenceRequired();
	}

	/** 기여자에게 줄 진행 필수품 (핵 · 심장). */
	protected abstract Item keyItem();

	/** 체력이 처음 절반 아래로 내려갈 때 한 번: 화남 소리 · 입자 (M11 X2). */
	protected abstract void announcePhase(ServerLevel level);

	/** 기여자가 확률로 받는 전용 장비 (확장 3 C4). */
	protected abstract Item gearItem();

	/** 2페이즈에 들어간 직후 한 번 (보스마다: 다음 패턴을 새 패턴으로). */
	protected void onPhaseTwo(ServerLevel level) {
	}

	/** 패턴을 쓰는 중인가 ({@link PatternGoal}이 걷기 · 근접 공격을 멈춘다). */
	protected abstract boolean inPattern();

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_PHASE_TWO, false);
	}

	@Override
	public final BossParty party() {
		return party;
	}

	/** 보스 바에 보이는 제목 (시험용). */
	public Component bossBarName() {
		return bossEvent.getName();
	}

	/** 화났나: 2페이즈이거나 체력이 절반({@link #PHASE_TWO_HEALTH}) 이하 ({@link #checkPhaseTwo}와 같은 기준, 한 번 2페이즈면 체력이 차도 화난 채). */
	public boolean isEnraged() {
		return isPhaseTwo() || halfHealthOrLess();
	}

	/** 체력이 절반({@link #PHASE_TWO_HEALTH}) 이하인가 (2페이즈 여부와 상관없이 지금 체력만 본다). */
	private boolean halfHealthOrLess() {
		return getHealth() <= getMaxHealth() * PHASE_TWO_HEALTH;
	}

	/** 2페이즈인가 (서버 · 클라이언트, 한 번 들어가면 체력이 차도 그대로). */
	public boolean isPhaseTwo() {
		return entityData.get(DATA_PHASE_TWO);
	}

	/** 2페이즈에 들어간 횟수 (시험용). */
	public int phaseTwoEntries() {
		return phaseTwoEntries;
	}

	/**
	 * 체력이 {@link #PHASE_TWO_HEALTH} 이하이고 아직 1페이즈면 2페이즈로 들어간다: 화남 알림 · 안내 · 발전 과제 · 새 패턴 준비.
	 * AI 틱마다 부르고, 시험이 직접 부를 수도 있다. 들어갔으면 true.
	 */
	public boolean checkPhaseTwo(ServerLevel level) {
		if (isPhaseTwo() || !halfHealthOrLess()) {
			return false;
		}
		entityData.set(DATA_PHASE_TWO, true);
		phaseTwoEntries++;
		announcePhase(level);
		io.github.yhyeop.sanupcraft.boss.phase.BossPhaseTwo.enter(level, this);
		onPhaseTwo(level);
		return true;
	}

	/** 보스 바 체력 비율 · 화남 알림. 자식은 {@code super} 다음에 패턴을 돌린다. */
	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		bossEvent.setProgress(getHealth() / getMaxHealth());
		checkPhaseTwo(level);
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

	/** 필수품은 전리품 표가 아니라 기여자 각자에게 준다. */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		party.giveKeyItem(level, this, keyItem());
		io.github.yhyeop.sanupcraft.boss.gear.BossGearDrops.reward(level, this, gearItem()); // 확장 3 C4
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		party.save(output);
		output.putBoolean("phase_two", isPhaseTwo());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		party.load(input);
		refreshBossBar();
		// 예전 저장(키 없음)은 화남을 이미 알렸는지 = 체력이 절반 아래인지로 본다
		entityData.set(DATA_PHASE_TWO, input.getBooleanOr("phase_two", halfHealthOrLess()));
	}

	/** 공격 동작을 시작하고 클라이언트에 알린다. */
	protected void playAttackAnimation(ServerLevel level) {
		attackTick = ATTACK_ANIMATION_TICKS;
		level.broadcastEntityEvent(this, EVENT_ATTACK);
	}

	/** 공격 동작 남은 틱 (렌더러용). */
	public int getAttackTick() {
		return attackTick;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == EVENT_ATTACK) {
			attackTick = ATTACK_ANIMATION_TICKS;
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

	/** 패턴 중에는 걷기 · 근접 공격을 멈춘다. 보스마다 {@code registerGoals}에서 우선순위 1로 넣는다. */
	protected final class PatternGoal extends Goal {
		public PatternGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return inPattern();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}
	}
}
