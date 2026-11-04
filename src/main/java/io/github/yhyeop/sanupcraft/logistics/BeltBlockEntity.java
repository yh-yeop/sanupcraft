package io.github.yhyeop.sanupcraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 벨트 한 칸. 모터가 {@link #power}로 잠깐씩 켜 준다.
 * 깔때기에게는 칸 하나짜리 상자로 보이고, 들어온 아이템을 벨트 위에 올려놓는다.
 */
public class BeltBlockEntity extends BlockEntity implements Container {
	/** 칸/틱. 걷는 속도의 절반쯤. */
	public static final double SPEED = 0.125;
	/** 벨트 한 칸에 깔때기로 올릴 수 있는 아이템 덩어리 수. */
	private static final int MAX_ITEMS_FROM_HOPPER = 4;

	private long poweredUntil;

	public BeltBlockEntity(BlockPos pos, BlockState state) {
		super(ModLogistics.BELT_BLOCK_ENTITY, pos, state);
	}

	/** 모터가 부른다: {@code until} 틱까지 돈다. */
	public void power(long until) {
		poweredUntil = Math.max(poweredUntil, until);
		BlockState state = getBlockState();
		if (level != null && !state.getValue(BeltBlock.POWERED)) {
			level.setBlockAndUpdate(worldPosition, state.setValue(BeltBlock.POWERED, true));
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BeltBlockEntity belt) {
		if (!state.getValue(BeltBlock.POWERED)) {
			return;
		}
		if (level instanceof ServerLevel && level.getGameTime() > belt.poweredUntil) {
			level.setBlockAndUpdate(pos, state.setValue(BeltBlock.POWERED, false));
			return;
		}
		Direction facing = state.getValue(BeltBlock.FACING);
		BlockPos front = pos.relative(facing);
		boolean frontIsBelt = level.getBlockState(front).getBlock() instanceof BeltBlock;
		for (Entity entity : level.getEntitiesOfClass(Entity.class, surface(pos), BeltBlockEntity::rides)) {
			move(entity, pos, facing);
			if (!frontIsBelt && level instanceof ServerLevel && entity instanceof ItemEntity item) {
				deliver(level, pos, facing, front, item);
			}
		}
	}

	private static AABB surface(BlockPos pos) {
		return new AABB(pos.getX(), pos.getY() + 0.1, pos.getZ(), pos.getX() + 1, pos.getY() + BeltBlock.HEIGHT + 0.4, pos.getZ() + 1);
	}

	private static boolean rides(Entity entity) {
		return entity.isAlive() && !entity.isSpectator() && !entity.isShiftKeyDown()
				&& (entity instanceof ItemEntity || entity.isPushable() || entity instanceof Player);
	}

	/** 앞으로 밀고, 옆으로는 벨트 가운데로 모은다. */
	private static void move(Entity entity, BlockPos pos, Direction facing) {
		Vec3 motion = entity.getDeltaMovement();
		double x = facing.getStepX() * SPEED;
		double z = facing.getStepZ() * SPEED;
		if (facing.getAxis() == Direction.Axis.X) {
			z = (pos.getZ() + 0.5 - entity.getZ()) * 0.2;
		} else {
			x = (pos.getX() + 0.5 - entity.getX()) * 0.2;
		}
		entity.setDeltaMovement(x, motion.y, z);
	}

	/** 벨트 끝까지 온 아이템을 앞의 상자 · 기계에 넣는다. 다 안 들어가면 남은 만큼 벨트에 남는다. */
	private static void deliver(Level level, BlockPos pos, Direction facing, BlockPos front, ItemEntity item) {
		double along = (item.getX() - (pos.getX() + 0.5)) * facing.getStepX() + (item.getZ() - (pos.getZ() + 0.5)) * facing.getStepZ();
		if (along < 0.3) {
			return;
		}
		Container target = HopperBlockEntity.getContainerAt(level, front);
		if (target == null) {
			return;
		}
		ItemStack rest = HopperBlockEntity.addItem(null, target, item.getItem().copy(), facing.getOpposite());
		if (rest.isEmpty()) {
			item.discard();
		} else {
			item.setItem(rest);
		}
	}

	// ---- 깔때기용: 칸 하나짜리 상자인 척 ----

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return level != null && level.getEntitiesOfClass(ItemEntity.class, surface(worldPosition)).size() < MAX_ITEMS_FROM_HOPPER;
	}

	/** 깔때기가 넣은 아이템을 벨트 위 가운데에 올린다. */
	@Override
	public void setItem(int slot, ItemStack stack) {
		if (stack.isEmpty() || !(level instanceof ServerLevel)) {
			return;
		}
		ItemEntity item = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + BeltBlock.HEIGHT + 0.05,
				worldPosition.getZ() + 0.5, stack.copy(), 0.0, 0.0, 0.0);
		item.setDefaultPickUpDelay();
		level.addFreshEntity(item);
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
	}
}
