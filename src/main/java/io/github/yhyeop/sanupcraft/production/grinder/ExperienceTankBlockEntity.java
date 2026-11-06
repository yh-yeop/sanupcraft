package io.github.yhyeop.sanupcraft.production.grinder;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.fluid.FluidContents;
import io.github.yhyeop.sanupcraft.fluid.FluidHolder;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;
import io.github.yhyeop.sanupcraft.fluid.FluidTank;
import io.github.yhyeop.sanupcraft.fluid.ModFluid;
import io.github.yhyeop.sanupcraft.power.SteamGaugeBlockEntity;

/**
 * 경험치 탱크: 경험치(액체 {@link FluidKind#EXPERIENCE}, 1점 = 20 mB)만 5000점(100000 mB)까지 담는다.
 * 몹 분쇄기가 옆에서 채우고, 우클릭으로 플레이어에게 준다. 부수면 내용이 아이템 컴포넌트 {@code sanupcraft:fluid}로 남는다.
 */
public class ExperienceTankBlockEntity extends BlockEntity implements FluidHolder {
	public static final int CAPACITY_POINTS = 5000;
	public static final int CAPACITY = CAPACITY_POINTS * FluidKind.MB_PER_XP;

	private final FluidTank tank = new FluidTank(CAPACITY, this::setChanged);

	public ExperienceTankBlockEntity(BlockPos pos, BlockState state) {
		super(ModGrinder.EXPERIENCE_TANK_BLOCK_ENTITY, pos, state);
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

	/** 경험치 말고는 받지 않는다. */
	@Override
	public int fill(FluidKind kind, int amount, boolean simulate) {
		return kind == FluidKind.EXPERIENCE ? tank.fill(kind, amount, simulate) : 0;
	}

	@Override
	public int drain(FluidKind kind, int amount, boolean simulate) {
		return kind == FluidKind.EXPERIENCE ? tank.drain(kind, amount, simulate) : 0;
	}

	/** 든 경험치 점수. */
	public int points() {
		return tank.fluidAmount() / FluidKind.MB_PER_XP;
	}

	/** 경험치를 최대 {@code points}점 넣고 실제로 들어간 점수를 돌려준다. */
	public int addPoints(int points) {
		return tank.fill(FluidKind.EXPERIENCE, points * FluidKind.MB_PER_XP, false) / FluidKind.MB_PER_XP;
	}

	/** 경험치를 최대 {@code points}점 빼고 실제로 빠진 점수를 돌려준다. */
	public int takePoints(int points) {
		return tank.drain(FluidKind.EXPERIENCE, points * FluidKind.MB_PER_XP, false) / FluidKind.MB_PER_XP;
	}

	/** 내용을 점수로 바로 정한다 (시험 · 명령). */
	public void setPoints(int points) {
		tank.set(points > 0 ? FluidKind.EXPERIENCE : null, points * FluidKind.MB_PER_XP);
		setChanged();
	}

	/** 비교기 신호 0~15 (탱크와 같은 계산). */
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
		if (contents != null && contents.kind() == FluidKind.EXPERIENCE) {
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
