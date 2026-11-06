package io.github.yhyeop.sanupcraft.end;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.state.BlockState;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.production.SteamWorkerBlockEntity;

/**
 * 후렴 수확기 (M6). 가로 반경 {@link #RADIUS}, 높이 ±{@link #REACH} 안에서 다 자란 후렴 식물(자라는 꽃이 하나도 없음)을 찾아
 * 맨 아래 줄기 하나만 남기고 위를 전부 거둔다 (후렴과). 남긴 줄기 위에 새 꽃(나이 0)을 심어 다시 자라게 한다.
 * {@link #TICKS_PER_SCAN}틱(설정)마다 한 번 훑어 한 그루를 거두고, 그때 증기 {@link #STEAM_PER_PLANT}을 쓴다. 거둔 것은 9칸에.
 */
public class ChorusHarvesterBlockEntity extends SteamWorkerBlockEntity {
	public static final int RADIUS = 4;
	public static final int REACH = 2;
	public static final int STEAM_CAPACITY = 200;
	public static final int STEAM_PER_PLANT = 20;
	/** 기본값. 실제로 쓰는 값은 설정 파일. */
	public static final int TICKS_PER_SCAN = 40;
	/** 한 그루에서 따라가는 블록 수 한도. */
	private static final int MAX_BLOCKS = 256;

	public ChorusHarvesterBlockEntity(BlockPos pos, BlockState state) {
		super(ModEnd.CHORUS_HARVESTER_BLOCK_ENTITY, pos, state);
	}

	@Override
	protected boolean work(ServerLevel level, BlockPos pos, BlockState state) {
		if (!flushPending() || steam < STEAM_PER_PLANT) {
			return false;
		}
		if (level.getGameTime() % SanupcraftConfig.current().chorusHarvesterTicksPerScan != 0) {
			return true;
		}
		BlockPos bottom = findRipe(level, pos);
		return bottom != null && harvest(level, bottom);
	}

	/** 범위 안에서 다 자란 후렴 식물의 맨 아래 줄기. 없으면 null. */
	public @Nullable BlockPos findRipe(ServerLevel level, BlockPos pos) {
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RADIUS, -REACH, -RADIUS), pos.offset(RADIUS, REACH, RADIUS))) {
			BlockState state = level.getBlockState(p);
			if (state.is(Blocks.CHORUS_PLANT) && !isChorus(level.getBlockState(p.below())) && isRipe(level, p.immutable())) {
				return p.immutable();
			}
		}
		return null;
	}

	private static boolean isChorus(BlockState state) {
		return state.is(Blocks.CHORUS_PLANT) || state.is(Blocks.CHORUS_FLOWER);
	}

	/** 맨 아래 줄기 위로 이어진 블록 (맨 아래는 빼고, 아래쪽부터). */
	public static List<BlockPos> plantAbove(ServerLevel level, BlockPos bottom) {
		List<BlockPos> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		seen.add(bottom);
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(bottom);
		while (!queue.isEmpty() && found.size() < MAX_BLOCKS) {
			BlockPos current = queue.poll();
			for (Direction direction : Direction.values()) {
				BlockPos next = current.relative(direction);
				if (next.getY() <= bottom.getY() || !seen.add(next) || !isChorus(level.getBlockState(next))) {
					continue;
				}
				found.add(next);
				queue.add(next);
			}
		}
		return found;
	}

	/** 다 자랐나: 위에 블록이 있고, 자라는 꽃(나이 5 미만)이 없다. */
	public static boolean isRipe(ServerLevel level, BlockPos bottom) {
		List<BlockPos> above = plantAbove(level, bottom);
		if (above.isEmpty()) {
			return false;
		}
		for (BlockPos p : above) {
			BlockState state = level.getBlockState(p);
			if (state.is(Blocks.CHORUS_FLOWER) && state.getValue(ChorusFlowerBlock.AGE) < ChorusFlowerBlock.DEAD_AGE) {
				return false;
			}
		}
		return true;
	}

	/** 한 그루를 거둔다 (맨 아래 줄기는 남기고 그 위에 새 꽃). 거뒀으면 true. 시험이 직접 부른다. */
	public boolean harvest(ServerLevel level, BlockPos bottom) {
		if (steam < STEAM_PER_PLANT) {
			return false;
		}
		List<BlockPos> above = plantAbove(level, bottom);
		if (above.isEmpty()) {
			return false;
		}
		List<ItemStack> drops = new ArrayList<>();
		// 위에서부터 지워야 이웃 줄기가 저절로 부서져 떨어지지 않는다
		for (int i = above.size() - 1; i >= 0; i--) {
			BlockPos p = above.get(i);
			BlockState state = level.getBlockState(p);
			drops.addAll(Block.getDrops(state, level, p, null));
			level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		// 시든 꽃을 되살려 남긴 줄기 위에 다시 심는다 (바닐라 꽃은 사람이 부술 때만 떨어지므로 거두지 않는다)
		level.setBlock(bottom.above(), Blocks.CHORUS_FLOWER.defaultBlockState(), Block.UPDATE_ALL);
		BlockState stem = level.getBlockState(bottom);
		level.setBlock(bottom, ChorusPlantBlock.getStateWithConnections(level, bottom, stem), Block.UPDATE_ALL);
		steam -= STEAM_PER_PLANT;
		level.playSound(null, bottom, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 0.8F, 1.0F);
		level.sendParticles(ParticleTypes.PORTAL, bottom.getX() + 0.5, bottom.getY() + 1.5, bottom.getZ() + 0.5, 12, 0.3, 0.8, 0.3, 0.2);
		for (ItemStack drop : drops) {
			if (!drop.isEmpty()) {
				stash(drop);
			}
		}
		setChanged();
		return true;
	}

	@Override
	public int steamCapacity() {
		return STEAM_CAPACITY;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.sanupcraft.chorus_harvester");
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, this, 1);
	}
}
