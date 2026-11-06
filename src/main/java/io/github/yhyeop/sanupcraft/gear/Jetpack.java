package io.github.yhyeop.sanupcraft.gear;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 증기 제트팩의 규칙. 점프 키를 누르고 있는 동안 위로 뜬다.
 * <ul>
 * <li>클라이언트는 키를 누른 매 틱 {@link ThrustPayload}를 서버에 보내고, 같은 식({@link #liftedSpeed})으로 자기 속도를 바로 올린다 (움직임은 클라이언트 몫이라 반응이 늦지 않게).</li>
 * <li>서버는 패킷을 받으면 {@link #thrust}: 가슴 칸에 제트팩이 있고 증기가 있으면 틱당 증기를 빼고, 위 속도를 올리고 상한 안으로 자르고,
 * 낙하 거리와 "공중에 너무 오래 떠 있음" 감시를 초기화한다 (안 하면 낙하 피해를 받고 80틱 뒤 쫓겨난다).</li>
 * </ul>
 * 증기가 없으면 아무것도 안 하고 그냥 떨어진다.
 */
public final class Jetpack {
	public static final int CAPACITY = 3000;
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int STEAM_PER_TICK = 5;
	/** 틱당 위 속도 증가 (칸/틱). */
	public static final double ACCEL = 0.12;
	/** 오르는 속도 상한 (칸/틱, 초당 약 8칸). */
	public static final double MAX_RISE = 0.4;

	/** 점프 키를 누르는 동안 매 틱 클라이언트 → 서버. 내용 없음. */
	public record ThrustPayload() implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<ThrustPayload> TYPE = new CustomPacketPayload.Type<>(Sanupcraft.id("jetpack_thrust"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ThrustPayload> CODEC = StreamCodec.unit(new ThrustPayload());

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private Jetpack() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(ThrustPayload.TYPE, ThrustPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ThrustPayload.TYPE, (payload, context) -> thrust(context.player()));
	}

	/** 지금 입고 있는 제트팩 (없으면 빈 것). 제트팩 모듈을 끼운 증기 흉갑(확장 3 G5 파워 아머)도 제트팩이다. */
	public static ItemStack worn(Player player) {
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		boolean module = SteamCharge.holdsSteam(chest)
				&& io.github.yhyeop.sanupcraft.module.GearModules.has(chest, io.github.yhyeop.sanupcraft.module.GearModule.JETPACK);
		return chest.is(ProductionGear.STEAM_JETPACK) || module ? chest : ItemStack.EMPTY;
	}

	/** 한 틱 뜰 증기가 들어 있나 (설정값, 0이어도 1 이상은 있어야 뜬다). 서버 · 클라이언트가 같은 판정을 쓴다. */
	public static boolean hasThrustSteam(ItemStack pack) {
		return SteamCharge.get(pack) >= Math.max(SanupcraftConfig.current().jetpackSteamPerTick, 1);
	}

	/** 위로 뜰 수 있는 상태인가 (날기 · 겉날개 · 탈것 · 관전이면 안 된다). */
	public static boolean canThrust(Player player) {
		return !player.isSpectator() && !player.getAbilities().flying && !player.isFallFlying() && !player.isPassenger();
	}

	/** 지금 위 속도에서 한 틱 밀어 올린 위 속도. 내려가는 중이면 먼저 속도를 줄여 준다. 상한은 {@link #MAX_RISE}. */
	public static double liftedSpeed(double currentY) {
		double base = currentY < 0 ? currentY * 0.5 : currentY;
		return Math.min(MAX_RISE, base + ACCEL);
	}

	/**
	 * 서버가 패킷 한 개를 처리한다. 떴으면 true. 시험이 직접 부른다.
	 * 창작 모드는 증기를 덜지 않는다 (그래도 1 이상 들어 있어야 뜬다).
	 */
	public static boolean thrust(ServerPlayer player) {
		ItemStack pack = worn(player);
		if (pack.isEmpty() || !canThrust(player)) {
			return false;
		}
		if (!hasThrustSteam(pack)) {
			return false;
		}
		int cost = SanupcraftConfig.current().jetpackSteamPerTick;
		if (!player.hasInfiniteMaterials()) {
			SteamCharge.take(pack, cost);
		}
		Vec3 known = player.getKnownMovement();
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x, liftedSpeed(Math.max(known.y, motion.y)), motion.z);
		player.fallDistance = 0.0;
		if (player.connection != null) {
			player.connection.resetFlyingTicks();
		}
		if (player.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.15, 0.0, 0.15, 0.02);
			if (level.getGameTime() % 6 == 0) {
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.35F, 1.6F);
			}
		}
		return true;
	}
}
