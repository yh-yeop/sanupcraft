package io.github.yhyeop.sanupcraft.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.Jetpack;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;

/**
 * 증기 제트팩의 클라이언트 쪽: 점프 키를 누르고 있고 제트팩에 증기가 있으면
 * 매 틱 서버에 {@link Jetpack.ThrustPayload}를 보내고, 자기 위 속도도 같은 식({@link Jetpack#liftedSpeed})으로 바로 올린다.
 * (플레이어의 움직임은 클라이언트가 정하므로 속도를 직접 올려야 늦지 않게 뜬다. 증기를 빼고 감시를 초기화하는 것은 서버.)
 */
final class JetpackClient {
	private JetpackClient() {
	}

	static void init() {
		ClientTickEvents.START_CLIENT_TICK.register(JetpackClient::tick);
	}

	private static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.gui.screen() != null || !minecraft.options.keyJump.isDown()) {
			return;
		}
		ItemStack pack = Jetpack.worn(player);
		if (pack.isEmpty() || !Jetpack.canThrust(player) || player.onGround() || player.isInWater() || player.isInLava()) {
			return;
		}
		if (SteamCharge.get(pack) < Math.max(SanupcraftConfig.current().jetpackSteamPerTick, 1) || !ClientPlayNetworking.canSend(Jetpack.ThrustPayload.TYPE)) {
			return;
		}
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x, Jetpack.liftedSpeed(motion.y), motion.z);
		player.fallDistance = 0.0;
		ClientPlayNetworking.send(new Jetpack.ThrustPayload());
	}
}
