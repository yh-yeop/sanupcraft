package io.github.yhyeop.sanupcraft.test;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.MachineRecipe;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.AreaMining;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.mining.ModMining;
import io.github.yhyeop.sanupcraft.mining.QuarryBlockEntity;

/** 공장 개편 (실테스트 피드백 1): 부산물 · 쓰레기 블록 → 자원 · 합금(강철) · 3x3 해머 · 증기 굴착기. */
public class SanupcraftFactoryGameTest {
	private static final BlockPos MACHINE = new BlockPos(1, 1, 1);

	/** 합금로: 철가루 2 + 석탄 가루 2 → 강철 (원석 철 1개분). 재료 칸 순서는 상관없다. */
	@GameTest(maxTicks = 300)
	public void alloySmelterMakesSteel(GameTestHelper helper) {
		MachineBlockEntity smelter = machine(helper, MACHINE, ModMachines.ALLOY_SMELTER);
		smelter.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(ModItems.COAL_DUST, 2));
		smelter.setItem(MachineBlockEntity.SLOT_INPUT2, new ItemStack(ModItems.IRON_DUST, 2));
		smelter.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		helper.succeedWhen(() -> {
			helper.assertTrue(smelter.result().is(ModItems.STEEL_INGOT), "강철이 아님: " + smelter.result());
			helper.assertTrue(smelter.getItem(MachineBlockEntity.SLOT_INPUT).isEmpty(), "석탄 가루 2개를 다 쓰지 않음");
			helper.assertTrue(smelter.getItem(MachineBlockEntity.SLOT_INPUT2).isEmpty(), "철가루 2개를 다 쓰지 않음");
		});
	}

	@GameTest(maxTicks = 300)
	public void alloySmelterWaitsForEnoughIronDust(GameTestHelper helper) {
		MachineBlockEntity smelter = machine(helper, MACHINE, ModMachines.ALLOY_SMELTER);
		smelter.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(ModItems.IRON_DUST, 1));
		smelter.setItem(MachineBlockEntity.SLOT_INPUT2, new ItemStack(ModItems.COAL_DUST, 2));
		smelter.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		helper.runAfterDelay(250, () -> {
			helper.assertTrue(smelter.result().isEmpty(), "철가루 1개로 강철이 나옴");
			helper.succeed();
		});
	}

	/** 원석 → 가루 2 + 다른 금속 부산물 확률, 쓰레기 블록 → 자원 + 부산물. */
	@GameTest
	public void crusherRecipesHaveByproducts(GameTestHelper helper) {
		var recipes = helper.getLevel().getServer().getRecipeManager();
		Object[][] expected = {
				{"crushing/raw_iron", ModItems.GOLD_DUST, 0.1F},
				{"crushing/raw_copper", ModItems.GOLD_DUST, 0.1F},
				{"crushing/raw_gold", ModItems.COPPER_DUST, 0.1F},
				{"crushing/cobblestone", Items.IRON_NUGGET, 0.1F},
				{"crushing/gravel", Items.FLINT, 0.25F},
		};
		for (Object[] row : expected) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Sanupcraft.id((String) row[0]));
			var holder = recipes.byKey(key).orElse(null);
			helper.assertTrue(holder != null && holder.value() instanceof MachineRecipe, "레시피가 없음: " + row[0]);
			MachineRecipe recipe = (MachineRecipe) holder.value();
			helper.assertTrue(recipe.byproduct().isPresent(), "부산물이 없음: " + row[0]);
			helper.assertTrue(recipe.byproduct().get().item().create().is((Item) row[1]), "부산물이 다름: " + row[0]);
			helper.assertValueEqual(recipe.byproduct().get().chance(), (float) row[2], "부산물 확률: " + row[0]);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 300)
	public void crusherTurnsCoalIntoDust(GameTestHelper helper) {
		MachineBlockEntity crusher = machine(helper, MACHINE, ModMachines.CRUSHER);
		crusher.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.COAL, 2));
		crusher.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		helper.succeedWhen(() -> helper.assertValueEqual(crusher.result().getCount(), 4, "석탄 가루"));
	}

	@GameTest(maxTicks = 300)
	public void pressMakesSteelPlate(GameTestHelper helper) {
		MachineBlockEntity press = machine(helper, MACHINE, ModMachines.PRESS);
		press.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(ModItems.STEEL_INGOT));
		press.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		helper.succeedWhen(() -> helper.assertTrue(press.result().is(ModItems.STEEL_PLATE), "강철판이 아님: " + press.result()));
	}

	@GameTest
	public void factoryRecipesLoad(GameTestHelper helper) {
		String[] ids = {"alloy_smelter", "steam_quarry", "steel_hammer", "steel_excavator", "alloying/steel_ingot",
				"pressing/steel_plate", "crushing/coal", "crushing/raw_copper", "crushing/bone",
				"copper_ingot_from_smelting", "gold_ingot_from_blasting"};
		var recipes = helper.getLevel().getServer().getRecipeManager();
		for (String id : ids) {
			helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Sanupcraft.id(id))).isPresent(), "레시피가 없음: " + id);
		}
		helper.succeed();
	}

	/** 강철 해머: 바라본 면에 평평한 3x3. 흑요석처럼 훨씬 단단한 블록은 남긴다. */
	@GameTest
	public void hammerBreaksThreeByThree(GameTestHelper helper) {
		BlockPos center = new BlockPos(3, 3, 3);
		List<BlockPos> plane = AreaMining.plane(center, Direction.NORTH);
		helper.setBlock(center, Blocks.STONE);
		for (BlockPos pos : plane) {
			helper.setBlock(pos, Blocks.STONE);
		}
		BlockPos hard = plane.get(0);
		helper.setBlock(hard, Blocks.OBSIDIAN);
		helper.setBlock(center.north(), Blocks.STONE);

		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(SteelGear.STEEL_HAMMER));
		int broken = AreaMining.breakAround(helper.getLevel(), player, helper.absolutePos(center), Blocks.STONE.defaultBlockState(), Direction.NORTH);

		helper.assertValueEqual(broken, 7, "같이 캔 블록 수 (흑요석 제외)");
		helper.assertBlockPresent(Blocks.OBSIDIAN, hard);
		helper.assertBlockPresent(Blocks.STONE, center.north());
		helper.assertTrue(player.getMainHandItem().getDamageValue() >= 7, "해머 내구도를 안 씀");
		helper.succeed();
	}

	/** 해머 · 굴삭 삽은 3x3 대가로 강철 곡괭이 · 삽보다 느리다 (60%). 강철은 인챈트가 다이아와 같다(10). */
	@GameTest
	public void areaToolsAreSlowerThanSingleTools(GameTestHelper helper) {
		float hammer = new ItemStack(SteelGear.STEEL_HAMMER).getDestroySpeed(Blocks.STONE.defaultBlockState());
		float ironPick = new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState());
		helper.assertTrue(hammer < ironPick, "해머 " + hammer + " ≥ 철 곡괭이 " + ironPick);
		helper.assertValueEqual(hammer, SteelGear.STEEL_TOOL.speed() * SteelGear.AREA_SPEED_FACTOR, "해머 속도");
		helper.assertValueEqual(SteelGear.STEEL_TOOL.enchantmentValue(), 10, "강철 도구 인챈트");
		helper.assertValueEqual(SteelGear.STEEL_ARMOR.enchantmentValue(), 10, "강철 갑옷 인챈트");
		helper.succeed();
	}

	/** 증기 굴착기: 증기가 있으면 아래를 캐서 위 상자에 넣는다. 주변 테스트를 파지 않도록 따로 돌린다. */
	@GameTest(environment = "sanupcraft-test:alone_quarry", maxTicks = 200)
	public void quarryMinesIntoChestAbove(GameTestHelper helper) {
		BlockPos quarryPos = new BlockPos(3, 3, 3);
		helper.setBlock(quarryPos.below(), Blocks.STONE);
		helper.setBlock(quarryPos, ModMining.STEAM_QUARRY);
		helper.setBlock(quarryPos.above(), Blocks.CHEST);
		QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
		quarry.acceptSteam(QuarryBlockEntity.STEAM_CAPACITY);
		ChestBlockEntity chest = helper.getBlockEntity(quarryPos.above(), ChestBlockEntity.class);
		helper.succeedWhen(() -> {
			helper.assertTrue(quarry.mined() >= 3, "캔 블록 수: " + quarry.mined());
			helper.assertTrue(!chest.isEmpty(), "상자에 들어온 게 없음");
			helper.assertTrue(quarry.steam() < QuarryBlockEntity.STEAM_CAPACITY, "증기를 안 씀");
		});
	}

	@GameTest(maxTicks = 100)
	public void quarryIdlesWithoutSteam(GameTestHelper helper) {
		BlockPos quarryPos = new BlockPos(3, 3, 3);
		helper.setBlock(quarryPos.below(), Blocks.STONE);
		helper.setBlock(quarryPos, ModMining.STEAM_QUARRY);
		helper.setBlock(quarryPos.above(), Blocks.CHEST);
		QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
		helper.runAfterDelay(60, () -> {
			helper.assertValueEqual(quarry.mined(), 0, "증기 없이 캠");
			helper.assertBlockPresent(Blocks.STONE, quarryPos.below());
			helper.succeed();
		});
	}

	private static MachineBlockEntity machine(GameTestHelper helper, BlockPos pos, Block block) {
		helper.setBlock(pos, block);
		return helper.getBlockEntity(pos, MachineBlockEntity.class);
	}
}
