package io.github.yhyeop.sanupcraft.storage;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 창고 단말 · 제작 단말. 우클릭으로 연다 (증기가 있어야 한다). 웅크리고 빈손 우클릭: 주인이면 잠그기 · 풀기.
 * 휴대 단말을 들고 우클릭하면 그 휴대 단말이 이 단말에 연결된다 (휴대 단말 아이템이 처리).
 */
public class TerminalBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

	private final boolean crafting;

	public TerminalBlock(boolean crafting, BlockBehaviour.Properties properties) {
		super(properties);
		this.crafting = crafting;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public boolean isCrafting() {
		return crafting;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TerminalBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel ? createTickerHelper(type, ModStorage.TERMINAL_BLOCK_ENTITY, TerminalBlockEntity::serverTick) : null;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
		super.setPlacedBy(level, pos, state, by, itemStack);
		if (!level.isClientSide() && by instanceof Player player && level.getBlockEntity(pos) instanceof TerminalBlockEntity terminal) {
			terminal.lock().setOwner(player);
			terminal.setChanged();
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		// 휴대 단말은 아이템 쪽 useOn이 연결을 맡는다
		return stack.getItem() instanceof PortableTerminalItem ? InteractionResult.PASS : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TerminalBlockEntity terminal) {
			if (player.isSecondaryUseActive()) {
				OwnerLock lock = terminal.lock();
				if (lock.toggle(player)) {
					terminal.setChanged();
					player.sendOverlayMessage(Component.translatable(lock.isLocked() ? "block.sanupcraft.storage_terminal.lock_on" : "block.sanupcraft.storage_terminal.lock_off"));
				} else {
					player.sendOverlayMessage(Component.translatable("block.sanupcraft.storage_terminal.locked", lock.ownerName()));
				}
			} else {
				terminal.tryOpen(player, terminal);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		StorageNetwork.invalidate();
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		StorageNetwork.invalidate();
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation,
			boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
		StorageNetwork.invalidate();
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
