package io.github.yhyeop.sanupcraft.gear.special;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.level.ServerPlayer;

/**
 * 특수 장비(G5)의 서버 틱 · 이벤트 연결. 플레이어마다: 광부 갑옷(매 틱) · 파워 아머(매 틱, 증기 · 효과는 1초마다) ·
 * 잠수 헬멧(1초마다) · 머리 조명(2틱마다) · 가스 경고(1초마다, 반 박자 늦게). 어둠 효과는 광부 헬멧이면 걸리기 전에 막는다.
 */
final class SpecialGearTicks {
	private SpecialGearTicks() {
	}

	static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tick = server.getTickCount();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				MinerGear.tick(player);
				PowerArmor.tick(player, tick % 20 == 0);
				if (tick % 20 == 0) {
					DivingHelmet.tickSecond(player);
				}
				if (tick % 2 == 0) {
					HeadLamp.tick(player);
				}
				if (tick % 20 == 10) {
					MinerGear.warnGas(player);
				}
			}
		});
		ServerMobEffectEvents.ALLOW_ADD.register((effect, entity, context) -> !MinerGear.blocks(entity, effect));
		ServerPlayerEvents.LEAVE.register(HeadLamp::clear);
		// 차원을 옮긴 그 틱에 지운다 (옛 칸이 아직 불러와져 있다. 다음 조명 틱까지 기다리면 칸이 언로드될 수 있다)
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> HeadLamp.clear(player));
		ServerLifecycleEvents.SERVER_STOPPING.register(HeadLamp::clearAll);
	}
}
