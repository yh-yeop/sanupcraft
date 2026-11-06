package io.github.yhyeop.sanupcraft.storage;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 재고 유지기 화면: 견본 1칸 (유령 칸) + 유지할 개수 (버튼 -64 · -8 · -1 · +1 · +8 · +64, 바닐라 버튼 패킷).
 * 인벤토리를 Shift 클릭하면 그 아이템이 견본이 된다.
 */
public class StockerMenu extends AbstractContainerMenu {
	public static final int[] STEPS = {-64, -8, -1, 1, 8, 64};

	private final GhostContainer target;
	private final ContainerData data;
	private final @Nullable StockerBlockEntity stocker;

	public StockerMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, null, new GhostContainer(1), new SimpleContainerData(StockerBlockEntity.DATA_COUNT));
	}

	public StockerMenu(int containerId, Inventory inventory, StockerBlockEntity stocker, ContainerData data) {
		this(containerId, inventory, stocker, stocker.target(), data);
	}

	private StockerMenu(int containerId, Inventory inventory, @Nullable StockerBlockEntity stocker, GhostContainer target, ContainerData data) {
		super(ModStorage.STOCKER_MENU, containerId);
		this.stocker = stocker;
		this.target = target;
		this.data = data;
		addSlot(new Slot(target, 0, 26, 35) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public boolean mayPickup(Player player) {
				return false;
			}
		});
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
	}

	public int targetCount() {
		return data.get(StockerBlockEntity.DATA_TARGET);
	}

	public int have() {
		return data.get(StockerBlockEntity.DATA_HAVE);
	}

	public StockerBlockEntity.Status status() {
		int id = data.get(StockerBlockEntity.DATA_STATUS);
		StockerBlockEntity.Status[] values = StockerBlockEntity.Status.values();
		return id >= 0 && id < values.length ? values[id] : StockerBlockEntity.Status.IDLE;
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (stocker == null || buttonId < 0 || buttonId >= STEPS.length) {
			return false;
		}
		stocker.setTargetCount(stocker.targetCount() + STEPS[buttonId]);
		return true;
	}

	@Override
	public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
		if (slotIndex == 0 && target.click(0, button, input, player, getCarried())) {
			return;
		}
		super.clicked(slotIndex, button, input, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex > 0 && slotIndex < slots.size() && slots.get(slotIndex).hasItem()) {
			target.setSample(0, slots.get(slotIndex).getItem());
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return stocker == null || Container.stillValidBlockEntity(stocker, player);
	}
}
