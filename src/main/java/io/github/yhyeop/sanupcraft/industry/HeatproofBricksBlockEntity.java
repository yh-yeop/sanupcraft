package io.github.yhyeop.sanupcraft.industry;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.github.yhyeop.sanupcraft.power.HPSteamConsumer;

/**
 * 내열 벽돌의 고압 증기 받이. 모양이 선 용광로 탑 제어기가 자기 위치를 알려 주면 받은 고압 증기를 그대로 넘긴다.
 * 고압 파이프는 늘 붙는다 (모양이 서기 전에도 이어진 모습). 제어기 위치는 저장하지 않고, 제어기가 40틱마다 다시 알려 준다.
 */
public class HeatproofBricksBlockEntity extends BlockEntity implements HPSteamConsumer, MultiblockShape.LinkedCasing {
	private @Nullable BlockPos controller;

	public HeatproofBricksBlockEntity(BlockPos pos, BlockState state) {
		super(ModFoundry.HEATPROOF_BRICKS_BLOCK_ENTITY, pos, state);
	}

	/** 이 벽돌을 쓰는 제어기 (모양이 풀리면 null). 두 탑이 같이 쓰는 벽돌은 마지막으로 확인한 쪽. */
	@Override
	public void link(@Nullable BlockPos controller) {
		this.controller = controller;
	}

	/** 이 벽돌이 고압 증기를 넘길 제어기 위치. */
	@Override
	public @Nullable BlockPos linkedController() {
		return controller;
	}

	private @Nullable FurnaceTowerControllerBlockEntity controller() {
		if (controller == null || level == null || !level.isLoaded(controller)) {
			return null;
		}
		return level.getBlockEntity(controller) instanceof FurnaceTowerControllerBlockEntity tower && tower.isFormed() ? tower : null;
	}

	@Override
	public int acceptHighPressureSteam(int amount) {
		FurnaceTowerControllerBlockEntity tower = controller();
		return tower == null ? 0 : tower.acceptHighPressureSteam(amount);
	}
}
