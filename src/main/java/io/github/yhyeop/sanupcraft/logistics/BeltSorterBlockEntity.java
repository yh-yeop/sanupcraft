package io.github.yhyeop.sanupcraft.logistics;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 벨트 분류기. 한 칸짜리 받는 칸이 있어서 벨트(끝 전달) · 깔때기가 넣어 주면, 몇 틱마다 필터를 보고 한 덩어리를 내보낸다.
 * 맞으면 왼쪽(바라보는 쪽 기준), 아니면 앞. 고른 쪽이 막혔으면(없음 · 가득 참) 받는 칸에 그대로 두고 다시 시도한다 - 아이템이 사라지지 않는다.
 * 빈 필터는 아무것도 안 맞으니 전부 앞으로 간다. 받는 칸은 밖에서 뽑아 갈 수 없다.
 */
public class BeltSorterBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
	/** 내보내기를 시도하는 간격 (틱). 서버 설정으로 바꾼다. */
	public static final int TICKS_PER_MOVE = 4;
	private static final int[] SLOTS = {0};

	private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private final ItemFilter filter = new ItemFilter(FilterMenu.SORTER_SIZE);
	private int cooldown;

	public BeltSorterBlockEntity(BlockPos pos, BlockState state) {
		super(ModLogistics.BELT_SORTER_BLOCK_ENTITY, pos, state);
		filter.onChange(this::setChanged);
	}

	public ItemFilter filter() {
		return filter;
	}

	/** 맞는 것이 나가는 쪽 (바라보는 쪽 기준 왼쪽). */
	public static Direction matchSide(Direction facing) {
		return facing.getCounterClockWise();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BeltSorterBlockEntity sorter) {
		if (sorter.items.get(0).isEmpty()) {
			sorter.cooldown = 0;
			return;
		}
		if (sorter.cooldown > 0) {
			sorter.cooldown--;
			return;
		}
		sorter.cooldown = Math.max(0, SanupcraftConfig.current().sorterTicksPerMove - 1);
		sorter.route(level, pos, state);
	}

	/** 받는 칸의 것을 맞으면 왼쪽, 아니면 앞으로 한 번 내보내 본다. */
	void route(Level level, BlockPos pos, BlockState state) {
		ItemStack stack = items.get(0);
		Direction facing = state.getValue(BeltSorterBlock.FACING);
		Direction out = filter.matches(stack) ? matchSide(facing) : facing;
		BlockPos targetPos = pos.relative(out);
		BlockState targetState = level.getBlockState(targetPos);
		// 나를 되먹이는 벨트로는 내보내지 않는다 (제자리 돌기 방지)
		if (targetState.getBlock() instanceof BeltBlock && targetState.getValue(BeltBlock.FACING) == out.getOpposite()) {
			return;
		}
		Container target = HopperBlockEntity.getContainerAt(level, targetPos);
		if (target == null || target == this) {
			return;
		}
		int before = stack.getCount();
		ItemStack rest = BeltBlockEntity.insert(target, stack.copy(), out);
		if (rest.getCount() != before) {
			items.set(0, rest.isEmpty() ? ItemStack.EMPTY : rest);
			target.setChanged();
			setChanged();
		}
	}

	// ---- 받는 칸: 벨트 · 깔때기가 넣기만 한다 ----

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
		return true;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
		return false;
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return items.get(0).isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(0);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(items, 0, amount);
		if (!removed.isEmpty()) {
			setChanged();
		}
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, 0);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(0, stack);
		stack.limitSize(getMaxStackSize(stack));
		setChanged();
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.set(0, ItemStack.EMPTY);
	}

	// ---- 필터 화면 ----

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new FilterMenu(containerId, inventory, filter, this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.set(0, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		filter.load(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		filter.save(output);
	}
}
