package io.github.yhyeop.sanupcraft.storage;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 자동 제작대 화면: 왼쪽 견본 3x3 (유령 칸), 가운데 견본 결과 (보기만), 오른쪽 재료 3x3, 맨 오른쪽 결과, 아래 인벤토리.
 * 인벤토리를 Shift 클릭하면 견본에 맞는 재료 칸으로 간다.
 */
public class AutocrafterMenu extends AbstractContainerMenu {
	public static final int PATTERN_START = 0;
	public static final int INPUT_START = 9;
	public static final int OUTPUT = 18;
	public static final int PREVIEW = 19;
	public static final int INVENTORY_START = 20;

	private final Container container;
	private final GhostContainer pattern;
	private final ContainerData data;
	private final @Nullable AutocrafterBlockEntity crafter;
	private final SimpleContainer preview = new SimpleContainer(1);

	/** 클라이언트 쪽. */
	public AutocrafterMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, null, new SimpleContainer(AutocrafterBlockEntity.SLOT_COUNT), new GhostContainer(AutocrafterBlockEntity.GRID),
				new SimpleContainerData(AutocrafterBlockEntity.DATA_COUNT));
	}

	public AutocrafterMenu(int containerId, Inventory inventory, AutocrafterBlockEntity crafter, GhostContainer pattern, ContainerData data) {
		this(containerId, inventory, crafter, crafter, pattern, data);
	}

	private AutocrafterMenu(int containerId, Inventory inventory, @Nullable AutocrafterBlockEntity crafter, Container container, GhostContainer pattern,
			ContainerData data) {
		super(ModStorage.AUTOCRAFTER_MENU, containerId);
		this.crafter = crafter;
		this.container = container;
		this.pattern = pattern;
		this.data = data;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				addSlot(new LockedSlot(pattern, col + row * 3, 8 + col * 18, 18 + row * 18));
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int index = col + row * 3;
				addSlot(new Slot(container, index, 92 + col * 18, 18 + row * 18) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return AutocrafterMenu.this.crafter == null || AutocrafterMenu.this.crafter.matchesPattern(index, stack);
					}
				});
			}
		}
		addSlot(new Slot(container, AutocrafterBlockEntity.SLOT_OUTPUT, 152, 36) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});
		addSlot(new LockedSlot(preview, 0, 66, 36));
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
	}

	public int steam() {
		return data.get(AutocrafterBlockEntity.DATA_STEAM);
	}

	@Override
	public void broadcastChanges() {
		if (crafter != null) {
			ItemStack result = crafter.patternResult();
			if (!ItemStack.matches(preview.getItem(0), result)) {
				preview.setItem(0, result.copy());
			}
		}
		super.broadcastChanges();
	}

	@Override
	public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
		if (slotIndex >= PATTERN_START && slotIndex < INPUT_START) {
			if (pattern.click(slotIndex, button, input, player, getCarried())) {
				return;
			}
		} else if (slotIndex == PREVIEW) {
			return;
		}
		super.clicked(slotIndex, button, input, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem() || slotIndex < INPUT_START || slotIndex == PREVIEW) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex >= INVENTORY_START) {
			if (!moveItemStackTo(stack, INPUT_START, OUTPUT, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, INVENTORY_START, slots.size(), true)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	/** 놓기 · 집기를 막은 칸 (견본 · 결과 미리보기). 클릭은 {@link #clicked}가 직접 처리한다. */
	private static class LockedSlot extends Slot {
		LockedSlot(Container container, int slot, int x, int y) {
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
	}
}
