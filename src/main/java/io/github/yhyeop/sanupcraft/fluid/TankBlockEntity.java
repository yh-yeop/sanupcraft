package io.github.yhyeop.sanupcraft.fluid;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.power.SteamGaugeBlockEntity;

/**
 * 탱크: 한 가지 유체를 16양동이(16000 mB)까지. 스스로는 아무것도 하지 않는다 — 펌프가 채우고, 양동이로 넣고 빼고,
 * 옆의 보일러가 물을 끌어가고, 옆의 지열 분출구는 용암 1양동이 이상이면 용암 원천 하나로 센다 (용암은 줄지 않는다).
 * 부수면 내용이 아이템 컴포넌트 {@code sanupcraft:fluid}로 남는다 (다시 놓으면 그대로).
 */
public class TankBlockEntity extends BlockEntity implements FluidHolder {
	public static final int CAPACITY = 16 * FluidKind.BUCKET;

	private final FluidTank tank = new FluidTank(CAPACITY, this::setChanged);

	public TankBlockEntity(BlockPos pos, BlockState state) {
		super(ModFluid.TANK_BLOCK_ENTITY, pos, state);
	}

	@Override
	public @Nullable FluidKind fluid() {
		return tank.fluid();
	}

	@Override
	public int fluidAmount() {
		return tank.fluidAmount();
	}

	@Override
	public int fluidCapacity() {
		return CAPACITY;
	}

	@Override
	public int fill(FluidKind kind, int amount, boolean simulate) {
		return tank.fill(kind, amount, simulate);
	}

	@Override
	public int drain(FluidKind kind, int amount, boolean simulate) {
		return tank.drain(kind, amount, simulate);
	}

	/** 내용을 바로 정한다 (시험 · 명령). */
	public void set(@Nullable FluidKind kind, int amount) {
		tank.set(kind, amount);
		setChanged();
	}

	/** 비교기 신호 0~15 (계기판과 같은 계산: 비면 0, 조금이라도 있으면 1 이상, 가득이면 15). */
	public int signal() {
		return SteamGaugeBlockEntity.signalFor(tank.fluidAmount(), CAPACITY);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		tank.load(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		tank.save(output);
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		FluidContents contents = components.get(ModFluid.FLUID);
		if (contents != null) {
			tank.set(contents.kind(), contents.amount());
		}
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		FluidKind kind = tank.fluid();
		if (kind != null) {
			components.set(ModFluid.FLUID, new FluidContents(kind, tank.fluidAmount()));
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void removeComponentsFromTag(ValueOutput output) {
		super.removeComponentsFromTag(output);
		output.discard("fluid");
		output.discard("amount");
	}
}
