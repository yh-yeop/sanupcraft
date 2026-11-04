package io.github.yhyeop.sanupcraft.logistics;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.Codec;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 엔더 닻 (엔드 단계). 튀긴 후렴과 하나로 10분 동안 자기 청크와 둘레 8청크(3x3)를 계속 불러 둔다.
 * 아무도 없는 네더 전초기지(지열 분출구 · 굴착기 · 심장 제련로 · 엔더 송신기)가 오버월드에 있는 동안에도 돈다.
 * 바닐라 강제 로드(/forceload와 같은 것)를 써서 서버를 다시 켜도 이어진다. 연료가 떨어지거나 부서지면 푼다.
 * 이미 강제 로드돼 있던 청크(/forceload 등)는 건드리지 않고, 자기가 붙잡은 청크만 푼다.
 * 다른 켜진 닻이 같은 청크를 덮고 있으면 풀지 않고 그 닻에게 넘긴다.
 */
public class EnderAnchorBlockEntity extends BlockEntity implements WorldlyContainer {
	/** 튀긴 후렴과 하나가 버티는 틱 (10분). */
	public static final int TICKS_PER_FUEL = 12000;
	/** 둘레 몇 청크까지 (1 = 3x3). */
	public static final int RADIUS = 1;
	private static final int[] SLOTS = {0};

	/** 지금 켜져 있는 닻들 (차원별). 겹치는 청크를 넘겨주려고 본다. */
	private static final Map<ResourceKey<Level>, Set<EnderAnchorBlockEntity>> ACTIVE = new ConcurrentHashMap<>();

	private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private int fuelTicks;
	private boolean forced;
	/** 이 닻이 붙잡은 청크 (ChunkPos.pack). 풀 때 이것만 푼다. */
	private final LongSet ownChunks = new LongOpenHashSet();

	public EnderAnchorBlockEntity(BlockPos pos, BlockState state) {
		super(ModLogistics.ENDER_ANCHOR_BLOCK_ENTITY, pos, state);
	}

	public static void clearActive() {
		ACTIVE.clear();
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, EnderAnchorBlockEntity anchor) {
		anchor.tick(level, pos);
		boolean active = anchor.isActive();
		if (state.getValue(EnderAnchorBlock.ACTIVE) != active) {
			level.setBlockAndUpdate(pos, state.setValue(EnderAnchorBlock.ACTIVE, active));
		}
	}

	/** 한 틱: 연료를 쓰고, 켜짐 · 꺼짐이 바뀌면 청크를 붙잡거나 놓는다. */
	public void tick(ServerLevel level, BlockPos pos) {
		if (fuelTicks <= 0) {
			ItemStack fuel = items.get(0);
			if (!fuel.isEmpty()) {
				fuel.shrink(1);
				fuelTicks = TICKS_PER_FUEL;
				setChanged();
			}
		}
		if (fuelTicks > 0) {
			fuelTicks--;
			if (fuelTicks % 200 == 0) {
				setChanged();
			}
		}
		boolean active = fuelTicks > 0;
		if (active) {
			ACTIVE.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(this);
		}
		if (active && !forced) {
			setForced(level, pos, true);
		} else if (!active && forced) {
			setForced(level, pos, false);
		}
	}

	/** 3x3 청크를 붙잡거나 놓는다. 붙잡을 때 이미 강제 로드된 청크는 건드리지 않는다. */
	private void setForced(ServerLevel level, BlockPos pos, boolean value) {
		if (value) {
			ChunkPos center = ChunkPos.containing(pos);
			LongSet already = level.getForceLoadedChunks();
			for (int dx = -RADIUS; dx <= RADIUS; dx++) {
				for (int dz = -RADIUS; dz <= RADIUS; dz++) {
					long chunk = ChunkPos.pack(center.x() + dx, center.z() + dz);
					if (!already.contains(chunk) && level.setChunkForced(center.x() + dx, center.z() + dz, true)) {
						ownChunks.add(chunk);
					}
				}
			}
		} else {
			Set<EnderAnchorBlockEntity> set = ACTIVE.get(level.dimension());
			if (set != null) {
				set.remove(this);
			}
			for (long chunk : ownChunks) {
				EnderAnchorBlockEntity heir = coveringAnchor(level, chunk);
				if (heir != null) {
					heir.ownChunks.add(chunk);
					heir.setChanged();
				} else {
					level.setChunkForced(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false);
				}
			}
			ownChunks.clear();
		}
		forced = value;
		setChanged();
	}

	/** 이 청크를 덮는 다른 켜진 닻 (같은 차원). 없으면 null. */
	private @Nullable EnderAnchorBlockEntity coveringAnchor(ServerLevel level, long chunk) {
		Set<EnderAnchorBlockEntity> set = ACTIVE.get(level.dimension());
		if (set == null) {
			return null;
		}
		for (EnderAnchorBlockEntity other : set) {
			if (other == this || other.isRemoved() || !other.isActive()) {
				continue;
			}
			ChunkPos c = ChunkPos.containing(other.getBlockPos());
			if (Math.abs(c.x() - ChunkPos.getX(chunk)) <= RADIUS && Math.abs(c.z() - ChunkPos.getZ(chunk)) <= RADIUS) {
				return other;
			}
		}
		return null;
	}

	/** 이 닻이 붙잡은 청크 수 (시험용). */
	public int ownChunkCount() {
		return ownChunks.size();
	}

	/** 블록이 부서질 때 (청크가 내려갈 때가 아니라): 청크를 놓고 연료를 떨군다. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level instanceof ServerLevel serverLevel) {
			if (forced) {
				setForced(serverLevel, pos, false);
			}
			Containers.dropContents(serverLevel, pos, this);
		}
	}

	public boolean isActive() {
		return fuelTicks > 0;
	}

	public boolean isForced() {
		return forced;
	}

	public int fuelTicks() {
		return fuelTicks;
	}

	/** 남은 시간 (틱): 타는 중인 것 + 칸에 남은 연료. */
	public long remainingTicks() {
		return (long) fuelTicks + (long) items.get(0).getCount() * TICKS_PER_FUEL;
	}

	public static boolean isFuel(ItemStack stack) {
		return stack.is(Items.POPPED_CHORUS_FRUIT);
	}

	/** 손에 든 연료를 넣는다. 넣은 수. */
	public int insertFuel(ItemStack stack) {
		if (!isFuel(stack)) {
			return 0;
		}
		ItemStack current = items.get(0);
		int room = current.isEmpty() ? stack.getMaxStackSize() : current.getMaxStackSize() - current.getCount();
		int move = Math.min(room, stack.getCount());
		if (move <= 0) {
			return 0;
		}
		if (current.isEmpty()) {
			items.set(0, stack.split(move));
		} else {
			current.grow(move);
			stack.shrink(move);
		}
		setChanged();
		return move;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(1, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		fuelTicks = input.getIntOr("fuel_ticks", 0);
		forced = input.getBooleanOr("forced", false);
		ownChunks.clear();
		input.read("own_chunks", Codec.LONG.listOf()).ifPresent(ownChunks::addAll);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("fuel_ticks", fuelTicks);
		output.putBoolean("forced", forced);
		output.store("own_chunks", Codec.LONG.listOf(), List.copyOf(ownChunks));
	}

	// ---- 연료 칸 (깔때기 · 우클릭) ----

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
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
		setChanged();
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
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return isFuel(stack);
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
	public int[] getSlotsForFace(Direction direction) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return false;
	}
}
