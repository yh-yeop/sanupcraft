package io.github.yhyeop.sanupcraft.storage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 창고 네트워크: 단말 · 재고 유지기에서 창고 관을 따라가며 (너비 우선) 관 · 네트워크 블록에 붙은 보관함을 모은다.
 * 보관함 = 서랍과 {@link Container} 블록 (상자 · 통 · 기계 …). 큰 상자는 한 번만 센다. 자동 제작대는 보관함으로 보지 않는다
 * (재고 유지기가 따로 다룬다). 따라가는 블록 수는 설정 {@code storage.maxNodes} (기본 512)까지.
 * <p>
 * 찾은 결과는 {@link Cache}로 블록 엔티티마다 들고 있다가, 관 · 네트워크 블록 근처가 바뀌면({@link #invalidate}) 또는 2초가 지나면 다시 찾는다.
 * 넣기 순서: 같은 아이템이 든 곳 (서랍 먼저) → 보관함의 빈 칸 → 빈 서랍.
 */
public final class StorageNetwork {
	public static final int MAX_NODES = 512;
	/** 바뀐 게 없어도 다시 찾는 간격 (틱). */
	public static final int MAX_AGE_TICKS = 40;

	private static final AtomicLong EPOCH = new AtomicLong();

	/** 네트워크를 이루는 블록 엔티티 (단말 · 재고 유지기). 관처럼 지나간다. */
	public interface Member {
	}

	private record Store(@Nullable DrawerBlockEntity drawer, @Nullable Container container, @Nullable Direction face, @Nullable BlockEntity owner) {
	}

	private final List<Store> stores;
	private StorageNetwork(List<Store> stores) {
		this.stores = stores;
	}

	/** 관 · 네트워크 블록 근처가 바뀌었다: 모든 캐시를 버린다. */
	public static void invalidate() {
		EPOCH.incrementAndGet();
	}

	/** 보관함 수. */
	public int storeCount() {
		return stores.size();
	}

	/** {@code start}(네트워크 블록)에서 찾는다. */
	public static StorageNetwork discover(Level level, BlockPos start) {
		int maxNodes = SanupcraftConfig.current().storageMaxNodes;
		List<Store> stores = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(start);
		queue.add(start);
		int nodes = 1;
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			for (Direction direction : Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (!seen.add(next) || !level.isLoaded(next)) {
					continue;
				}
				if (isConduit(level, next)) {
					if (nodes < maxNodes) {
						nodes++;
						queue.add(next);
					}
					continue;
				}
				addStore(level, next, direction.getOpposite(), stores);
			}
		}
		return new StorageNetwork(stores);
	}

	/** 관 또는 네트워크 블록 (따라 지나가는 곳). */
	static boolean isConduit(Level level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof StorageCableBlock || level.getBlockEntity(pos) instanceof Member;
	}

	/** 네트워크가 보관함으로 보는 블록인가 (관이 이어지는 모양에도 쓴다). */
	public static boolean isStorage(@Nullable BlockEntity be) {
		return be instanceof DrawerBlockEntity || (be instanceof Container && !(be instanceof AutocrafterBlockEntity));
	}

	private static void addStore(Level level, BlockPos pos, Direction face, List<Store> stores) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof DrawerBlockEntity drawer) {
			stores.add(new Store(drawer, null, face, drawer));
			return;
		}
		if (!isStorage(be)) {
			return;
		}
		// 큰 상자는 두 칸이 한 보관함으로 나온다. 이미 센 반쪽이면 건너뛴다.
		for (Store store : stores) {
			if (store.container() == be || store.container() instanceof CompoundContainer compound && compound.contains((Container) be)) {
				return;
			}
		}
		Container container = HopperBlockEntity.getContainerAt(level, pos);
		if (container == null) {
			return;
		}
		stores.add(new Store(null, container, face, be));
	}

	// ---- 읽기 ----

	/** 아이템 종류 (아이템 + 컴포넌트). 맵 열쇠로 쓴다. */
	public static final class Key {
		private final ItemStack stack;
		private final int hash;

		public Key(ItemStack stack) {
			this.stack = stack.copyWithCount(1);
			this.hash = ItemStack.hashItemAndComponents(this.stack);
		}

		public ItemStack stack() {
			return stack;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof Key key && ItemStack.isSameItemSameComponents(stack, key.stack);
		}

		@Override
		public int hashCode() {
			return hash;
		}
	}

	/** 한 종류와 그 개수. */
	public record Entry(ItemStack stack, long count) {
	}

	/** 네트워크 전체의 아이템과 개수 (찾은 순서대로). */
	public Map<Key, Long> contents() {
		Map<Key, Long> result = new LinkedHashMap<>();
		for (Store store : stores) {
			if (removed(store)) {
				continue;
			}
			if (store.drawer() != null) {
				DrawerBlockEntity drawer = store.drawer();
				if (drawer.count() > 0) {
					result.merge(new Key(drawer.template()), (long) drawer.count(), Long::sum);
				}
				continue;
			}
			Container container = store.container();
			for (int slot : slots(store)) {
				ItemStack stack = container.getItem(slot);
				if (!stack.isEmpty() && canTake(store, slot, stack)) {
					result.merge(new Key(stack), (long) stack.getCount(), Long::sum);
				}
			}
		}
		return result;
	}

	/** 화면에 보낼 목록: 개수 많은 순. */
	public List<Entry> entries() {
		List<Entry> list = new ArrayList<>();
		contents().forEach((key, count) -> list.add(new Entry(key.stack(), count)));
		list.sort(Comparator.comparingLong(Entry::count).reversed());
		return list;
	}

	/** 이 아이템이 네트워크에 몇 개 있는가. */
	public long count(ItemStack template) {
		return contents().getOrDefault(new Key(template), 0L);
	}

	// ---- 넣기 · 꺼내기 ----

	/** 넣고 남은 것을 돌려준다 (다 들어가면 빈 스택). {@code stack}은 건드리지 않는다. */
	public ItemStack insert(ItemStack stack) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack rest = stack.copy();
		// 1) 같은 아이템이 든 서랍
		for (Store store : stores) {
			if (store.drawer() != null && !removed(store) && store.drawer().holds(rest)) {
				rest.shrink(store.drawer().insert(rest, rest.getCount(), false));
				if (rest.isEmpty()) {
					return ItemStack.EMPTY;
				}
			}
		}
		// 2) 같은 아이템이 든 칸
		for (Store store : stores) {
			if (store.container() != null && !removed(store)) {
				rest = fill(store, rest, true);
				if (rest.isEmpty()) {
					return ItemStack.EMPTY;
				}
			}
		}
		// 3) 빈 칸
		for (Store store : stores) {
			if (store.container() != null && !removed(store)) {
				rest = fill(store, rest, false);
				if (rest.isEmpty()) {
					return ItemStack.EMPTY;
				}
			}
		}
		// 4) 빈 서랍
		for (Store store : stores) {
			if (store.drawer() != null && !removed(store) && store.drawer().accepts(rest)) {
				rest.shrink(store.drawer().insert(rest, rest.getCount(), false));
				if (rest.isEmpty()) {
					return ItemStack.EMPTY;
				}
			}
		}
		return rest;
	}

	private static ItemStack fill(Store store, ItemStack rest, boolean mergeOnly) {
		Container container = store.container();
		boolean changed = false;
		for (int slot : slots(store)) {
			ItemStack current = container.getItem(slot);
			if (mergeOnly ? current.isEmpty() || !ItemStack.isSameItemSameComponents(current, rest) : !current.isEmpty()) {
				continue;
			}
			if (!canPlace(store, slot, rest)) {
				continue;
			}
			int max = Math.min(container.getMaxStackSize(rest), rest.getMaxStackSize());
			int room = max - current.getCount();
			if (room <= 0) {
				continue;
			}
			int move = Math.min(room, rest.getCount());
			if (current.isEmpty()) {
				container.setItem(slot, rest.copyWithCount(move));
			} else {
				current.grow(move);
			}
			rest.shrink(move);
			changed = true;
			if (rest.isEmpty()) {
				break;
			}
		}
		if (changed) {
			container.setChanged();
		}
		return rest;
	}

	/** {@code template}과 같은 아이템을 최대 {@code amount}개 꺼낸다 (서랍 먼저). */
	public ItemStack extract(ItemStack template, int amount) {
		return extractMatching(stack -> ItemStack.isSameItemSameComponents(stack, template), amount);
	}

	/** 조건에 맞는 아이템을 최대 {@code amount}개 꺼낸다. 처음 찾은 종류만 모은다. */
	public ItemStack extractMatching(Predicate<ItemStack> filter, int amount) {
		ItemStack result = ItemStack.EMPTY;
		if (amount <= 0) {
			return result;
		}
		for (Store store : stores) {
			if (removed(store)) {
				continue;
			}
			if (store.drawer() != null) {
				DrawerBlockEntity drawer = store.drawer();
				ItemStack template = drawer.template();
				if (template.isEmpty() || !filter.test(template) || (!result.isEmpty() && !ItemStack.isSameItemSameComponents(result, template))) {
					continue;
				}
				ItemStack got = drawer.extract(amount - result.getCount());
				result = merge(result, got);
			} else {
				Container container = store.container();
				boolean changed = false;
				for (int slot : slots(store)) {
					ItemStack stack = container.getItem(slot);
					if (stack.isEmpty() || !filter.test(stack) || !canTake(store, slot, stack)
							|| (!result.isEmpty() && !ItemStack.isSameItemSameComponents(result, stack))) {
						continue;
					}
					ItemStack got = container.removeItem(slot, amount - result.getCount());
					result = merge(result, got);
					changed = true;
					if (result.getCount() >= amount) {
						break;
					}
				}
				if (changed) {
					container.setChanged();
				}
			}
			if (result.getCount() >= amount) {
				break;
			}
		}
		return result;
	}

	private static ItemStack merge(ItemStack result, ItemStack got) {
		if (got.isEmpty()) {
			return result;
		}
		if (result.isEmpty()) {
			return got;
		}
		result.grow(got.getCount());
		return result;
	}

	private static boolean removed(Store store) {
		return store.owner() != null && store.owner().isRemoved();
	}

	private static int[] slots(Store store) {
		Container container = store.container();
		if (container instanceof WorldlyContainer worldly && store.face() != null) {
			return worldly.getSlotsForFace(store.face());
		}
		int[] all = new int[container.getContainerSize()];
		for (int i = 0; i < all.length; i++) {
			all[i] = i;
		}
		return all;
	}

	private static boolean canPlace(Store store, int slot, ItemStack stack) {
		Container container = store.container();
		if (!container.canPlaceItem(slot, stack)) {
			return false;
		}
		return !(container instanceof WorldlyContainer worldly) || worldly.canPlaceItemThroughFace(slot, stack, store.face());
	}

	private static boolean canTake(Store store, int slot, ItemStack stack) {
		return !(store.container() instanceof WorldlyContainer worldly) || worldly.canTakeItemThroughFace(slot, stack, store.face());
	}

	// ---- 캐시 ----

	/** 블록 엔티티마다 하나: 바뀐 게 없으면 찾은 네트워크를 다시 쓴다. */
	public static final class Cache {
		private @Nullable StorageNetwork network;
		private long epoch = -1;
		private long builtAt;

		public StorageNetwork get(Level level, BlockPos pos) {
			long now = level.getGameTime();
			long current = EPOCH.get();
			if (network == null || epoch != current || now - builtAt > MAX_AGE_TICKS || now < builtAt) {
				network = discover(level, pos);
				epoch = current;
				builtAt = now;
			}
			return network;
		}
	}
}
