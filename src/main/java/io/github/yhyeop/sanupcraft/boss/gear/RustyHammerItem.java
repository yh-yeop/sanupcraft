package io.github.yhyeop.sanupcraft.boss.gear;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.effect.ModEffects;

/** 녹슨 망치 (녹슨 수호자 전용 장비, 확장 3 C4): 피해 9 · 매우 느린 무거운 무기. 맞은 상대는 녹 II (방어 −4, 5초). 막는 방패를 3초 끈다. */
public class RustyHammerItem extends Item {
	public static final float DAMAGE = 9.0F;
	/** 공격 속도 4 + 이 값 = 0.8 (1.25초에 한 번). */
	public static final float ATTACK_SPEED = -3.2F;
	public static final float DISABLE_BLOCKING_SECONDS = 3.0F;
	public static final int DURABILITY = 900;
	/** 효과 수치는 보스 설정(statusEffect.*)과 일부러 따로 둔 장비 수치 (툴팁이 이 수치를 적는다). */
	public static final int RUST_TICKS = 100;
	public static final int RUST_LEVEL = 2;

	public RustyHammerItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		ModEffects.inflict(target, ModEffects.RUST, RUST_TICKS, RUST_LEVEL, attacker);
	}
}
