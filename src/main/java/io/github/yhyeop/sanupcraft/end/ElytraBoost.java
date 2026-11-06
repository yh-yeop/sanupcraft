package io.github.yhyeop.sanupcraft.end;

import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteamGear;

/**
 * 겉날개 가속 (M6). 겉날개로 나는 중에 증기 통을 들고 우클릭하면 폭죽 한 발처럼 바라보는 쪽으로 가속한다.
 * 쓰는 통에서 증기 {@link #STEAM_COST} (설정 {@code elytraBoost.steamCost}, 창작은 공짜). 폭발 없는 폭죽을 몸에 붙여 바닐라와 같은 힘을 낸다.
 * 증기 통 아이템 클래스를 고치지 않으려고 아이템 사용 이벤트로 붙인다 (날고 있지 않으면 그냥 넘긴다).
 */
public final class ElytraBoost {
	/** 기본값. 실제로 쓰는 값은 설정 파일. */
	public static final int STEAM_COST = 100;

	public enum Result {
		NOT_FLYING, NO_STEAM, BOOSTED
	}

	private ElytraBoost() {
	}

	static void init() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!stack.is(SteamGear.STEAM_CANISTER) || !player.isFallFlying()) {
				return InteractionResult.PASS;
			}
			if (!(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.SUCCESS;
			}
			Result result = boost(serverLevel, player, stack);
			if (result == Result.NO_STEAM) {
				player.sendOverlayMessage(Component.translatable("item.sanupcraft.steam_canister.boost_empty", steamCost()));
				return InteractionResult.FAIL;
			}
			return InteractionResult.SUCCESS_SERVER;
		});
	}

	public static int steamCost() {
		return SanupcraftConfig.current().elytraBoostSteamCost;
	}

	/** 나는 중이면 이 통의 증기로 가속한다. */
	public static Result boost(ServerLevel level, Player player, ItemStack canister) {
		if (!player.isFallFlying()) {
			return Result.NOT_FLYING;
		}
		int cost = steamCost();
		if (!player.isCreative()) {
			if (SteamCharge.get(canister) < cost) {
				return Result.NO_STEAM;
			}
			SteamCharge.take(canister, cost);
		}
		Projectile.spawnProjectile(new FireworkRocketEntity(level, new ItemStack(Items.FIREWORK_ROCKET), player), level, canister);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.6F);
		return Result.BOOSTED;
	}
}
