package io.github.yhyeop.sanupcraft.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * 창고 단말 화면. 네트워크 목록은 칸이 아니라 {@link StoragePayloads.Contents}로 보내고 (내용이 바뀔 때만, 10틱마다 확인),
 * 목록 클릭은 {@link StoragePayloads.Action}으로 받아 서버에서 처리한다. 인벤토리 칸을 Shift 클릭하면 네트워크에 넣는다.
 * <p>
 * 제작 단말이면 3x3 제작 칸과 결과 칸이 더 있다. 결과를 가져가면 비게 된 제작 칸을 네트워크의 같은 아이템으로 다시 채운다
 * (Shift 클릭이면 재료가 떨어질 때까지 계속 만든다). 화면을 닫으면 제작 칸의 것은 네트워크로 (안 들어가면 인벤토리로) 돌아간다.
 */
public class TerminalMenu extends AbstractContainerMenu {
	public static final int GRID_SIZE = 9;
	public static final int ACTION_TAKE_STACK = 0;
	public static final int ACTION_TAKE_HALF = 1;
	public static final int ACTION_TAKE_TO_INVENTORY = 2;
	public static final int ACTION_INSERT_CARRIED = 3;
	public static final int ACTION_INSERT_ONE = 4;
	private static final int SYNC_TICKS = 10;

	/** 화면 배치 (클라이언트 화면과 맞춘다). */
	public static final int LIST_COLUMNS = 9;
	public static final int LIST_ROWS = 6;
	public static final int LIST_ROWS_CRAFTING = 3;
	public static final int LIST_X = 8;
	public static final int LIST_Y = 18;

	/** 화면을 열 때 클라이언트에 보내는 것: 제작 단말인가. */
	public record OpenData(boolean crafting) {
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, OpenData::crafting, OpenData::new);
	}

	private final boolean crafting;
	private final Player player;
	private final @Nullable TerminalBlockEntity terminal;
	private final Predicate<Player> validator;
	private final TransientCraftingContainer craftSlots;
	private final ResultContainer resultSlots = new ResultContainer();
	private final int inventoryStart;
	/** 클라이언트: 서버가 보낸 목록. */
	private List<StorageNetwork.Entry> clientEntries = List.of();
	private int clientVersion;
	/** 서버: 마지막으로 보낸 목록. */
	private @Nullable List<StorageNetwork.Entry> lastSent;
	private int syncTimer;
	private boolean refilling;

	/** 클라이언트 쪽. */
	public TerminalMenu(int containerId, Inventory inventory, OpenData data) {
		this(containerId, inventory, data.crafting(), null, p -> true);
	}

	/** 서버 쪽: 블록 단말 또는 휴대 단말. validator가 화면을 계속 열어 둘지 정한다. */
	public TerminalMenu(int containerId, Inventory inventory, TerminalBlockEntity terminal, Predicate<Player> validator) {
		this(containerId, inventory, terminal.isCrafting(), terminal, validator);
		terminal.addUser();
	}

	private TerminalMenu(int containerId, Inventory inventory, boolean crafting, @Nullable TerminalBlockEntity terminal, Predicate<Player> validator) {
		super(ModStorage.TERMINAL_MENU, containerId);
		this.crafting = crafting;
		this.player = inventory.player;
		this.terminal = terminal;
		this.validator = validator;
		this.craftSlots = new TransientCraftingContainer(this, 3, 3);
		int listBottom = LIST_Y + (crafting ? LIST_ROWS_CRAFTING : LIST_ROWS) * 18;
		int inventoryY;
		if (crafting) {
			int gridY = listBottom + 6;
			addSlot(new RefillingResultSlot(player, craftSlots, resultSlots, 0, 124, gridY + 18));
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 3; col++) {
					addSlot(new Slot(craftSlots, col + row * 3, 30 + col * 18, gridY + row * 18));
				}
			}
			inventoryY = gridY + 3 * 18 + 14;
		} else {
			inventoryY = listBottom + 14;
		}
		inventoryStart = slots.size();
		addStandardInventorySlots(inventory, 8, inventoryY);
	}

	public boolean isCrafting() {
		return crafting;
	}

	/** 화면 높이 (클라이언트 화면이 쓴다). */
	public int imageHeight() {
		return slots.get(slots.size() - 1).y + 24;
	}

	public int inventoryStart() {
		return inventoryStart;
	}

	public CraftingResultView craftingView() {
		return new CraftingResultView(craftSlots, resultSlots);
	}

	/** 테스트용: 제작 칸과 결과 칸. */
	public record CraftingResultView(TransientCraftingContainer grid, ResultContainer result) {
	}

	// ---- 클라이언트 목록 ----

	public List<StorageNetwork.Entry> clientEntries() {
		return clientEntries;
	}

	/** 목록이 바뀔 때마다 하나씩 는다 (화면이 다시 거를 때 쓴다). */
	public int clientVersion() {
		return clientVersion;
	}

	public void setClientEntries(List<StorageNetwork.Entry> entries) {
		clientEntries = entries;
		clientVersion++;
	}

	// ---- 서버 ----

	private @Nullable StorageNetwork network() {
		return terminal == null || terminal.isRemoved() ? null : terminal.network();
	}

	@Override
	public boolean stillValid(Player player) {
		return terminal == null ? true : !terminal.isRemoved() && validator.test(player);
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (terminal == null || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (--syncTimer > 0 && lastSent != null) {
			return;
		}
		syncTimer = SYNC_TICKS;
		sendContents(serverPlayer, false);
	}

	private void sendContents(ServerPlayer serverPlayer, boolean force) {
		StorageNetwork network = network();
		List<StorageNetwork.Entry> entries = network == null ? List.of() : network.entries();
		if (entries.size() > StoragePayloads.MAX_ENTRIES) {
			entries = entries.subList(0, StoragePayloads.MAX_ENTRIES);
		}
		if (!force && sameEntries(entries, lastSent)) {
			return;
		}
		lastSent = entries;
		if (serverPlayer.connection != null && ServerPlayNetworking.canSend(serverPlayer, StoragePayloads.Contents.TYPE)) {
			ServerPlayNetworking.send(serverPlayer, new StoragePayloads.Contents(containerId, entries));
		}
	}

	private static boolean sameEntries(List<StorageNetwork.Entry> a, @Nullable List<StorageNetwork.Entry> b) {
		if (b == null || a.size() != b.size()) {
			return false;
		}
		for (int i = 0; i < a.size(); i++) {
			if (a.get(i).count() != b.get(i).count() || !ItemStack.isSameItemSameComponents(a.get(i).stack(), b.get(i).stack())) {
				return false;
			}
		}
		return true;
	}

	/** 바로 다음 틱에 목록을 다시 보낸다 (넣기 · 꺼내기 뒤). */
	private void syncSoon() {
		syncTimer = 0;
	}

	/** 목록 칸 클릭 (서버). template은 클라이언트가 고른 아이템 (꺼낼 때만 쓰고, 실제 있는 만큼만 꺼낸다). */
	public void handleAction(ServerPlayer player, int action, ItemStack template) {
		StorageNetwork network = network();
		if (network == null) {
			return;
		}
		ItemStack carried = getCarried();
		switch (action) {
			case ACTION_TAKE_STACK, ACTION_TAKE_HALF -> {
				if (!carried.isEmpty()) {
					insertCarried(network, action == ACTION_TAKE_HALF ? 1 : carried.getCount());
				} else if (!template.isEmpty()) {
					int stack = template.getMaxStackSize();
					int want = stack;
					if (action == ACTION_TAKE_HALF) {
						long have = Math.min(stack, network.count(template));
						want = (int) Math.max(1, (have + 1) / 2);
					}
					setCarried(network.extract(template, want));
				}
			}
			case ACTION_TAKE_TO_INVENTORY -> {
				if (!template.isEmpty()) {
					ItemStack got = network.extract(template, template.getMaxStackSize());
					if (!got.isEmpty()) {
						moveItemStackTo(got, inventoryStart, slots.size(), true);
						if (!got.isEmpty()) {
							ItemStack back = network.insert(got);
							if (!back.isEmpty()) {
								player.getInventory().placeItemBackInInventory(back, Prediction.SERVER_ONLY);
							}
						}
					}
				}
			}
			case ACTION_INSERT_CARRIED -> insertCarried(network, carried.getCount());
			case ACTION_INSERT_ONE -> insertCarried(network, 1);
			default -> {
			}
		}
		syncSoon();
		broadcastChanges();
	}

	private void insertCarried(StorageNetwork network, int amount) {
		ItemStack carried = getCarried();
		if (carried.isEmpty() || amount <= 0) {
			return;
		}
		int move = Math.min(amount, carried.getCount());
		ItemStack rest = network.insert(carried.copyWithCount(move));
		carried.shrink(move - rest.getCount());
		setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (crafting && slotIndex == 0) {
			// 결과: 인벤토리로 (가져가면 칸을 다시 채우므로 바닐라처럼 같은 결과가 나오는 동안 되풀이된다)
			stack.getItem().onCraftedBy(stack, player);
			if (!moveItemStackTo(stack, inventoryStart, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(stack, original);
		} else if (slotIndex >= inventoryStart) {
			// 인벤토리 → 네트워크
			StorageNetwork network = network();
			if (network == null) {
				return ItemStack.EMPTY;
			}
			ItemStack rest = network.insert(stack);
			stack.setCount(rest.getCount());
			syncSoon();
		} else if (!moveItemStackTo(stack, inventoryStart, slots.size(), false)) {
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
		return slotIndex >= inventoryStart ? ItemStack.EMPTY : original;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
		return target.container != resultSlots && super.canTakeItemForPickAll(carried, target);
	}

	// ---- 제작 ----

	@Override
	public void slotsChanged(Container container) {
		if (refilling || !crafting || !(player.level() instanceof ServerLevel level)) {
			return;
		}
		updateResult(level);
	}

	private void updateResult(ServerLevel level) {
		CraftingInput input = craftSlots.asCraftInput();
		ItemStack result = ItemStack.EMPTY;
		Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
		if (recipe.isPresent()) {
			resultSlots.setRecipeUsed(recipe.get());
			ItemStack made = recipe.get().value().assemble(input);
			if (made.isItemEnabled(level.enabledFeatures())) {
				result = made;
			}
		}
		resultSlots.setItem(0, result);
	}

	/** 결과를 가져간 뒤: 비게 된 칸을 네트워크의 같은 아이템으로 채운다 (서버만). */
	private void refill(List<ItemStack> before) {
		StorageNetwork network = network();
		if (network == null) {
			return;
		}
		refilling = true;
		try {
			for (int i = 0; i < GRID_SIZE; i++) {
				ItemStack was = before.get(i);
				if (!was.isEmpty() && craftSlots.getItem(i).isEmpty()) {
					ItemStack got = network.extract(was, 1);
					if (!got.isEmpty()) {
						craftSlots.setItem(i, got);
					}
				}
			}
		} finally {
			refilling = false;
		}
		if (player.level() instanceof ServerLevel level) {
			updateResult(level);
		}
		syncSoon();
	}

	/**
	 * JEI 레시피 옮기기 (서버): 제작 칸의 것을 네트워크로 돌려놓고, 칸마다 후보 중 네트워크(없으면 인벤토리)에 있는 것을 하나씩 놓는다.
	 * 후보는 클라이언트가 보낸 것이라 실제로 가진 것만 옮긴다.
	 */
	public void fillGrid(ServerPlayer player, List<List<ItemStack>> choices) {
		StorageNetwork network = network();
		if (!crafting || network == null) {
			return;
		}
		refilling = true;
		try {
			clearGrid(player, network);
			for (int i = 0; i < Math.min(GRID_SIZE, choices.size()); i++) {
				for (ItemStack choice : choices.get(i)) {
					if (choice.isEmpty()) {
						continue;
					}
					ItemStack got = network.extract(choice, 1);
					if (got.isEmpty()) {
						got = takeFromInventory(player.getInventory(), choice);
					}
					if (!got.isEmpty()) {
						craftSlots.setItem(i, got);
						break;
					}
				}
			}
		} finally {
			refilling = false;
		}
		if (player.level() instanceof ServerLevel level) {
			updateResult(level);
		}
		syncSoon();
		broadcastChanges();
	}

	private static ItemStack takeFromInventory(Inventory inventory, ItemStack template) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
				return inventory.removeItem(i, 1);
			}
		}
		return ItemStack.EMPTY;
	}

	/** 제작 칸을 비운다: 네트워크로, 안 들어가면 인벤토리로. */
	private void clearGrid(Player player, @Nullable StorageNetwork network) {
		for (int i = 0; i < GRID_SIZE; i++) {
			ItemStack stack = craftSlots.removeItemNoUpdate(i);
			if (stack.isEmpty()) {
				continue;
			}
			ItemStack rest = network == null ? stack : network.insert(stack);
			if (!rest.isEmpty()) {
				player.getInventory().placeItemBackInInventory(rest, Prediction.SERVER_ONLY);
			}
		}
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (terminal != null) {
			terminal.removeUser();
			if (crafting) {
				clearGrid(player, network());
			}
		}
	}

	/** 결과 칸: 가져가면 바닐라처럼 재료를 하나씩 덜고, 서버에서는 빈 칸을 네트워크로 다시 채운다. */
	private class RefillingResultSlot extends ResultSlot {
		RefillingResultSlot(Player player, TransientCraftingContainer grid, Container result, int id, int x, int y) {
			super(player, grid, result, id, x, y);
		}

		@Override
		public void onTake(Player player, ItemStack carried) {
			List<ItemStack> before = new ArrayList<>(GRID_SIZE);
			for (int i = 0; i < GRID_SIZE; i++) {
				before.add(craftSlots.getItem(i).copy());
			}
			super.onTake(player, carried);
			if (terminal != null) {
				refill(before);
			}
		}
	}
}
