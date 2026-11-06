package io.github.yhyeop.sanupcraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 필터 깔때기. 바닐라 깔때기처럼 5칸 짐칸을 갖고, 몇 틱마다 위(상자 · 바닥의 아이템)에서 하나 빨아들이고 향한 쪽으로 하나 내보낸다.
 * 다른 점은 짐칸에 들어오는 것을 필터 5칸으로 거르는 것 (깔때기 · 벨트 · 위 상자 어느 쪽이 넣든 같다). 빈 필터는 거르지 않는다.
 * 옮기는 일은 바닐라 {@link HopperBlockEntity}의 도우미를 그대로 쓴다.
 */
public class FilterHopperBlockEntity extends BlockEntity implements Hopper, MenuProvider {
	/** 옮기는 간격 (틱). 바닐라 깔때기는 8. 서버 설정으로 바꾼다. */
	public static final int TICKS_PER_MOVE = 8;
	public static final int CARGO_SLOTS = 5;

	private final NonNullList<ItemStack> items = NonNullList.withSize(CARGO_SLOTS, ItemStack.EMPTY);
	private final ItemFilter filter = new ItemFilter(FilterMenu.HOPPER_SIZE);
	private int cooldown;

	public FilterHopperBlockEntity(BlockPos pos, BlockState state) {
		super(ModLogistics.FILTER_HOPPER_BLOCK_ENTITY, pos, state);
		filter.onChange(this::setChanged);
	}

	public ItemFilter filter() {
		return filter;
	}

	/** 이 아이템이 지나갈 수 있는가: 필터가 비었으면 모두, 아니면 맞는 것만. */
	public boolean passes(ItemStack stack) {
		return !filter.hasEntries() || filter.matches(stack);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FilterHopperBlockEntity hopper) {
		hopper.cooldown--;
		if (hopper.cooldown > 0) {
			return;
		}
		hopper.cooldown = 0;
		if (hopper.tryMove(level, pos, state, () -> HopperBlockEntity.suckInItems(level, hopper))) {
			hopper.cooldown = SanupcraftConfig.current().filterHopperTicksPerMove;
		}
	}

	/** 위에서 떨어진 아이템 덩어리를 받아 본다 (쉬는 시간이 아닐 때만). */
	void entityInside(BlockState state, Entity entity) {
		if (cooldown <= 0 && entity instanceof ItemEntity item && !item.getItem().isEmpty()
				&& entity.getBoundingBox().move(-worldPosition.getX(), -worldPosition.getY(), -worldPosition.getZ()).intersects(getSuckAabb())
				&& level != null && tryMove(level, worldPosition, state, () -> HopperBlockEntity.addItem(this, item))) {
			cooldown = SanupcraftConfig.current().filterHopperTicksPerMove;
		}
	}

	/** 바닐라 깔때기와 같은 순서: 먼저 내보내고, 자리가 있으면 받아들인다. 하나라도 옮겼으면 true. */
	private boolean tryMove(Level level, BlockPos pos, BlockState state, java.util.function.BooleanSupplier intake) {
		if (level.isClientSide() || !state.getValue(HopperBlock.ENABLED)) {
			return false;
		}
		boolean changed = false;
		if (!isEmpty()) {
			changed = eject(level, pos, state.getValue(HopperBlock.FACING));
		}
		if (!full()) {
			changed |= intake.getAsBoolean();
		}
		if (changed) {
			setChanged();
		}
		return changed;
	}

	private boolean full() {
		for (ItemStack stack : items) {
			if (stack.isEmpty() || stack.getCount() < getMaxStackSize(stack)) {
				return false;
			}
		}
		return true;
	}

	/** 향한 쪽 상자 · 기계 · 벨트에 하나 넣는다. 못 넣으면 그대로 돌려놓는다 (아이템이 사라지지 않는다). */
	private boolean eject(Level level, BlockPos pos, Direction facing) {
		Container target = HopperBlockEntity.getContainerAt(level, pos.relative(facing));
		if (target == null) {
			return false;
		}
		for (int slot = 0; slot < items.size(); slot++) {
			if (items.get(slot).isEmpty()) {
				continue;
			}
			ItemStack one = removeItem(slot, 1);
			ItemStack rest = HopperBlockEntity.addItem(this, target, one, facing.getOpposite());
			if (rest.isEmpty()) {
				target.setChanged();
				return true;
			}
			ItemStack current = items.get(slot);
			if (current.isEmpty()) {
				items.set(slot, rest);
			} else {
				current.grow(rest.getCount());
			}
		}
		return false;
	}

	// ---- 짐칸 (필터에 맞는 것만 들어온다) ----

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return passes(stack);
	}

	@Override
	public int getContainerSize() {
		return CARGO_SLOTS;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : items) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
		if (!removed.isEmpty()) {
			setChanged();
		}
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		stack.limitSize(getMaxStackSize(stack));
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	@Override
	public double getLevelX() {
		return worldPosition.getX() + 0.5;
	}

	@Override
	public double getLevelY() {
		return worldPosition.getY() + 0.5;
	}

	@Override
	public double getLevelZ() {
		return worldPosition.getZ() + 0.5;
	}

	@Override
	public boolean isGridAligned() {
		return true;
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
		items.clear();
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
