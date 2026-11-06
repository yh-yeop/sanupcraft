package io.github.yhyeop.sanupcraft.end;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 숨결 수집기 (M6). 9칸. 반경 {@link #RADIUS} 안에 용의 숨결 구름(엔더 드래곤이 남긴 구름, 또는 용의 숨결 입자 구름)이 있으면
 * {@link #TICKS_PER_BOTTLE}틱마다 칸의 유리병 하나를 용의 숨결로 바꾼다. 병을 채울 때마다 구름이 손으로 담을 때처럼 0.5 줄어든다.
 * 깔때기는 유리병만 넣고 용의 숨결만 뺀다.
 */
public class BreathCollectorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	public static final int SLOTS = 9;
	public static final double RADIUS = 8.0;
	public static final int TICKS_PER_BOTTLE = 20;
	private static final float RADIUS_PER_BOTTLE = 0.5F;
	private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private int timer;

	public BreathCollectorBlockEntity(BlockPos pos, BlockState state) {
		super(ModEnd.BREATH_COLLECTOR_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, BreathCollectorBlockEntity collector) {
		if (++collector.timer >= TICKS_PER_BOTTLE) {
			collector.timer = 0;
			collector.collect(level, pos);
		}
	}

	/** 용의 숨결 구름인가. */
	public static boolean isBreathCloud(AreaEffectCloud cloud) {
		return cloud.isAlive() && (cloud.getOwner() instanceof EnderDragon || cloud.getParticle().getType() == ParticleTypes.DRAGON_BREATH);
	}

	/** 가장 가까운 숨결 구름. 없으면 null. */
	public static @Nullable AreaEffectCloud findCloud(ServerLevel level, BlockPos pos) {
		AABB area = new AABB(pos).inflate(RADIUS);
		List<AreaEffectCloud> clouds = level.getEntitiesOfClass(AreaEffectCloud.class, area,
				cloud -> isBreathCloud(cloud) && cloud.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= RADIUS + cloud.getRadius());
		AreaEffectCloud best = null;
		double bestDistance = Double.MAX_VALUE;
		for (AreaEffectCloud cloud : clouds) {
			double d = cloud.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos));
			if (d < bestDistance) {
				bestDistance = d;
				best = cloud;
			}
		}
		return best;
	}

	/** 병 하나를 채워 본다. 채웠으면 true. 시험이 직접 부른다. */
	public boolean collect(ServerLevel level, BlockPos pos) {
		int bottle = -1;
		for (int i = 0; i < SLOTS; i++) {
			if (items.get(i).is(Items.GLASS_BOTTLE)) {
				bottle = i;
				break;
			}
		}
		if (bottle < 0) {
			return false;
		}
		AreaEffectCloud cloud = findCloud(level, pos);
		if (cloud == null) {
			return false;
		}
		int target = slotForBreath(bottle);
		if (target < 0) {
			return false;
		}
		items.get(bottle).shrink(1);
		if (items.get(target).isEmpty()) {
			items.set(target, new ItemStack(Items.DRAGON_BREATH));
		} else {
			items.get(target).grow(1);
		}
		cloud.setRadius(cloud.getRadius() - RADIUS_PER_BOTTLE);
		level.playSound(null, pos, SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.sendParticles(net.minecraft.core.particles.PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.25, 0.1, 0.25, 0.01);
		setChanged();
		return true;
	}

	/** 용의 숨결을 넣을 칸: 같은 것이 든 자리 있는 칸 → 병이 하나뿐이던 칸 → 빈 칸. 없으면 -1. */
	private int slotForBreath(int bottleSlot) {
		for (int i = 0; i < SLOTS; i++) {
			ItemStack stack = items.get(i);
			if (stack.is(Items.DRAGON_BREATH) && stack.getCount() < stack.getMaxStackSize()) {
				return i;
			}
		}
		if (items.get(bottleSlot).getCount() == 1) {
			return bottleSlot;
		}
		for (int i = 0; i < SLOTS; i++) {
			if (items.get(i).isEmpty()) {
				return i;
			}
		}
		return -1;
	}

	public int count(net.minecraft.world.item.Item item) {
		int total = 0;
		for (ItemStack stack : items) {
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.sanupcraft.breath_collector");
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new DispenserMenu(containerId, inventory, this);
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
	public int getContainerSize() {
		return SLOTS;
	}

	/** 손으로는 유리병만 넣는다 (꺼내는 것은 무엇이든). */
	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return stack.is(Items.GLASS_BOTTLE);
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return ALL_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return stack.is(Items.GLASS_BOTTLE);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return stack.is(Items.DRAGON_BREATH);
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}
}
