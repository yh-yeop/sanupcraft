package io.github.yhyeop.sanupcraft.logistics;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 엔더 운반기 (송신기 · 수신기). 같은 색 채널끼리 아이템을 바로 옮긴다. 차원을 넘어서도 된다 (로드된 수신기만).
 * <ul>
 *   <li>송신기: 깔때기 · 벨트가 넣는 아이템을 같은 채널의 수신기로 보낸다. 받을 수신기가 없으면 받지 않는다.</li>
 *   <li>수신기: 9칸 상자. 우클릭으로 열고, 깔때기로 뽑아 갈 수 있다.</li>
 * </ul>
 */
public class EnderLinkBlockEntity extends BaseContainerBlockEntity {
	public static final int RECEIVER_SLOTS = 9;

	/** 로드된 수신기들. 채널별로 모은다. */
	private static final Map<DyeColor, Set<EnderLinkBlockEntity>> RECEIVERS = new ConcurrentHashMap<>();

	private final boolean sender;
	private DyeColor channel = DyeColor.WHITE;
	private NonNullList<ItemStack> items;
	private boolean registered;

	public EnderLinkBlockEntity(BlockPos pos, BlockState state) {
		super(ModLogistics.ENDER_LINK_BLOCK_ENTITY, pos, state);
		this.sender = ((EnderLinkBlock) state.getBlock()).isSender();
		this.items = NonNullList.withSize(sender ? 1 : RECEIVER_SLOTS, ItemStack.EMPTY);
	}

	public boolean isSender() {
		return sender;
	}

	public DyeColor channel() {
		return channel;
	}

	public void setChannel(DyeColor color) {
		unregister();
		channel = color;
		setChanged();
		register();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, EnderLinkBlockEntity link) {
		if (!link.registered) {
			link.register();
		}
	}

	private void register() {
		if (!sender && level instanceof ServerLevel && !isRemoved()) {
			RECEIVERS.computeIfAbsent(channel, c -> ConcurrentHashMap.newKeySet()).add(this);
			registered = true;
		}
	}

	private void unregister() {
		Set<EnderLinkBlockEntity> set = RECEIVERS.get(channel);
		if (set != null) {
			set.remove(this);
		}
		registered = false;
	}

	@Override
	public void setRemoved() {
		unregister();
		super.setRemoved();
	}

	/** 이 송신기가 보낼 수 있는 수신기 (같은 서버 · 같은 채널 · 자리 있음). 없으면 null. */
	private @Nullable EnderLinkBlockEntity target(ItemStack stack) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return null;
		}
		Set<EnderLinkBlockEntity> set = RECEIVERS.get(channel);
		if (set == null) {
			return null;
		}
		for (EnderLinkBlockEntity receiver : set) {
			if (receiver.isRemoved() || !(receiver.level instanceof ServerLevel other) || other.getServer() != serverLevel.getServer()) {
				continue;
			}
			if (receiver.hasRoomFor(stack)) {
				return receiver;
			}
		}
		return null;
	}

	private boolean hasRoomFor(ItemStack stack) {
		for (ItemStack slot : items) {
			if (slot.isEmpty() || (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize())) {
				return true;
			}
		}
		return false;
	}

	// ---- 송신기: 들어온 아이템을 바로 보낸다 ----

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return !sender || target(stack) != null;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (!sender) {
			super.setItem(slot, stack);
			return;
		}
		if (stack.isEmpty() || level == null) {
			return;
		}
		EnderLinkBlockEntity receiver = target(stack);
		ItemStack rest = receiver == null ? stack : HopperBlockEntity.addItem(null, receiver, stack.copy(), null);
		if (!rest.isEmpty()) {
			Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, rest);
		}
	}

	@Override
	public ItemStack getItem(int slot) {
		return sender ? ItemStack.EMPTY : super.getItem(slot);
	}

	@Override
	public boolean isEmpty() {
		return sender || super.isEmpty();
	}

	@Override
	public int getContainerSize() {
		return items.size();
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
	protected Component getDefaultName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, this, 1);
	}

	@Override
	public boolean stillValid(Player player) {
		return !sender && Container.stillValidBlockEntity(this, player);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		channel = input.read("channel", DyeColor.CODEC).orElse(DyeColor.WHITE);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.store("channel", DyeColor.CODEC, channel);
	}
}
