package io.github.yhyeop.sanupcraft.industry;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.power.BoilerBlock;
import io.github.yhyeop.sanupcraft.power.BoilerBlockEntity;
import io.github.yhyeop.sanupcraft.power.SteamOutlet;

/**
 * 대형 보일러 제어기. 보일러의 물 · 연료 · 증기 처리를 그대로 물려받고, 모양({@link BigBoilerShape})이 맞을 때만 돈다.
 * 보일러 6대 몫 증기를 연료 5대 몫으로 만든다 (증기를 만든 틱마다 연소 틱이 5씩 준다). 연료 칸 4개, 물 · 증기 칸이 6배.
 * 모양은 40틱마다, 그리고 외벽이 부서지거나 제어기 옆이 바뀌면 바로 다시 확인한다.
 * 모양이 맞으면 증기를 제어기와 모든 외벽에 붙은 파이프 · 기계로 내보낸다.
 */
public class BigBoilerControllerBlockEntity extends BoilerBlockEntity {
	public static final int BOILERS = 6;
	public static final int STEAM_PER_TICK = BoilerBlockEntity.STEAM_PER_TICK * BOILERS;
	public static final int WATER_CAPACITY = BoilerBlockEntity.WATER_CAPACITY * BOILERS;
	public static final int STEAM_CAPACITY = BoilerBlockEntity.STEAM_CAPACITY * BOILERS;
	public static final int FUEL_SLOTS = 4;
	/** 증기를 만든 틱마다 줄어드는 연소 틱 (보일러는 1). 6대 몫을 5대 몫 연료로. */
	public static final int BURN_PER_TICK = 5;
	public static final int CHECK_INTERVAL = 40;

	private boolean formed;
	private BigBoilerShape.@Nullable Problem problem;
	/** 0이 되면 모양을 확인한다. 처음 틱에 바로 확인하려고 0에서 시작 (저장하지 않음). */
	private int checkTimer;
	private List<BlockPos> outletStarts = List.of();
	private final SteamOutlet structureOutlet = new SteamOutlet();

	public BigBoilerControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModIndustry.BIG_BOILER_CONTROLLER_BLOCK_ENTITY, pos, state, FUEL_SLOTS);
	}

	@Override
	protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
		if (--checkTimer <= 0) {
			check(level, pos, state);
		}
		super.tick(level, pos, state);
	}

	/** 다음 틱에 모양을 다시 확인한다 (외벽이 부서짐 · 제어기 옆이 바뀜). */
	public void requestCheck() {
		checkTimer = 0;
	}

	/** 모양을 지금 확인하고 외벽 무늬를 맞춘다. 맞으면 true. */
	public boolean check(ServerLevel level, BlockPos pos, BlockState state) {
		checkTimer = CHECK_INTERVAL;
		Direction facing = state.getValue(BoilerBlock.FACING);
		problem = BigBoilerShape.check(level, pos, facing);
		boolean now = problem == null;
		boolean changed = now != formed;
		if (changed) {
			formed = now;
			outletStarts = now ? BigBoilerShape.positions(pos, facing) : List.of();
			setChanged();
		}
		// 맞았을 때는 매번 (다른 제어기가 풀어 놓은 외벽도) 맞추고, 풀렸을 때는 풀린 그 순간에만 끈다
		if (now || changed) {
			setCasingsFormed(level, pos, facing, now);
		}
		return formed;
	}

	/** 외벽 무늬를 켜거나 끈다. 이웃 갱신 없이 (파이프 · 제어기를 다시 건드리지 않게). */
	static void setCasingsFormed(ServerLevel level, BlockPos controller, Direction facing, boolean formed) {
		for (BlockPos casing : BigBoilerShape.casings(controller, facing)) {
			BlockState state = level.getBlockState(casing);
			if (state.getBlock() instanceof BigBoilerCasingBlock && state.getValue(BigBoilerCasingBlock.FORMED) != formed) {
				level.setBlock(casing, state.setValue(BigBoilerCasingBlock.FORMED, formed), Block.UPDATE_CLIENTS);
			}
		}
	}

	public boolean isFormed() {
		return formed;
	}

	/** 마지막 확인에서 찾은 틀린 칸. 모양이 맞으면 null. */
	public BigBoilerShape.@Nullable Problem problem() {
		return problem;
	}

	@Override
	protected boolean canRun() {
		return formed;
	}

	@Override
	protected int steamPerTick() {
		return SanupcraftConfig.current().bigBoilerSteamPerTick;
	}

	@Override
	protected int burnPerTick() {
		return BURN_PER_TICK;
	}

	@Override
	public int waterCapacity() {
		return WATER_CAPACITY;
	}

	@Override
	public int steamCapacity() {
		return STEAM_CAPACITY;
	}

	@Override
	protected int distributeSteam(ServerLevel level, BlockPos pos, int steam) {
		return formed ? structureOutlet.distribute(level, outletStarts, steam) : super.distributeSteam(level, pos, steam);
	}
}
