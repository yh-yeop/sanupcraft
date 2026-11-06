package io.github.yhyeop.sanupcraft.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 기절 (증기 해머 강타). 이동 속도 −100% · 점프 −100% · 캐는 속도 −90% (플레이어).
 * 공격은 {@link ModCombat#init}의 피해 검사가 막는다 (기절한 쪽이 일으킨 피해는 들어가지 않음).
 * 몹은 길 찾기도 멈춘다. 머리 위에 별(치명타) 파티클.
 */
public class StunnedEffect extends CombatEffect {
	public StunnedEffect() {
		super(MobEffectCategory.HARMFUL, 0xE8D25A);
		addAttributeModifier(Attributes.MOVEMENT_SPEED, Sanupcraft.id("effect.stunned.speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addAttributeModifier(Attributes.JUMP_STRENGTH, Sanupcraft.id("effect.stunned.jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, Sanupcraft.id("effect.stunned.mining"), -0.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplification) {
		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
		}
		if (entity.tickCount % 4 == 0) {
			level.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + entity.getBbHeight() + 0.3, entity.getZ(), 2, 0.25, 0.05, 0.25, 0.0);
		}
		return true;
	}
}
