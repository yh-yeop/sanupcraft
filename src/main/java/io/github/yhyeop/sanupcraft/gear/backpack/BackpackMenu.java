package io.github.yhyeop.sanupcraft.gear.backpack;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 배낭 창: 바닐라 상자 창(9x3 · 9x6)과 같은 모양이다 (클라이언트는 바닐라 창을 그대로 쓴다).
 * 상자 창과 다른 점: 칸에 배낭을 넣을 수 없고, 열려 있는 배낭 자신은 집거나 옮기거나 버릴 수 없다.
 */
public class BackpackMenu extends AbstractContainerMenu {
	private final Container container;
	private final int rows;
	private final ItemStack backpack;

	public BackpackMenu(int containerId, Inventory inventory, BackpackContainer container, int rows) {
		super(rows == 6 ? MenuType.GENERIC_9x6 : MenuType.GENERIC_9x3, containerId);
		this.container = container;
		this.rows = rows;
		this.backpack = container.backpack();
		checkContainerSize(container, rows * 9);
		container.startOpen(inventory.player);
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < 9; x++) {
				addSlot(new BackpackSlot(container, x + y * 9, 8 + x * 18, 18 + y * 18));
			}
		}
		addStandardInventorySlots(inventory, 8, 18 + rows * 18 + 13);
	}

	/** 배낭 칸: 배낭은 넣을 수 없다. */
	private static final class BackpackSlot extends Slot {
		BackpackSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return BackpackContainer.canHold(stack);
		}
	}

	@Override
	public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
		if (touchesOpenBackpack(slotIndex, button, input, player)) {
			return;
		}
		super.clicked(slotIndex, button, input, player);
	}

	/** 이 클릭이 열려 있는 배낭 자신을 건드리나 (칸을 누르거나, 숫자 키 바꾸기로 가져가거나). */
	private boolean touchesOpenBackpack(int slotIndex, int button, ContainerInput input, Player player) {
		if (slotIndex >= 0 && slotIndex < slots.size() && slots.get(slotIndex).getItem() == backpack) {
			return true;
		}
		if (input == ContainerInput.SWAP && button >= 0 && button < player.getInventory().getContainerSize()) {
			return player.getInventory().getItem(button) == backpack;
		}
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = slots.get(slotIndex);
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			int containerSlots = rows * 9;
			if (slotIndex < containerSlots) {
				if (!moveItemStackTo(stack, containerSlots, slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!moveItemStackTo(stack, 0, containerSlots, false)) {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return clicked;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		container.stopOpen(player);
		container.setChanged();
	}
}
