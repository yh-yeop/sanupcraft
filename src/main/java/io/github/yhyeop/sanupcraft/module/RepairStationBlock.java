package io.github.yhyeop.sanupcraft.module;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 증기 수리대 (G4). 증기 파이프를 잇고, 닳은 장비를 들고 우클릭해 올린 뒤 수리 재료를 들고 우클릭해 넣는다 (한 묶음째).
 * 빈손 우클릭: 장비를 꺼낸다 (장비가 없으면 남은 재료). 웅크리고 빈손 우클릭: 상태만.
 */
public class RepairStationBlock extends BaseEntityBlock {
	public RepairStationBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RepairStationBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModModules.REPAIR_STATION_BLOCK_ENTITY,
						(l, pos, s, station) -> RepairStationBlockEntity.serverTick(serverLevel, pos, s, station))
				: null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof RepairStationBlockEntity station)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		boolean gear = RepairStationBlockEntity.accepts(stack) && station.gear().isEmpty();
		boolean material = !station.gear().isEmpty() && station.gear().isValidRepairItem(stack);
		if (!gear && !material) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			if (gear) {
				station.setItem(RepairStationBlockEntity.SLOT_GEAR, stack.copyWithCount(1));
				if (!player.hasInfiniteMaterials()) {
					stack.shrink(1);
				}
			} else {
				ItemStack have = station.material();
				int room = have.isEmpty() ? stack.getMaxStackSize()
						: ItemStack.isSameItemSameComponents(have, stack) ? have.getMaxStackSize() - have.getCount() : 0;
				int moved = Math.min(room, stack.getCount());
				if (moved <= 0) {
					player.sendOverlayMessage(station.status());
					return InteractionResult.SUCCESS;
				}
				if (have.isEmpty()) {
					station.setItem(RepairStationBlockEntity.SLOT_MATERIAL, stack.copyWithCount(moved));
				} else {
					have.grow(moved);
					station.setChanged();
				}
				if (!player.hasInfiniteMaterials()) {
					stack.shrink(moved);
				}
			}
			level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
			player.sendOverlayMessage(station.status());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RepairStationBlockEntity station) {
			if (!player.isShiftKeyDown()) {
				int slot = !station.gear().isEmpty() ? RepairStationBlockEntity.SLOT_GEAR : RepairStationBlockEntity.SLOT_MATERIAL;
				ItemStack taken = station.removeItemNoUpdate(slot);
				if (!taken.isEmpty()) {
					station.setChanged();
					if (!player.getInventory().add(taken)) {
						player.spawnAtLocation((ServerLevel) level, taken);
					}
					level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.2F);
				}
			}
			player.sendOverlayMessage(station.status());
		}
		return InteractionResult.SUCCESS;
	}
}
