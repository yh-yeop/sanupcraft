package io.github.yhyeop.sanupcraft.boss.phase;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.BossParty;
import io.github.yhyeop.sanupcraft.boss.PartyBossMonster;

/**
 * 보스 2페이즈에 들어갈 때 (확장 3 C4): 둘레 {@link BossParty#RADIUS}칸 안 플레이어(관전 제외)에게 화면 안내와
 * 발전 과제 {@code <보스 id>_phase_two}를 준다. 화남 소리 · 입자는 보스의 {@code announcePhase}가 낸다.
 */
public final class BossPhaseTwo {
	private BossPhaseTwo() {
	}

	/** {@link PartyBossMonster#checkPhaseTwo}가 2페이즈로 들어간 순간 한 번 부른다. */
	public static void enter(ServerLevel level, PartyBossMonster boss) {
		String id = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).getPath() + "_phase_two";
		var advancement = level.getServer().getAdvancements().get(Sanupcraft.id(id));
		Component message = Component.translatable("entity.sanupcraft.boss.phase_two", boss.getDisplayName()).withStyle(ChatFormatting.RED);
		AABB box = boss.getBoundingBox().inflate(BossParty.RADIUS);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box,
				p -> !p.isSpectator() && p.distanceTo(boss) <= BossParty.RADIUS)) {
			if (advancement != null) {
				player.getAdvancements().award(advancement, "done");
			}
			if (player.connection != null) {
				player.sendOverlayMessage(message);
			}
		}
	}
}
