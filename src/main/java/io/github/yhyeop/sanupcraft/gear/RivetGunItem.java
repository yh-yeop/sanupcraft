package io.github.yhyeop.sanupcraft.gear;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 리벳총. 우클릭하면 인벤토리의 리벳 1개와 증기 통의 증기를 쓰고 빠른 리벳을 쏜다.
 * 창작 모드는 탄약 · 증기가 들지 않는다. 리벳이나 증기가 없으면 짧은 안내만 띄우고 쿨다운도 걸지 않는다.
 */
public class RivetGunItem extends Item {
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int STEAM_COST = 20;
	public static final float DAMAGE = 6.0F;
	public static final int COOLDOWN_TICKS = 10;

	/** 쏴 본 결과. */
	public enum Result {
		FIRED, NO_RIVET, NO_STEAM, COOLDOWN
	}

	public RivetGunItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack gun = player.getItemInHand(hand);
		if (level instanceof ServerLevel serverLevel) {
			Result result = fire(serverLevel, player, gun);
			if (result == Result.NO_RIVET) {
				player.sendOverlayMessage(Component.translatable("item.sanupcraft.rivet_gun.no_rivet"));
			} else if (result == Result.NO_STEAM) {
				player.sendOverlayMessage(Component.translatable("item.sanupcraft.rivet_gun.no_steam", SanupcraftConfig.current().rivetGunSteam));
			}
			return result == Result.FIRED ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}
		// 클라이언트: 쿨다운 중이 아니면 팔을 휘두르게 둔다 (서버 결과가 곧 맞춘다)
		return player.getCooldowns().isOnCooldown(gun) ? InteractionResult.FAIL : InteractionResult.SUCCESS;
	}

	/** 한 발 쏜다. 쏘면 리벳 1 + 증기를 쓰고 쿨다운을 건다. 시험이 직접 부른다. */
	public static Result fire(ServerLevel level, Player player, ItemStack gun) {
		if (player.getCooldowns().isOnCooldown(gun)) {
			return Result.COOLDOWN;
		}
		SanupcraftConfig config = SanupcraftConfig.current();
		if (!player.hasInfiniteMaterials()) {
			ItemStack rivet = findRivet(player);
			if (rivet.isEmpty()) {
				return Result.NO_RIVET;
			}
			if (SteamCharge.total(player) < config.rivetGunSteam) {
				return Result.NO_STEAM;
			}
			SteamCharge.drain(player, config.rivetGunSteam);
			rivet.shrink(1);
		}
		RivetProjectile projectile = new RivetProjectile(level, player);
		projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, RivetProjectile.SPEED, 0.4F);
		level.addFreshEntity(projectile);
		Vec3 muzzle = player.getEyePosition().add(player.getLookAngle().scale(0.8));
		level.sendParticles(ParticleTypes.CLOUD, muzzle.x, muzzle.y - 0.1, muzzle.z, 3, 0.05, 0.05, 0.05, 0.02);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.8F, 1.6F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 1.8F);
		player.getCooldowns().addCooldown(gun, config.rivetGunCooldown);
		return Result.FIRED;
	}

	/** 인벤토리의 첫 리벳 묶음. 없으면 빈 아이템. */
	public static ItemStack findRivet(Player player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(SteamGear.RIVET)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}
}
