package io.github.yhyeop.sanupcraft.mob;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.effect.ModEffects;

/**
 * 확장 3 C2 몹 셋(녹슨 자동인형 · 잿불 정령 · 공허 그림자)의 공통 부모: 정예.
 * 자연 스폰(청크 생성 포함) 때 {@link #ELITE_CHANCE}(설정 {@code mobs.eliteChance}) 확률로 정예가 된다:
 * 최대 체력 2배, 능력 하나({@link EliteAbility}), 능력 색 이름표, 전리품 · 경험치 2배. 정예를 잡은 플레이어는 발전 과제.
 * 정예 여부는 저장 키 {@code SanupcraftElite}(능력 이름)에 남고, 클라이언트에는 동기화 값으로 보낸다 (빛 레이어가 더 밝다).
 * 체력 · 속도 수정자는 영구 수정자라 속성과 함께 저장된다.
 */
public abstract class EliteMonster extends Monster {
	public static final float ELITE_CHANCE = 0.05F;
	public static final String SAVE_KEY = "SanupcraftElite";
	public static final String ADVANCEMENT = "elite_mob";
	private static final Identifier HEALTH_ID = Sanupcraft.id("elite_health");
	private static final Identifier SPEED_ID = Sanupcraft.id("elite_speed");
	/** 0 = 보통, 1 이상 = 정예 (능력 순서 + 1). */
	private static final EntityDataAccessor<Byte> DATA_ELITE = SynchedEntityData.defineId(EliteMonster.class, EntityDataSerializers.BYTE);

	protected EliteMonster(EntityType<? extends EliteMonster> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ELITE, (byte) 0);
	}

	public boolean isElite() {
		return entityData.get(DATA_ELITE) > 0;
	}

	public @Nullable EliteAbility eliteAbility() {
		int value = entityData.get(DATA_ELITE);
		return value > 0 && value <= EliteAbility.values().length ? EliteAbility.values()[value - 1] : null;
	}

	/**
	 * 정예로 만든다: 체력 2배(체력도 가득), 능력, 색 이름. 이미 수정자가 있으면 (저장에서 읽음) 체력은 그대로 둔다.
	 */
	public void makeElite(EliteAbility ability) {
		entityData.set(DATA_ELITE, (byte) (ability.ordinal() + 1));
		boolean added = addModifier(Attributes.MAX_HEALTH, HEALTH_ID, 1.0);
		if (ability == EliteAbility.SPEED) {
			addModifier(Attributes.MOVEMENT_SPEED, SPEED_ID, EliteAbility.SPEED_BONUS);
			addModifier(Attributes.FLYING_SPEED, SPEED_ID, EliteAbility.SPEED_BONUS);
		}
		if (added) {
			setHealth(getMaxHealth());
		}
		if (!hasCustomName()) {
			setCustomName(Component.translatable("entity.sanupcraft.elite", getType().getDescription()).withStyle(ability.color()));
		}
	}

	/** 수정자를 붙인다. 속성이 없거나 이미 있으면 false. */
	private boolean addModifier(Holder<Attribute> attribute, Identifier id, double amount) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance == null || instance.hasModifier(id)) {
			return false;
		}
		instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		return true;
	}

	/** 자연 스폰 · 청크 생성 때만 정예를 굴린다 (스폰 알 · 명령 · 분쇄기 시험은 보통). */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		if ((reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION)
				&& level.getRandom().nextFloat() < SanupcraftConfig.current().mobEliteChance) {
			makeElite(EliteAbility.random(level.getRandom()));
		}
		return data;
	}

	/** 효과를 거는 근접 공격: 맞았을 때만, 막는 장비는 {@link ModEffects#inflict}가 본다. */
	protected boolean hurtWithEffect(ServerLevel level, Entity target, Holder<MobEffect> effect, int ticks, int effectLevel) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			ModEffects.inflict(living, effect, ticks, effectLevel, this);
		}
		return hit;
	}

	/** 반사 능력: 때린 쪽에 들어온 피해(방어 전)의 30%를 가시 피해로 돌려준다 (가시 피해 · 자기 자신에게는 안 돌려준다). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && eliteAbility() == EliteAbility.REFLECT && source.getEntity() instanceof LivingEntity attacker && attacker != this
				&& !source.is(DamageTypes.THORNS) && !source.is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS)) {
			attacker.hurtServer(level, damageSources().thorns(this), damage * EliteAbility.REFLECT_SHARE);
		}
		return hurt;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (eliteAbility() == EliteAbility.REGEN && tickCount % EliteAbility.REGEN_INTERVAL == 0 && getHealth() < getMaxHealth()) {
			heal(1.0F);
		}
	}

	/** 정예는 전리품 표를 두 번 굴린다. */
	@Override
	protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
		Optional<ResourceKey<LootTable>> table = getLootTable();
		super.dropFromLootTable(level, source, playerKilled);
		if (isElite() && table.isPresent()) {
			dropFromLootTable(level, source, playerKilled, table.get());
		}
	}

	@Override
	protected int getBaseExperienceReward(ServerLevel level) {
		int base = super.getBaseExperienceReward(level);
		return isElite() ? base * 2 : base;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (isElite() && source.getEntity() instanceof ServerPlayer player) {
			var advancement = player.level().getServer().getAdvancements().get(Sanupcraft.id(ADVANCEMENT));
			if (advancement != null) {
				player.getAdvancements().award(advancement, "done");
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		EliteAbility ability = eliteAbility();
		if (ability != null) {
			output.putString(SAVE_KEY, ability.id());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		EliteAbility ability = EliteAbility.byId(input.getStringOr(SAVE_KEY, ""));
		if (ability != null) {
			makeElite(ability);
		}
	}
}
