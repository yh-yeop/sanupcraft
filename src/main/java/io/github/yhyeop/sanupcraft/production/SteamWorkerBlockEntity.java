package io.github.yhyeop.sanupcraft.production;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.power.SteamConsumer;
import io.github.yhyeop.sanupcraft.power.SteamFlowMeter;
import io.github.yhyeop.sanupcraft.power.SteamStorage;

/**
 * 증기로 도는 생산 블록(작물 수확기 · 벌목기)의 바탕. 증기 파이프로 증기를 받고, 거둔 것을 9칸에 모은다.
 * 칸이 꽉 차서 못 넣은 것은 {@link #pending}에 쌓아 두고 (사라지지 않는다) 자리가 나면 먼저 채운다.
 * 아래에 상자 · 깔때기 · 벨트가 있으면 {@link #OUTPUT_INTERVAL}틱마다 한 덩어리씩 내보낸다. 깔때기는 옆 · 아래에서 뽑을 수도 있다.
 * 칸에는 사람이 넣지 못한다 (벌목기는 묘목만).
 */
public abstract class SteamWorkerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, SteamConsumer, SteamStorage {
	public static final int SLOTS = 9;
	/** 아래 컨테이너로 내보내는 간격(틱). */
	public static final int OUTPUT_INTERVAL = 8;
	/** 마지막으로 일한 뒤 이만큼 틱 동안은 "일하는 중" 표시를 유지한다. */
	private static final int ACTIVE_HOLD_TICKS = 20;
	private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	/** 칸이 꽉 차서 못 넣은 것. */
	protected final List<ItemStack> pending = new ArrayList<>();
	protected int steam;
	private final SteamFlowMeter flow = new SteamFlowMeter();
	/** 마지막으로 일한 게임 시각 (저장하지 않는다). */
	protected long lastWorkTick = Long.MIN_VALUE / 2;

	protected SteamWorkerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** 한 틱: 일하기 → 아래로 내보내기. 시험이 직접 부른다. */
	public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (work(level, pos, state)) {
			lastWorkTick = level.getGameTime();
		}
		if (level.getGameTime() % OUTPUT_INTERVAL == 0) {
			pushOut(level, pos);
		}
	}

	/** 이번 틱에 일했거나 일할 차례를 기다리는 중이면 true. */
	protected abstract boolean work(ServerLevel level, BlockPos pos, BlockState state);

	/** 이 칸에 사람이 / 깔때기가 넣을 수 있는 것. 기본은 아무것도 못 넣는다. */
	protected boolean acceptsInput(ItemStack stack) {
		return false;
	}

	public Direction facing() {
		return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
	}

	public boolean isActive() {
		return level != null && level.getGameTime() - lastWorkTick < ACTIVE_HOLD_TICKS;
	}

	public boolean hasPending() {
		return !pending.isEmpty();
	}

	// ---- 거둔 것 보관 ----

	/** 거둔 것을 칸에 넣는다. 못 넣은 나머지는 pending에 쌓는다. */
	protected void stash(ItemStack stack) {
		ItemStack rest = insert(stack.copy());
		if (!rest.isEmpty()) {
			pending.add(rest);
		}
		setChanged();
	}

	/** 같은 아이템 칸에 합치고, 빈 칸에 넣는다. 남은 것을 돌려준다. */
	private ItemStack insert(ItemStack stack) {
		for (int i = 0; i < SLOTS && !stack.isEmpty(); i++) {
			ItemStack slot = items.get(i);
			if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
				int room = Math.min(slot.getMaxStackSize(), getMaxStackSize()) - slot.getCount();
				int moved = Math.min(room, stack.getCount());
				if (moved > 0) {
					slot.grow(moved);
					stack.shrink(moved);
				}
			}
		}
		for (int i = 0; i < SLOTS && !stack.isEmpty(); i++) {
			if (items.get(i).isEmpty()) {
				int moved = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), getMaxStackSize()));
				items.set(i, stack.split(moved));
			}
		}
		return stack;
	}

	/** 쌓인 것을 칸에 넣어 본다. 다 넣었으면 true. */
	protected boolean flushPending() {
		if (pending.isEmpty()) {
			return true;
		}
		for (int i = 0; i < pending.size(); i++) {
			pending.set(i, insert(pending.get(i)));
		}
		pending.removeIf(ItemStack::isEmpty);
		setChanged();
		return pending.isEmpty();
	}

	/** 칸에 들어 있는 {@code predicate}에 맞는 첫 아이템 하나를 꺼내 돌려준다 (없으면 빈 것). */
	protected ItemStack takeOne(Predicate<ItemStack> predicate) {
		for (int i = 0; i < SLOTS; i++) {
			ItemStack slot = items.get(i);
			if (!slot.isEmpty() && predicate.test(slot)) {
				ItemStack one = slot.split(1);
				setChanged();
				return one;
			}
		}
		return ItemStack.EMPTY;
	}

	/** 아래 상자 · 깔때기 · 벨트로 한 덩어리를 보낸다. */
	private void pushOut(ServerLevel level, BlockPos pos) {
		Container below = HopperBlockEntity.getContainerAt(level, pos.below());
		if (below == null) {
			return;
		}
		for (int i = 0; i < SLOTS; i++) {
			ItemStack slot = items.get(i);
			if (slot.isEmpty()) {
				continue;
			}
			ItemStack rest = HopperBlockEntity.addItem(null, below, slot.copy(), Direction.UP);
			if (rest.getCount() != slot.getCount()) {
				items.set(i, rest);
				setChanged();
				return;
			}
		}
	}

	/** 부서질 때 칸 · 쌓인 것을 그 자리에 떨군다. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level instanceof ServerLevel serverLevel) {
			// 칸 내용물은 super(바닐라)가 떨군다
			for (ItemStack stack : pending) {
				Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
			}
		}
		pending.clear();
	}

	// ---- 증기망 ----

	@Override
	public int acceptSteam(int amount) {
		int accepted = Math.max(0, Math.min(amount, steamCapacity() - steam));
		steam += accepted;
		if (accepted > 0) {
			flow.add(gameTime(), accepted);
			setChanged();
		}
		return accepted;
	}

	@Override
	public int steamStored() {
		return steam;
	}

	@Override
	public int steamReceivedLastSecond() {
		return flow.lastSecond(gameTime());
	}

	private long gameTime() {
		return level != null ? level.getGameTime() : 0L;
	}

	// ---- 컨테이너 ----

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return acceptsInput(stack);
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return ALL_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return acceptsInput(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return true;
	}

	// ---- 저장 ----

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		steam = Math.max(0, Math.min(steamCapacity(), input.getIntOr("steam", 0)));
		pending.clear();
		input.read("pending", ItemStack.CODEC.listOf()).ifPresent(pending::addAll);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("steam", steam);
		output.store("pending", ItemStack.CODEC.listOf(), pending);
	}
}
