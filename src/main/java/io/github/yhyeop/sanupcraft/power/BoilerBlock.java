package io.github.yhyeop.sanupcraft.power;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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

/**
 * 보일러. 연료를 들고 우클릭하면 넣고, 물 양동이로 물을 붓고, 빈손이면 상태(물 · 증기 · 연료)를 보여 준다.
 */
public class BoilerBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public BoilerBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BoilerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModPower.BOILER_BLOCK_ENTITY, (l, pos, s, boiler) -> BoilerBlockEntity.serverTick(serverLevel, pos, s, boiler))
				: null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof BoilerBlockEntity boiler)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (stack.is(Items.WATER_BUCKET)) {
			if (!level.isClientSide()) {
				if (!boiler.pourBucket()) {
					player.sendOverlayMessage(Component.translatable("block.sanupcraft.boiler.water_full"));
					return InteractionResult.FAIL;
				}
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
				level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.has(DataComponents.COOKING_FUEL)) {
			if (!level.isClientSide() && !boiler.insertFuel(stack)) {
				player.sendOverlayMessage(Component.translatable("block.sanupcraft.boiler.other_fuel"));
				return InteractionResult.FAIL;
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BoilerBlockEntity boiler) {
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.boiler.status",
					boiler.water(), BoilerBlockEntity.WATER_CAPACITY, boiler.steam(), BoilerBlockEntity.STEAM_CAPACITY, boiler.fuelCount())
					.append(" · ").append(Component.translatable(hint(boiler))));
		}
		return InteractionResult.SUCCESS;
	}

	/** 상태 줄 끝에 붙일 힌트 키: 가장 먼저 해결할 것 하나만 알려 준다. */
	private static String hint(BoilerBlockEntity boiler) {
		if (boiler.water() <= 0) {
			return "block.sanupcraft.boiler.hint.no_water";
		}
		// 증기가 가득이면 연료를 아끼려고 불이 잠시 멎는다 (isLit 거짓) — 연료 없음보다 먼저 본다
		if (boiler.steam() >= BoilerBlockEntity.STEAM_CAPACITY) {
			return "block.sanupcraft.boiler.hint.steam_full";
		}
		if (!boiler.isLit() && boiler.fuelCount() <= 0) {
			return "block.sanupcraft.boiler.hint.no_fuel";
		}
		return "block.sanupcraft.boiler.hint.ok";
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
		level.addParticle(ParticleTypes.CLOUD, x + random.nextDouble() * 0.3 - 0.15, pos.getY() + 1.05, z + random.nextDouble() * 0.3 - 0.15, 0.0, 0.08, 0.0);
		if (random.nextFloat() < 0.1F) {
			// M11 X2: 가끔 옆면 틈으로 김이 픽 샌다
			Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			for (int i = 0; i < 4; i++) {
				level.addParticle(ParticleTypes.WHITE_SMOKE, x + side.getStepX() * 0.55, pos.getY() + 0.6 + random.nextDouble() * 0.2, z + side.getStepZ() * 0.55,
						side.getStepX() * 0.08, 0.03, side.getStepZ() * 0.08);
			}
		}
		if (random.nextDouble() < 0.08) {
			level.playLocalSound(x, pos.getY() + 0.5, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.7F, 0.9F, false);
		}
	}
}
