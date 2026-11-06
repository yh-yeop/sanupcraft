package io.github.yhyeop.sanupcraft.power;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import io.github.yhyeop.sanupcraft.nether.ModNether;

/**
 * 영혼 화로 (M5). 영혼 모래 · 영혼 흙 위에 두고 블레이즈 가루를 넣으면 고압 증기를 만든다 (물 필요 없음).
 * 블레이즈 가루를 들고 우클릭하면 넣고, 빈손이면 상태를 보여 준다. 고압 파이프로 잇는다. 깔때기로도 가루를 넣는다.
 */
public class SoulFurnaceBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public SoulFurnaceBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SoulFurnaceBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModNether.SOUL_FURNACE_BLOCK_ENTITY, (l, pos, s, furnace) -> SoulFurnaceBlockEntity.serverTick(serverLevel, pos, s, furnace))
				: null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!SoulFurnaceBlockEntity.isFuel(stack) || !(level.getBlockEntity(pos) instanceof SoulFurnaceBlockEntity furnace)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			ItemStack current = furnace.getItem(0);
			if (current.isEmpty()) {
				furnace.setItem(0, stack.split(stack.getCount()));
			} else {
				int move = Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount());
				current.grow(move);
				stack.shrink(move);
				furnace.setChanged();
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SoulFurnaceBlockEntity furnace) {
			player.sendOverlayMessage(statusMessage(furnace));
		}
		return InteractionResult.SUCCESS;
	}

	/** 빈손 우클릭 문구: 고압 증기 · 가루 수 + 가장 먼저 할 일 하나. */
	public static Component statusMessage(SoulFurnaceBlockEntity furnace) {
		String hint = switch (furnace.status()) {
			case NO_SOUL_GROUND -> "block.sanupcraft.soul_furnace.hint.no_soul";
			case NO_FUEL -> "block.sanupcraft.soul_furnace.hint.no_fuel";
			case FULL -> "block.sanupcraft.soul_furnace.hint.full";
			case RUNNING -> "block.sanupcraft.soul_furnace.hint.ok";
		};
		return Component.translatable("block.sanupcraft.soul_furnace.status", furnace.steam(), SoulFurnaceBlockEntity.STEAM_CAPACITY, furnace.fuelCount())
				.append(" · ").append(Component.translatable(hint));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
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
		builder.add(FACING, LIT);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		double x = pos.getX() + 0.5;
		double z = pos.getZ() + 0.5;
		level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + random.nextDouble() * 0.4 - 0.2, pos.getY() + 1.02, z + random.nextDouble() * 0.4 - 0.2, 0.0, 0.03, 0.0);
		level.addParticle(ParticleTypes.CLOUD, x, pos.getY() + 1.05, z, 0.0, 0.12, 0.0);
		if (random.nextDouble() < 0.08) {
			level.playLocalSound(x, pos.getY() + 0.5, z, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 0.5F, 0.8F, false);
		}
	}
}
