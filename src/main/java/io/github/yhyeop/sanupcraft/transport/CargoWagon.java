package io.github.yhyeop.sanupcraft.transport;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * 화물칸: 27칸 상자 수레. 통이 실려 있어 상자 수레와 구별된다. 기관차 뒤에 연결 고리로 잇는다 (웅크리고 빈손 우클릭으로도).
 * 적재 · 하역 역 옆 레일에 서거나 지나가면 역과 아이템을 주고받는다.
 */
public class CargoWagon extends AbstractMinecartContainer {
	public CargoWagon(EntityType<? extends CargoWagon> type, Level level) {
		super(type, level);
	}

	@Override
	protected Item getDropItem() {
		return ModTransport.CARGO_WAGON_ITEM;
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(ModTransport.CARGO_WAGON_ITEM);
	}

	@Override
	public int getContainerSize() {
		return 27;
	}

	@Override
	public BlockState getDefaultDisplayBlockState() {
		return Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
	}

	@Override
	public int getDefaultDisplayOffset() {
		return 8;
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return ChestMenu.threeRows(containerId, inventory, this);
	}

	@Override
	public void stopOpen(ContainerUser containerUser) {
		level().gameEvent(GameEvent.CONTAINER_CLOSE, position(), GameEvent.Context.of(containerUser.getLivingEntity()));
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		InteractionResult result = interactWithContainerVehicle(player);
		if (result.consumesAction() && player.level() instanceof ServerLevel) {
			gameEvent(GameEvent.CONTAINER_OPEN, player);
		}
		return result;
	}
}
