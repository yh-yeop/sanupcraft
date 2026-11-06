package io.github.yhyeop.sanupcraft.nether;

import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 금 분배기 (M5): 금괴 칸 하나. 60틱(설정 {@code goldDispenser.ticksPerThrow})마다 6칸 안의 가장 가까운 어른 피글린에게 금괴 하나를 던진다.
 * 이미 금을 들고 살펴보는(손에 든 · 감탄 중인) 피글린, 아기, 좀비 피글린 · 피글린 야수는 건너뛴다.
 * 던진 금괴에는 {@link #THROWN_TAG}를 붙여, 교역 수집판이 2초 동안은 빨아들이지 않게 한다 (그 사이 피글린이 줍는다).
 * 금괴는 우클릭 · 깔때기로 넣는다. 웅크리고 빈손 우클릭으로 꺼낸다.
 */
public class GoldDispenserBlockEntity extends BlockEntity implements WorldlyContainer {
	public static final int RANGE = 6;
	/** 기본값. 실제로 쓰는 값은 설정 파일. */
	public static final int TICKS_PER_THROW = 60;
	/** 분배기가 던진 금괴 아이템 엔티티의 태그. */
	public static final String THROWN_TAG = "sanupcraft.gold_dispenser";
	private static final int[] SLOTS = {0};

	private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private int cooldown;

	public GoldDispenserBlockEntity(BlockPos pos, BlockState state) {
		super(ModNether.GOLD_DISPENSER_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, GoldDispenserBlockEntity dispenser) {
		if (--dispenser.cooldown > 0) {
			return;
		}
		dispenser.cooldown = SanupcraftConfig.current().goldDispenserTicksPerThrow;
		dispenser.throwGold(level, pos);
	}

	/** 금괴가 있고 받을 피글린이 있으면 하나 던진다. 던진 아이템 엔티티 (없으면 null). */
	public @Nullable ItemEntity throwGold(ServerLevel level, BlockPos pos) {
		ItemStack gold = items.get(0);
		if (!gold.is(Items.GOLD_INGOT)) {
			return null;
		}
		Vec3 from = Vec3.atCenterOf(pos).add(0.0, 0.7, 0.0);
		Piglin target = targets(level, pos).stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(from))).orElse(null);
		if (target == null) {
			return null;
		}
		ItemEntity item = new ItemEntity(level, from.x, from.y, from.z, gold.split(1));
		item.setDeltaMovement(throwVelocity(from, target.position()));
		item.setPickUpDelay(10);
		item.addTag(THROWN_TAG);
		level.addFreshEntity(item);
		level.playSound(null, pos, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 0.6F, 1.2F);
		setChanged();
		return item;
	}

	/** 피글린 발치 쪽으로 낮게 던지는 속도 (멀수록 세게, 최대 0.45). */
	static Vec3 throwVelocity(Vec3 from, Vec3 to) {
		Vec3 flat = new Vec3(to.x - from.x, 0.0, to.z - from.z);
		double distance = flat.length();
		if (distance < 1.0E-4) {
			return new Vec3(0.0, 0.2, 0.0);
		}
		double speed = Math.min(0.45, 0.06 * distance + 0.1);
		return flat.scale(speed / distance).add(0.0, 0.22, 0.0);
	}

	/** 금괴를 받을 수 있는 피글린들: 6칸 안, 어른, 살아 있고, 손이 비었고, 감탄 중이 아님. */
	public static List<Piglin> targets(ServerLevel level, BlockPos pos) {
		Vec3 center = Vec3.atCenterOf(pos);
		AABB box = new AABB(pos).inflate(RANGE);
		return level.getEntitiesOfClass(Piglin.class, box, piglin -> piglin.isAlive()
				&& !piglin.isBaby()
				&& piglin.distanceToSqr(center) <= RANGE * RANGE
				&& piglin.getOffhandItem().isEmpty()
				&& !piglin.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_ITEM));
	}

	public int goldCount() {
		return items.get(0).getCount();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(1, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}

	// ---- 금괴 칸 ----

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
		return stack.is(Items.GOLD_INGOT);
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
