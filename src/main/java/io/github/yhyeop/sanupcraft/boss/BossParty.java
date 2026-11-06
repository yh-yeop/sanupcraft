package io.github.yhyeop.sanupcraft.boss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 여러 명이 함께 잡는 보스의 인원 규칙. 보스마다 하나씩 들고 있는다.
 * <ul>
 *   <li>체력: 소환할 때 제단 48칸 안의 플레이어 수 n으로 최대 체력 × min(3, 1 + 0.5(n − 1)). 혼자 300이면 3명 600, 5명 이상 900.</li>
 *   <li>패턴 규모: {@link #size()}로 화염 장판 수 · 졸개 수를 늘린다 (보스 쪽에서 쓴다).</li>
 *   <li>보상: 최대 체력의 5% 이상을 깎은 플레이어 각자에게 진행 필수품(핵 · 심장) 하나씩 인벤토리로 준다.
 *       기여자가 없으면 (명령어 · 환경 피해로 죽음) 보스 자리에 하나만 떨군다. 나머지 전리품(판 · 부품 · 조각)은 전리품 표 그대로.</li>
 *   <li>강화 (C3, {@link BossTier}): 강화 소환석으로 불렀으면 {@link #setTier}로 단계 1 ~ 3을 건다. 최대 체력은 인원 배율 위에
 *       × (1 + 0.5 · 단계), 공격력 속성 × (1 + 0.25 · 단계) (패턴 피해는 {@link #damageScale()}로 보스가 직접 곱한다).
 *       보상은 핵 · 심장 {@code 1 + 단계}개 + 기계 업그레이드 하나, 보스 기록에도 단계가 남는다.</li>
 * </ul>
 */
public final class BossParty {
	public static final double RADIUS = 48.0;
	// 아래 세 값은 기본값이다. 실제로 쓰는 값은 config/sanupcraft.json (SanupcraftConfig)
	public static final double HEALTH_PER_EXTRA_PLAYER = 0.5;
	public static final double MAX_HEALTH_SCALE = 3.0;
	/** 이만큼(최대 체력 대비) 깎아야 보상을 받는다. */
	public static final float CONTRIBUTION_SHARE = 0.05F;

	private static final Identifier HEALTH_MODIFIER = Sanupcraft.id("party_health");
	private static final Identifier TIER_HEALTH_MODIFIER = Sanupcraft.id("tier_health");
	private static final Identifier TIER_DAMAGE_MODIFIER = Sanupcraft.id("tier_damage");
	private static final Codec<Map<UUID, Float>> DAMAGE_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.FLOAT);

	private int size = 1;
	/** 강화 단계 0 ~ {@link BossTier#MAX} (강화 소환석으로 불렀을 때). */
	private int tier;
	/** 소환한 게임 시각 (M10 보스 기록용). 모르면 -1 (명령어로 불러낸 보스는 기록을 남기지 않는다). */
	private long summonedAt = -1L;
	private final Map<UUID, Float> damage = new HashMap<>();
	/** 이번 세션에 때린 플레이어 객체 (목록에 없는 경우 대비. 저장하지 않는다). */
	private final Map<UUID, ServerPlayer> seen = new HashMap<>();

	/** 인원 n일 때 체력 배율. */
	public static double healthScale(int players) {
		SanupcraftConfig config = SanupcraftConfig.current();
		return Math.min(config.bossMaxHealthScale, 1.0 + config.bossHealthPerExtraPlayer * (Math.max(1, players) - 1));
	}

	/** {@code center}에서 {@link #RADIUS} 안의 플레이어 수 (관전 · 크리에이티브 제외). 최소 1. */
	public static int countPlayers(ServerLevel level, Vec3 center) {
		AABB area = new AABB(center, center).inflate(RADIUS);
		int count = level.getEntitiesOfClass(Player.class, area,
				p -> !p.isSpectator() && !p.isCreative() && p.position().distanceTo(center) <= RADIUS).size();
		return Math.max(1, count);
	}

	/** 소환 때 인원수. */
	public int size() {
		return size;
	}

	/** 인원 {@code players}에 맞춰 최대 체력을 올리고 꽉 채운다. */
	public void scale(Monster boss, int players) {
		size = Math.max(1, players);
		summonedAt = boss.level().getGameTime();
		AttributeInstance maxHealth = boss.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.removeModifier(HEALTH_MODIFIER);
			double extra = healthScale(size) - 1.0;
			if (extra > 0.0) {
				maxHealth.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER, extra, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
		}
		boss.setHealth(boss.getMaxHealth());
	}

	/** 강화 단계. */
	public int tier() {
		return tier;
	}

	/** 패턴 피해 배율 (근접 공격은 공격력 속성에 이미 들어 있다). */
	public double damageScale() {
		return BossTier.damageScale(tier);
	}

	/** 강화 단계를 건다: 최대 체력 × 단계 배율(인원 배율 위에 곱), 공격력 속성 올림. 체력을 꽉 채우고 보스 바 제목을 고친다. */
	public void setTier(Monster boss, int newTier) {
		tier = Math.max(0, Math.min(BossTier.MAX, newTier));
		AttributeInstance maxHealth = boss.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.removeModifier(TIER_HEALTH_MODIFIER);
			if (tier > 0) {
				maxHealth.addPermanentModifier(new AttributeModifier(TIER_HEALTH_MODIFIER, BossTier.healthScale(tier) - 1.0,
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		}
		AttributeInstance attack = boss.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attack != null) {
			attack.removeModifier(TIER_DAMAGE_MODIFIER);
			if (tier > 0) {
				attack.addPermanentModifier(new AttributeModifier(TIER_DAMAGE_MODIFIER, BossTier.damageScale(tier) - 1.0,
						AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
		}
		boss.setHealth(boss.getMaxHealth());
		if (boss instanceof PartyBoss partyBoss) {
			partyBoss.refreshBossBar();
		}
	}

	/** 피해를 기록한다. 화살 · 삼지창도 쏜 사람에게 센다. */
	public void record(@Nullable Entity attacker, float dealt) {
		if (attacker instanceof Player player && dealt > 0.0F) {
			damage.merge(player.getUUID(), dealt, Float::sum);
			if (player instanceof ServerPlayer serverPlayer) {
				seen.put(player.getUUID(), serverPlayer);
			}
		}
	}

	/** 이 플레이어가 깎은 체력 (시험용). */
	public float damageBy(UUID player) {
		return damage.getOrDefault(player, 0.0F);
	}

	/** 보상을 받을 수 있는, 접속 중인 기여자들. */
	public List<ServerPlayer> contributors(ServerLevel level, float maxHealth) {
		float needed = maxHealth * (float) SanupcraftConfig.current().bossContributionShare;
		List<ServerPlayer> result = new ArrayList<>();
		damage.forEach((uuid, dealt) -> {
			if (dealt < needed) {
				return;
			}
			// 다시 접속했으면 새 객체가 목록에 있다. 나간 플레이어는 받지 못한다
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
			if (player == null) {
				ServerPlayer last = seen.get(uuid);
				player = last != null && !last.isRemoved() ? last : null;
			}
			if (player != null) {
				result.add(player);
			}
		});
		result.sort((a, b) -> a.getUUID().compareTo(b.getUUID()));
		return result;
	}

	/**
	 * 진행 필수품을 기여자 각자에게 준다 ({@code 1 + 단계}개, 강화했으면 기계 업그레이드 하나 더).
	 * 줄 사람이 없으면 보스 자리에 필수품만 떨군다. 받은 사람 수를 돌려준다.
	 */
	public int giveKeyItem(ServerLevel level, Monster boss, Item key) {
		List<ServerPlayer> players = contributors(level, boss.getMaxHealth());
		io.github.yhyeop.sanupcraft.server.BossRecords.record(level, boss, summonedAt, size, tier, players); // M10 보스 기록판
		if (players.isEmpty()) {
			boss.spawnAtLocation(level, new ItemStack(key, 1 + tier));
			return 0;
		}
		for (ServerPlayer player : players) {
			ItemStack stack = new ItemStack(key, 1 + tier);
			Component name = stack.getHoverName();
			int count = stack.getCount();
			give(level, player, stack);
			if (tier > 0) {
				ItemStack upgrade = BossTier.randomUpgrade(level.getRandom());
				Component upgradeName = upgrade.getHoverName();
				give(level, player, upgrade);
				if (player.connection != null) {
					player.sendSystemMessage(Component.translatable("entity.sanupcraft.boss.reward_tier", boss.getDisplayName(), name, count, upgradeName));
				}
			} else if (player.connection != null) {
				player.sendSystemMessage(Component.translatable("entity.sanupcraft.boss.reward", boss.getDisplayName(), name));
			}
		}
		return players.size();
	}

	private static void give(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack) && !stack.isEmpty()) {
			player.spawnAtLocation(level, stack);
		}
	}

	public void save(ValueOutput output) {
		output.putInt("party_size", size);
		output.putLong("party_summoned", summonedAt);
		output.putInt("party_tier", tier);
		output.store("party_damage", DAMAGE_CODEC, damage);
	}

	public void load(ValueInput input) {
		size = Math.max(1, input.getIntOr("party_size", 1));
		summonedAt = input.getLongOr("party_summoned", -1L);
		tier = Math.max(0, Math.min(BossTier.MAX, input.getIntOr("party_tier", 0)));
		damage.clear();
		input.read("party_damage", DAMAGE_CODEC).ifPresent(damage::putAll);
	}
}
