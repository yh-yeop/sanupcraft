package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import io.github.yhyeop.sanupcraft.industry.CasingLook;
import io.github.yhyeop.sanupcraft.industry.FurnaceTowerControllerBlockEntity;
import io.github.yhyeop.sanupcraft.industry.HeatproofBricksBlock;

/**
 * 확장 M15 R5 다중 블록 이음매: 모양이 서면 외벽마다 구조물 안 위치(cx · cy · cz)가 정해지고, 공유 외벽은 확인할 때마다 바뀌지 않으며,
 * 한쪽이 풀리면 남은 구조물 기준으로 고쳐진다. 탑 짓기는 M8b 시험과 같은 자리 (제어기 (2,1,1) 북향 → 벽돌 x 1..3, y 1..3, z 1..3).
 */
public class SanupcraftA5GameTest {
	private static final BlockPos CONTROLLER = new BlockPos(2, 1, 1);
	/** 이웃 탑 제어기: 동쪽 벽 x 3을 같이 쓴다 (벽돌 x 3..5). */
	private static final BlockPos NEIGHBOUR = new BlockPos(4, 1, 1);
	private static final BlockPos SHARED_BRICK = new BlockPos(3, 2, 2);

	private static String look(GameTestHelper helper, BlockPos pos) {
		BlockState state = helper.getBlockState(pos);
		return "" + state.getValue(CasingLook.X) + state.getValue(CasingLook.Y) + state.getValue(CasingLook.Z);
	}

	private static boolean check(GameTestHelper helper, FurnaceTowerControllerBlockEntity tower, BlockPos controller) {
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(controller);
		return tower.check(level, abs, level.getBlockState(abs));
	}

	/** 모서리 · 가운데 줄 · 윗줄 벽돌의 위치 값, 잘못된 값(예전 세이브)은 다음 확인 때 고쳐진다. */
	@GameTest(maxTicks = 20)
	public void towerCasingLookFollowsPosition(GameTestHelper helper) {
		FurnaceTowerControllerBlockEntity tower = SanupcraftM8bGameTest.buildTowerAt(helper, CONTROLLER);
		helper.assertTrue(check(helper, tower, CONTROLLER), "탑이 서지 않음: " + tower.problem());
		helper.assertValueEqual(look(helper, new BlockPos(1, 1, 1)), "000", "서북 아래 모서리");
		helper.assertValueEqual(look(helper, new BlockPos(3, 3, 3)), "222", "동남 위 모서리");
		helper.assertValueEqual(look(helper, new BlockPos(2, 3, 1)), "120", "앞 윗줄 가운데");
		helper.assertValueEqual(look(helper, new BlockPos(1, 2, 3)), "012", "서쪽 뒤 가운데 줄");
		BlockPos corner = new BlockPos(1, 1, 1);
		helper.setBlock(corner, helper.getBlockState(corner).setValue(CasingLook.X, 2).setValue(CasingLook.Z, 1));
		helper.assertTrue(check(helper, tower, CONTROLLER), "다시 확인했는데 서지 않음");
		helper.assertValueEqual(look(helper, corner), "000", "잘못된 위치 값이 고쳐지지 않음");
		helper.succeed();
	}

	/** 공유 벽돌은 먼저 선 탑(A) 기준을 유지하고 (B · A를 다시 확인해도 그대로), A가 풀리면 B 기준으로 바뀐다. */
	@GameTest(maxTicks = 20)
	public void sharedCasingLookIsStable(GameTestHelper helper) {
		FurnaceTowerControllerBlockEntity a = SanupcraftM8bGameTest.buildTowerAt(helper, CONTROLLER);
		FurnaceTowerControllerBlockEntity b = SanupcraftM8bGameTest.buildTowerAt(helper, NEIGHBOUR);
		helper.assertTrue(check(helper, a, CONTROLLER), "A가 서지 않음: " + a.problem());
		helper.assertTrue(check(helper, b, NEIGHBOUR), "B가 서지 않음: " + b.problem());
		helper.assertValueEqual(look(helper, SHARED_BRICK), "211", "공유 벽돌이 A 기준이 아님");
		check(helper, a, CONTROLLER);
		check(helper, b, NEIGHBOUR);
		helper.assertValueEqual(look(helper, SHARED_BRICK), "211", "다시 확인하자 공유 벽돌 모양이 바뀜");
		helper.destroyBlock(new BlockPos(1, 2, 2));
		helper.runAfterDelay(3, () -> {
			helper.assertFalse(a.isFormed(), "벽돌을 부쉈는데 A가 그대로");
			helper.assertTrue(helper.getBlockState(SHARED_BRICK).getValue(HeatproofBricksBlock.FORMED), "공유 벽돌 무늬가 꺼짐");
			helper.assertValueEqual(look(helper, SHARED_BRICK), "011", "A가 풀린 뒤 공유 벽돌이 B 기준이 아님");
			helper.succeed();
		});
	}
}
