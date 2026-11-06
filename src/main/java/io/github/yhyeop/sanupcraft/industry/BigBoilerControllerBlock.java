package io.github.yhyeop.sanupcraft.industry;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import io.github.yhyeop.sanupcraft.power.BoilerBlock;
import io.github.yhyeop.sanupcraft.power.BoilerBlockEntity;

/**
 * 대형 보일러 제어기. 보일러 블록을 물려받아 물 양동이 · 연료 우클릭, 파이프 연결은 그대로 쓴다.
 * 빈손 우클릭: 모양을 바로 확인하고 상태 또는 틀린 칸을 알려 준다. 비교기: 증기 저장률.
 */
public class BigBoilerControllerBlock extends BoilerBlock {
	public BigBoilerControllerBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BigBoilerControllerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModIndustry.BIG_BOILER_CONTROLLER_BLOCK_ENTITY,
						(l, pos, s, controller) -> BoilerBlockEntity.serverTick(serverLevel, pos, s, controller))
				: null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof BigBoilerControllerBlockEntity controller) {
			player.sendOverlayMessage(status(serverLevel, pos, state, controller));
		}
		return InteractionResult.SUCCESS;
	}

	/** 상태 한 줄: 모양이 맞으면 물 · 증기 · 연료 + 힌트, 틀리면 첫 틀린 칸. */
	static Component status(ServerLevel level, BlockPos pos, BlockState state, BigBoilerControllerBlockEntity controller) {
		if (!controller.check(level, pos, state)) {
			MultiblockShape.Problem problem = controller.problem();
			BlockPos at = problem.pos();
			String key = problem.expected() == MultiblockShape.Part.CASING
					? "block.sanupcraft.big_boiler_controller.need_casing"
					: "block.sanupcraft.big_boiler_controller.need_hollow";
			return Component.translatable(key, at.getX(), at.getY(), at.getZ(), level.getBlockState(at).getBlock().getName());
		}
		return Component.translatable("block.sanupcraft.big_boiler_controller.status",
				controller.water(), controller.waterCapacity(), controller.steam(), controller.steamCapacity(), controller.fuelCount())
				.append(" · ").append(Component.translatable(hint(controller)));
	}

	/** 보일러와 같은 힌트 키: 가장 먼저 해결할 것 하나만. */
	private static String hint(BigBoilerControllerBlockEntity controller) {
		if (controller.water() <= 0) {
			return "block.sanupcraft.boiler.hint.no_water";
		}
		if (controller.steam() >= controller.steamCapacity()) {
			return "block.sanupcraft.boiler.hint.steam_full";
		}
		if (!controller.isLit() && controller.fuelCount() <= 0) {
			return "block.sanupcraft.boiler.hint.no_fuel";
		}
		return "block.sanupcraft.boiler.hint.ok";
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
		if (level.getBlockEntity(pos) instanceof BigBoilerControllerBlockEntity controller) {
			controller.requestCheck();
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		BigBoilerControllerBlockEntity.setCasingsFormed(level, pos, state.getValue(FACING), false);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		if (!(level.getBlockEntity(pos) instanceof BigBoilerControllerBlockEntity controller) || controller.steam() <= 0) {
			return 0;
		}
		return 1 + controller.steam() * 14 / controller.steamCapacity();
	}

	/** 불이 켜지면 모양 꼭대기 가운데에서 김이 오른다. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		BlockPos top = MultiblockShape.at(pos, state.getValue(FACING), 0, 1, BigBoilerControllerBlockEntity.SHAPE.height() - 1);
		double x = top.getX() + 0.5;
		double z = top.getZ() + 0.5;
		for (int i = 0; i < 2; i++) {
			level.addParticle(ParticleTypes.CLOUD, x + random.nextDouble() * 0.6 - 0.3, top.getY() + 1.05, z + random.nextDouble() * 0.6 - 0.3, 0.0, 0.12, 0.0);
		}
		if (random.nextDouble() < 0.08) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.9F, 0.7F, false);
		}
	}
}
