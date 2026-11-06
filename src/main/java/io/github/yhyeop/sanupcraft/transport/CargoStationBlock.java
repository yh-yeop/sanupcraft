package io.github.yhyeop.sanupcraft.transport;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 적재 · 하역 역 (레일 옆에 둔다). 우클릭: 적재 ↔ 하역 바꾸기 + 상태. 웅크리고 빈손 우클릭: 9칸 창고 열기.
 * 비교기: 창고가 찬 정도.
 */
public class CargoStationBlock extends BaseEntityBlock {
	/** true = 하역 (화물칸 → 역), false = 적재 (역 → 화물칸). */
	public static final BooleanProperty UNLOAD = BooleanProperty.create("unload");

	public CargoStationBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(UNLOAD, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CargoStationBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModTransport.CARGO_STATION_BLOCK_ENTITY, (l, pos, s, station) -> CargoStationBlockEntity.serverTick(serverLevel, pos, s, station))
				: null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CargoStationBlockEntity station)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) {
			player.openMenu(station);
			return InteractionResult.SUCCESS;
		}
		BlockState next = state.cycle(UNLOAD);
		level.setBlockAndUpdate(pos, next);
		level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.6F, next.getValue(UNLOAD) ? 0.6F : 0.5F);
		player.sendOverlayMessage(status(next, station));
		return InteractionResult.SUCCESS;
	}

	static Component status(BlockState state, CargoStationBlockEntity station) {
		String mode = state.getValue(UNLOAD) ? "unload" : "load";
		return Component.translatable("block.sanupcraft.cargo_station.status",
				Component.translatable("block.sanupcraft.cargo_station.mode." + mode), station.itemCount());
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
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(UNLOAD);
	}
}
