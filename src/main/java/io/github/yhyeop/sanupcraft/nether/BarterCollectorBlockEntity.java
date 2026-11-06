package io.github.yhyeop.sanupcraft.nether;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * 교역 수집판 (M5): 10틱마다 반경 3(블록 둘레 3칸 상자) 안의 떨어진 아이템을 9칸에 빨아들인다. 피글린 교역품을 모으는 용도.
 * 금 분배기가 던진 금괴는 2초({@link #DISPENSED_GRACE_TICKS}) 동안 건드리지 않는다 (피글린이 먼저 줍게). 그 뒤에도 남은 금괴는 모은다.
 * 깔때기는 어느 면으로든 뺄 수 있고, 넣지는 못한다. 손으로는 발사기 모양 화면에서 넣고 뺀다.
 */
public class BarterCollectorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	public static final int SLOTS = 9;
	public static final int RADIUS = 3;
	public static final int TICKS_PER_SWEEP = 10;
	public static final int DISPENSED_GRACE_TICKS = 40;
	private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private int cooldown;

	public BarterCollectorBlockEntity(BlockPos pos, BlockState state) {
		super(ModNether.BARTER_COLLECTOR_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, BarterCollectorBlockEntity collector) {
		if (--collector.cooldown > 0) {
			return;
		}
		collector.cooldown = TICKS_PER_SWEEP;
		collector.sweep(level, pos);
	}

	/** 둘레의 떨어진 아이템을 빨아들인다. 빨아들인 아이템 개수를 돌려준다. */
	public int sweep(ServerLevel level, BlockPos pos) {
		int taken = 0;
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(RADIUS), BarterCollectorBlockEntity::collects)) {
			ItemStack stack = entity.getItem();
			int before = stack.getCount();
			ItemStack left = insert(stack.copy());
			int moved = before - left.getCount();
			if (moved > 0) {
				taken += moved;
				if (left.isEmpty()) {
					entity.discard();
				} else {
					entity.setItem(left);
				}
			}
		}
		if (taken > 0) {
			setChanged();
		}
		return taken;
	}

	/** 빨아들일 아이템인가: 살아 있고, 분배기가 막 던진 금괴(2초 안)가 아님. */
	static boolean collects(ItemEntity entity) {
		return entity.isAlive() && !entity.getItem().isEmpty()
				&& !(entity.entityTags().contains(GoldDispenserBlockEntity.THROWN_TAG) && entity.getAge() < DISPENSED_GRACE_TICKS);
	}

	/** 같은 아이템 칸에 먼저 합치고, 남으면 빈 칸에. 못 넣은 나머지를 돌려준다. */
	private ItemStack insert(ItemStack stack) {
		for (int i = 0; i < SLOTS && !stack.isEmpty(); i++) {
			ItemStack current = items.get(i);
			if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack)) {
				int move = Math.min(stack.getCount(), Math.min(getMaxStackSize(), current.getMaxStackSize()) - current.getCount());
				if (move > 0) {
					current.grow(move);
					stack.shrink(move);
				}
			}
		}
		for (int i = 0; i < SLOTS && !stack.isEmpty(); i++) {
			if (items.get(i).isEmpty()) {
				items.set(i, stack.split(Math.min(stack.getCount(), getMaxStackSize(stack))));
			}
		}
		return stack;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}

	@Override
	protected Component getDefaultName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new DispenserMenu(containerId, inventory, this);
	}

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return ALL_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return true;
	}
}
