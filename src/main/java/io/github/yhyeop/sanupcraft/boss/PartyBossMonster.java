package io.github.yhyeop.sanupcraft.boss;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
 */
public abstract class PartyBossMonster extends Monster implements PartyBoss {
	private final ServerBossEvent bossEvent;
	private final BossParty party = new BossParty();
	/** 화남(체력 절반 아래)을 알렸는가 (M11 X2). 불러올 때 체력으로 다시 정한다. */
	private boolean phaseAnnounced;

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

	@Override
	public final BossParty party() {
		return party;
	}

	/** 보스 바에 보이는 제목 (시험용). */
	public Component bossBarName() {
		return bossEvent.getName();
	}

	public boolean isEnraged() {
		return getHealth() < getMaxHealth() / 2.0F;
	}

	/** 보스 바 체력 비율 · 화남 알림. 자식은 {@code super} 다음에 패턴을 돌린다. */
	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		bossEvent.setProgress(getHealth() / getMaxHealth());
		if (!phaseAnnounced && isEnraged()) {
			phaseAnnounced = true;
			announcePhase(level);
		}
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
}
