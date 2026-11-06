package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.logistics.EnderAnchorBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.mining.ModMining;
import io.github.yhyeop.sanupcraft.mining.QuarryBlockEntity;
import io.github.yhyeop.sanupcraft.power.GeothermalVentBlockEntity;
import io.github.yhyeop.sanupcraft.power.ModPower;

/**
 * 네더 · 엔드 심화 검사: 지열 분출구 (네더 동력), 내열 드릴 (굴착기로 고대 잔해 · 흑요석), 엔더 닻 (청크 붙잡기), 네더 자원 분쇄.
 * GameTest는 오버월드에서 돌아서, 분출구는 {@code work(level, pos, hot)}로 네더의 열기를 흉내 낸다.
 */
public class SanupcraftNetherEndGameTest {
	// ---- 지열 분출구 ----

	/** 용암 원천 둘이 닿으면 틱당 증기 4. 오버월드(차가움)에서는 0. */
	@GameTest
	public void ventMakesSteamFromLavaOnlyWhenHot(GameTestHelper helper) {
		BlockPos ventPos = new BlockPos(2, 2, 2);
		helper.setBlock(ventPos, ModPower.GEOTHERMAL_VENT);
		helper.setBlock(ventPos.north(), Blocks.LAVA);
		helper.setBlock(ventPos.south(), Blocks.LAVA);
		GeothermalVentBlockEntity vent = helper.getBlockEntity(ventPos, GeothermalVentBlockEntity.class);
		BlockPos abs = helper.absolutePos(ventPos);

		helper.assertFalse(GeothermalVentBlockEntity.isHot(helper.getLevel()), "오버월드가 뜨겁다고 함");
		helper.assertValueEqual(vent.work(helper.getLevel(), abs, false), 0, "차가운 곳에서 만든 증기");
		helper.assertValueEqual(vent.status(), GeothermalVentBlockEntity.Status.TOO_COLD, "오버월드 상태");
		helper.assertValueEqual(vent.work(helper.getLevel(), abs, true), 2 * GeothermalVentBlockEntity.STEAM_PER_LAVA, "용암 둘의 증기");
		helper.succeed();
	}

	/** 용암이 다섯 곳에 닿아도 넷까지만 센다 (틱당 8). */
	@GameTest
	public void ventCountsAtMostFourLava(GameTestHelper helper) {
		BlockPos ventPos = new BlockPos(2, 2, 2);
		helper.setBlock(ventPos, ModPower.GEOTHERMAL_VENT);
		for (BlockPos lava : new BlockPos[] {ventPos.north(), ventPos.south(), ventPos.east(), ventPos.west(), ventPos.above()}) {
			helper.setBlock(lava, Blocks.LAVA);
		}
		GeothermalVentBlockEntity vent = helper.getBlockEntity(ventPos, GeothermalVentBlockEntity.class);
		helper.assertValueEqual(vent.work(helper.getLevel(), helper.absolutePos(ventPos), true),
				GeothermalVentBlockEntity.MAX_LAVA * GeothermalVentBlockEntity.STEAM_PER_LAVA, "용암 다섯의 증기");
		helper.succeed();
	}

	/** 분출구 옆 기계(바로 붙은 분쇄기)가 증기를 받는다. */
	@GameTest
	public void ventFeedsAdjacentMachine(GameTestHelper helper) {
		BlockPos ventPos = new BlockPos(2, 2, 2);
		helper.setBlock(ventPos, ModPower.GEOTHERMAL_VENT);
		helper.setBlock(ventPos.north(), Blocks.LAVA);
		helper.setBlock(ventPos.east(), ModMachines.CRUSHER);
		GeothermalVentBlockEntity vent = helper.getBlockEntity(ventPos, GeothermalVentBlockEntity.class);
		MachineBlockEntity crusher = helper.getBlockEntity(ventPos.east(), MachineBlockEntity.class);
		vent.work(helper.getLevel(), helper.absolutePos(ventPos), true);
		helper.assertTrue(crusher.steam() > 0, "분쇄기가 증기를 받지 못함");
		helper.succeed();
	}

	// ---- 내열 드릴 ----

	/** 드릴을 끼운 굴착기는 흑요석을 캐서 위 상자로 올린다 (철 곡괭이 등급이면 남긴다). */
	@GameTest(environment = "sanupcraft-test:alone_quarry", maxTicks = 300)
	public void drillLetsQuarryMineObsidian(GameTestHelper helper) {
		BlockPos quarryPos = new BlockPos(3, 3, 3);
		helper.setBlock(quarryPos.below(), Blocks.OBSIDIAN);
		helper.setBlock(quarryPos, ModMining.STEAM_QUARRY);
		helper.setBlock(quarryPos.above(), Blocks.CHEST);
		QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
		helper.assertTrue(quarry.installDrill(), "드릴을 끼우지 못함");
		helper.assertFalse(quarry.installDrill(), "드릴을 두 번 끼움");
		quarry.acceptSteam(QuarryBlockEntity.STEAM_CAPACITY);
		ChestBlockEntity chest = helper.getBlockEntity(quarryPos.above(), ChestBlockEntity.class);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(Blocks.OBSIDIAN, quarryPos.below());
			helper.assertTrue(chest.countItem(Items.OBSIDIAN) >= 1, "흑요석이 올라오지 않음");
		});
	}

	/** 드릴을 끼운 굴착기를 부수면 드릴이 떨어진다. */
	@GameTest
	public void drillDropsWhenQuarryRemoved(GameTestHelper helper) {
		BlockPos quarryPos = new BlockPos(1, 1, 1);
		helper.setBlock(quarryPos, ModMining.STEAM_QUARRY);
		helper.getBlockEntity(quarryPos, QuarryBlockEntity.class).installDrill();
		helper.setBlock(quarryPos, Blocks.AIR);
		helper.assertItemEntityPresent(ModMining.HEATPROOF_DRILL, quarryPos, 2.0);
		helper.succeed();
	}

	// ---- 엔더 닻 ----

	/**
	 * 연료가 있으면 둘레 3x3 청크가 강제 로드되고, 부수면 자기가 붙잡은 청크만 푼다
	 * (시험 구역처럼 원래 강제 로드돼 있던 청크는 그대로).
	 */
	@GameTest
	public void anchorForcesChunksAndReleasesOnlyItsOwn(GameTestHelper helper) {
		BlockPos anchorPos = new BlockPos(1, 1, 1);
		helper.setBlock(anchorPos, ModLogistics.ENDER_ANCHOR);
		EnderAnchorBlockEntity anchor = helper.getBlockEntity(anchorPos, EnderAnchorBlockEntity.class);
		BlockPos abs = helper.absolutePos(anchorPos);
		ChunkPos center = ChunkPos.containing(abs);
		var level = helper.getLevel();
		boolean centerWasForced = level.getForceLoadedChunks().contains(center.pack());

		anchor.tick(level, abs);
		helper.assertFalse(anchor.isForced(), "연료 없이 청크를 붙잡음");
		ItemStack fuel = new ItemStack(Items.POPPED_CHORUS_FRUIT, 2);
		helper.assertValueEqual(anchor.insertFuel(fuel), 2, "넣은 연료 수");
		anchor.tick(level, abs);
		helper.assertTrue(anchor.isActive() && anchor.isForced(), "연료를 넣었는데 켜지지 않음");
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				helper.assertTrue(level.getForceLoadedChunks().contains(ChunkPos.pack(center.x() + dx, center.z() + dz)),
						"강제 로드되지 않은 청크: " + dx + ", " + dz);
			}
		}
		helper.assertValueEqual(anchor.remainingTicks(), (long) EnderAnchorBlockEntity.TICKS_PER_FUEL * 2 - 1, "남은 시간");

		int own = anchor.ownChunkCount();
		helper.setBlock(anchorPos, Blocks.AIR);
		helper.assertValueEqual(level.getForceLoadedChunks().contains(center.pack()), centerWasForced, "원래 상태로 돌아오지 않은 가운데 청크");
		helper.assertItemEntityPresent(Items.POPPED_CHORUS_FRUIT, anchorPos, 2.0);
		helper.assertTrue(own <= 9, "붙잡은 청크 수: " + own);
		helper.succeed();
	}

	/**
	 * 서버를 다시 켠 직후 흉내: 아직 tick하지 않은 닻 A도 청크에 들어올 때(clearRemoved) 켜진 닻 목록에 올라,
	 * 먼저 꺼지는 닻 C가 공유 청크를 풀지 않고 A에게 넘긴다.
	 * 전제: C가 실제로 청크를 하나 이상 소유해야 한다 (시험 구역 청크가 이미 강제 로드돼 0개면 고치기 전에도 통과하므로).
	 */
	// 전용 환경에서 혼자 돈다: 같은 묶음의 다른 시험 구역이 강제 로드돼 있으면 C가 소유할 청크가 없다
	@GameTest(environment = "sanupcraft-test:alone_anchor")
	public void anchorHandsOverAfterReload(GameTestHelper helper) {
		BlockPos cPos = new BlockPos(1, 1, 1);
		BlockPos aPos = new BlockPos(3, 1, 1);
		helper.setBlock(cPos, ModLogistics.ENDER_ANCHOR);
		helper.setBlock(aPos, ModLogistics.ENDER_ANCHOR);
		EnderAnchorBlockEntity c = helper.getBlockEntity(cPos, EnderAnchorBlockEntity.class);
		EnderAnchorBlockEntity a = helper.getBlockEntity(aPos, EnderAnchorBlockEntity.class);
		helper.assertValueEqual(c.insertFuel(new ItemStack(Items.POPPED_CHORUS_FRUIT, 1)), 1, "C에 넣은 연료 수");
		helper.assertValueEqual(a.insertFuel(new ItemStack(Items.POPPED_CHORUS_FRUIT, 1)), 1, "A에 넣은 연료 수");
		var level = helper.getLevel();

		c.tick(level, helper.absolutePos(cPos));
		helper.assertTrue(c.ownChunkCount() > 0, "C가 소유한 청크가 없음 (시험이 아무것도 지키지 못함)");
		int owned = c.ownChunkCount();
		a.tick(level, helper.absolutePos(aPos));

		// 재시작 흉내: 켜진 닻 목록을 비우고, A는 청크에 다시 들어온 것처럼 clearRemoved만 부른다
		EnderAnchorBlockEntity.clearActive();
		helper.assertFalse(a.isTracked(), "목록을 비웠는데 A가 남아 있음");
		a.clearRemoved();
		helper.assertTrue(a.isTracked(), "clearRemoved 뒤 A가 켜진 닻 목록에 없음");

		helper.setBlock(cPos, Blocks.AIR);
		helper.assertValueEqual(a.ownChunkCount(), owned, "A가 넘겨받은 청크 수");

		helper.setBlock(aPos, Blocks.AIR);
		helper.succeed();
	}

	/** 블록 엔티티가 내려가면(setRemoved) 켜진 닻 목록에서 빠진다. 청크는 풀지 않는다. */
	@GameTest
	public void anchorUntracksOnRemoved(GameTestHelper helper) {
		BlockPos anchorPos = new BlockPos(1, 1, 1);
		helper.setBlock(anchorPos, ModLogistics.ENDER_ANCHOR);
		EnderAnchorBlockEntity anchor = helper.getBlockEntity(anchorPos, EnderAnchorBlockEntity.class);
		helper.assertValueEqual(anchor.insertFuel(new ItemStack(Items.POPPED_CHORUS_FRUIT, 1)), 1, "넣은 연료 수");
		anchor.tick(helper.getLevel(), helper.absolutePos(anchorPos));
		helper.assertTrue(anchor.isTracked(), "켜진 닻이 목록에 없음");
		boolean forcedBefore = anchor.isForced();

		anchor.setRemoved();
		helper.assertFalse(anchor.isTracked(), "setRemoved 뒤에도 목록에 남음");
		helper.assertValueEqual(anchor.isForced(), forcedBefore, "setRemoved가 청크를 풀었음");

		anchor.clearRemoved();
		helper.assertTrue(anchor.isTracked(), "clearRemoved 뒤 다시 목록에 오르지 않음");
		helper.setBlock(anchorPos, Blocks.AIR);
		helper.succeed();
	}

	/** 연료는 튀긴 후렴과만 받는다. */
	@GameTest
	public void anchorOnlyTakesPoppedChorus(GameTestHelper helper) {
		BlockPos anchorPos = new BlockPos(1, 1, 1);
		helper.setBlock(anchorPos, ModLogistics.ENDER_ANCHOR);
		EnderAnchorBlockEntity anchor = helper.getBlockEntity(anchorPos, EnderAnchorBlockEntity.class);
		helper.assertValueEqual(anchor.insertFuel(new ItemStack(Items.CHORUS_FRUIT, 4)), 0, "후렴과(안 튀긴 것)를 받음");
		helper.assertFalse(anchor.canPlaceItem(0, new ItemStack(Items.COAL)), "석탄을 받음");
		helper.assertTrue(anchor.canPlaceItem(0, new ItemStack(Items.POPPED_CHORUS_FRUIT)), "튀긴 후렴과를 안 받음");
		helper.succeed();
	}

	// ---- 네더 자원 분쇄 ----

	@GameTest(maxTicks = 400)
	public void crusherTakesNetherOres(GameTestHelper helper) {
		crush(helper, new BlockPos(1, 1, 1), Items.NETHER_QUARTZ_ORE, Items.QUARTZ, 2);
		crush(helper, new BlockPos(3, 1, 1), Items.GLOWSTONE, Items.GLOWSTONE_DUST, 4);
		crush(helper, new BlockPos(5, 1, 1), Items.MAGMA_BLOCK, Items.MAGMA_CREAM, 2);
		helper.succeedWhen(() -> {
			assertResult(helper, new BlockPos(1, 1, 1), Items.QUARTZ, 2);
			assertResult(helper, new BlockPos(3, 1, 1), Items.GLOWSTONE_DUST, 4);
			assertResult(helper, new BlockPos(5, 1, 1), Items.MAGMA_CREAM, 2);
		});
	}

	private static void crush(GameTestHelper helper, BlockPos pos, net.minecraft.world.item.Item input, net.minecraft.world.item.Item expected, int count) {
		helper.setBlock(pos, (Block) ModMachines.CRUSHER);
		MachineBlockEntity machine = helper.getBlockEntity(pos, MachineBlockEntity.class);
		machine.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(input));
		machine.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
	}

	private static void assertResult(GameTestHelper helper, BlockPos pos, net.minecraft.world.item.Item expected, int count) {
		ItemStack result = helper.getBlockEntity(pos, MachineBlockEntity.class).getItem(MachineBlockEntity.SLOT_RESULT);
		helper.assertTrue(result.is(expected) && result.getCount() == count, "결과: " + result);
	}
}
