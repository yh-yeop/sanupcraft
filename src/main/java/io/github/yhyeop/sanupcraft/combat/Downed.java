package io.github.yhyeop.sanupcraft.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 쓰러짐 (C5). 플레이어가 죽을 피해를 받았을 때, 32칸 안에 쓰러지지 않은 다른 플레이어가 있고 최근 60초 안에 쓰러진 적이 없으면
 * 죽는 대신 체력 1로 쓰러진다. 쓰러진 동안:
 * <ul>
 *   <li>기어 다닌다 (헤엄 자세 · 느림 · 점프 불가), 빛나서 동료가 찾기 쉽다. 공격 · 아이템 · 블록 쓰기 · 캐기를 못 한다.</li>
 *   <li>피해를 받지 않는다. 대신 맞을 때마다 (10틱에 한 번) 남은 시간이 2초 준다. 공허 · /kill은 그대로 죽는다.</li>
 *   <li>20초(설정) 안에 동료가 응급 증기 주사기로 우클릭하면 체력 4로 일어난다. 스스로는 못 일어난다.</li>
 *   <li>시간이 다 되면 진짜로 죽는다 (보통 죽음이라 시체 모드가 그대로 시체를 만든다). 쓰러진 동안에는 죽지 않아서 시체와 겹치지 않는다.</li>
 * </ul>
 * 상태는 플레이어에 붙인 값(쓰러짐이 끝나는 오버월드 게임 시각)이고, 모든 클라이언트에 맞춘다 (기는 자세 · 클릭 막기).
 * 불사의 토템을 들고 있으면 토템이 먼저다.
 */
public final class Downed {
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int BLEED_OUT_TICKS = 400;
	public static final int COOLDOWN_TICKS = 1200;
	public static final int HELPER_RANGE = 32;
	public static final int HIT_PENALTY_TICKS = 40;
	/** 맞아서 시간이 줄어드는 간격 (불 · 용암처럼 매 틱 맞는 것). */
	private static final int HIT_INTERVAL = 10;

	/** 쓰러짐이 끝나는(죽는) 오버월드 게임 시각. 있으면 쓰러진 상태. */
	public static final AttachmentType<Long> DOWNED = AttachmentRegistry.<Long>builder()
			.persistent(Codec.LONG)
			.syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.all())
			.buildAndRegister(Sanupcraft.id("downed"));
	/** 마지막으로 쓰러진 시각 (다시 쓰러지기까지의 대기). */
	public static final AttachmentType<Long> LAST_DOWNED = AttachmentRegistry.<Long>builder()
			.persistent(Codec.LONG)
			.buildAndRegister(Sanupcraft.id("last_downed"));
	/** 출혈로 죽을 때의 피해 종류 (사망 메시지). */
	public static final ResourceKey<DamageType> BLED_OUT = ResourceKey.create(Registries.DAMAGE_TYPE, Sanupcraft.id("bled_out"));

	private static final Map<UUID, Long> LAST_HIT = new HashMap<>();

	private Downed() {
	}

	static void init() {
		ServerLivingEntityEvents.ALLOW_DEATH.register(Downed::allowDeath);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Player player) {
				clear(player);
			}
		});
		// 우유로 풀리지 않는다
		ServerMobEffectEvents.ALLOW_EARLY_REMOVE.register((instance, entity, context) ->
				!(entity instanceof Player player && isDowned(player) && (instance.is(ModCombat.DOWNED) || instance.is(MobEffects.GLOWING))));
		ServerTickEvents.END_SERVER_TICK.register(Downed::tick);
		// 쓰러진 사람은 때리지도 쓰지도 못한다 (클라이언트도 같은 값을 받아 미리 막는다)
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> isDowned(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> isDowned(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseItemCallback.EVENT.register((player, level, hand) -> isDowned(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> isDowned(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> isDowned(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !isDowned(player));
	}

	public static boolean isDowned(Player player) {
		return player.hasAttached(DOWNED);
	}

	/** 쓰러짐이 끝나기까지 남은 틱 (쓰러지지 않았으면 0). */
	public static int ticksLeft(ServerPlayer player) {
		Long until = player.getAttached(DOWNED);
		return until == null ? 0 : (int) Math.max(0L, until - now(player.level().getServer()));
	}

	static long now(MinecraftServer server) {
		return server.overworld().getGameTime();
	}

	// ---- 쓰러지기 ----

	private static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayer player) || isDowned(player)) {
			return true; // 쓰러진 채 죽는 것(공허 · 출혈)은 그대로
		}
		if (!canGoDown(player, source)) {
			return true;
		}
		goDown(player);
		return false;
	}

	/** 이번 죽음을 쓰러짐으로 바꿀 수 있나. */
	public static boolean canGoDown(ServerPlayer player, DamageSource source) {
		SanupcraftConfig config = SanupcraftConfig.current();
		if (config.downedEnabled == 0 || player.isSpectator() || player.isCreative()) {
			return false;
		}
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(BLED_OUT) || holdsTotem(player)) {
			return false;
		}
		Long last = player.getAttached(LAST_DOWNED);
		long now = now(player.level().getServer());
		if (last != null && now - last < config.downedCooldownTicks) {
			return false;
		}
		return hasHelper(player);
	}

	/** 32칸 안에 살릴 수 있는 (쓰러지지 않은) 다른 플레이어가 있나. */
	public static boolean hasHelper(ServerPlayer player) {
		double range = SanupcraftConfig.current().downedHelperRange;
		return player.level().players().stream().anyMatch(other -> other != player && other.isAlive() && !other.isSpectator()
				&& !isDowned(other) && other.distanceToSqr(player) <= range * range);
	}

	private static boolean holdsTotem(Player player) {
		for (InteractionHand hand : InteractionHand.values()) {
			if (player.getItemInHand(hand).has(DataComponents.DEATH_PROTECTION)) {
				return true;
			}
		}
		return false;
	}

	/** 쓰러뜨린다: 체력 1, 끝나는 시각, 느림 · 빛남, 주변에 알림. */
	public static void goDown(ServerPlayer player) {
		SanupcraftConfig config = SanupcraftConfig.current();
		ServerLevel level = player.level();
		long now = now(level.getServer());
		LAST_HIT.remove(player.getUUID());
		player.setHealth(1.0F);
		player.clearFire();
		player.setAttached(DOWNED, now + config.downedBleedOutTicks);
		player.setAttached(LAST_DOWNED, now);
		player.stopUsingItem();
		player.stopRiding();
		player.stopFallFlying();
		player.setSprinting(false);
		applyLook(player, config.downedBleedOutTicks);
		player.setPose(Pose.SWIMMING);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 0.6F);
		Component notice = Component.translatable("message.sanupcraft.downed.notice", player.getDisplayName(), config.downedBleedOutTicks / 20);
		for (ServerPlayer other : level.players()) {
			if (other != player && other.distanceToSqr(player) <= 64.0 * 64.0) {
				tell(other, notice, false);
			}
		}
		tell(player, Component.translatable("message.sanupcraft.downed.self"), false);
	}

	private static void applyLook(ServerPlayer player, int ticks) {
		player.addEffect(new MobEffectInstance(ModCombat.DOWNED, ticks, 0, false, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false, false));
	}

	// ---- 쓰러진 동안 ----

	/** 쓰러진 사람이 받는 피해: 공허 · /kill 등만 들어가고, 나머지는 막고 남은 시간을 줄인다. 쓰러진 사람이 일으킨 피해도 막는다. */
	static boolean allowDamage(LivingEntity entity, DamageSource source) {
		if (source.getEntity() instanceof Player attacker && attacker != entity && isDowned(attacker)) {
			return false;
		}
		if (!(entity instanceof ServerPlayer player) || !isDowned(player)) {
			return true;
		}
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(BLED_OUT)) {
			return true;
		}
		long now = now(player.level().getServer());
		Long last = LAST_HIT.get(player.getUUID());
		if (last == null || now - last >= HIT_INTERVAL) {
			LAST_HIT.put(player.getUUID(), now);
			Long until = player.getAttached(DOWNED);
			if (until != null) {
				player.setAttached(DOWNED, until - SanupcraftConfig.current().downedHitPenaltyTicks);
			}
		}
		return false;
	}

	private static void tick(MinecraftServer server) {
		long now = now(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (isDowned(player)) {
				tickPlayer(player, now);
			}
		}
	}

	/** 쓰러진 사람 한 틱: 시간이 다 되면 죽고, 아니면 기는 자세 · 1초마다 남은 시간 표시. 시험이 시각을 넘겨 부른다. */
	public static void tickPlayer(ServerPlayer player, long now) {
		Long until = player.getAttached(DOWNED);
		if (until == null) {
			return;
		}
		if (!player.isAlive()) {
			clear(player);
			return;
		}
		if (now >= until) {
			bleedOut(player);
			return;
		}
		player.setPose(Pose.SWIMMING);
		player.setSprinting(false);
		if (now % 20 == 0) {
			int left = (int) (until - now);
			if (!player.hasEffect(ModCombat.DOWNED) || !player.hasEffect(MobEffects.GLOWING)) {
				applyLook(player, left);
			}
			tell(player, Component.translatable("message.sanupcraft.downed.timer", (left + 19) / 20), true);
		}
	}

	/** 시간이 다 됨: 진짜로 죽는다 (보통 죽음 → 시체 모드 · 사망 메시지 그대로). */
	public static void bleedOut(ServerPlayer player) {
		clear(player);
		ServerLevel level = player.level();
		DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(BLED_OUT));
		player.damageCooldownTime = 0; // 방금 맞은 무적 시간에 막히지 않게
		player.hurtServer(level, source, 1_000_000.0F);
	}

	// ---- 살리기 ----

	/** 쓰러진 사람을 일으킨다 (응급 증기 주사기). 쓰러지지 않았으면 false. */
	public static boolean revive(ServerPlayer target, Player reviver) {
		if (!isDowned(target)) {
			return false;
		}
		clear(target);
		target.setHealth(Math.min(target.getMaxHealth(), (float) SanupcraftConfig.current().downedReviveHealth));
		target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
		target.setPose(Pose.STANDING);
		ServerLevel level = target.level();
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
		tell(target, Component.translatable("message.sanupcraft.downed.revived", reviver.getDisplayName()), false);
		if (reviver instanceof ServerPlayer helper) {
			tell(helper, Component.translatable("message.sanupcraft.downed.revived_other", target.getDisplayName()), true);
		}
		return true;
	}

	/** 쓰러짐 표시를 다 지운다 (마지막으로 쓰러진 시각은 남긴다). */
	private static void clear(Player player) {
		if (player.removeAttached(DOWNED) != null) {
			player.removeEffect(ModCombat.DOWNED);
			player.removeEffect(MobEffects.GLOWING);
		}
		LAST_HIT.remove(player.getUUID());
	}

	/** 연결이 없는 시험용 플레이어에게는 보내지 않는다. */
	private static void tell(ServerPlayer player, Component message, boolean overlay) {
		if (player.connection != null) {
			player.sendSystemMessage(message, overlay);
		}
	}
}
