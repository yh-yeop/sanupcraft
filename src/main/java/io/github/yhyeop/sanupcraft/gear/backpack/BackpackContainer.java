package io.github.yhyeop.sanupcraft.gear.backpack;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * 배낭 안의 칸들. 배낭 아이템의 컴포넌트 {@code minecraft:container}를 읽어 오고, 바뀔 때마다 거기에 되쓴다
 * (그래서 떨어뜨리거나 상자에 넣어도 내용이 그대로고, 따로 저장할 곳이 없다).
 */
public class BackpackContainer extends SimpleContainer {
	private final ItemStack backpack;

	public BackpackContainer(ItemStack backpack, int size) {
		super(size);
		this.backpack = backpack;
		backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(getItems());
	}

	public ItemStack backpack() {
		return backpack;
	}

	/** 배낭끼리는 넣을 수 없다 (자기 자신 포함). 셜커 상자처럼 "상자 속 상자 속 상자"를 막는다. */
	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return canHold(stack);
	}

	public static boolean canHold(ItemStack stack) {
		return !(stack.getItem() instanceof BackpackItem);
	}

	@Override
	public void setChanged() {
		super.setChanged();
		backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
	}

	/** 이 배낭이 아직 플레이어의 가방(손 포함)에 있나. 옮겨지거나 사라지면 창을 닫는다. */
	@Override
	public boolean stillValid(Player player) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i) == backpack) {
				return true;
			}
		}
		return false;
	}
}
