package io.github.yhyeop.sanupcraft.effect;

import java.util.function.Predicate;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 우리 상태 이상의 공통 부모. 막는 장비({@link Resistances})를 입고 있으면 다음 틱에 풀린다
 * (걸린 뒤 갑옷을 다 입어도 풀리고, 명령어 · 다른 몹이 건 효과도 같다). 속성 값 변화는 바닐라처럼 효과에 붙인 수정자로 한다.
 */
public class SanupcraftEffect extends MobEffect {
	private final Predicate<LivingEntity> resists;

	public SanupcraftEffect(int color, Predicate<LivingEntity> resists) {
		super(MobEffectCategory.HARMFUL, color);
		this.resists = resists;
	}

	public boolean resists(LivingEntity entity) {
		return resists.test(entity);
	}

	/** 매 틱 막는 장비를 본다. */
	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
		return true;
	}

	/** false를 돌려주면 효과가 풀린다 (속성 수정자도 같이 빠진다). */
	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
		return !resists(mob);
	}
}
