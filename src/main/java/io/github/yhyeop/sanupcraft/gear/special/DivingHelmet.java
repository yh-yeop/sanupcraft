package io.github.yhyeop.sanupcraft.gear.special;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;

/**
 * 잠수 헬멧 (G5): 증기 = 산소. 머리가 물에 잠겨 있고 증기가 있으면 1초(20틱)마다 증기 1을 쓰고
 * 수중 호흡 + 짧은 야간 투시(물속이 맑게 보이도록)를 건다. 물 밖으로 나오면 야간 투시는 거둔다 (수중 호흡은 몇 초 뒤 저절로 끝).
 * 효과는 {@link SpecialEffects}의 모양으로 걸어서 야간 투시 모듈 · 물약과 구별해 우리 것만 거둔다.
 */
public final class DivingHelmet {
	public static final int CAPACITY = 1000;
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json (1초마다 드는 증기). */
	public static final int STEAM_PER_SECOND = 1;
	/** 수중 호흡 시간 (1초마다 다시 걸어서 이어진다). */
	static final int BREATHING_TICKS = 60;
	/** 야간 투시는 200틱 아래부터 깜빡여서 넉넉히 건다. */
	static final int VISION_TICKS = 300;
	private static final int VISION_REFRESH = 240;

	private DivingHelmet() {
	}

	/** 입고 있는 잠수 헬멧 (없으면 빈 것). */
	public static ItemStack worn(LivingEntity entity) {
		ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
		return head.is(ModSpecialGear.DIVING_HELMET) ? head : ItemStack.EMPTY;
	}

	/** 머리(눈 높이)가 물속인가. */
	public static boolean submerged(LivingEntity entity) {
		BlockPos eye = BlockPos.containing(entity.getEyePosition());
		FluidState fluid = entity.level().getFluidState(eye);
		return fluid.is(FluidTags.WATER) && entity.getEyeY() < eye.getY() + fluid.getHeight(entity.level(), eye) + 0.11;
	}

	/** 1초에 한 번 (시험이 직접 부른다). 물속에서 증기를 썼으면 true. 창작 모드도 증기는 그대로 쓴다 (작은 값이라). */
	public static boolean tickSecond(LivingEntity entity) {
		ItemStack helmet = worn(entity);
		if (!helmet.isEmpty() && submerged(entity) && SteamCharge.get(helmet) > 0) {
			SteamCharge.take(helmet, Math.max(0, SanupcraftConfig.current().divingSteamPerSecond));
			// 수중 호흡은 은은하게 (아이콘이 깜빡이지 않게), 야간 투시는 은은하지 않게 (야간 투시 모듈이 거두는 것과 구별)
			SpecialEffects.give(entity, MobEffects.WATER_BREATHING, 0, BREATHING_TICKS, BREATHING_TICKS, true);
			SpecialEffects.give(entity, MobEffects.NIGHT_VISION, 0, VISION_TICKS, VISION_REFRESH, false);
			return true;
		}
		SpecialEffects.take(entity, MobEffects.NIGHT_VISION, 0, VISION_TICKS, false);
		return false;
	}
}
