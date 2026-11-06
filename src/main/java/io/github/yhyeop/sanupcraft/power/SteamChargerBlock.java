package io.github.yhyeop.sanupcraft.power;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

import io.github.yhyeop.sanupcraft.gear.SteamCharge;

/**
 * 증기 충전대. 증기 파이프를 이어 두고, 증기 통 · 증기 부츠를 들고 우클릭해 올려 놓으면 가득 찰 때까지 채운다.
 * 빈손 우클릭: 올려 둔 것을 꺼낸다 (비었으면 상태). 웅크리고 빈손 우클릭: 항상 상태. 깔때기: 빈 칸에 넣기, 가득 차면 뽑기.
 */
public class SteamChargerBlock extends BaseEntityBlock {
	public SteamChargerBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SteamChargerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModPower.STEAM_CHARGER_BLOCK_ENTITY,
						(l, pos, s, charger) -> SteamChargerBlockEntity.serverTick(serverLevel, pos, s, charger))
				: null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!SteamCharge.holdsSteam(stack) || !(level.getBlockEntity(pos) instanceof SteamChargerBlockEntity charger)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!charger.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			charger.setItem(0, stack.copyWithCount(1));
			if (!player.hasInfiniteMaterials()) {
				stack.shrink(1);
			}
			level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 1.2F);
			player.sendOverlayMessage(status(charger));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SteamChargerBlockEntity charger) {
			if (!charger.isEmpty() && !player.isShiftKeyDown()) {
				ItemStack taken = charger.removeItemNoUpdate(0);
				charger.setChanged();
				if (!player.getInventory().add(taken)) {
					player.spawnAtLocation((ServerLevel) level, taken);
				}
				level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.2F);
			}
			player.sendOverlayMessage(status(charger));
		}
		return InteractionResult.SUCCESS;
	}

	private static Component status(SteamChargerBlockEntity charger) {
		ItemStack held = charger.stored();
		if (held.isEmpty()) {
			return Component.translatable("block.sanupcraft.steam_charger.status_empty", charger.steamStored(), charger.steamCapacity());
		}
		return Component.translatable("block.sanupcraft.steam_charger.status", held.getHoverName(),
				SteamCharge.get(held), SteamCharge.capacity(held), charger.steamStored(), charger.steamCapacity());
	}
}
