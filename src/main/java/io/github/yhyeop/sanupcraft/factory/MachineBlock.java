package io.github.yhyeop.sanupcraft.factory;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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

import io.github.yhyeop.sanupcraft.fluid.FluidBuckets;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;

/** 화로처럼 재료 · 연료 · 결과 칸이 있는 기계. 종류만 다르고 동작은 같다. */
public class MachineBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	private final MachineKind kind;

	public MachineBlock(MachineKind kind, BlockBehaviour.Properties properties) {
		super(properties);
		this.kind = kind;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	public MachineKind kind() {
		return kind;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MachineBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModMachines.MACHINE_BLOCK_ENTITY, (l, pos, s, machine) -> MachineBlockEntity.serverTick(serverLevel, pos, s, machine))
				: null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
			player.openMenu(machine);
		}
		return InteractionResult.SUCCESS;
	}

	/** 세척기 (M8): 물 양동이를 들고 우클릭하면 물을 붓는다. 다른 것은 화면 열기로. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (kind.usesWater() && FluidKind.ofBucket(stack) == FluidKind.WATER && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
			return FluidBuckets.use(machine, true, stack, level, pos, player, hand);
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
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
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof MachineBlockEntity machine ? machine.comparatorSignal() : 0;
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
		double y = pos.getY() + 1.0;
		double z = pos.getZ() + 0.5;
		if (random.nextDouble() < 0.15) {
			level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 0.8F, false);
		}
		level.addParticle(ParticleTypes.SMOKE, x + random.nextDouble() * 0.4 - 0.2, y, z + random.nextDouble() * 0.4 - 0.2, 0.0, 0.05, 0.0);
		kindParticles(state, level, pos, random);
	}

	/** M11 X2: 종류마다 덧붙이는 입자. 정밀 조립기 = 앞면 불꽃 튐, 세척기 = 위로 물방울, 고압 정련기 = 김. */
	private void kindParticles(BlockState state, Level level, BlockPos pos, RandomSource random) {
		Direction front = state.getValue(FACING);
		switch (kind) {
			case PRECISION -> {
				if (random.nextFloat() < 0.6F) {
					double fx = pos.getX() + 0.5 + front.getStepX() * 0.52 + (front.getStepX() == 0 ? random.nextDouble() * 0.6 - 0.3 : 0.0);
					double fz = pos.getZ() + 0.5 + front.getStepZ() * 0.52 + (front.getStepZ() == 0 ? random.nextDouble() * 0.6 - 0.3 : 0.0);
					level.addParticle(ParticleTypes.ELECTRIC_SPARK, fx, pos.getY() + 0.4 + random.nextDouble() * 0.4, fz,
							front.getStepX() * 0.05, 0.02, front.getStepZ() * 0.05);
				}
			}
			case WASHER -> level.addParticle(ParticleTypes.SPLASH, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.02,
					pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.1, 0.0);
			case REFINER -> level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5 + random.nextDouble() * 0.3 - 0.15, pos.getY() + 1.05,
					pos.getZ() + 0.5 + random.nextDouble() * 0.3 - 0.15, 0.0, 0.1, 0.0);
			default -> {
			}
		}
	}
}
