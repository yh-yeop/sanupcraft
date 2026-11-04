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

/**
 * 여러 명이 함께 잡는 보스의 인원 규칙. 보스마다 하나씩 들고 있는다.
 * <ul>
 *   <li>체력: 소환할 때 제단 48칸 안의 플레이어 수 n으로 최대 체력 × min(3, 1 + 0.5(n − 1)). 혼자 300이면 3명 600, 5명 이상 900.</li>
 *   <li>패턴 규모: {@link #size()}로 화염 장판 수 · 졸개 수를 늘린다 (보스 쪽에서 쓴다).</li>
 *   <li>보상: 최대 체력의 5% 이상을 깎은 플레이어 각자에게 진행 필수품(핵 · 심장) 하나씩 인벤토리로 준다.
 *       기여자가 없으면 (명령어 · 환경 피해로 죽음) 보스 자리에 하나만 떨군다. 나머지 전리품(판 · 부품 · 조각)은 전리품 표 그대로.</li>
 * </ul>
 */
public final class BossParty {
	public static final double RADIUS = 48.0;
	public static final double HEALTH_PER_EXTRA_PLAYER = 0.5;
	public static final double MAX_HEALTH_SCALE = 3.0;
	/** 이만큼(최대 체력 대비) 깎아야 보상을 받는다. */
	public static final float CONTRIBUTION_SHARE = 0.05F;

	private static final Identifier HEALTH_MODIFIER = Sanupcraft.id("party_health");
	private static final Codec<Map<UUID, Float>> DAMAGE_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.FLOAT);

	private int size = 1;
	private final Map<UUID, Float> damage = new HashMap<>();
	/** 이번 세션에 때린 플레이어 객체 (목록에 없는 경우 대비. 저장하지 않는다). */
	private final Map<UUID, ServerPlayer> seen = new HashMap<>();

	/** 인원 n일 때 체력 배율. */
	public static double healthScale(int players) {
		return Math.min(MAX_HEALTH_SCALE, 1.0 + HEALTH_PER_EXTRA_PLAYER * (Math.max(1, players) - 1));
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
		float needed = maxHealth * CONTRIBUTION_SHARE;
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

	/** 진행 필수품을 기여자 각자에게 준다. 줄 사람이 없으면 보스 자리에 하나 떨군다. 받은 사람 수를 돌려준다. */
	public int giveKeyItem(ServerLevel level, Monster boss, Item key) {
		List<ServerPlayer> players = contributors(level, boss.getMaxHealth());
		if (players.isEmpty()) {
			boss.spawnAtLocation(level, new ItemStack(key));
			return 0;
		}
		for (ServerPlayer player : players) {
			ItemStack stack = new ItemStack(key);
			Component name = stack.getHoverName();
			if (!player.getInventory().add(stack) && !stack.isEmpty()) {
				player.spawnAtLocation(level, stack);
			}
			if (player.connection != null) {
				player.sendSystemMessage(Component.translatable("entity.sanupcraft.boss.reward", boss.getDisplayName(), name));
			}
		}
		return players.size();
	}

	public void save(ValueOutput output) {
		output.putInt("party_size", size);
		output.store("party_damage", DAMAGE_CODEC, damage);
	}

	public void load(ValueInput input) {
		size = Math.max(1, input.getIntOr("party_size", 1));
		damage.clear();
		input.read("party_damage", DAMAGE_CODEC).ifPresent(damage::putAll);
	}
}
