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

/** 기계 화면. 칸 배치는 화로와 같아서 화로 GUI 텍스처를 그대로 쓴다. */
public class MachineMenu extends AbstractContainerMenu {
	private static final int PLAYER_START = 3;
	private static final int HOTBAR_START = 30;
	private static final int PLAYER_END = 39;

	private final Container container;
	private final ContainerData data;

	/** 클라이언트 쪽: 서버가 칸 내용과 진행 값을 보내 준다. */
	public MachineMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(MachineBlockEntity.DATA_COUNT));
	}

	public MachineMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMachines.MACHINE_MENU, containerId);
		checkContainerSize(container, 3);
		checkContainerDataCount(data, MachineBlockEntity.DATA_COUNT);
		this.container = container;
		this.data = data;
		container.startOpen(inventory.player);
		addSlot(new Slot(container, MachineBlockEntity.SLOT_INPUT, 56, 17));
		addSlot(new Slot(container, MachineBlockEntity.SLOT_FUEL, 56, 53) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return MachineBlockEntity.isFuel(stack);
			}
		});
		addSlot(new Slot(container, MachineBlockEntity.SLOT_RESULT, 116, 35) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
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
		} else if (MachineBlockEntity.isFuel(stack)) {
			if (!moveItemStackTo(stack, MachineBlockEntity.SLOT_FUEL, MachineBlockEntity.SLOT_FUEL + 1, false)
					&& !moveItemStackTo(stack, MachineBlockEntity.SLOT_INPUT, MachineBlockEntity.SLOT_INPUT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, MachineBlockEntity.SLOT_INPUT, MachineBlockEntity.SLOT_INPUT + 1, false)) {
			if (slotIndex < HOTBAR_START ? !moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false) : !moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false)) {
				return ItemStack.EMPTY;
			}
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
}
