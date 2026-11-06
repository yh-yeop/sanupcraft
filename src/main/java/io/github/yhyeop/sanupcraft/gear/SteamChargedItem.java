package io.github.yhyeop.sanupcraft.gear;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * 증기를 담는 아이템의 바탕 (증기 통 · 증기 부츠). 저장량은 {@link SteamCharge} 컴포넌트에 있고,
 * 여기서는 최대치, 아이템 막대(내구도 막대처럼), 툴팁의 "증기 n / 최대"만 맡는다.
 * 증기가 있으면 막대는 증기량, 없으면 내구도(있다면)를 보여 준다.
 */
public class SteamChargedItem extends Item {
	private final int capacity;

	public SteamChargedItem(Item.Properties properties, int capacity) {
		super(properties);
		this.capacity = capacity;
	}

	public int steamCapacity() {
		return capacity;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return SteamCharge.get(stack) > 0 || super.isBarVisible(stack);
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		int steam = SteamCharge.get(stack);
		return steam > 0 ? Mth.clamp(Math.round(13.0F * steam / capacity), 1, 13) : super.getBarWidth(stack);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return SteamCharge.get(stack) > 0 ? SteamCharge.BAR_COLOR : super.getBarColor(stack);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, builder, flag);
		builder.accept(Component.translatable("item.sanupcraft.steam_charge", SteamCharge.get(stack), capacity).withStyle(ChatFormatting.AQUA));
	}
}
