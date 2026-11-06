package io.github.yhyeop.sanupcraft.power;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.nether.ModNether;

/**
 * 영혼 화로 (M5): 영혼 모래 · 영혼 흙(태그 {@code minecraft:soul_fire_base_blocks}) 바로 위에서만 돈다.
 * 블레이즈 가루 1 = 고압 증기 400을 틱당 4씩 (100틱). 물은 쓰지 않는다. 고압 증기는 고압 파이프로 고압 기계에만 나간다.
 * 고압 증기가 가득하면 새 가루를 태우지 않는다 (타던 가루는 남은 만큼 기다린다). 수치는 설정 {@code soulFurnace}.
 * 가루 칸 하나 (우클릭 · 깔때기로 넣고, 빼지는 못한다).
 */
public class SoulFurnaceBlockEntity extends BlockEntity implements WorldlyContainer, SteamStorage {
	public static final int STEAM_CAPACITY = 1000;
	/** 기본값. 실제로 쓰는 값은 설정 파일. */
	public static final int STEAM_PER_POWDER = 400;
	public static final int STEAM_PER_TICK = 4;
	private static final int LIT_HOLD_TICKS = 10;
	private static final int[] SLOTS = {0};

	/** 상태 (빈손 우클릭 표시용). */
	public enum Status {
		NO_SOUL_GROUND, NO_FUEL, FULL, RUNNING
	}

	private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private int steam;
	/** 지금 타는 가루에서 아직 나올 고압 증기. */
	private int burnLeft;
	private boolean onSoulGround;
	private int idleTicks = LIT_HOLD_TICKS;
	private final SteamFlowMeter flow = new SteamFlowMeter();
	private final SteamOutlet outlet = new SteamOutlet(true);

	public SoulFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(ModNether.SOUL_FURNACE_BLOCK_ENTITY, pos, state);
	}

	public static boolean isFuel(ItemStack stack) {
		return stack.is(Items.BLAZE_POWDER);
	}

	/** 바로 아래가 영혼 모래 · 영혼 흙인가. */
	public static boolean isSoulGround(Level level, BlockPos pos) {
		return level.getBlockState(pos.below()).is(BlockTags.SOUL_FIRE_BASE_BLOCKS);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SoulFurnaceBlockEntity furnace) {
		int made = furnace.work(level, pos);
		boolean lit = furnace.isLit();
		if (state.getValue(SoulFurnaceBlock.LIT) != lit) {
			level.setBlockAndUpdate(pos, state.setValue(SoulFurnaceBlock.LIT, lit));
		}
		if (made > 0) {
			setChanged(level, pos, state);
		}
	}

	/** 한 틱 일한다: 가루를 태워 고압 증기를 만들고 고압망에 나눈다. 만든 양을 돌려준다. */
	public int work(ServerLevel level, BlockPos pos) {
		SanupcraftConfig config = SanupcraftConfig.current();
		int before = steam;
		onSoulGround = isSoulGround(level, pos);
		int made = 0;
		if (onSoulGround) {
			ItemStack fuel = items.get(0);
			if (burnLeft <= 0 && steam < STEAM_CAPACITY && isFuel(fuel)) {
				fuel.shrink(1);
				burnLeft = config.soulFurnaceSteamPerPowder;
			}
			made = Math.min(config.soulFurnaceSteamPerTick, Math.min(STEAM_CAPACITY - steam, burnLeft));
			if (made > 0) {
				steam += made;
				burnLeft -= made;
				flow.add(level.getGameTime(), made);
			}
		}
		idleTicks = made > 0 ? 0 : Math.min(idleTicks + 1, LIT_HOLD_TICKS);
		steam = outlet.distribute(level, pos, steam);
		if (steam != before) {
			setChanged();
		}
		return made;
	}

	/** 최근에 고압 증기를 만들었나 (불빛). */
	public boolean isLit() {
		return idleTicks < LIT_HOLD_TICKS;
	}

	public Status status() {
		if (level != null && !isSoulGround(level, worldPosition)) {
			return Status.NO_SOUL_GROUND;
		}
		if (steam >= STEAM_CAPACITY) {
			return Status.FULL;
		}
		if (burnLeft <= 0 && items.get(0).isEmpty()) {
			return Status.NO_FUEL;
		}
		return Status.RUNNING;
	}

	public int steam() {
		return steam;
	}

	public int fuelCount() {
		return items.get(0).getCount();
	}

	@Override
	public int steamStored() {
		return steam;
	}

	@Override
	public int steamCapacity() {
		return STEAM_CAPACITY;
	}

	/** 화로는 만든 양을 센다. */
	@Override
	public int steamReceivedLastSecond() {
		return flow.lastSecond(level != null ? level.getGameTime() : 0L);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(1, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		steam = input.getIntOr("steam", 0);
		burnLeft = input.getIntOr("burn_left", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("steam", steam);
		output.putInt("burn_left", burnLeft);
	}

	// ---- 가루 칸 ----

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
