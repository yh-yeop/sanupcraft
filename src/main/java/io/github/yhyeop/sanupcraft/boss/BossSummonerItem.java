package io.github.yhyeop.sanupcraft.boss;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/** 보스 소환석. 강화 단계({@link BossTier})가 있으면 이름에 "+N"과 툴팁 한 줄이 붙는다. */
public class BossSummonerItem extends Item {
	public BossSummonerItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		return BossTier.barName(super.getName(stack), BossTier.of(stack));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, builder, flag);
		int tier = BossTier.of(stack);
		if (tier > 0) {
			SanupcraftConfig config = SanupcraftConfig.current();
			builder.accept(Component.translatable("item.sanupcraft.boss_tier.tooltip", tier,
					Math.round(config.bossTierHealthPerTier * tier * 100), Math.round(config.bossTierDamagePerTier * tier * 100))
					.withStyle(ChatFormatting.GOLD));
		}
	}
}
