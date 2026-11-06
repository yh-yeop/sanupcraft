package io.github.yhyeop.sanupcraft.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteamChargedItem;

/**
 * 증기 해머 (무기, C1). 피해 10, 매우 느림. 자기 몸에 증기 600을 담는다 (증기 충전대).
 * 때릴 때 증기가 30 이상 있으면 30을 쓰고 상대를 0.6초(12틱) 기절시킨다. 창작 모드는 증기가 들지 않는다.
 * 3x3으로 캐는 강철 해머(SteelGear)와는 다른 아이템.
 */
public class SteamHammerItem extends SteamChargedItem {
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int CAPACITY = 600;
	public static final int STUN_STEAM = 30;
	public static final int STUN_TICKS = 12;

	public SteamHammerItem(Item.Properties properties) {
		super(properties, CAPACITY);
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		if (attacker.level() instanceof ServerLevel level) {
			strike(level, stack, target, attacker);
		}
	}

	/** 증기로 강타한다. 증기가 모자라면 아무것도 안 하고 false. 시험이 직접 부른다. */
	public static boolean strike(ServerLevel level, ItemStack hammer, LivingEntity target, LivingEntity attacker) {
		SanupcraftConfig config = SanupcraftConfig.current();
		boolean free = attacker instanceof Player player && player.hasInfiniteMaterials();
		if (!free) {
			if (SteamCharge.get(hammer) < config.steamHammerStunSteam) {
				return false;
			}
			SteamCharge.take(hammer, config.steamHammerStunSteam);
		}
		target.addEffect(new MobEffectInstance(ModCombat.STUNNED, config.steamHammerStunTicks, 0), attacker);
		level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.3, 0.2, 0.3, 0.05);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 1.6F);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.4F);
		return true;
	}
}
