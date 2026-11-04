package io.github.yhyeop.sanupcraft.mining;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.power.SteamConsumer;

/**
 * 증기 굴착기: 아래 9x9를 위층부터 한 층씩 바닥까지 캔다. 철 곡괭이로 캔 것처럼 떨어뜨린 것을 바로 위 상자 · 깔때기 · 벨트에 넣는다.
 * 위가 꽉 차거나 비어 있으면 멈춘다. 상자 · 화로처럼 블록 엔티티가 있는 블록과 액체 · 부술 수 없는 블록은 건너뛴다.
 */
public class QuarryBlockEntity extends BlockEntity implements SteamConsumer {
	public static final int RADIUS = 4;
	public static final int STEAM_CAPACITY = 400;
	public static final int STEAM_PER_BLOCK = 8;
	/** 이만큼 틱마다 한 블록 (증기 평균 틱당 2). */
	public static final int TICKS_PER_BLOCK = 4;
	/** 한 번에 빈 칸을 이만큼까지 건너뛴다 (공기 층에서 멈추지 않게). */
	private static final int MAX_SCAN = 81;
	private static final ItemStack TOOL = new ItemStack(Items.IRON_PICKAXE);

	private int steam;
	private int cursor;
	private int layer;
	private boolean started;
	private int mined;
	private boolean finished;
	private final List<ItemStack> pending = new ArrayList<>();

	public QuarryBlockEntity(BlockPos pos, BlockState state) {
		super(ModMining.QUARRY_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, QuarryBlockEntity quarry) {
		boolean running = quarry.work(level, pos);
		if (state.getValue(QuarryBlock.POWERED) != running) {
			level.setBlockAndUpdate(pos, state.setValue(QuarryBlock.POWERED, running));
		}
	}

	/** 이번 틱에 일했으면(또는 일할 차례를 기다리는 중이면) true. */
	private boolean work(ServerLevel level, BlockPos pos) {
		if (!flush(level, pos)) {
			return false;
		}
		if (finished || steam < STEAM_PER_BLOCK) {
			return false;
		}
		if (level.getGameTime() % TICKS_PER_BLOCK != 0) {
			return true;
		}
		if (!started) {
			layer = pos.getY() - 1;
			started = true;
		}
		for (int scanned = 0; scanned < MAX_SCAN; scanned++) {
			if (layer < level.getMinY()) {
				finished = true;
				setChanged();
				return false;
			}
			BlockPos target = target(pos);
			advance();
			if (mine(level, target)) {
				steam -= STEAM_PER_BLOCK;
				setChanged();
				return true;
			}
		}
		setChanged();
		return true;
	}

	private BlockPos target(BlockPos pos) {
		int side = RADIUS * 2 + 1;
		int dx = cursor % side - RADIUS;
		int dz = cursor / side - RADIUS;
		return new BlockPos(pos.getX() + dx, layer, pos.getZ() + dz);
	}

	private void advance() {
		int side = RADIUS * 2 + 1;
		if (++cursor >= side * side) {
			cursor = 0;
			layer--;
		}
	}

	/** 캘 수 있는 블록이면 캐서 떨어진 것을 모아 둔다. */
	private boolean mine(ServerLevel level, BlockPos target) {
		BlockState state = level.getBlockState(target);
		if (state.isAir() || !state.getFluidState().isEmpty() || state.getDestroySpeed(level, target) < 0
				|| level.getBlockEntity(target) != null || state.getBlock() instanceof QuarryBlock) {
			return false;
		}
		pending.addAll(Block.getDrops(state, level, target, null, null, TOOL));
		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, target, Block.getId(state));
		level.removeBlock(target, false);
		mined++;
		return true;
	}

	/** 모아 둔 것을 위로 보낸다. 다 보냈으면 true. */
	private boolean flush(ServerLevel level, BlockPos pos) {
		if (pending.isEmpty()) {
			return true;
		}
		Container above = HopperBlockEntity.getContainerAt(level, pos.above());
		if (above == null) {
			return false;
		}
		for (int i = 0; i < pending.size(); i++) {
			ItemStack rest = HopperBlockEntity.addItem(null, above, pending.get(i), Direction.DOWN);
			pending.set(i, rest);
		}
		pending.removeIf(ItemStack::isEmpty);
		return pending.isEmpty();
	}

	@Override
	public int acceptSteam(int amount) {
		int accepted = Math.min(amount, STEAM_CAPACITY - steam);
		steam += accepted;
		return accepted;
	}

	public int steam() {
		return steam;
	}

	public int mined() {
		return mined;
	}

	public int layer() {
		return layer;
	}

	public boolean finished() {
		return finished;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		steam = input.getIntOr("steam", 0);
		cursor = input.getIntOr("cursor", 0);
		layer = input.getIntOr("layer", 0);
		started = input.getBooleanOr("started", false);
		mined = input.getIntOr("mined", 0);
		finished = input.getBooleanOr("finished", false);
		pending.clear();
		input.read("pending", ItemStack.CODEC.listOf()).ifPresent(pending::addAll);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("steam", steam);
		output.putInt("cursor", cursor);
		output.putInt("layer", layer);
		output.putBoolean("started", started);
		output.putInt("mined", mined);
		output.putBoolean("finished", finished);
		output.store("pending", ItemStack.CODEC.listOf(), pending);
	}
}
