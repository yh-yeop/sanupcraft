package io.github.yhyeop.sanupcraft.industry;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.power.BoilerBlock;
import io.github.yhyeop.sanupcraft.power.BoilerBlockEntity;
import io.github.yhyeop.sanupcraft.power.SteamOutlet;

/**
 * 대형 보일러 제어기. 보일러의 물 · 연료 · 증기 처리를 그대로 물려받고, 모양({@link #SHAPE})이 맞을 때만 돈다.
 * 보일러 6대 몫 증기를 연료 5대 몫으로 만든다 (증기를 만든 틱마다 연소 틱이 5씩 준다). 연료 칸 4개, 물 · 증기 칸이 6배.
 * 모양은 40틱마다, 그리고 외벽이 부서지거나 제어기 옆이 바뀌면 바로 다시 확인한다.
 * 모양이 맞으면 증기를 제어기와 모든 외벽에 붙은 파이프 · 기계로 내보낸다.
 */
public class BigBoilerControllerBlockEntity extends BoilerBlockEntity implements MultiblockShape.Controller {
	public static final int BOILERS = 6;
	public static final int STEAM_PER_TICK = BoilerBlockEntity.STEAM_PER_TICK * BOILERS;
	public static final int WATER_CAPACITY = BoilerBlockEntity.WATER_CAPACITY * BOILERS;
	public static final int STEAM_CAPACITY = BoilerBlockEntity.STEAM_CAPACITY * BOILERS;
	public static final int FUEL_SLOTS = 4;
	/** 증기를 만든 틱마다 줄어드는 연소 틱 (보일러는 1). 6대 몫을 5대 몫 연료로. */
	public static final int BURN_PER_TICK = 5;
	public static final int CHECK_INTERVAL = 40;

	/**
	 * 3x3 바닥 · 높이 4. 제어기는 앞면 가운데 맨 아랫줄. 가운데 기둥의 2 · 3층(제어기 기준 위로 1 · 2칸, 뒤로 1칸)은 비워 두고,
	 * 나머지 33칸은 전부 대형 보일러 외벽.
	 */
	public static final MultiblockShape SHAPE = new MultiblockShape(3, 4, (side, depth, up) -> {
		if (side == 0 && depth == 0 && up == 0) {
			return MultiblockShape.Part.CONTROLLER;
		}
		return side == 0 && depth == 1 && (up == 1 || up == 2) ? MultiblockShape.Part.HOLLOW : MultiblockShape.Part.CASING;
	}, state -> state.getBlock() instanceof BigBoilerCasingBlock, BigBoilerCasingBlock.FORMED);

	private boolean formed;
	private MultiblockShape.@Nullable Problem problem;
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

	@Override
	public MultiblockShape shape() {
		return SHAPE;
	}

	@Override
	public Direction facing() {
		return getBlockState().getValue(BoilerBlock.FACING);
	}

	/** 다음 틱에 모양을 다시 확인한다 (외벽이 부서짐 · 제어기 옆이 바뀜). */
	@Override
	public void requestCheck() {
		checkTimer = 0;
	}

	/** 모양을 지금 확인하고 외벽 무늬를 맞춘다. 맞으면 true. */
	public boolean check(ServerLevel level, BlockPos pos, BlockState state) {
		checkTimer = CHECK_INTERVAL;
		Direction facing = state.getValue(BoilerBlock.FACING);
		problem = SHAPE.check(level, pos, facing);
		boolean now = problem == null;
		boolean changed = now != formed;
		if (changed) {
			formed = now;
			outletStarts = now ? SHAPE.positions(pos, facing) : List.of();
			setChanged();
		}
		// 맞으면 매번 (이웃 보일러가 끈 공유 외벽도) 켠다. 풀린 순간에는 이웃 보일러가 쓰는 외벽만 남기고 끈다
		if (now) {
			SHAPE.form(level, pos, facing);
		} else if (changed) {
			SHAPE.unform(level, pos, facing);
		}
		return formed;
	}

	@Override
	public boolean isFormed() {
		return formed;
	}

	/** 마지막 확인에서 찾은 틀린 칸. 모양이 맞으면 null. */
	public MultiblockShape.@Nullable Problem problem() {
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
