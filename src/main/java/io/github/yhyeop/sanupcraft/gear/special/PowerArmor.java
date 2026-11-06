package io.github.yhyeop.sanupcraft.gear.special;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;

/**
 * 증기 파워 아머 (G5). 흉갑에 증기 8000. 한 벌을 다 입고 흉갑에 증기가 있으면:
 * 힘 II · 점프 강화 I (1초마다 다시 거는 짧은 효과), 넉백 저항 +0.5 · 오르는 높이 +0.5 (속성 수정자).
 * 대신 1초마다 증기 {@link #STEAM_PER_SECOND}(설정)를 쓴다 (창작 모드는 공짜). 증기가 떨어지거나 벗으면 다 거둔다.
 * 흉갑에 제트팩 모듈을 끼우면 제트팩처럼 뜬다 (같은 흉갑 증기를 씀, {@code gear.Jetpack#worn}).
 */
public final class PowerArmor {
	public static final int CAPACITY = 8000;
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int STEAM_PER_SECOND = 4;
	public static final double KNOCKBACK_RESISTANCE = 0.5;
	public static final double STEP_HEIGHT = 0.5;
	/** 효과 시간 (1초마다 다시 건다). */
	static final int EFFECT_TICKS = 60;
	static final int STRENGTH_AMPLIFIER = 1;
	static final int JUMP_AMPLIFIER = 0;

	public static final Identifier KNOCKBACK_ID = Sanupcraft.id("power_armor_knockback");
	public static final Identifier STEP_ID = Sanupcraft.id("power_armor_step");

	private PowerArmor() {
	}

	public static boolean wearsFullSet(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModSpecialGear.POWER_HELMET)
				&& entity.getItemBySlot(EquipmentSlot.CHEST).is(ModSpecialGear.POWER_CHESTPLATE)
				&& entity.getItemBySlot(EquipmentSlot.LEGS).is(ModSpecialGear.POWER_LEGGINGS)
				&& entity.getItemBySlot(EquipmentSlot.FEET).is(ModSpecialGear.POWER_BOOTS);
	}

	/** 한 벌 + 흉갑 증기가 있어 힘이 들어간 상태인가. */
	public static boolean active(LivingEntity entity) {
		return wearsFullSet(entity) && SteamCharge.get(entity.getItemBySlot(EquipmentSlot.CHEST)) > 0;
	}

	/**
	 * 한 틱 (시험이 직접 부른다). {@code second}면 증기를 쓰고 효과를 다시 건다.
	 * 속성은 매 틱 맞춘다 (벗자마자 풀리게).
	 */
	public static void tick(LivingEntity entity, boolean second) {
		if (second && active(entity)) {
			ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
			if (!(entity instanceof Player player && player.hasInfiniteMaterials())) {
				SteamCharge.take(chest, Math.max(0, SanupcraftConfig.current().powerArmorSteamPerSecond));
			}
		}
		boolean on = active(entity);
		SpecialEffects.modifier(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, on, KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE);
		SpecialEffects.modifier(entity, Attributes.STEP_HEIGHT, STEP_ID, on, STEP_HEIGHT, AttributeModifier.Operation.ADD_VALUE);
		if (on) {
			if (second) {
				SpecialEffects.give(entity, MobEffects.STRENGTH, STRENGTH_AMPLIFIER, EFFECT_TICKS, EFFECT_TICKS, true);
				SpecialEffects.give(entity, MobEffects.JUMP_BOOST, JUMP_AMPLIFIER, EFFECT_TICKS, EFFECT_TICKS, true);
			}
		} else {
			SpecialEffects.take(entity, MobEffects.STRENGTH, STRENGTH_AMPLIFIER, EFFECT_TICKS, true);
			SpecialEffects.take(entity, MobEffects.JUMP_BOOST, JUMP_AMPLIFIER, EFFECT_TICKS, true);
		}
	}
}
