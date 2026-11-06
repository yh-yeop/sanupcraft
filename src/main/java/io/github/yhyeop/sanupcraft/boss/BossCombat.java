package io.github.yhyeop.sanupcraft.boss;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 보스 패턴이 같이 쓰는 정적 도우미: 방패 깨기 · 수평 거리 · 플레이어별 안내 쿨다운 · 패턴 대상 목록. */
public final class BossCombat {
	/** 패턴 타격(과 수호자 · 파수꾼 근접)이 방패를 깨는 시간 (초). 바닐라 워든과 같다. */
	public static final float SHIELD_BREAK_SECONDS = 5.0F;

	private BossCombat() {
	}

	/** 막는 중인 방패를 깨고 쿨다운을 건다. */
	public static void breakShield(ServerLevel level, LivingEntity target) {
		ItemStack shield = target.getItemBlockingWith();
		if (shield == null) {
			return;
		}
		BlocksAttacks blocksAttacks = shield.get(DataComponents.BLOCKS_ATTACKS);
		if (blocksAttacks != null) {
			blocksAttacks.disable(level, target, SHIELD_BREAK_SECONDS, shield);
		}
	}

	/** 높이를 뺀 두 점 사이 거리. */
	public static double horizontalDistance(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** 상자 안 패턴 대상 플레이어 (관전 · 크리에이티브 제외). */
	public static List<Player> targets(ServerLevel level, AABB box) {
		return level.getEntitiesOfClass(Player.class, box, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
	}

	/** {@link #targets(ServerLevel, AABB)}에 조건을 하나 더 건다. */
	public static List<Player> targets(ServerLevel level, AABB box, Predicate<Player> extra) {
		return level.getEntitiesOfClass(Player.class, box, p -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p) && extra.test(p));
	}

	/** 대상이 접속한 플레이어이고 쿨다운이 지났으면 화면 아래 안내 문구를 띄운다. */
	public static void warn(ServerLevel level, @Nullable Entity target, PlayerCooldown cooldown, String key) {
		if (target instanceof ServerPlayer player && cooldown.tryUse(player.getUUID(), level.getGameTime())) {
			player.sendOverlayMessage(Component.translatable(key));
		}
	}

	/** 플레이어마다 {@code ticks}틱에 한 번만 허락한다. 지난 기록은 쓸 때마다 지운다 (저장하지 않음). */
	public static final class PlayerCooldown {
		private final int ticks;
		private final Map<UUID, Long> lastUsed = new HashMap<>();

		public PlayerCooldown(int ticks) {
			this.ticks = ticks;
		}

		/** 이 플레이어가 지금 쓸 수 있으면 기록하고 true. */
		public boolean tryUse(UUID player, long now) {
			lastUsed.values().removeIf(last -> now - last >= ticks);
			return lastUsed.putIfAbsent(player, now) == null;
		}
	}
}
