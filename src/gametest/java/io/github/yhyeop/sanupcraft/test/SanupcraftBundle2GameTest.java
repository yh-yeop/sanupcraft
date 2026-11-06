package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.logistics.BeltBlock;
import io.github.yhyeop.sanupcraft.logistics.BeltBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.BeltSorterBlock;
import io.github.yhyeop.sanupcraft.logistics.BeltSorterBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.FilterHopperBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.FilterMenu;
import io.github.yhyeop.sanupcraft.logistics.ItemFilter;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;

/**
 * 묶음 2 검사: 벨트 분류기 · 필터 깔때기 · 원석 표지 (A2).
 * 분류기는 동쪽(+x)을 보고 놓는다: 앞 = 동쪽, 왼쪽 = 북쪽(-z).
 */
public class SanupcraftBundle2GameTest {
	// ---- 분류기 ----

	/** 벨트가 밀어 넣은 아이템: 필터에 맞는 철 주괴는 왼쪽 상자로, 안 맞는 석탄은 앞 상자로. */
	@GameTest(maxTicks = 300)
	public void sorterRoutesMatchingLeftOthersForward(GameTestHelper helper) {
		BlockPos belt = new BlockPos(1, 1, 2);
		BlockPos sorterPos = new BlockPos(2, 1, 2);
		BlockPos leftChest = new BlockPos(2, 1, 1);
		BlockPos frontChest = new BlockPos(3, 1, 2);
		helper.setBlock(belt, ModLogistics.BELT.defaultBlockState().setValue(BeltBlock.FACING, Direction.EAST));
		helper.setBlock(sorterPos, ModLogistics.BELT_SORTER.defaultBlockState().setValue(BeltSorterBlock.FACING, Direction.EAST));
		helper.setBlock(leftChest, Blocks.CHEST);
		helper.setBlock(frontChest, Blocks.CHEST);
		BeltSorterBlockEntity sorter = helper.getBlockEntity(sorterPos, BeltSorterBlockEntity.class);
		sorter.filter().setItem(0, new ItemStack(Items.IRON_INGOT));
		helper.getBlockEntity(belt, BeltBlockEntity.class).power(helper.getLevel().getGameTime() + 1000);
		dropOnBelt(helper, belt, 0.35, new ItemStack(Items.IRON_INGOT, 2));
		dropOnBelt(helper, belt, 0.0, new ItemStack(Items.COAL, 3));
		helper.succeedWhen(() -> {
			ChestBlockEntity left = helper.getBlockEntity(leftChest, ChestBlockEntity.class);
			ChestBlockEntity front = helper.getBlockEntity(frontChest, ChestBlockEntity.class);
			helper.assertValueEqual(left.countItem(Items.IRON_INGOT), 2, "왼쪽 상자의 철 주괴");
			helper.assertValueEqual(left.countItem(Items.COAL), 0, "왼쪽 상자에 석탄이 섞임");
			helper.assertValueEqual(front.countItem(Items.COAL), 3, "앞 상자의 석탄");
			helper.assertValueEqual(front.countItem(Items.IRON_INGOT), 0, "앞 상자에 철 주괴가 섞임");
		});
	}

	/** 분류기 앞에 벨트가 이어져 있으면 안 맞는 것은 그 벨트 위로 올라간다. */
	@GameTest(maxTicks = 200)
	public void sorterFeedsNextBelt(GameTestHelper helper) {
		BlockPos sorterPos = new BlockPos(2, 1, 2);
		BlockPos nextBelt = new BlockPos(3, 1, 2);
		helper.setBlock(sorterPos, ModLogistics.BELT_SORTER.defaultBlockState().setValue(BeltSorterBlock.FACING, Direction.EAST));
		helper.setBlock(nextBelt, ModLogistics.BELT.defaultBlockState().setValue(BeltBlock.FACING, Direction.EAST));
		HopperBlockEntity.addItem(null, helper.getBlockEntity(sorterPos, BeltSorterBlockEntity.class), new ItemStack(Items.COAL, 5), Direction.WEST);
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getBlockEntity(sorterPos, BeltSorterBlockEntity.class).isEmpty(), "분류기가 아직 안 비었음");
			int onBelt = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(1.0)).stream()
					.filter(e -> e.getItem().is(Items.COAL)).mapToInt(e -> e.getItem().getCount()).sum();
			helper.assertValueEqual(onBelt, 5, "벨트 위 석탄");
		});
	}

	/** 고른 출구가 막혔거나 없으면 아이템은 분류기 안에서 기다린다 (사라지지 않는다). 길이 열리면 나간다. */
	@GameTest(maxTicks = 300)
	public void blockedOutputKeepsItem(GameTestHelper helper) {
		BlockPos sorterPos = new BlockPos(2, 1, 2);
		BlockPos leftChest = new BlockPos(2, 1, 1);
		BlockPos frontChest = new BlockPos(3, 1, 2);
		helper.setBlock(sorterPos, ModLogistics.BELT_SORTER.defaultBlockState().setValue(BeltSorterBlock.FACING, Direction.EAST));
		BeltSorterBlockEntity sorter = helper.getBlockEntity(sorterPos, BeltSorterBlockEntity.class);
		sorter.filter().setItem(0, new ItemStack(Items.IRON_INGOT));
		// 왼쪽은 가득 찬 상자, 앞은 아무것도 없음
		helper.setBlock(leftChest, Blocks.CHEST);
		ChestBlockEntity left = helper.getBlockEntity(leftChest, ChestBlockEntity.class);
		for (int i = 0; i < left.getContainerSize(); i++) {
			left.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
		}
		ItemStack rest = HopperBlockEntity.addItem(null, sorter, new ItemStack(Items.IRON_INGOT, 4), Direction.WEST);
		helper.assertTrue(rest.isEmpty(), "분류기가 받지 않음: " + rest);
		// 안 맞는 것을 넣어 보면 이미 차 있어서 받지 않는다
		helper.runAfterDelay(30, () -> {
			helper.assertValueEqual(sorter.getItem(0).getCount(), 4, "막힌 왼쪽 출구 앞에서 줄어든 철 주괴");
			helper.assertValueEqual(left.countItem(Items.IRON_INGOT), 0, "가득 찬 상자에 들어감");
			// 가득 찬 상자를 비우면 나간다
			left.setItem(0, ItemStack.EMPTY);
			helper.runAfterDelay(20, () -> {
				helper.assertTrue(sorter.isEmpty(), "길이 열렸는데 분류기가 안 비었음");
				helper.assertValueEqual(left.countItem(Items.IRON_INGOT), 4, "열린 왼쪽 상자의 철 주괴");
				// 앞 출구 없음: 안 맞는 석탄은 기다린다
				HopperBlockEntity.addItem(null, sorter, new ItemStack(Items.COAL, 2), Direction.WEST);
				helper.runAfterDelay(30, () -> {
					helper.assertValueEqual(sorter.getItem(0).getCount(), 2, "앞이 비었을 때 석탄이 사라짐");
					helper.setBlock(frontChest, Blocks.CHEST);
					helper.runAfterDelay(20, () -> {
						helper.assertTrue(sorter.isEmpty(), "앞에 상자를 놓았는데 분류기가 안 비었음");
						helper.assertValueEqual(helper.getBlockEntity(frontChest, ChestBlockEntity.class).countItem(Items.COAL), 2, "앞 상자의 석탄");
						helper.succeed();
					});
				});
			});
		});
	}

	/** 분류기의 받는 칸은 밖에서 뽑을 수 없다. */
	@GameTest
	public void sorterBufferCannotBeTakenOut(GameTestHelper helper) {
		BlockPos sorterPos = new BlockPos(2, 1, 2);
		helper.setBlock(sorterPos, ModLogistics.BELT_SORTER);
		BeltSorterBlockEntity sorter = helper.getBlockEntity(sorterPos, BeltSorterBlockEntity.class);
		sorter.setItem(0, new ItemStack(Items.COAL));
		for (Direction direction : Direction.values()) {
			helper.assertFalse(sorter.canTakeItemThroughFace(0, sorter.getItem(0), direction), "받는 칸을 " + direction + " 쪽으로 뽑을 수 있음");
		}
		helper.succeed();
	}

	// ---- 원석 표지 · 필터 ----

	/** 원석 표지는 원석 · 광석 태그의 아이템을 전부 맞추고, 주괴 · 석탄 · 돌은 안 맞춘다. */
	@GameTest
	public void oreMarkerMatchesRawAndOres(GameTestHelper helper) {
		ItemFilter filter = new ItemFilter(9);
		filter.setItem(3, new ItemStack(ModLogistics.ORE_MARKER));
		ItemStack[] matching = {new ItemStack(Items.RAW_IRON), new ItemStack(Items.IRON_ORE), new ItemStack(Items.DEEPSLATE_IRON_ORE),
				new ItemStack(Items.RAW_COPPER), new ItemStack(Items.RAW_GOLD), new ItemStack(Items.GOLD_ORE), new ItemStack(Items.DIAMOND_ORE)};
		for (ItemStack stack : matching) {
			helper.assertTrue(filter.matches(stack), "원석 표지가 못 맞춤: " + stack);
		}
		ItemStack[] others = {new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COAL), new ItemStack(Items.STONE), new ItemStack(Items.COBBLESTONE)};
		for (ItemStack stack : others) {
			helper.assertFalse(filter.matches(stack), "원석 표지가 잘못 맞춤: " + stack);
		}
		helper.succeed();
	}

	/** 일반 견본은 종류가 같으면 개수와 상관없이 맞고, 빈 필터는 아무것도 안 맞는다. */
	@GameTest
	public void sampleFilterMatchesByItem(GameTestHelper helper) {
		ItemFilter filter = new ItemFilter(5);
		helper.assertFalse(filter.matches(new ItemStack(Items.COAL)), "빈 필터가 맞춤");
		helper.assertFalse(filter.hasEntries(), "빈 필터에 칸이 있다고 나옴");
		filter.setItem(4, new ItemStack(Items.COAL));
		helper.assertTrue(filter.matches(new ItemStack(Items.COAL, 40)), "석탄 견본이 석탄 덩어리를 못 맞춤");
		helper.assertFalse(filter.matches(new ItemStack(Items.CHARCOAL)), "석탄 견본이 숯을 맞춤");
		helper.succeed();
	}

	/** 필터 칸은 저장했다 읽어도 남고, 짐칸의 아이템도 남는다. */
	@GameTest
	public void filterAndBufferSurviveSaveLoad(GameTestHelper helper) {
		BlockPos sorterPos = new BlockPos(2, 1, 2);
		BlockPos hopperPos = new BlockPos(4, 1, 2);
		helper.setBlock(sorterPos, ModLogistics.BELT_SORTER);
		helper.setBlock(hopperPos, ModLogistics.FILTER_HOPPER);
		BeltSorterBlockEntity sorter = helper.getBlockEntity(sorterPos, BeltSorterBlockEntity.class);
		sorter.filter().setItem(1, new ItemStack(Items.COAL));
		sorter.filter().setItem(8, new ItemStack(ModLogistics.ORE_MARKER));
		sorter.setItem(0, new ItemStack(Items.IRON_INGOT, 7));
		FilterHopperBlockEntity hopper = helper.getBlockEntity(hopperPos, FilterHopperBlockEntity.class);
		hopper.filter().setItem(2, new ItemStack(Items.GOLD_INGOT));
		hopper.setItem(3, new ItemStack(Items.GOLD_INGOT, 5));

		var registries = helper.getLevel().registryAccess();
		BlockEntity sorterCopy = BlockEntity.loadStatic(sorter.getBlockPos(), sorter.getBlockState(), sorter.saveWithFullMetadata(registries), registries);
		BlockEntity hopperCopy = BlockEntity.loadStatic(hopper.getBlockPos(), hopper.getBlockState(), hopper.saveWithFullMetadata(registries), registries);
		helper.assertTrue(sorterCopy instanceof BeltSorterBlockEntity && hopperCopy instanceof FilterHopperBlockEntity, "블록 엔티티를 못 읽음");
		BeltSorterBlockEntity s = (BeltSorterBlockEntity) sorterCopy;
		FilterHopperBlockEntity h = (FilterHopperBlockEntity) hopperCopy;
		helper.assertTrue(s.filter().getItem(1).is(Items.COAL) && s.filter().getItem(8).is(ModLogistics.ORE_MARKER) && s.filter().getItem(0).isEmpty(), "분류기 필터가 안 남음");
		helper.assertValueEqual(s.getItem(0).getCount(), 7, "분류기 받는 칸");
		helper.assertTrue(h.filter().getItem(2).is(Items.GOLD_INGOT), "깔때기 필터가 안 남음");
		helper.assertValueEqual(h.getItem(3).getCount(), 5, "깔때기 짐칸");
		helper.succeed();
	}

	/** 필터 화면: 들고 클릭하면 1개짜리 견본이 놓이고 손의 것은 줄지 않는다. 빈손 클릭 · Shift 클릭으로 비운다. 인벤토리 Shift 클릭은 견본만 놓는다. */
	@GameTest
	public void filterMenuIsGhost(GameTestHelper helper) {
		BlockPos sorterPos = new BlockPos(2, 1, 2);
		helper.setBlock(sorterPos, ModLogistics.BELT_SORTER);
		BeltSorterBlockEntity sorter = helper.getBlockEntity(sorterPos, BeltSorterBlockEntity.class);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		FilterMenu menu = (FilterMenu) sorter.createMenu(1, player.getInventory(), player);

		menu.setCarried(new ItemStack(Items.IRON_INGOT, 5));
		menu.clicked(0, 0, ContainerInput.PICKUP, player);
		helper.assertTrue(sorter.filter().getItem(0).is(Items.IRON_INGOT) && sorter.filter().getItem(0).getCount() == 1, "견본이 놓이지 않음: " + sorter.filter().getItem(0));
		helper.assertValueEqual(menu.getCarried().getCount(), 5, "손에 든 아이템이 줄었음");
		menu.setCarried(ItemStack.EMPTY);
		menu.clicked(0, 0, ContainerInput.PICKUP, player);
		helper.assertTrue(sorter.filter().getItem(0).isEmpty(), "빈손 클릭이 칸을 안 비움");

		sorter.filter().setItem(2, new ItemStack(Items.COAL));
		menu.clicked(2, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(sorter.filter().getItem(2).isEmpty(), "Shift 클릭이 칸을 안 비움");

		// 인벤토리 칸(필터 9칸 뒤) Shift 클릭: 인벤토리는 그대로, 필터의 첫 빈 칸에 견본
		player.getInventory().setItem(9, new ItemStack(Items.GOLD_INGOT, 12));
		menu.clicked(FilterMenu.SORTER_SIZE, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(sorter.filter().getItem(0).is(Items.GOLD_INGOT), "인벤토리 Shift 클릭이 견본을 안 놓음");
		helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 12, "인벤토리 아이템이 줄었음");
		menu.clicked(FilterMenu.SORTER_SIZE, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(sorter.filter().getItem(1).isEmpty(), "같은 견본을 또 놓음");
		helper.succeed();
	}

	// ---- 필터 깔때기 ----

	/** 위 상자에서 맞는 것(철 주괴)만 빨아들여 옆 상자로 보내고, 안 맞는 석탄은 위 상자에 남는다. */
	@GameTest(maxTicks = 400)
	public void filterHopperPassesOnlyMatching(GameTestHelper helper) {
		BlockPos source = new BlockPos(2, 2, 2);
		BlockPos hopperPos = new BlockPos(2, 1, 2);
		BlockPos target = new BlockPos(3, 1, 2);
		helper.setBlock(source, Blocks.CHEST);
		helper.setBlock(target, Blocks.CHEST);
		helper.setBlock(hopperPos, ModLogistics.FILTER_HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
		FilterHopperBlockEntity hopper = helper.getBlockEntity(hopperPos, FilterHopperBlockEntity.class);
		hopper.filter().setItem(0, new ItemStack(Items.IRON_INGOT));
		helper.assertFalse(hopper.canPlaceItem(0, new ItemStack(Items.COAL)), "석탄을 받음");
		helper.assertTrue(hopper.canPlaceItem(0, new ItemStack(Items.IRON_INGOT)), "철 주괴를 안 받음");
		ChestBlockEntity chest = helper.getBlockEntity(source, ChestBlockEntity.class);
		chest.setItem(0, new ItemStack(Items.COAL, 3));
		chest.setItem(1, new ItemStack(Items.IRON_INGOT, 3));
		helper.succeedWhen(() -> {
			ChestBlockEntity out = helper.getBlockEntity(target, ChestBlockEntity.class);
			helper.assertValueEqual(out.countItem(Items.IRON_INGOT), 3, "옆 상자의 철 주괴");
			helper.assertValueEqual(out.countItem(Items.COAL), 0, "옆 상자에 석탄이 들어옴");
			helper.assertValueEqual(chest.countItem(Items.COAL), 3, "위 상자의 석탄");
			helper.assertTrue(hopper.isEmpty(), "깔때기에 남은 것이 있음");
		});
	}

	/** 필터가 비면 바닐라 깔때기처럼 다 지나간다. 레드스톤 신호로 잠그면 멈춘다. */
	@GameTest(maxTicks = 300)
	public void emptyFilterHopperPassesAllAndLocks(GameTestHelper helper) {
		BlockPos source = new BlockPos(2, 2, 2);
		BlockPos hopperPos = new BlockPos(2, 1, 2);
		BlockPos target = new BlockPos(3, 1, 2);
		helper.setBlock(source, Blocks.CHEST);
		helper.setBlock(target, Blocks.CHEST);
		BlockPos lock = new BlockPos(2, 1, 3);
		helper.setBlock(lock, Blocks.REDSTONE_BLOCK);
		helper.setBlock(hopperPos, ModLogistics.FILTER_HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
		helper.getBlockEntity(source, ChestBlockEntity.class).setItem(0, new ItemStack(Items.COAL, 2));
		helper.assertFalse(helper.getBlockState(hopperPos).getValue(HopperBlock.ENABLED), "신호를 받는 깔때기가 열려 있음");
		helper.runAfterDelay(30, () -> {
			helper.assertValueEqual(helper.getBlockEntity(target, ChestBlockEntity.class).countItem(Items.COAL), 0, "잠긴 깔때기가 옮김");
			helper.setBlock(lock, Blocks.AIR);
		});
		helper.succeedWhen(() -> {
			helper.assertValueEqual(helper.getBlockEntity(target, ChestBlockEntity.class).countItem(Items.COAL), 2, "열린 깔때기가 빈 필터로 옮긴 석탄");
		});
	}

	// ---- 설정 · 레시피 ----

	/** 설정 기본값은 상수와 같고, 레시피 셋이 읽힌다. */
	@GameTest
	public void configAndRecipes(GameTestHelper helper) {
		SanupcraftConfig defaults = SanupcraftConfig.defaults();
		helper.assertValueEqual(defaults.sorterTicksPerMove, BeltSorterBlockEntity.TICKS_PER_MOVE, "분류기 간격");
		helper.assertValueEqual(defaults.filterHopperTicksPerMove, FilterHopperBlockEntity.TICKS_PER_MOVE, "필터 깔때기 간격");
		for (String id : new String[] {"belt_sorter", "filter_hopper", "ore_marker"}) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Sanupcraft.id(id));
			helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(key).isPresent(), "레시피가 없음: " + id);
		}
		helper.succeed();
	}

	// ---- 도우미 ----

	/** 벨트 칸 위, 가운데에서 바라보는 쪽으로 {@code ahead}칸 앞에 아이템을 올린다. */
	private static void dropOnBelt(GameTestHelper helper, BlockPos belt, double ahead, ItemStack stack) {
		BlockPos abs = helper.absolutePos(belt);
		Direction facing = helper.getBlockState(belt).getValue(BeltBlock.FACING);
		double x = abs.getX() + 0.5 + facing.getStepX() * ahead;
		double z = abs.getZ() + 0.5 + facing.getStepZ() * ahead;
		helper.getLevel().addFreshEntity(new ItemEntity(helper.getLevel(), x, abs.getY() + BeltBlock.HEIGHT + 0.05, z, stack, 0.0, 0.0, 0.0));
	}
}
