package io.github.yhyeop.sanupcraft.transport;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 적재 · 하역 역: 9칸 창고. 옆 레일(가로 한 칸, 대각선 포함)에 화물칸(상자 달린 수레)이 서거나 지나가면
 * {@link #TICKS_PER_TRANSFER}틱마다 아이템을 최대 설정값(기본 {@link #ITEMS_PER_TRANSFER})개 옮긴다.
 * 적재: 역 → 화물칸, 하역: 화물칸 → 역. 깔때기로 역에 넣고 뺄 수 있다.
 */
public class CargoStationBlockEntity extends BaseContainerBlockEntity {
	public static final int SIZE = 9;
	public static final int TICKS_PER_TRANSFER = 2;
	public static final int ITEMS_PER_TRANSFER = 8;

	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private int cooldown;

	public CargoStationBlockEntity(BlockPos pos, BlockState state) {
		super(ModTransport.CARGO_STATION_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, CargoStationBlockEntity station) {
		if (--station.cooldown > 0) {
			return;
		}
		station.cooldown = TICKS_PER_TRANSFER;
		boolean unload = state.getValue(CargoStationBlock.UNLOAD);
		int budget = SanupcraftConfig.current().stationItemsPerTransfer;
		for (AbstractMinecartContainer wagon : wagonsNear(level, pos)) {
			if (budget <= 0) {
				break;
			}
			budget -= unload ? move(wagon, station, budget) : move(station, wagon, budget);
		}
	}

	/** 옆 레일에 있는 화물칸 (상자 달린 수레). */
	public static List<AbstractMinecartContainer> wagonsNear(ServerLevel level, BlockPos pos) {
		return level.getEntitiesOfClass(AbstractMinecartContainer.class, new AABB(pos).inflate(1.0, 0.5, 1.0), wagon -> wagon.isAlive());
	}

	/** {@code from}에서 {@code to}로 최대 {@code budget}개 옮기고 옮긴 개수를 돌려준다. */
	static int move(Container from, Container to, int budget) {
		int moved = 0;
		for (int i = 0; i < from.getContainerSize() && moved < budget; i++) {
			ItemStack stack = from.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			int count = Math.min(stack.getCount(), budget - moved);
			ItemStack rest = HopperBlockEntity.addItem(from, to, stack.copyWithCount(count), null);
			int done = count - rest.getCount();
			if (done > 0) {
				from.removeItem(i, done);
				moved += done;
			}
		}
		return moved;
	}

	/** 들어 있는 아이템 수 (상태 표시). */
	public int itemCount() {
		int count = 0;
		for (ItemStack stack : items) {
			count += stack.getCount();
		}
		return count;
	}

	@Override
	public int getContainerSize() {
		return SIZE;
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
		return new DispenserMenu(containerId, inventory, this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}
}
