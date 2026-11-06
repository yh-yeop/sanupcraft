package io.github.yhyeop.sanupcraft.gear;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 증기 부츠의 서버 쪽 효과 (클라이언트 패킷 없이 서버가 웅크림을 안다).
 * <ul>
 * <li>공중에서 웅크리기 시작하는 순간 바라보는 쪽으로 돌진: 증기 {@link #DASH_COST}, 쿨다운 {@link #DASH_COOLDOWN}틱, 낙하 거리 절반.</li>
 * <li>증기가 있으면 낙하 피해 절반 (바닐라 낙하 피해 배율 속성에 임시 보정을 걸고 푼다). 맞은 피해 하트 1칸마다 증기 {@link #FALL_STEAM_PER_HEART}.</li>
 * </ul>
 */
public final class SteamBoots {
	public static final int CAPACITY = 500;
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int DASH_COST = 40;
	public static final int DASH_COOLDOWN = 20;
	/** 낙하 피해를 받을 때 하트 하나(피해 2)마다 드는 증기. */
	public static final int FALL_STEAM_PER_HEART = 5;
	/** 돌진 속도 (틱당 칸). */
	public static final double DASH_SPEED = 1.3;

	private static final Identifier FALL_MODIFIER = Sanupcraft.id("steam_boots_fall");

	/** 지금 웅크리고 있는 플레이어 (웅크림이 막 시작된 순간을 알아내려고). */
	private static final Set<UUID> SNEAKING = new HashSet<>();

	private SteamBoots() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player);
			}
		});
		ServerPlayerEvents.LEAVE.register(player -> SNEAKING.remove(player.getUUID()));
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (entity instanceof Player player && source.is(DamageTypeTags.IS_FALL) && damage > 0) {
				onFallDamage(player, damage);
			}
		});
	}

	/** 한 틱: 낙하 피해 보정을 맞추고, 웅크림이 막 시작됐으면 돌진을 건다. 시험이 직접 부른다. */
	public static void tickPlayer(Player player) {
		ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
		boolean wearing = boots.is(SteamGear.STEAM_BOOTS);
		setFallHalved(player, wearing && SteamCharge.get(boots) > 0);

		boolean started = false;
		if (player.isShiftKeyDown()) {
			started = SNEAKING.add(player.getUUID());
		} else {
			SNEAKING.remove(player.getUUID());
		}
		if (started && wearing && isAirborne(player)) {
			dash(player, boots);
		}
	}

	private static boolean isAirborne(Player player) {
		return !player.onGround() && !player.isInWater() && !player.isInLava() && !player.isFallFlying()
				&& !player.getAbilities().flying && !player.isPassenger();
	}

	/** 돌진한다. 쿨다운 중이거나 증기가 모자라면 안 하고 false. */
	public static boolean dash(Player player, ItemStack boots) {
		SanupcraftConfig config = SanupcraftConfig.current();
		if (player.getCooldowns().isOnCooldown(boots)) {
			return false;
		}
		if (SteamCharge.get(boots) < config.bootsDashSteam) {
			player.sendOverlayMessage(Component.translatable("item.sanupcraft.steam_boots.no_steam", config.bootsDashSteam));
			return false;
		}
		SteamCharge.take(boots, config.bootsDashSteam);
		Vec3 look = player.getLookAngle();
		player.setDeltaMovement(look.x * DASH_SPEED, look.y * DASH_SPEED + 0.15, look.z * DASH_SPEED);
		player.fallDistance *= 0.5;
		if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
			serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
		player.getCooldowns().addCooldown(boots, config.bootsDashCooldown);
		if (player.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 12, 0.3, 0.1, 0.3, 0.05);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.7F, 1.4F);
		}
		return true;
	}

	/** 낙하 피해를 입었다: 증기 부츠가 막아 준 몫으로 증기를 깎는다 (맞은 하트 1칸 = 5). */
	static void onFallDamage(Player player, float damage) {
		ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
		if (!boots.is(SteamGear.STEAM_BOOTS) || SteamCharge.get(boots) <= 0) {
			return;
		}
		SteamCharge.take(boots, Math.max(1, Math.round(damage / 2.0F * FALL_STEAM_PER_HEART)));
		setFallHalved(player, SteamCharge.get(boots) > 0);
	}

	/** 낙하 피해 배율에 -50% 임시 보정을 걸거나 푼다. */
	private static void setFallHalved(Player player, boolean halved) {
		AttributeInstance attribute = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
		if (attribute == null) {
			return;
		}
		if (halved) {
			if (!attribute.hasModifier(FALL_MODIFIER)) {
				attribute.addTransientModifier(new AttributeModifier(FALL_MODIFIER, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		} else {
			attribute.removeModifier(FALL_MODIFIER);
		}
	}
}
