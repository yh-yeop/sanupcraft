package io.github.yhyeop.sanupcraft.factory;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 기계 화면: 재료(합금로는 둘) · 연료 · 결과 · 부산물. 칸 위치는 MachineScreen의 텍스처와 맞춘다.
 */
public class MachineMenu extends AbstractContainerMenu {
	public static final int INPUT_X = 56;
	public static final int INPUT2_X = 34;
	public static final int INPUT_Y = 17;
	public static final int FUEL_Y = 53;
	public static final int RESULT_X = 116;
	public static final int RESULT_Y = 35;
	public static final int BYPRODUCT_X = 144;

	private static final int MACHINE_SLOTS = MachineBlockEntity.SLOT_COUNT;
	private static final int PLAYER_START = MACHINE_SLOTS;
	private static final int HOTBAR_START = PLAYER_START + 27;
	private static final int PLAYER_END = HOTBAR_START + 9;

	private final Container container;
	private final ContainerData data;

	/** 클라이언트 쪽: 서버가 칸 내용과 진행 값을 보내 준다. */
	public MachineMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(MachineBlockEntity.DATA_COUNT));
	}

	public MachineMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMachines.MACHINE_MENU, containerId);
		checkContainerSize(container, MACHINE_SLOTS);
		checkContainerDataCount(data, MachineBlockEntity.DATA_COUNT);
		this.container = container;
		this.data = data;
		container.startOpen(inventory.player);
		addSlot(new Slot(container, MachineBlockEntity.SLOT_INPUT, INPUT_X, INPUT_Y));
		addSlot(new Slot(container, MachineBlockEntity.SLOT_FUEL, INPUT_X, FUEL_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return MachineBlockEntity.isFuel(stack);
			}
		});
		addSlot(new OutputSlot(container, MachineBlockEntity.SLOT_RESULT, RESULT_X, RESULT_Y));
		addSlot(new OutputSlot(container, MachineBlockEntity.SLOT_BYPRODUCT, BYPRODUCT_X, RESULT_Y));
		addSlot(new Slot(container, MachineBlockEntity.SLOT_INPUT2, INPUT2_X, INPUT_Y) {
			@Override
			public boolean isActive() {
				return twoInputs();
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return twoInputs();
			}
		});
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
	}

	public MachineKind kind() {
		MachineKind[] kinds = MachineKind.values();
		int index = data.get(MachineBlockEntity.DATA_KIND);
		return index >= 0 && index < kinds.length ? kinds[index] : MachineKind.CRUSHER;
	}

	public boolean twoInputs() {
		return kind().twoInputs();
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		container.stopOpen(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex < PLAYER_START) {
			if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveToMachine(stack)
				&& (slotIndex < HOTBAR_START ? !moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false) : !moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false))) {
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

	/** 연료면 연료 칸, 아니면 재료 칸 (합금로는 둘째 칸까지). */
	private boolean moveToMachine(ItemStack stack) {
		if (MachineBlockEntity.isFuel(stack) && moveItemStackTo(stack, MachineBlockEntity.SLOT_FUEL, MachineBlockEntity.SLOT_FUEL + 1, false)) {
			return true;
		}
		if (moveItemStackTo(stack, MachineBlockEntity.SLOT_INPUT, MachineBlockEntity.SLOT_INPUT + 1, false)) {
			return true;
		}
		return twoInputs() && moveItemStackTo(stack, MachineBlockEntity.SLOT_INPUT2, MachineBlockEntity.SLOT_INPUT2 + 1, false);
	}

	public float progress() {
		int current = data.get(MachineBlockEntity.DATA_PROGRESS);
		int total = data.get(MachineBlockEntity.DATA_TOTAL_TIME);
		return total != 0 && current != 0 ? Mth.clamp((float) current / total, 0.0F, 1.0F) : 0.0F;
	}

	public float litProgress() {
		int duration = data.get(MachineBlockEntity.DATA_LIT_DURATION);
		if (duration == 0) {
			duration = 200;
		}
		return Mth.clamp((float) data.get(MachineBlockEntity.DATA_LIT_TIME) / duration, 0.0F, 1.0F);
	}

	public boolean isLit() {
		return data.get(MachineBlockEntity.DATA_LIT_TIME) > 0;
	}

	/** 결과 · 부산물 칸: 꺼내기만 한다. */
	private static class OutputSlot extends Slot {
		OutputSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}
}
