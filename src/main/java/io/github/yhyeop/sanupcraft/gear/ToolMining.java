package io.github.yhyeop.sanupcraft.gear;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import io.github.yhyeop.sanupcraft.production.TreeFelling;

/**
 * 증기 드릴의 3x3 캐기와 전기톱의 나무 통째 베기. 블록을 캔 뒤({@link PlayerBlockBreakEvents#AFTER})에 같은 손 공구로 이웃을 이어서 캔다.
 * 이웃도 보통 캐기({@code gameMode.destroyBlock})로 캐서 증기는 공구의 {@code mineBlock}이 블록마다 뺀다.
 * 증기가 모자라면 거기서 멈춘다. 웅크리고 캐면 둘 다 한 칸만. {@link AreaMining}(강철 해머 · 삽)과 같은 방식이고 서로 건드리지 않는다.
 */
public final class ToolMining {
	/** 가운데보다 이만큼 더 단단하면 같이 캐지 않는다 (드릴 3x3). */
	private static final float HARDNESS_MARGIN = 1.5F;

	/** 캐기 직전에 본 면 (캐고 나면 블록이 없어 다시 볼 수 없다). */
	private static final Map<UUID, Direction> FACES = new ConcurrentHashMap<>();
	private static boolean breaking;

	private ToolMining() {
	}

	public static void init() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!breaking && player.getMainHandItem().getItem() instanceof SteamDrillItem) {
				FACES.put(player.getUUID(), hitFace(player, pos));
			}
			return true;
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			Direction face = FACES.remove(player.getUUID());
			if (breaking || !(player instanceof ServerPlayer serverPlayer) || player.isShiftKeyDown()) {
				return;
			}
			ItemStack tool = player.getMainHandItem();
			if (tool.getItem() instanceof SteamDrillItem && SteamDrillItem.isWide(tool) && face != null) {
				breakAround(level, serverPlayer, pos, state, face);
			} else if (tool.getItem() instanceof ChainsawItem && TreeFelling.isLog(state) && level instanceof ServerLevel serverLevel) {
				fellTree(serverLevel, serverPlayer, pos);
			}
		});
	}

	private static Direction hitFace(Player player, BlockPos pos) {
		HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
		if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK && blockHit.getBlockPos().equals(pos)) {
			return blockHit.getDirection();
		}
		return Direction.orderedByNearest(player)[0].getOpposite();
	}

	/**
	 * 드릴 3x3: 캔 블록({@code center})을 뺀 둘레 8칸을 캔다. 공구로 드롭을 받을 수 없는 블록 · 가운데보다 훨씬 단단한 블록은 남긴다.
	 * 캔 블록 수를 돌려준다.
	 */
	public static int breakAround(Level level, ServerPlayer player, BlockPos center, BlockState centerState, Direction face) {
		float centerHardness = centerState.getDestroySpeed(level, center);
		int broken = 0;
		breaking = true;
		try {
			for (BlockPos pos : AreaMining.plane(center, face)) {
				ItemStack tool = player.getMainHandItem();
				if (!(tool.getItem() instanceof SteamDrillItem drill)) {
					break;
				}
				BlockState state = level.getBlockState(pos);
				float hardness = state.getDestroySpeed(level, pos);
				if (state.isAir() || hardness < 0 || hardness > centerHardness + HARDNESS_MARGIN || !tool.isCorrectToolForDrops(state)) {
					continue;
				}
				if (!drill.canMine(tool, state)) {
					break;
				}
				if (player.gameMode.destroyBlock(pos)) {
					broken++;
				}
			}
		} finally {
			breaking = false;
		}
		return broken;
	}

	/**
	 * 전기톱: 방금 캔 원목({@code broken}) 둘레에 이어진 나무를 마저 벤다. 자연 잎이 안 붙은 원목 무더기는 건드리지 않는다.
	 * 증기가 모자라면 거기서 멈춘다. 마저 벤 블록 수를 돌려준다.
	 */
	public static int fellTree(ServerLevel level, ServerPlayer player, BlockPos broken) {
		List<BlockPos> seeds = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos next = broken.offset(dx, dy, dz);
					if (TreeFelling.isLog(level.getBlockState(next))) {
						seeds.add(next);
					}
				}
			}
		}
		TreeFelling.Tree tree = TreeFelling.scan(level, seeds, TreeFelling.MAX_BLOCKS);
		if (!tree.isTree()) {
			return 0;
		}
		int felled = 0;
		breaking = true;
		try {
			for (BlockPos pos : tree.fellOrder()) {
				ItemStack tool = player.getMainHandItem();
				if (!(tool.getItem() instanceof ChainsawItem saw)) {
					break;
				}
				BlockState state = level.getBlockState(pos);
				if (state.isAir()) {
					continue;
				}
				if (!saw.canMine(tool, state)) {
					break;
				}
				if (player.gameMode.destroyBlock(pos)) {
					felled++;
				}
			}
		} finally {
			breaking = false;
		}
		return felled;
	}
}
