package io.github.yhyeop.sanupcraft.client.combat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Pose;

import io.github.yhyeop.sanupcraft.combat.Downed;

/**
 * 쓰러진 플레이어(C5)를 클라이언트에서 기는 자세로 그린다. 플레이어 자세는 매 틱 클라이언트가 다시 정하므로,
 * 틱이 끝난 뒤 (그리기 전) 쓰러짐 값이 붙은 플레이어를 헤엄 자세로 바꿔 둔다. 자기 화면의 눈높이도 같이 낮아진다.
 */
public final class DownedClient {
	private DownedClient() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(DownedClient::tick);
	}

	private static void tick(Minecraft minecraft) {
		if (minecraft.level == null) {
			return;
		}
		for (AbstractClientPlayer player : minecraft.level.players()) {
			if (Downed.isDowned(player) && player.getPose() != Pose.SWIMMING) {
				player.setPose(Pose.SWIMMING);
			}
		}
	}
}
