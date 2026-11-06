package io.github.yhyeop.sanupcraft.gear.backpack;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 배낭: 들고 우클릭하면 창고 창이 열린다. 칸은 {@code rows * 9} (배낭 27, 강철 배낭 54).
 * 내용은 아이템의 컴포넌트 {@code minecraft:container}에 담긴다 ({@link BackpackContainer}).
 */
public class BackpackItem extends Item {
	private final int rows;

	public BackpackItem(Item.Properties properties, int rows) {
		super(properties);
		this.rows = rows;
	}

	public int rows() {
		return rows;
	}

	public int size() {
		return rows * 9;
	}

	/** 배낭은 셜커 상자 · 묶음 속에 넣을 수 없다. */
	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			BackpackContainer container = new BackpackContainer(stack, size());
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BackpackMenu(id, inventory, container, rows), stack.getHoverName()));
			player.awardStat(Stats.ITEM_USED.get(this));
		}
		return InteractionResult.SUCCESS;
	}
}
