package io.github.yhyeop.sanupcraft.mining.blast;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 발파 장약. 레드스톤 신호나 원격 기폭기를 받으면 심지가 타고(기본 20틱, 쉭쉭 소리와 연기) 5x5x5를 캔다 ({@link BlastCharges#detonate}).
 * 방향은 상관없다. 심지가 타는 동안은 {@code lit}.
 */
public class BlastChargeBlock extends Block {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public BlastChargeBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this) && !level.isClientSide() && level.hasNeighborSignal(pos)) {
			ignite(level, pos, SanupcraftConfig.current().blastFuseTicks);
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
		if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
			ignite(level, pos, SanupcraftConfig.current().blastFuseTicks);
		}
	}

	/** 심지에 불을 붙인다. 이미 타고 있거나 장약이 아니면 false. */
	public static boolean ignite(Level level, BlockPos pos, int fuseTicks) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(ModBlast.BLAST_CHARGE) || state.getValue(LIT) || !(level instanceof ServerLevel serverLevel)) {
			return false;
		}
		serverLevel.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
		serverLevel.scheduleTick(pos, ModBlast.BLAST_CHARGE, Math.max(1, fuseTicks));
		serverLevel.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
		return true;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIT)) {
			BlastCharges.detonate(level, pos);
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIT)) {
			double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
			double y = pos.getY() + 1.0;
			double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
			level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.05, 0.0);
			level.addParticle(ParticleTypes.SMALL_FLAME, x, y - 0.1, z, 0.0, 0.0, 0.0);
		}
	}
}
