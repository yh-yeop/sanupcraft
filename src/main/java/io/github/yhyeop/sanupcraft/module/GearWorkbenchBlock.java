package io.github.yhyeop.sanupcraft.module;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 장비 작업대 (G3). 우클릭하면 장비에 모듈을 끼우고 빼는 화면이 열린다. 블록에는 아무것도 남지 않는다 (제작대처럼). */
public class GearWorkbenchBlock extends Block {
	private static final Component TITLE = Component.translatable("container.sanupcraft.gear_workbench");

	public GearWorkbenchBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			player.openMenu(state.getMenuProvider(level, pos));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider((containerId, inventory, player) -> new GearWorkbenchMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
				TITLE);
	}
}
