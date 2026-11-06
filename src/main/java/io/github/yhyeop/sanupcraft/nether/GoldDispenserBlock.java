package io.github.yhyeop.sanupcraft.nether;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 금 분배기 (M5). 금괴를 들고 우클릭하면 넣고, 빈손이면 상태, 웅크리고 빈손이면 금괴를 꺼낸다. */
public class GoldDispenserBlock extends BaseEntityBlock {
	public GoldDispenserBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GoldDispenserBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModNether.GOLD_DISPENSER_BLOCK_ENTITY, (l, pos, s, be) -> GoldDispenserBlockEntity.serverTick(serverLevel, pos, s, be))
				: null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.is(Items.GOLD_INGOT) || !(level.getBlockEntity(pos) instanceof GoldDispenserBlockEntity dispenser)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			ItemStack current = dispenser.getItem(0);
			if (current.isEmpty()) {
				dispenser.setItem(0, stack.split(stack.getCount()));
			} else {
				int move = Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount());
				current.grow(move);
				stack.shrink(move);
				dispenser.setChanged();
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GoldDispenserBlockEntity dispenser) {
			if (player.isSecondaryUseActive()) {
				ItemStack gold = dispenser.removeItemNoUpdate(0);
				dispenser.setChanged();
				if (!gold.isEmpty() && !player.getInventory().add(gold)) {
					Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), gold);
				}
			} else {
				int piglins = GoldDispenserBlockEntity.targets((ServerLevel) level, pos).size();
				player.sendOverlayMessage(Component.translatable("block.sanupcraft.gold_dispenser.status", dispenser.goldCount(), piglins));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}
}
