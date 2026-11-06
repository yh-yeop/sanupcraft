package io.github.yhyeop.sanupcraft.gear;

import java.util.function.ToIntFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 내구도 대신 증기를 쓰는 공구 (증기 드릴 · 전기톱). 블록 하나를 캘 때마다 {@code cost(블록)}만큼 자기 증기를 뺀다 (창작 모드는 공짜).
 * 증기가 한 블록치보다 모자라면 맨손과 같다: 캐는 속도 1배, 도구가 필요한 블록은 드롭도 없다.
 * 바닐라 {@code Tool} 컴포넌트가 규칙(어느 블록을 캐나)을 정하고, 여기서는 증기 계산만 맡는다.
 */
public class SteamToolItem extends SteamChargedItem {
	private final ToIntFunction<BlockState> cost;

	public SteamToolItem(Properties properties, int capacity, ToIntFunction<BlockState> cost) {
		super(properties, capacity);
		this.cost = cost;
	}

	/** 이 블록 하나를 캐는 데 드는 증기. */
	public int steamCost(BlockState state) {
		return Math.max(0, cost.applyAsInt(state));
	}

	/** 증기가 이 블록을 캘 만큼 있나. */
	public boolean canMine(ItemStack stack, BlockState state) {
		return SteamCharge.get(stack) >= steamCost(state);
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return canMine(stack, state) ? super.getDestroySpeed(stack, state) : 1.0F;
	}

	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		return canMine(stack, state) && super.isCorrectToolForDrops(stack, state);
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
		if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F) {
			SteamCharge.take(stack, steamCost(state));
		}
		return true;
	}
}
