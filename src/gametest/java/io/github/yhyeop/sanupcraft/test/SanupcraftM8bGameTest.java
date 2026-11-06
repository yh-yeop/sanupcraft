package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.MachineInput;
import io.github.yhyeop.sanupcraft.factory.MachineKind;
import io.github.yhyeop.sanupcraft.factory.MachineRecipe;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;
import io.github.yhyeop.sanupcraft.fluid.ModFluid;
import io.github.yhyeop.sanupcraft.fluid.TankBlockEntity;
import io.github.yhyeop.sanupcraft.industry.FurnaceTowerControllerBlock;
import io.github.yhyeop.sanupcraft.industry.FurnaceTowerControllerBlockEntity;
import io.github.yhyeop.sanupcraft.industry.HeatproofBricksBlock;
import io.github.yhyeop.sanupcraft.industry.ModFoundry;
import io.github.yhyeop.sanupcraft.industry.MultiblockShape;
import io.github.yhyeop.sanupcraft.nether.ModNether;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.power.SoulFurnaceBlockEntity;

/** M8 묶음 B 검사: 세척기 · 고압 정련기 · 정제 가루 (I3), 용광로 탑 (I2). */
public class SanupcraftM8bGameTest {
	/** 용광로 탑 제어기 자리 (북쪽을 봄): 모양은 x 1..3, z 1..3, y 1..3. */
	private static final BlockPos CONTROLLER = new BlockPos(2, 1, 1);
	private static final Direction FACING = Direction.NORTH;

	// ---- 세척기 ----

	/** 물 100 mB로 원석 하나 → 정제 철가루 3. 물이 떨어지면 다음 원석은 그대로 남는다. */
	@GameTest
	public void washerMakesThreeDustAndStopsWithoutWater(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModMachines.WASHER);
		MachineBlockEntity washer = helper.getBlockEntity(pos, MachineBlockEntity.class);
		washer.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 2));
		helper.assertValueEqual(washer.fill(FluidKind.WATER, 100, false), 100, "받은 물");
		helper.assertValueEqual(washer.fill(FluidKind.LAVA, 100, false), 0, "용암을 받음");
		tick(helper, pos, washer, 600, false);
		helper.assertTrue(washer.result().is(ModFoundry.REFINED_IRON_DUST), "정제 철가루가 아님: " + washer.result());
		helper.assertValueEqual(washer.result().getCount(), 3, "원석 하나의 정제 가루");
		helper.assertValueEqual(washer.water(), 0, "남은 물");
		helper.assertValueEqual(washer.getItem(MachineBlockEntity.SLOT_INPUT).getCount(), 1, "물이 없는데 원석을 썼음");
		helper.succeed();
	}

	/** 옆 물 탱크에서 물을 끌어오고, 다른 기계(분쇄기)는 물을 받지 않는다. */
	@GameTest
	public void washerDrawsWaterFromTank(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModMachines.WASHER);
		helper.setBlock(pos.east(), ModFluid.TANK);
		TankBlockEntity tank = helper.getBlockEntity(pos.east(), TankBlockEntity.class);
		tank.fill(FluidKind.WATER, 1000, false);
		MachineBlockEntity washer = helper.getBlockEntity(pos, MachineBlockEntity.class);
		BlockPos abs = helper.absolutePos(pos);
		MachineBlockEntity.serverTick(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), washer);
		helper.assertValueEqual(washer.water(), MachineBlockEntity.WATER_DRAW_PER_TICK, "한 틱에 끌어온 물");
		helper.assertValueEqual(tank.fluidAmount(), 1000 - MachineBlockEntity.WATER_DRAW_PER_TICK, "탱크에 남은 물");

		helper.setBlock(new BlockPos(0, 1, 0), ModMachines.CRUSHER);
		MachineBlockEntity crusher = helper.getBlockEntity(new BlockPos(0, 1, 0), MachineBlockEntity.class);
		helper.assertValueEqual(crusher.fill(FluidKind.WATER, 100, false), 0, "분쇄기가 물을 받음");
		helper.succeed();
	}

	// ---- 고압 정련기 ----

	/** 고압 증기로만 돌고, 원석 하나 → 정제 가루 4. 보통 증기 · 연료는 받지 않는다. */
	@GameTest
	public void refinerMakesFourWithHighPressureOnly(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModMachines.HP_REFINER);
		MachineBlockEntity refiner = helper.getBlockEntity(pos, MachineBlockEntity.class);
		helper.assertValueEqual(refiner.acceptSteam(50), 0, "보통 증기를 받음");
		helper.assertFalse(refiner.canPlaceItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL)), "연료 칸에 석탄이 들어감");
		refiner.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_GOLD));
		tick(helper, pos, refiner, 400, true);
		helper.assertTrue(refiner.result().is(ModFoundry.REFINED_GOLD_DUST), "정제 금가루가 아님: " + refiner.result());
		helper.assertValueEqual(refiner.result().getCount(), 4, "원석 하나의 정제 가루");
		helper.succeed();
	}

	/** 정제 가루는 화로 · 용광로에서 주괴가 되고, 희귀 가루 4 + 정밀 부품은 정밀 조립기에서 두 배 업그레이드가 된다. */
	@GameTest
	public void refinedDustSmeltsAndRareDustHasUse(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var manager = level.recipeAccess();
		Item[][] pairs = {{ModFoundry.REFINED_IRON_DUST, Items.IRON_INGOT}, {ModFoundry.REFINED_COPPER_DUST, Items.COPPER_INGOT},
				{ModFoundry.REFINED_GOLD_DUST, Items.GOLD_INGOT}};
		for (Item[] pair : pairs) {
			SingleRecipeInput input = new SingleRecipeInput(new ItemStack(pair[0]));
			var smelt = manager.getRecipeFor(RecipeType.SMELTING, input, level);
			helper.assertTrue(smelt.isPresent() && smelt.get().value().assemble(input).is(pair[1]), "화로 레시피가 없음: " + pair[0]);
			var blast = manager.getRecipeFor(RecipeType.BLASTING, input, level);
			helper.assertTrue(blast.isPresent() && blast.get().value().assemble(input).is(pair[1]), "용광로 레시피가 없음: " + pair[0]);
		}
		MachineInput precision = new MachineInput(new ItemStack(ModFoundry.RARE_DUST, 4), new ItemStack(ModNether.PRECISION_PART));
		var upgrade = manager.getRecipeFor(MachineKind.PRECISION.recipeType(), precision, level).map(RecipeHolder::value);
		helper.assertTrue(upgrade.isPresent() && upgrade.get().output().create().is(ModMachines.DOUBLE_UPGRADE), "희귀 가루 정밀 레시피가 없음");
		for (MachineKind kind : new MachineKind[] {MachineKind.WASHER, MachineKind.REFINER}) {
			MachineRecipe recipe = manager.getRecipeFor(kind.recipeType(), new MachineInput(new ItemStack(Items.RAW_COPPER), ItemStack.EMPTY), level)
					.map(RecipeHolder::value).orElse(null);
			helper.assertTrue(recipe != null && recipe.output().create().is(ModFoundry.REFINED_COPPER_DUST), kind + " 구리 원석 레시피가 없음");
			helper.assertTrue(recipe.byproduct().map(b -> b.item().create().is(ModFoundry.RARE_DUST)).orElse(false), kind + " 부산물이 희귀 가루가 아님");
		}
		helper.succeed();
	}

	// ---- 용광로 탑 ----

	/** 벽돌이 하나 빠지면 그 칸을, 가운데 기둥이 막히면 그 칸을 알려 주고, 다 맞으면 서고 벽돌 무늬가 바뀐다. 벽돌을 부수면 풀린다. */
	@GameTest(maxTicks = 60)
	public void furnaceTowerFormsAndUnforms(GameTestHelper helper) {
		FurnaceTowerControllerBlockEntity tower = buildTower(helper);
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(CONTROLLER);
		BlockState state = level.getBlockState(abs);
		helper.assertValueEqual(FurnaceTowerControllerBlockEntity.SHAPE.casings(CONTROLLER, FACING).size(), 23, "벽돌 칸 수");

		BlockPos corner = new BlockPos(3, 3, 3);
		helper.setBlock(corner, Blocks.AIR);
		helper.assertFalse(tower.check(level, abs, state), "벽돌 하나가 빠졌는데 모양이 섬");
		helper.assertValueEqual(tower.problem().pos(), helper.absolutePos(corner), "알려 준 틀린 칸");
		helper.assertValueEqual(tower.problem().expected(), MultiblockShape.Part.CASING, "틀린 칸에 와야 하는 것");

		helper.setBlock(corner, ModFoundry.HEATPROOF_BRICKS);
		BlockPos hollow = new BlockPos(2, 1, 2);
		helper.setBlock(hollow, Blocks.STONE);
		helper.assertFalse(tower.check(level, abs, state), "가운데 기둥이 막혔는데 모양이 섬");
		helper.assertValueEqual(tower.problem().pos(), helper.absolutePos(hollow), "막힌 빈칸");
		helper.assertValueEqual(tower.problem().expected(), MultiblockShape.Part.HOLLOW, "빈칸이어야 함");

		helper.setBlock(hollow, Blocks.AIR);
		helper.assertTrue(tower.check(level, abs, state), "모양이 맞는데 서지 않음: " + tower.problem());
		BlockPos other = new BlockPos(1, 3, 3);
		helper.assertTrue(helper.getBlockState(other).getValue(HeatproofBricksBlock.FORMED), "벽돌 무늬가 바뀌지 않음");

		helper.destroyBlock(new BlockPos(3, 2, 2));
		helper.succeedWhen(() -> {
			helper.assertFalse(tower.isFormed(), "벽돌을 부쉈는데 모양이 그대로");
			helper.assertFalse(helper.getBlockState(other).getValue(HeatproofBricksBlock.FORMED), "남은 벽돌 무늬가 켜져 있음");
		});
	}

	/** 영혼 화로 → 고압 파이프 → 옆 벽돌: 철가루 10개 중 8개를 한 번에 굽는다 (남은 2개는 다음 번). */
	@GameTest(maxTicks = 400)
	public void furnaceTowerSmeltsEightAtOnceOnHighPressure(GameTestHelper helper) {
		FurnaceTowerControllerBlockEntity tower = buildTower(helper);
		// 동쪽 벽 (x 3) 가운데 너머: 고압 파이프 (4, 2, 2), 영혼 화로 (5, 2, 2) + 아래 영혼 모래
		helper.setBlock(new BlockPos(5, 1, 2), Blocks.SOUL_SAND);
		helper.setBlock(new BlockPos(4, 2, 2), ModNether.HIGH_PRESSURE_PIPE);
		helper.setBlock(new BlockPos(5, 2, 2), ModNether.SOUL_FURNACE);
		SoulFurnaceBlockEntity furnace = helper.getBlockEntity(new BlockPos(5, 2, 2), SoulFurnaceBlockEntity.class);
		furnace.setItem(0, new ItemStack(Items.BLAZE_POWDER, 4));
		tower.setItem(0, new ItemStack(ModItems.IRON_DUST, 6));
		tower.setItem(4, new ItemStack(ModItems.IRON_DUST, 4));
		helper.succeedWhen(() -> {
			int ingots = count(tower, Items.IRON_INGOT);
			helper.assertValueEqual(ingots, 8, "한 번에 구운 철 주괴 (증기 " + tower.steam() + ")");
			int dustLeft = tower.getItem(0).getCount() + tower.getItem(4).getCount();
			helper.assertValueEqual(dustLeft, 2, "남은 철가루");
		});
	}

	/** 합금로 레시피를 먼저 본다: 철가루 4 + 석탄 가루 4 → 강철 2를 한 번에. 모양이 풀리면 고압 증기를 받지 않는다. */
	@GameTest
	public void furnaceTowerAlloysSteelInOneBatch(GameTestHelper helper) {
		FurnaceTowerControllerBlockEntity tower = buildTower(helper);
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(CONTROLLER);
		tower.setItem(0, new ItemStack(ModItems.IRON_DUST, 4));
		tower.setItem(1, new ItemStack(ModItems.COAL_DUST, 4));
		helper.assertTrue(tower.check(level, abs, level.getBlockState(abs)), "모양이 서지 않음: " + tower.problem());
		for (int i = 0; i < 260; i++) {
			tower.acceptHighPressureSteam(FurnaceTowerControllerBlockEntity.STEAM_CAPACITY);
			FurnaceTowerControllerBlockEntity.serverTick(level, abs, level.getBlockState(abs), tower);
		}
		helper.assertValueEqual(count(tower, ModItems.STEEL_INGOT), 2, "강철 주괴");
		helper.assertValueEqual(count(tower, Items.IRON_INGOT), 0, "철가루를 그냥 구움");
		helper.assertTrue(tower.getItem(0).isEmpty() && tower.getItem(1).isEmpty(), "재료가 남음");

		helper.setBlock(new BlockPos(1, 1, 1), Blocks.AIR);
		helper.assertFalse(tower.check(level, abs, level.getBlockState(abs)), "벽돌이 빠졌는데 모양이 섬");
		int before = tower.steam();
		helper.assertValueEqual(tower.acceptHighPressureSteam(10), 0, "모양이 풀렸는데 증기를 받음");
		helper.assertValueEqual(tower.steam(), before, "증기");
		helper.succeed();
	}

	/** 실테스트 단계 industry2: 용광로 탑 한 벌 · 세척기 · 고압 정련기를 준다. */
	@GameTest
	public void playtestIndustry2KitGivesTower(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		PlaytestCommand.apply(player, PlaytestCommand.Stage.INDUSTRY2);
		var inventory = player.getInventory();
		helper.assertTrue(inventory.countItem(ModFoundry.FURNACE_TOWER_CONTROLLER.asItem()) == 1, "제어기를 주지 않음");
		helper.assertTrue(inventory.countItem(ModFoundry.HEATPROOF_BRICKS.asItem()) >= 23, "벽돌이 모자람");
		helper.assertTrue(inventory.countItem(ModMachines.WASHER.asItem()) == 1, "세척기를 주지 않음");
		helper.assertTrue(inventory.countItem(ModMachines.HP_REFINER.asItem()) == 1, "고압 정련기를 주지 않음");
		helper.succeed();
	}

	// ---- 도우미 ----

	/** 기계를 {@code ticks}번 직접 돌린다 (틱마다 증기를 가득 채움: {@code highPressure}면 고압). */
	private static void tick(GameTestHelper helper, BlockPos pos, MachineBlockEntity machine, int ticks, boolean highPressure) {
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(pos);
		for (int i = 0; i < ticks; i++) {
			if (highPressure) {
				machine.acceptHighPressureSteam(MachineBlockEntity.STEAM_CAPACITY);
			} else {
				machine.acceptSteam(MachineBlockEntity.STEAM_CAPACITY);
			}
			MachineBlockEntity.serverTick(level, abs, level.getBlockState(abs), machine);
		}
	}

	private static int count(FurnaceTowerControllerBlockEntity tower, Item item) {
		int total = 0;
		for (ItemStack stack : tower.outputs()) {
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	static FurnaceTowerControllerBlockEntity buildTower(GameTestHelper helper) {
		helper.setBlock(CONTROLLER, ModFoundry.FURNACE_TOWER_CONTROLLER.defaultBlockState().setValue(FurnaceTowerControllerBlock.FACING, FACING));
		for (BlockPos casing : FurnaceTowerControllerBlockEntity.SHAPE.casings(CONTROLLER, FACING)) {
			helper.setBlock(casing, ModFoundry.HEATPROOF_BRICKS);
		}
		return helper.getBlockEntity(CONTROLLER, FurnaceTowerControllerBlockEntity.class);
	}
}
