package io.github.yhyeop.sanupcraft.logistics;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 필터 화면 (벨트 분류기 9칸 · 필터 깔때기 5칸). 필터 칸은 유령 칸이다: 아이템을 들고 클릭하면 1개짜리 견본이 놓이고
 * (손에 든 것은 줄지 않는다), 빈손으로 클릭하거나 Shift 클릭하면 비운다. 인벤토리 칸을 Shift 클릭하면 빈 필터 칸에 견본을 놓는다.
 * 클라이언트와 서버가 같은 메서드를 돌리므로 따로 패킷은 필요 없다. 칸 위치는 바닐라 발사기 · 깔때기 화면 그림과 맞춘다.
 */
public class FilterMenu extends AbstractContainerMenu {
	public static final int SORTER_SIZE = 9;
	public static final int HOPPER_SIZE = 5;

	private final Container filter;
	private final int size;
	private final @Nullable BlockEntity owner;

	/** 클라이언트 쪽 (칸 내용은 서버가 보내 준다). */
	public static FilterMenu sorter(int containerId, Inventory inventory) {
		return new FilterMenu(containerId, inventory, new ItemFilter(SORTER_SIZE), null);
	}

	public static FilterMenu hopper(int containerId, Inventory inventory) {
		return new FilterMenu(containerId, inventory, new ItemFilter(HOPPER_SIZE), null);
	}

	public FilterMenu(int containerId, Inventory inventory, Container filter, @Nullable BlockEntity owner) {
		super(typeOf(filter.getContainerSize()), containerId);
		this.filter = filter;
		this.size = filter.getContainerSize();
		this.owner = owner;
		if (size == SORTER_SIZE) {
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 3; col++) {
					addSlot(new GhostSlot(filter, col + row * 3, 62 + col * 18, 17 + row * 18));
				}
			}
			addStandardInventorySlots(inventory, 8, 84);
		} else {
			for (int i = 0; i < size; i++) {
				addSlot(new GhostSlot(filter, i, 44 + i * 18, 20));
			}
			addStandardInventorySlots(inventory, 8, 51);
		}
	}

	private static MenuType<FilterMenu> typeOf(int size) {
		return size == SORTER_SIZE ? ModLogistics.SORTER_MENU : ModLogistics.FILTER_HOPPER_MENU;
	}

	/** 화면이 고를 그림: 칸이 9개이면 발사기, 아니면 깔때기 모양. */
	public int filterSize() {
		return size;
	}

	@Override
	public boolean stillValid(Player player) {
		return owner == null || Container.stillValidBlockEntity(owner, player);
	}

	@Override
	public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
		if (slotIndex >= 0 && slotIndex < size) {
			switch (input) {
				case PICKUP -> setSample(slotIndex, getCarried());
				case QUICK_MOVE -> setSample(slotIndex, ItemStack.EMPTY);
				case SWAP -> setSample(slotIndex, swapSource(player, buttonNum));
				case CLONE, THROW -> {
				}
				// 끌어 놓기 · 두 번 클릭은 일반 처리 (유령 칸은 놓기 · 집기를 거부한다)
				default -> super.clicked(slotIndex, buttonNum, input, player);
			}
			return;
		}
		super.clicked(slotIndex, buttonNum, input, player);
	}

	private static ItemStack swapSource(Player player, int button) {
		if (button == 40) {
			return player.getOffhandItem();
		}
		return button >= 0 && button < 9 ? player.getInventory().getItem(button) : ItemStack.EMPTY;
	}

	private void setSample(int slot, ItemStack stack) {
		filter.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
	}

	/** Shift 클릭: 인벤토리 아이템은 줄지 않고, 빈 필터 칸에 견본만 놓는다. */
	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex >= size && slotIndex < slots.size()) {
			ItemStack stack = slots.get(slotIndex).getItem();
			if (!stack.isEmpty() && !sampleExists(stack)) {
				for (int i = 0; i < size; i++) {
					if (filter.getItem(i).isEmpty()) {
						setSample(i, stack);
						break;
					}
				}
			}
		}
		return ItemStack.EMPTY;
	}

	private boolean sampleExists(ItemStack stack) {
		for (int i = 0; i < size; i++) {
			if (ItemStack.isSameItem(filter.getItem(i), stack)) {
				return true;
			}
		}
		return false;
	}

	/** 놓기 · 집기를 막은 칸 (클릭은 {@link #clicked}가 직접 처리한다). */
	private static class GhostSlot extends Slot {
		GhostSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}
}
