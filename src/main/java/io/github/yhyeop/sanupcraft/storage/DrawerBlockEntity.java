package io.github.yhyeop.sanupcraft.storage;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 저장 서랍: 한 종류의 아이템을 대량으로 담는다 (철 32더미 · 강철 128더미, 용량 업그레이드 하나에 ×2, 최대 2개).
 * 넘침 버림 업그레이드가 있으면 가득 찬 뒤 들어오는 같은 아이템은 버린다.
 * <p>
 * 깔때기 · 벨트용 {@link Container}로 보이는 칸은 둘:
 * 0번 = 넣는 칸 (늘 비어 보이고, 넣은 것은 바로 개수로 바뀐다), 1번 = 꺼내는 칸 (한 더미까지 보인다).
 * 1번 칸은 바닐라 깔때기처럼 돌려받은 스택을 직접 줄이고 늘리는 쪽도 있어서, 보여 준 스택을 기억했다가 바뀐 만큼 개수에 반영한다.
 * 창고 네트워크는 {@link #insert} · {@link #extract}로 전체 개수를 바로 다룬다.
 */
public class DrawerBlockEntity extends BlockEntity implements Container {
	public static final int MAX_UPGRADES = 2;
	/** 기본 더미 수. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int IRON_STACKS = 32;
	public static final int STEEL_STACKS = 128;
	public static final int SLOT_INPUT = 0;
	public static final int SLOT_OUTPUT = 1;
	/** 빠른 두 번 우클릭으로 보는 간격 (틱). */
	public static final int DOUBLE_CLICK_TICKS = 10;

	private ItemStack template = ItemStack.EMPTY;
	private int count;
	private int upgrades;
	private boolean voiding;
	/** 1번 칸으로 내보인 스택과 그때의 개수. */
	private ItemStack view = ItemStack.EMPTY;
	private int viewCount;
	private long lastClickTime = -100;
	private @Nullable UUID lastClickPlayer;
	private long lastExtractTime = -100;

	public DrawerBlockEntity(BlockPos pos, BlockState state) {
		super(ModStorage.DRAWER_BLOCK_ENTITY, pos, state);
	}

	/** 기본 더미 수 (업그레이드 전). */
	public int baseStacks() {
		SanupcraftConfig config = SanupcraftConfig.current();
		return getBlockState().getBlock() instanceof DrawerBlock drawer && drawer.isSteel() ? config.steelDrawerStacks : config.ironDrawerStacks;
	}

	/** 이 아이템을 몇 개까지 담는가. */
	public int capacityFor(ItemStack stack) {
		long max = (long) baseStacks() * stack.getMaxStackSize() << upgrades;
		return (int) Math.min(Integer.MAX_VALUE, max);
	}

	public int capacity() {
		return template.isEmpty() ? capacityFor(new ItemStack(Items.STONE)) : capacityFor(template);
	}

	public ItemStack template() {
		sync();
		return template;
	}

	public int count() {
		sync();
		return count;
	}

	public int upgrades() {
		return upgrades;
	}

	public boolean isVoiding() {
		return voiding;
	}

	/** 이 아이템을 받는 서랍인가 (비었거나 같은 아이템). 서랍 아이템 자체(내용물이 든 것)는 받지 않는다. */
	public boolean accepts(ItemStack stack) {
		if (stack.isEmpty() || stack.has(ModStorage.DRAWER_CONTENTS)) {
			return false;
		}
		sync();
		return template.isEmpty() || ItemStack.isSameItemSameComponents(template, stack);
	}

	/** 같은 아이템이 들어 있는가 (네트워크가 넣을 곳을 고를 때). */
	public boolean holds(ItemStack stack) {
		sync();
		return count > 0 && !template.isEmpty() && ItemStack.isSameItemSameComponents(template, stack);
	}

	/**
	 * {@code stack}에서 최대 {@code amount}개를 받아들이고 받은 수(버린 것 포함)를 돌려준다. {@code stack}은 건드리지 않는다.
	 * simulate면 바꾸지 않고 수만 센다.
	 */
	public int insert(ItemStack stack, int amount, boolean simulate) {
		if (amount <= 0 || !accepts(stack)) {
			return 0;
		}
		int room = Math.max(0, capacityFor(stack) - count);
		int stored = Math.min(room, amount);
		int accepted = voiding ? amount : stored;
		if (!simulate && accepted > 0) {
			if (template.isEmpty()) {
				template = stack.copyWithCount(1);
			}
			count += stored;
			changed();
		}
		return accepted;
	}

	/** 최대 {@code amount}개를 꺼낸다. 비면 빈 스택. */
	public ItemStack extract(int amount) {
		sync();
		int take = Math.min(amount, count);
		if (take <= 0 || template.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack result = template.copyWithCount(take);
		count -= take;
		if (count <= 0) {
			count = 0;
			template = ItemStack.EMPTY;
		}
		changed();
		return result;
	}

	/** 플레이어 손의 것을 넣는다. 넣은 수를 돌려준다. */
	public int insertFromHand(Player player, ItemStack held) {
		int accepted = insert(held, held.getCount(), false);
		held.shrink(accepted);
		return accepted;
	}

	/** 인벤토리의 같은 아이템을 전부 넣는다 (빠른 두 번 우클릭). 넣은 수를 돌려준다. */
	public int insertAllFrom(Inventory inventory) {
		sync();
		if (template.isEmpty()) {
			return 0;
		}
		int total = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && accepts(stack)) {
				int accepted = insert(stack, stack.getCount(), false);
				stack.shrink(accepted);
				total += accepted;
				if (accepted == 0) {
					break;
				}
			}
		}
		if (total > 0) {
			inventory.setChanged();
		}
		return total;
	}

	/** 같은 사람이 {@link #DOUBLE_CLICK_TICKS} 안에 다시 눌렀으면 true. 누른 시각을 기록한다. */
	public boolean registerClick(Player player, long gameTime) {
		boolean isDouble = player.getUUID().equals(lastClickPlayer) && gameTime - lastClickTime <= DOUBLE_CLICK_TICKS;
		lastClickPlayer = player.getUUID();
		lastClickTime = isDouble ? -100 : gameTime;
		return isDouble;
	}

	/** 좌클릭 꺼내기 간격: 지난번에서 {@link DrawerBlock#EXTRACT_COOLDOWN} 틱이 지났으면 true (시각을 기록). */
	public boolean tryExtractCooldown(long gameTime) {
		if (gameTime - lastExtractTime < DrawerBlock.EXTRACT_COOLDOWN && gameTime >= lastExtractTime) {
			return false;
		}
		lastExtractTime = gameTime;
		return true;
	}

	/** 용량 업그레이드를 하나 끼운다. 이미 최대면 false. */
	public boolean addCapacityUpgrade() {
		if (upgrades >= MAX_UPGRADES) {
			return false;
		}
		upgrades++;
		changed();
		return true;
	}

	/** 넘침 버림 업그레이드를 끼운다. 이미 있으면 false. */
	public boolean addVoidUpgrade() {
		if (voiding) {
			return false;
		}
		voiding = true;
		changed();
		return true;
	}

	/**
	 * 업그레이드를 하나 뺀다 (넘침 버림 먼저, 그다음 용량). 뺀 업그레이드 아이템, 없으면 빈 스택.
	 * 용량을 빼면 지금 개수가 새 용량을 넘을 때는 빼지 않는다 (아이템이 사라지지 않게).
	 */
	public ItemStack removeUpgrade() {
		sync();
		if (voiding) {
			voiding = false;
			changed();
			return new ItemStack(ModStorage.DRAWER_VOID_UPGRADE);
		}
		if (upgrades > 0) {
			int after = template.isEmpty() ? 0 : (int) Math.min(Integer.MAX_VALUE, (long) baseStacks() * template.getMaxStackSize() << (upgrades - 1));
			if (count > after && !template.isEmpty()) {
				return ItemStack.EMPTY;
			}
			upgrades--;
			changed();
			return new ItemStack(ModStorage.DRAWER_UPGRADE);
		}
		return ItemStack.EMPTY;
	}

	// ---- 1번 칸 맞추기 ----

	/** 1번 칸 스택이 밖에서 바뀌었으면 그만큼 개수에 반영하고, 칸을 다시 채운다. */
	private void sync() {
		if (viewCount > 0 || !view.isEmpty()) {
			int now = view.isEmpty() ? 0 : view.getCount();
			if (now != viewCount) {
				count = Math.max(0, count + now - viewCount);
				if (count == 0) {
					template = ItemStack.EMPTY;
				}
				setChangedOnly();
			}
		}
		refreshView();
	}

	private void refreshView() {
		if (count <= 0 || template.isEmpty()) {
			view = ItemStack.EMPTY;
			viewCount = 0;
			return;
		}
		int shown = Math.min(count, template.getMaxStackSize());
		if (view.isEmpty() || !ItemStack.isSameItemSameComponents(view, template)) {
			view = template.copyWithCount(shown);
		} else {
			view.setCount(shown);
		}
		viewCount = shown;
	}

	private void changed() {
		refreshView();
		setChangedOnly();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	private void setChangedOnly() {
		super.setChanged();
	}

	@Override
	public void setChanged() {
		boolean before = viewCount != (view.isEmpty() ? 0 : view.getCount());
		sync();
		if (before) {
			changed();
		} else {
			super.setChanged();
		}
	}

	// ---- Container (깔때기 · 벨트) ----

	@Override
	public int getContainerSize() {
		return 2;
	}

	@Override
	public boolean isEmpty() {
		sync();
		return count <= 0;
	}

	@Override
	public ItemStack getItem(int slot) {
		sync();
		return slot == SLOT_OUTPUT ? view : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return slot == SLOT_OUTPUT ? extract(amount) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return slot == SLOT_OUTPUT ? extract(viewCount) : ItemStack.EMPTY;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		sync();
		if (slot == SLOT_INPUT) {
			if (!stack.isEmpty()) {
				insert(stack, stack.getCount(), false);
			}
			return;
		}
		// 꺼내는 칸에 놓기: 보여 주던 것과의 차이만큼 개수를 바꾼다
		if (stack.isEmpty()) {
			count = Math.max(0, count - viewCount);
		} else if (template.isEmpty() || ItemStack.isSameItemSameComponents(template, stack)) {
			if (template.isEmpty()) {
				template = stack.copyWithCount(1);
			}
			count = Math.max(0, count - viewCount + stack.getCount());
		}
		if (count == 0) {
			template = ItemStack.EMPTY;
		}
		changed();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot != SLOT_INPUT || !accepts(stack)) {
			return false;
		}
		return voiding || capacityFor(stack) - count >= stack.getCount();
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
		template = ItemStack.EMPTY;
		count = 0;
		changed();
	}

	// ---- 저장 · 아이템 컴포넌트 · 클라이언트 동기화 ----

	public DrawerContents contents() {
		sync();
		return new DrawerContents(template.isEmpty() ? ItemStack.EMPTY : template.copyWithCount(1), count, upgrades, voiding);
	}

	private void apply(DrawerContents contents) {
		template = contents.item().isEmpty() || contents.count() <= 0 ? ItemStack.EMPTY : contents.item().copyWithCount(1);
		count = template.isEmpty() ? 0 : contents.count();
		upgrades = Math.min(MAX_UPGRADES, contents.upgrades());
		voiding = contents.voiding();
		view = ItemStack.EMPTY;
		viewCount = 0;
		refreshView();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		apply(input.read("drawer", DrawerContents.CODEC).orElse(DrawerContents.EMPTY));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("drawer", DrawerContents.CODEC, contents());
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		DrawerContents contents = components.get(ModStorage.DRAWER_CONTENTS);
		if (contents != null) {
			apply(contents);
		}
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		DrawerContents contents = contents();
		if (!contents.isEmpty()) {
			components.set(ModStorage.DRAWER_CONTENTS, contents);
		}
	}

	@Override
	public void removeComponentsFromTag(ValueOutput output) {
		output.discard("drawer");
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
