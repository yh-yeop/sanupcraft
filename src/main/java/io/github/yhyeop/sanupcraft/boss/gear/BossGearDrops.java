package io.github.yhyeop.sanupcraft.boss.gear;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.boss.PartyBossMonster;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 보스 전용 장비 보상 (확장 3 C4): 필수품을 받는 기여자({@link io.github.yhyeop.sanupcraft.boss.BossParty#contributors}) 각자
 * 확률 {@code 기본 + 단계 × 단계당}(기본 10% + 5%/단계, 설정 {@code bossGear.*})로 그 보스의 장비 하나를 인벤토리로 받는다.
 * 기여자가 없으면 (명령어 · 환경 피해로 죽음) 아무도 받지 않는다.
 */
public final class BossGearDrops {
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final double BASE_CHANCE = 0.10;
	public static final double CHANCE_PER_TIER = 0.05;

	private BossGearDrops() {
	}

	/** 강화 단계 {@code tier}에서 기여자 한 명이 장비를 받을 확률 (0 ~ 1). */
	public static double chance(int tier) {
		SanupcraftConfig config = SanupcraftConfig.current();
		return Math.max(0.0, Math.min(1.0, config.bossGearChance + config.bossGearChancePerTier * Math.max(0, tier)));
	}

	/** 기여자마다 굴려 장비를 준다. 받은 사람 수를 돌려준다. */
	public static int reward(ServerLevel level, PartyBossMonster boss, Item gear) {
		double chance = chance(boss.party().tier());
		if (chance <= 0.0) {
			return 0;
		}
		List<ServerPlayer> players = boss.party().contributors(level, boss.getMaxHealth());
		int given = 0;
		for (ServerPlayer player : players) {
			if (level.getRandom().nextDouble() >= chance) {
				continue;
			}
			ItemStack stack = new ItemStack(gear);
			Component name = stack.getHoverName();
			if (!player.getInventory().add(stack) && !stack.isEmpty()) {
				player.spawnAtLocation(level, stack);
			}
			if (player.connection != null) {
				player.sendSystemMessage(Component.translatable("entity.sanupcraft.boss.reward_gear", boss.getDisplayName(), name).withStyle(ChatFormatting.LIGHT_PURPLE));
			}
			given++;
		}
		return given;
	}
}
