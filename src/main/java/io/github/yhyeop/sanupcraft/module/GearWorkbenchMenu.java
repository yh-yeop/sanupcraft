package io.github.yhyeop.sanupcraft.module;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 장비 작업대 화면 (G3): 장비 칸 하나 + 모듈 칸 셋 (장비의 칸 수만큼만 열림) + 인벤토리.
 * 모듈 칸은 장비에 끼운 모듈을 보여 주는 창이다: 모듈 아이템을 칸에 넣으면 장비에 끼워지고, 꺼내면 빠져 아이템으로 돌아온다.
 * 장비를 꺼내면 모듈 칸은 비고(모듈은 장비에 남음), 화면을 닫으면 장비 칸의 장비만 돌려준다.
 */
public class GearWorkbenchMenu extends AbstractContainerMenu {
	public static final int GEAR_SLOT = 0;
	public static final int FIRST_MODULE_SLOT = 1;
	public static final int INVENTORY_START = FIRST_MODULE_SLOT + ModuleSlots.MAX;
	public static final int INVENTORY_END = INVENTORY_START + 36;
	public static final int GEAR_X = 35;
	public static final int GEAR_Y = 35;
	public static final int MODULE_X = 89;
	public static final int MODULE_Y = 35;
	public static final int MODULE_STEP = 24;

	private final ContainerLevelAccess access;
	private final SimpleContainer gear;
	private final SimpleContainer modules;
	private boolean syncing;

	public GearWorkbenchMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public GearWorkbenchMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(ModModules.GEAR_WORKBENCH_MENU, containerId);
		this.access = access;
		this.gear = new SimpleContainer(1) {
			@Override
			public void setChanged() {
				super.setChanged();
				gearChanged();
			}
		};
		this.modules = new SimpleContainer(ModuleSlots.MAX) {
			@Override
			public void setChanged() {
				super.setChanged();
				modulesChanged();
			}
		};
		addSlot(new Slot(gear, 0, GEAR_X, GEAR_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return ModuleSlots.capacity(stack) > 0;
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		for (int i = 0; i < ModuleSlots.MAX; i++) {
			addSlot(new ModuleSlot(i));
		}
		addStandardInventorySlots(inventory, 8, 84);
	}

	/** 장비 칸의 장비 (없으면 빈 아이템). */
	public ItemStack gear() {
		return gear.getItem(0);
	}

	/** 지금 장비의 칸 수. */
	public int capacity() {
		return ModuleSlots.capacity(gear());
	}

	/** 장비가 바뀌면 모듈 칸을 그 장비의 모듈로 다시 채운다. */
	private void gearChanged() {
		if (syncing || modules == null) {
			return;
		}
		syncing = true;
		try {
			List<GearModule> installed = GearModules.get(gear()).modules();
			for (int i = 0; i < ModuleSlots.MAX; i++) {
				modules.setItem(i, i < installed.size() ? new ItemStack(installed.get(i).item()) : ItemStack.EMPTY);
			}
		} finally {
			syncing = false;
		}
	}

	/** 모듈 칸이 바뀌면 장비의 모듈 목록을 칸 순서대로 다시 적는다. */
	private void modulesChanged() {
		if (syncing || gear == null) {
			return;
		}
		ItemStack stack = gear();
		if (stack.isEmpty()) {
			return;
		}
		List<GearModule> list = new ArrayList<>();
		for (int i = 0; i < ModuleSlots.MAX; i++) {
			if (modules.getItem(i).getItem() instanceof ModuleItem item) {
				list.add(item.module());
			}
		}
		GearModules.set(stack, list);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < INVENTORY_START) {
			if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.getItem() instanceof ModuleItem) {
			Slot target = null;
			for (int i = 0; i < ModuleSlots.MAX && target == null; i++) {
				Slot candidate = slots.get(FIRST_MODULE_SLOT + i);
				if (!candidate.hasItem() && candidate.mayPlace(stack)) {
					target = candidate;
				}
			}
			if (target == null) {
				return ItemStack.EMPTY;
			}
			target.setByPlayer(stack.split(1));
		} else if (ModuleSlots.capacity(stack) > 0 && !slots.get(GEAR_SLOT).hasItem()) {
			if (!moveItemStackTo(stack, GEAR_SLOT, GEAR_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (index < INVENTORY_START + 27) {
			if (!moveItemStackTo(stack, INVENTORY_START + 27, INVENTORY_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_START + 27, false)) {
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
	public void removed(Player player) {
		super.removed(player);
		access.execute((level, pos) -> clearContainer(player, gear));
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, ModModules.GEAR_WORKBENCH);
	}

	/** 모듈 칸 하나. 장비의 칸 수 안쪽만 열리고, 맞는 모듈 하나만 받는다. */
	private final class ModuleSlot extends Slot {
		private final int index;

		ModuleSlot(int index) {
			super(modules, index, MODULE_X + index * MODULE_STEP, MODULE_Y);
			this.index = index;
		}

		@Override
		public boolean isActive() {
			return index < capacity();
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			if (!isActive() || !(stack.getItem() instanceof ModuleItem item)) {
				return false;
			}
			// 이 칸에 있던 모듈은 빠진다고 보고 본다 (칸이 꽉 찼을 때 바꿔 끼우기)
			ItemStack test = gear().copy();
			if (getItem().getItem() instanceof ModuleItem current) {
				GearModules.remove(test, current.module());
			}
			return GearModules.check(test, item.module()) == GearModules.Check.OK;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}
}
