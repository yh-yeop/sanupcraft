package io.github.yhyeop.sanupcraft.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 저장 서랍 (철 · 강철). 앞면만 쓴다:
 * 우클릭 = 손의 것 넣기 (빨리 두 번 = 인벤토리의 같은 것 전부), 업그레이드를 들고 우클릭 = 끼우기, 웅크리고 빈손 우클릭 = 업그레이드 빼기,
 * 좌클릭 = 하나 꺼내기, 웅크리고 좌클릭 = 한 더미 꺼내기 (좌클릭은 {@link ModStorage}의 공격 이벤트가 받는다).
 * 앞면 좌클릭은 부수지 않으니 서랍은 옆 · 위에서 캔다. 부숴도 내용물은 서랍 아이템에 남는다.
 */
public class DrawerBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	/** 좌클릭을 누르고 있을 때 꺼내는 간격 (틱). */
	public static final int EXTRACT_COOLDOWN = 4;

	private final boolean steel;

	public DrawerBlock(boolean steel, BlockBehaviour.Properties properties) {
		super(properties);
		this.steel = steel;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public boolean isSteel() {
		return steel;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DrawerBlockEntity(pos, state);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (hit.getDirection() != state.getValue(FACING) || !(level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (stack.is(ModStorage.DRAWER_UPGRADE) || stack.is(ModStorage.DRAWER_VOID_UPGRADE)) {
			boolean added = stack.is(ModStorage.DRAWER_UPGRADE) ? drawer.addCapacityUpgrade() : drawer.addVoidUpgrade();
			if (added) {
				stack.consume(1, player);
				player.sendOverlayMessage(Component.translatable("block.sanupcraft.drawer.upgraded", drawer.upgrades(), DrawerBlockEntity.MAX_UPGRADES,
						capacityText(drawer)));
			} else {
				player.sendOverlayMessage(Component.translatable("block.sanupcraft.drawer.upgrade_full"));
			}
			return InteractionResult.SUCCESS;
		}
		if (drawer.registerClick(player, level.getGameTime())) {
			drawer.insertAllFrom(player.getInventory());
		} else {
			drawer.insertFromHand(player, stack);
		}
		player.sendOverlayMessage(status(drawer));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (hit.getDirection() != state.getValue(FACING) || !(level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			if (player.isSecondaryUseActive()) {
				ItemStack upgrade = drawer.removeUpgrade();
				if (upgrade.isEmpty()) {
					player.sendOverlayMessage(Component.translatable(drawer.upgrades() > 0 ? "block.sanupcraft.drawer.upgrade_stuck" : "block.sanupcraft.drawer.no_upgrade"));
				} else {
					give(player, upgrade);
					player.sendOverlayMessage(Component.translatable("block.sanupcraft.drawer.upgrade_removed", capacityText(drawer)));
				}
			} else {
				if (drawer.registerClick(player, level.getGameTime())) {
					drawer.insertAllFrom(player.getInventory());
				}
				player.sendOverlayMessage(status(drawer));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** 앞면 좌클릭 (서버): 하나 또는 (웅크리면) 한 더미를 꺼내 플레이어에게 준다. 누르고 있으면 {@link #EXTRACT_COOLDOWN}마다. */
	static void giveExtracted(Player player, DrawerBlockEntity drawer, boolean wholeStack, long gameTime) {
		ItemStack template = drawer.template();
		if (template.isEmpty() || !drawer.tryExtractCooldown(gameTime)) {
			return;
		}
		ItemStack out = drawer.extract(wholeStack ? template.getMaxStackSize() : 1);
		if (!out.isEmpty()) {
			give(player, out);
		}
	}

	static void give(Player player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
		}
	}

	static Component status(DrawerBlockEntity drawer) {
		ItemStack template = drawer.template();
		if (template.isEmpty()) {
			return Component.translatable("block.sanupcraft.drawer.empty", capacityText(drawer));
		}
		return Component.translatable("block.sanupcraft.drawer.status", template.getHoverName(), String.format("%,d", drawer.count()),
				String.format("%,d", drawer.capacityFor(template)));
	}

	private static Component capacityText(DrawerBlockEntity drawer) {
		return Component.translatable("block.sanupcraft.drawer.stacks", drawer.baseStacks() << drawer.upgrades());
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		if (!(level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) || drawer.count() <= 0) {
			return 0;
		}
		return 1 + (int) (14L * drawer.count() / Math.max(1, drawer.capacity()));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
