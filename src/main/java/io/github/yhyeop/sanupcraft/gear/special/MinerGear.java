package io.github.yhyeop.sanupcraft.gear.special;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.hazard.ModHazard;

/**
 * 광부 갑옷 (G5). 헬멧: 머리 조명({@link HeadLamp}) · 어둠 효과가 안 걸림 · 유독 가스 면역({@code Resistances.toxicGasImmune}) ·
 * 둘레 4칸 안에 가스가 있으면 행동 막대에 경고. 한 벌: 캐는 속도 +20% · 낙하 피해 -50% (속성 수정자, 벗으면 뗀다).
 */
public final class MinerGear {
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int MINING_SPEED_PERCENT = 20;
	public static final int FALL_REDUCTION_PERCENT = 50;
	public static final int GAS_WARN_RANGE = 4;

	public static final Identifier MINING_ID = Sanupcraft.id("miner_set_mining");
	public static final Identifier FALL_ID = Sanupcraft.id("miner_set_fall");

	private MinerGear() {
	}

	public static boolean wearsHelmet(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModSpecialGear.MINER_HELMET);
	}

	public static boolean wearsFullSet(LivingEntity entity) {
		return wearsHelmet(entity) && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModSpecialGear.MINER_CHESTPLATE)
				&& entity.getItemBySlot(EquipmentSlot.LEGS).is(ModSpecialGear.MINER_LEGGINGS)
				&& entity.getItemBySlot(EquipmentSlot.FEET).is(ModSpecialGear.MINER_BOOTS);
	}

	/** 매 틱 (시험이 직접 부른다): 한 벌 속성을 맞추고, 헬멧이면 이미 걸린 어둠을 푼다 (새로 걸리는 것은 효과 이벤트가 막는다). */
	public static void tick(LivingEntity entity) {
		SanupcraftConfig config = SanupcraftConfig.current();
		boolean set = wearsFullSet(entity);
		SpecialEffects.modifier(entity, Attributes.BLOCK_BREAK_SPEED, MINING_ID, set, config.minerMiningSpeedPercent / 100.0,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		SpecialEffects.modifier(entity, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_ID, set, -config.minerFallReductionPercent / 100.0,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		if (wearsHelmet(entity) && entity.hasEffect(MobEffects.DARKNESS)) {
			entity.removeEffect(MobEffects.DARKNESS);
		}
	}

	/** 이 효과를 막는가 (광부 헬멧 = 어둠). */
	public static boolean blocks(LivingEntity entity, MobEffectInstance effect) {
		return effect.is(MobEffects.DARKNESS) && wearsHelmet(entity);
	}

	/** 머리 둘레 {@code range}칸(정육면체) 안의 가장 가까운 유독 가스 칸. 없으면 null. */
	public static @Nullable BlockPos nearestGas(LivingEntity entity, int range) {
		Level level = entity.level();
		BlockPos head = BlockPos.containing(entity.getEyePosition());
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(head.offset(-range, -range, -range), head.offset(range, range, range))) {
			if (level.getBlockState(pos).is(ModHazard.TOXIC_GAS)) {
				double distance = pos.distSqr(head);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = pos.immutable();
				}
			}
		}
		return best;
	}

	/** 1초에 한 번: 헬멧을 쓴 플레이어 둘레에 가스가 있으면 행동 막대에 경고 (거리 칸 수). 경고했으면 true. */
	public static boolean warnGas(ServerPlayer player) {
		int range = SanupcraftConfig.current().minerGasWarnRange;
		if (range <= 0 || !wearsHelmet(player)) {
			return false;
		}
		BlockPos gas = nearestGas(player, range);
		if (gas == null) {
			return false;
		}
		if (player.connection != null) {
			int distance = (int) Math.round(Math.sqrt(gas.distSqr(BlockPos.containing(player.getEyePosition()))));
			player.sendOverlayMessage(Component.translatable("item.sanupcraft.miner_helmet.gas_warning", distance)
					.withStyle(ChatFormatting.RED));
		}
		return true;
	}
}
