package io.github.yhyeop.sanupcraft.server;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 설명서: 우클릭하면 책 화면이 열린다. 쪽 내용은 아이템의 written_book_content 컴포넌트 ({@link GuideBook#content()}). */
public class GuideBookItem extends Item {
	public GuideBookItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		player.openItemGui(stack, hand);
		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResult.SUCCESS;
	}
}
