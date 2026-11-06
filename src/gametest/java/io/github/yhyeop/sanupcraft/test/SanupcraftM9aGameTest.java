package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.ChainsawItem;
import io.github.yhyeop.sanupcraft.gear.Jetpack;
import io.github.yhyeop.sanupcraft.gear.ProductionGear;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteamDrillItem;
import io.github.yhyeop.sanupcraft.gear.ToolMining;
import io.github.yhyeop.sanupcraft.power.ModPower;
import io.github.yhyeop.sanupcraft.power.SteamNetwork;
import io.github.yhyeop.sanupcraft.production.CropHarvesterBlockEntity;
import io.github.yhyeop.sanupcraft.production.ModProduction;
import io.github.yhyeop.sanupcraft.production.ProductionBlock;
import io.github.yhyeop.sanupcraft.production.SteamWorkerBlockEntity;
import io.github.yhyeop.sanupcraft.production.TreeFelling;
import io.github.yhyeop.sanupcraft.production.TreeFellerBlockEntity;

/** 확장 M9a 검사: 작물 수확기 · 벌목기(P1), 증기 제트팩(P3), 증기 드릴 · 전기톱(P4). */
public class SanupcraftM9aGameTest {
	// ---- 도우미 ----

	private static BlockState wheat(int age) {
		return Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, age);
	}

	private static int count(SteamWorkerBlockEntity entity, net.minecraft.world.item.Item item) {
		int sum = 0;
		for (int i = 0; i < entity.getContainerSize(); i++) {
			ItemStack stack = entity.getItem(i);
			if (stack.is(item)) {
				sum += stack.getCount();
			}
		}
		return sum;
	}

	private static void placeHarvester(GameTestHelper helper, BlockPos pos, Direction facing) {
		helper.setBlock(pos, ModProduction.CROP_HARVESTER.defaultBlockState().setValue(ProductionBlock.FACING, facing));
	}

	/** 작은 참나무: 원목 3개 + 자연 잎 (가운데 열은 원목). 바닥 y=0에 흙. 원목 맨 아래 칸을 돌려준다. */
	private static BlockPos buildTree(GameTestHelper helper, int x, int z) {
		helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT);
		BlockState leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				helper.setBlock(new BlockPos(x + dx, 3, z + dz), leaf);
				helper.setBlock(new BlockPos(x + dx, 4, z + dz), leaf);
			}
		}
		for (int y = 1; y <= 3; y++) {
			helper.setBlock(new BlockPos(x, y, z), Blocks.OAK_LOG);
		}
		return new BlockPos(x, 1, z);
	}

	private static int remaining(GameTestHelper helper, int x, int z, boolean logs) {
		int found = 0;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int y = 1; y <= 4; y++) {
					BlockState state = helper.getBlockState(new BlockPos(x + dx, y, z + dz));
					if (logs ? state.is(Blocks.OAK_LOG) : state.is(Blocks.OAK_LEAVES)) {
						found++;
					}
				}
			}
		}
		return found;
	}

	// ---- 작물 수확기 ----

	/** 다 자란 밀은 거두고 씨앗 하나로 나이 0으로 다시 심는다. 덜 자란 밀은 건드리지 않는다. 증기 3을 쓴다. */
	@GameTest
	public void harvesterHarvestsMatureWheatAndReplants(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 3);
		placeHarvester(helper, pos, Direction.EAST);
		CropHarvesterBlockEntity harvester = helper.getBlockEntity(pos, CropHarvesterBlockEntity.class);
		BlockPos ripe = new BlockPos(3, 1, 3);
		BlockPos young = new BlockPos(4, 1, 3);
		helper.setBlock(new BlockPos(3, 0, 3), Blocks.FARMLAND);
		helper.setBlock(new BlockPos(4, 0, 3), Blocks.FARMLAND);
		helper.setBlock(ripe, wheat(7));
		helper.setBlock(young, wheat(3));

		helper.assertFalse(harvester.harvest(helper.getLevel(), helper.absolutePos(ripe)), "증기가 없는데 거둠");
		helper.assertValueEqual(harvester.acceptSteam(100), 100, "받은 증기");
		helper.assertTrue(harvester.harvest(helper.getLevel(), helper.absolutePos(ripe)), "다 자란 밀을 못 거둠");
		helper.assertFalse(harvester.harvest(helper.getLevel(), helper.absolutePos(young)), "덜 자란 밀을 거둠");
		helper.assertBlockProperty(ripe, CropBlock.AGE, 0);
		helper.assertBlockProperty(young, CropBlock.AGE, 3);
		helper.assertTrue(count(harvester, Items.WHEAT) >= 1, "밀을 모으지 못함");
		helper.assertValueEqual(harvester.steamStored(), 97, "거두는 데 쓴 증기");
		helper.succeed();
	}

	/** 네더 사마귀 · 달콤한 열매 · 코코아도 거둔다. 열매 덤불은 씨앗 없이 나이 1로 돌아간다. */
	@GameTest
	public void harvesterHandlesOtherCrops(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 3);
		placeHarvester(helper, pos, Direction.EAST);
		CropHarvesterBlockEntity harvester = helper.getBlockEntity(pos, CropHarvesterBlockEntity.class);
		harvester.acceptSteam(100);
		BlockPos wart = new BlockPos(3, 1, 2);
		BlockPos berry = new BlockPos(3, 1, 3);
		BlockPos cocoa = new BlockPos(3, 1, 4);
		helper.setBlock(new BlockPos(3, 0, 2), Blocks.SOUL_SAND);
		helper.setBlock(wart, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE));
		helper.setBlock(new BlockPos(3, 0, 3), Blocks.GRASS_BLOCK);
		helper.setBlock(berry, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, SweetBerryBushBlock.MAX_AGE));
		helper.setBlock(cocoa.east(), Blocks.JUNGLE_LOG);
		helper.setBlock(cocoa, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE, CocoaBlock.MAX_AGE).setValue(CocoaBlock.FACING, Direction.EAST));

		helper.assertTrue(harvester.harvest(helper.getLevel(), helper.absolutePos(wart)), "네더 사마귀를 못 거둠");
		helper.assertTrue(harvester.harvest(helper.getLevel(), helper.absolutePos(berry)), "열매 덤불을 못 거둠");
		helper.assertTrue(harvester.harvest(helper.getLevel(), helper.absolutePos(cocoa)), "코코아를 못 거둠");
		helper.assertBlockProperty(wart, NetherWartBlock.AGE, 0);
		helper.assertBlockProperty(berry, SweetBerryBushBlock.AGE, 1);
		helper.assertBlockProperty(cocoa, CocoaBlock.AGE, 0);
		helper.assertTrue(count(harvester, Items.NETHER_WART) >= 1, "네더 사마귀를 모으지 못함");
		helper.assertTrue(count(harvester, Items.SWEET_BERRIES) >= 1, "달콤한 열매를 모으지 못함");
		helper.assertTrue(count(harvester, Items.COCOA_BEANS) >= 1, "코코아 콩을 모으지 못함");
		helper.succeed();
	}

	/** 칸 9개가 꽉 차면 거둔 것은 사라지지 않고 쌓여 있다가 (pending), 자리가 나면 들어간다. */
	@GameTest
	public void harvesterKeepsPendingWhenFull(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 3);
		placeHarvester(helper, pos, Direction.EAST);
		CropHarvesterBlockEntity harvester = helper.getBlockEntity(pos, CropHarvesterBlockEntity.class);
		harvester.acceptSteam(100);
		Block[] fillers = {Blocks.STONE, Blocks.DIRT, Blocks.SAND, Blocks.GRAVEL, Blocks.COBBLESTONE, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS,
				Blocks.BIRCH_PLANKS, Blocks.GLASS};
		for (int i = 0; i < fillers.length; i++) {
			harvester.setItem(i, new ItemStack(fillers[i].asItem(), 64));
		}
		BlockPos ripe = new BlockPos(3, 1, 3);
		helper.setBlock(new BlockPos(3, 0, 3), Blocks.FARMLAND);
		helper.setBlock(ripe, wheat(7));
		helper.assertTrue(harvester.harvest(helper.getLevel(), helper.absolutePos(ripe)), "거두지 못함");
		helper.assertTrue(harvester.hasPending(), "넘친 것이 쌓이지 않음");
		harvester.setItem(0, ItemStack.EMPTY);
		harvester.setItem(1, ItemStack.EMPTY);
		harvester.serverTick(helper.getLevel(), helper.absolutePos(pos), harvester.getBlockState());
		helper.assertFalse(harvester.hasPending(), "자리가 났는데 쌓인 것이 그대로");
		helper.assertTrue(count(harvester, Items.WHEAT) >= 1, "쌓인 밀이 칸에 안 들어감");
		helper.succeed();
	}

	/** 서버 틱이 알아서 앞 9x9를 훑어 다 자란 것만 거둔다. 아래 상자로 내보낸다. */
	@GameTest(environment = "sanupcraft-test:alone", maxTicks = 400)
	public void harvesterScansFieldAndOutputsBelow(GameTestHelper helper) {
		BlockPos pos = new BlockPos(0, 2, 4);
		placeHarvester(helper, pos, Direction.EAST);
		CropHarvesterBlockEntity harvester = helper.getBlockEntity(pos, CropHarvesterBlockEntity.class);
		harvester.acceptSteam(CropHarvesterBlockEntity.STEAM_CAPACITY);
		helper.setBlock(pos.below(), Blocks.CHEST);
		for (int x = 1; x <= 3; x++) {
			for (int z = 3; z <= 5; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND);
				helper.setBlock(new BlockPos(x, 2, z), x == 3 ? wheat(2) : wheat(7));
			}
		}
		helper.succeedWhen(() -> {
			ChestBlockEntity chest = helper.getBlockEntity(pos.below(), ChestBlockEntity.class);
			int wheat = 0;
			for (int i = 0; i < chest.getContainerSize(); i++) {
				if (chest.getItem(i).is(Items.WHEAT)) {
					wheat += chest.getItem(i).getCount();
				}
			}
			helper.assertTrue(wheat >= 6, "상자에 밀이 모자람: " + wheat);
			for (int z = 3; z <= 5; z++) {
				// 시험이 몇백 틱 걸려 무작위 틱으로 자랄 수 있다 → 정확한 단계 대신: 다시 심은 칸은 다 자라지 않은 밀, 어린 칸은 거두지 않음(2 이상 그대로)
				int replanted = helper.getBlockState(new BlockPos(1, 2, z)).getValue(CropBlock.AGE);
				helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, z)).is(Blocks.WHEAT) && replanted < CropBlock.MAX_AGE, "다시 심지 않음: " + replanted);
				helper.assertTrue(helper.getBlockState(new BlockPos(3, 2, z)).getValue(CropBlock.AGE) >= 2, "어린 밀을 거둠");
			}
		});
	}

	/** 파이프로 이으면 수확기 · 벌목기가 증기망의 소비자로 잡힌다. */
	@GameTest
	public void productionBlocksJoinSteamNetwork(GameTestHelper helper) {
		BlockPos boiler = new BlockPos(1, 1, 1);
		helper.setBlock(boiler, ModPower.BOILER);
		helper.setBlock(new BlockPos(2, 1, 1), ModPower.STEAM_PIPE);
		helper.setBlock(new BlockPos(3, 1, 1), ModPower.STEAM_PIPE);
		placeHarvester(helper, new BlockPos(4, 1, 1), Direction.EAST);
		helper.setBlock(new BlockPos(3, 1, 2), ModProduction.TREE_FELLER);
		var consumers = SteamNetwork.findConsumers(helper.getLevel(), helper.absolutePos(boiler));
		helper.assertTrue(consumers.contains(helper.getBlockEntity(new BlockPos(4, 1, 1), CropHarvesterBlockEntity.class)), "수확기가 증기망에 없음");
		helper.assertTrue(consumers.contains(helper.getBlockEntity(new BlockPos(3, 1, 2), TreeFellerBlockEntity.class)), "벌목기가 증기망에 없음");
		helper.succeed();
	}

	// ---- 벌목기 ----

	/** 앞의 작은 나무를 원목 · 잎까지 베고, 칸의 묘목 하나를 원목 자리에 심는다. 원목은 칸에 모인다. */
	@GameTest(environment = "sanupcraft-test:alone", maxTicks = 300)
	public void treeFellerFellsSmallTreeAndReplants(GameTestHelper helper) {
		BlockPos base = buildTree(helper, 4, 3);
		BlockPos pos = new BlockPos(2, 1, 3);
		helper.setBlock(pos, ModProduction.TREE_FELLER.defaultBlockState().setValue(ProductionBlock.FACING, Direction.EAST));
		TreeFellerBlockEntity feller = helper.getBlockEntity(pos, TreeFellerBlockEntity.class);
		feller.setItem(0, new ItemStack(Items.OAK_SAPLING, 2));
		feller.acceptSteam(TreeFellerBlockEntity.STEAM_CAPACITY);
		helper.succeedWhen(() -> {
			helper.assertTrue(remaining(helper, 4, 3, true) == 0, "원목이 남음");
			helper.assertTrue(remaining(helper, 4, 3, false) == 0, "잎이 남음");
			helper.assertBlockPresent(Blocks.OAK_SAPLING, base);
			helper.assertValueEqual(count(feller, Items.OAK_LOG), 3, "모은 원목");
			helper.assertValueEqual(count(feller, Items.OAK_SAPLING) >= 1, true, "묘목을 하나 심고 남아 있어야 함");
			helper.assertTrue(feller.steamStored() < TreeFellerBlockEntity.STEAM_CAPACITY, "증기를 안 씀");
		});
	}

	/** 자연 잎이 안 붙은 원목 더미(통나무집 같은)는 나무가 아니라서 베지 않는다. */
	@GameTest
	public void treeFellerIgnoresLogPiles(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 3);
		helper.setBlock(pos, ModProduction.TREE_FELLER.defaultBlockState().setValue(ProductionBlock.FACING, Direction.EAST));
		TreeFellerBlockEntity feller = helper.getBlockEntity(pos, TreeFellerBlockEntity.class);
		feller.acceptSteam(200);
		for (int y = 1; y <= 3; y++) {
			helper.setBlock(new BlockPos(3, y, 3), Blocks.OAK_LOG);
		}
		// 플레이어가 놓은 잎(persistent)도 나무로 치지 않는다
		helper.setBlock(new BlockPos(3, 4, 3), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
		helper.assertFalse(feller.startFelling(helper.getLevel(), helper.absolutePos(pos)), "원목 더미를 나무로 봄");
		helper.assertBlockPresent(Blocks.OAK_LOG, new BlockPos(3, 1, 3));
		helper.succeed();
	}

	/** 나무 찾기: 원목 26방향 + 자연 잎, 위쪽 원목부터 정렬, 한도(256) 안. */
	@GameTest
	public void treeScanFindsLogsAndLeaves(GameTestHelper helper) {
		BlockPos base = buildTree(helper, 4, 3);
		// 대각선으로 이어진 가지 하나
		helper.setBlock(new BlockPos(5, 4, 4), Blocks.OAK_LOG);
		TreeFelling.Tree tree = TreeFelling.scan(helper.getLevel(), java.util.List.of(helper.absolutePos(base)), TreeFelling.MAX_BLOCKS);
		helper.assertTrue(tree.isTree(), "나무로 못 봄");
		helper.assertValueEqual(tree.logs().size(), 4, "원목 수 (대각선 가지 포함)");
		helper.assertTrue(tree.leaves().size() >= 10, "잎이 모자람: " + tree.leaves().size());
		helper.assertTrue(tree.size() <= TreeFelling.MAX_BLOCKS, "한도 초과");
		helper.assertTrue(tree.fellOrder().get(0).getY() >= tree.fellOrder().get(1).getY(), "위쪽 원목부터가 아님");
		helper.assertValueEqual(tree.base(helper.absolutePos(base)).getY(), helper.absolutePos(base).getY(), "맨 아래 원목");
		helper.succeed();
	}

	// ---- 증기 드릴 ----

	private static ItemStack chargedDrill() {
		ItemStack drill = new ItemStack(ProductionGear.STEAM_DRILL);
		SteamCharge.set(drill, SteamDrillItem.CAPACITY);
		return drill;
	}

	/** 드릴은 내구도가 없고 증기만 담는다 (2000). 다이아 등급: 흑요석도 캐고, 흙(삽)도 캔다. 나무판자(도끼)는 못 캔다. */
	@GameTest
	public void drillTierAndSteamGate(GameTestHelper helper) {
		ItemStack drill = chargedDrill();
		helper.assertFalse(drill.isDamageableItem(), "드릴에 내구도가 있음");
		helper.assertValueEqual(SteamCharge.capacity(drill), 2000, "드릴 증기 용량");
		helper.assertTrue(drill.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()), "돌");
		helper.assertTrue(drill.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "흑요석 (다이아 등급)");
		helper.assertTrue(drill.isCorrectToolForDrops(Blocks.DIRT.defaultBlockState()), "흙 (삽)");
		helper.assertFalse(drill.isCorrectToolForDrops(Blocks.OAK_PLANKS.defaultBlockState()), "판자는 도끼 몫");
		helper.assertValueEqual(drill.getDestroySpeed(Blocks.STONE.defaultBlockState()), 8.0F, "충전된 드릴 속도");
		// 증기가 한 블록치(10)보다 적으면 맨손
		SteamCharge.set(drill, 9);
		helper.assertValueEqual(drill.getDestroySpeed(Blocks.STONE.defaultBlockState()), 1.0F, "증기 없는 드릴 속도");
		helper.assertFalse(drill.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()), "증기 없는데 드롭을 받음");
		helper.succeed();
	}

	/** 웅크리고 우클릭: 1x1 ↔ 3x3 모드가 바뀌고 아이템에 저장된다. 웅크리지 않으면 아무 일도 없다. */
	@GameTest
	public void drillModeTogglesWithSneakUse(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		ItemStack drill = chargedDrill();
		player.setItemInHand(InteractionHand.MAIN_HAND, drill);
		helper.assertFalse(SteamDrillItem.isWide(drill), "처음부터 3x3");
		helper.assertTrue(drill.use(helper.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "웅크리지 않았는데 반응");
		player.setShiftKeyDown(true);
		drill.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(SteamDrillItem.isWide(drill), "3x3로 안 바뀜");
		drill.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertFalse(SteamDrillItem.isWide(drill), "1x1로 안 돌아옴");
		helper.succeed();
	}

	/** 3x3 모드로 돌 하나를 캐면 바라본 면의 3x3 아홉 칸이 다 캐지고 증기 90을 쓴다. 웅크리고 캐면 한 칸만. */
	@GameTest
	public void drillWideModeBreaksNineAndUsesSteam(GameTestHelper helper) {
		BlockPos center = new BlockPos(3, 3, 3);
		for (BlockPos pos : plane(center)) {
			helper.setBlock(pos, Blocks.STONE);
		}
		helper.setBlock(center.north(), Blocks.STONE);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		ItemStack drill = chargedDrill();
		SteamDrillItem.setWide(drill, true);
		player.setItemSlot(EquipmentSlot.MAINHAND, drill);
		// 북쪽(-z)을 보고 있으면 캔 면은 남쪽 면: 같은 z 평면의 3x3
		player.setYRot(180.0F);
		player.setXRot(0.0F);
		helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(center)), "가운데를 못 캠");
		for (BlockPos pos : plane(center)) {
			helper.assertBlockPresent(Blocks.AIR, pos);
		}
		helper.assertBlockPresent(Blocks.STONE, center.north());
		helper.assertValueEqual(SteamCharge.get(player.getMainHandItem()), SteamDrillItem.CAPACITY - 90, "쓴 증기 (9칸 x 10)");
		helper.assertFalse(player.getMainHandItem().isDamaged(), "내구도를 씀");
		helper.succeed();
	}

	/** 3x3 모드여도 웅크리고 캐면 한 칸. 1x1 모드도 한 칸. 증기는 한 칸치만. */
	@GameTest
	public void drillSneakAndSingleModeBreakOne(GameTestHelper helper) {
		BlockPos center = new BlockPos(3, 3, 3);
		for (BlockPos pos : plane(center)) {
			helper.setBlock(pos, Blocks.STONE);
		}
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		ItemStack drill = chargedDrill();
		SteamDrillItem.setWide(drill, true);
		player.setItemSlot(EquipmentSlot.MAINHAND, drill);
		player.setYRot(180.0F);
		player.setXRot(0.0F);
		player.setShiftKeyDown(true);
		player.gameMode.destroyBlock(helper.absolutePos(center));
		helper.assertBlockPresent(Blocks.STONE, center.east());
		helper.assertValueEqual(SteamCharge.get(player.getMainHandItem()), SteamDrillItem.CAPACITY - 10, "웅크리고 캘 때 쓴 증기");
		player.setShiftKeyDown(false);
		SteamDrillItem.setWide(player.getMainHandItem(), false);
		player.gameMode.destroyBlock(helper.absolutePos(center.east()));
		helper.assertBlockPresent(Blocks.STONE, center.west());
		helper.assertValueEqual(SteamCharge.get(player.getMainHandItem()), SteamDrillItem.CAPACITY - 20, "1x1 모드에서 쓴 증기");
		helper.succeed();
	}

	/** 증기가 모자라면 3x3 도중에 멈춘다 (증기 30 = 3칸). */
	@GameTest
	public void drillStopsWhenSteamRunsOut(GameTestHelper helper) {
		BlockPos center = new BlockPos(3, 3, 3);
		for (BlockPos pos : plane(center)) {
			helper.setBlock(pos, Blocks.STONE);
		}
		helper.setBlock(center, Blocks.STONE);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		ItemStack drill = new ItemStack(ProductionGear.STEAM_DRILL);
		SteamCharge.set(drill, 30);
		SteamDrillItem.setWide(drill, true);
		player.setItemSlot(EquipmentSlot.MAINHAND, drill);
		int extra = ToolMining.breakAround(helper.getLevel(), player, helper.absolutePos(center), Blocks.STONE.defaultBlockState(), Direction.SOUTH);
		helper.assertValueEqual(extra, 3, "증기 30으로 캔 블록 수");
		helper.assertValueEqual(SteamCharge.get(player.getMainHandItem()), 0, "남은 증기");
		helper.succeed();
	}

	// ---- 전기톱 ----

	private static ItemStack chargedSaw() {
		ItemStack saw = new ItemStack(ProductionGear.CHAINSAW);
		SteamCharge.set(saw, ChainsawItem.CAPACITY);
		return saw;
	}

	/** 원목 하나를 캐면 이어진 나무가 통째로 쓰러진다: 원목 3 x 10 + 잎 x 2 만큼 증기. */
	@GameTest
	public void chainsawFellsWholeTree(GameTestHelper helper) {
		BlockPos base = buildTree(helper, 4, 3);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemSlot(EquipmentSlot.MAINHAND, chargedSaw());
		helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(base)), "원목을 못 캠");
		helper.assertValueEqual(remaining(helper, 4, 3, true), 0, "남은 원목");
		helper.assertValueEqual(remaining(helper, 4, 3, false), 0, "남은 잎");
		int steam = SteamCharge.get(player.getMainHandItem());
		int used = ChainsawItem.CAPACITY - steam;
		helper.assertTrue(used >= 3 * 10 + 10 * 2 && used <= 3 * 10 + 18 * 2, "증기 사용량이 이상함: " + used);
		helper.succeed();
	}

	/** 웅크리고 캐면 원목 하나만. 자연 잎이 없는 원목 더미도 하나만. */
	@GameTest
	public void chainsawSneakAndLogPileBreakOne(GameTestHelper helper) {
		BlockPos base = buildTree(helper, 4, 3);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemSlot(EquipmentSlot.MAINHAND, chargedSaw());
		player.setShiftKeyDown(true);
		player.gameMode.destroyBlock(helper.absolutePos(base));
		helper.assertValueEqual(remaining(helper, 4, 3, true), 2, "웅크렸는데 나무가 쓰러짐");
		helper.assertValueEqual(SteamCharge.get(player.getMainHandItem()), ChainsawItem.CAPACITY - 10, "원목 하나의 증기");

		player.setShiftKeyDown(false);
		for (int y = 1; y <= 3; y++) {
			helper.setBlock(new BlockPos(1, y, 6), Blocks.SPRUCE_LOG);
		}
		player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(1, 1, 6)));
		helper.assertBlockPresent(Blocks.SPRUCE_LOG, new BlockPos(1, 2, 6));
		helper.assertBlockPresent(Blocks.SPRUCE_LOG, new BlockPos(1, 3, 6));
		helper.succeed();
	}

	/** 전기톱도 증기가 모자라면 맨손처럼: 원목이 필요 없는 블록이라 드롭은 나오지만 속도는 1배. 증기 용량 2000. */
	@GameTest
	public void chainsawIsSteamPoweredAxe(GameTestHelper helper) {
		ItemStack saw = chargedSaw();
		helper.assertFalse(saw.isDamageableItem(), "전기톱에 내구도가 있음");
		helper.assertValueEqual(SteamCharge.capacity(saw), 2000, "전기톱 증기 용량");
		float full = saw.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState());
		helper.assertTrue(full > 4.0F, "충전된 전기톱 속도 " + full);
		SteamCharge.set(saw, 0);
		helper.assertValueEqual(saw.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()), 1.0F, "증기 없는 전기톱 속도");
		helper.succeed();
	}

	// ---- 제트팩 ----

	/** 서버 처리: 제트팩 증기를 틱당 5씩 쓰고 위 속도를 올리며, 낙하 거리를 지우고 속도는 상한을 넘지 않는다. */
	@GameTest
	public void jetpackThrustLiftsAndDrainsSteam(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		ItemStack pack = new ItemStack(ProductionGear.STEAM_JETPACK);
		SteamCharge.set(pack, 100);
		player.setItemSlot(EquipmentSlot.CHEST, pack);
		player.fallDistance = 8.0;
		player.setDeltaMovement(Vec3.ZERO);
		helper.assertTrue(Jetpack.thrust(player), "떠오르지 못함");
		helper.assertValueEqual(SteamCharge.get(player.getItemBySlot(EquipmentSlot.CHEST)), 95, "한 번에 쓴 증기");
		helper.assertValueEqual(player.getDeltaMovement().y, Jetpack.liftedSpeed(0.0), "위 속도");
		helper.assertValueEqual(player.fallDistance, 0.0, "낙하 거리");
		for (int i = 0; i < 10; i++) {
			Jetpack.thrust(player);
		}
		helper.assertTrue(player.getDeltaMovement().y <= Jetpack.MAX_RISE + 1.0E-9, "속도 상한을 넘김: " + player.getDeltaMovement().y);
		helper.assertValueEqual(player.getDeltaMovement().y, Jetpack.MAX_RISE, "상한까지는 올라가야 함");
		helper.assertValueEqual(SteamCharge.get(player.getItemBySlot(EquipmentSlot.CHEST)), 100 - 11 * 5, "누적 증기");
		helper.succeed();
	}

	/** 증기가 없거나 제트팩이 없으면 아무 일도 안 한다. 창작 모드는 증기를 안 쓴다. 날 때 · 겉날개 중에는 안 된다. */
	@GameTest
	public void jetpackNeedsSteamAndPack(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		helper.assertFalse(Jetpack.thrust(player), "제트팩이 없는데 뜸");
		ItemStack pack = new ItemStack(ProductionGear.STEAM_JETPACK);
		player.setItemSlot(EquipmentSlot.CHEST, pack);
		helper.assertFalse(Jetpack.thrust(player), "증기가 없는데 뜸");
		helper.assertValueEqual(player.getDeltaMovement().y, 0.0, "증기가 없는데 속도가 변함");
		SteamCharge.set(pack, 4);
		helper.assertFalse(Jetpack.thrust(player), "증기 4 (틱당 5 미만)로 뜸");
		SteamCharge.set(pack, 50);
		player.getAbilities().flying = true;
		helper.assertFalse(Jetpack.thrust(player), "나는 중에 제트팩이 작동");
		player.getAbilities().flying = false;

		ServerPlayer creative = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
		ItemStack cpack = new ItemStack(ProductionGear.STEAM_JETPACK);
		SteamCharge.set(cpack, 50);
		creative.setItemSlot(EquipmentSlot.CHEST, cpack);
		helper.assertTrue(Jetpack.thrust(creative), "창작 모드에서 안 뜸");
		helper.assertValueEqual(SteamCharge.get(creative.getItemBySlot(EquipmentSlot.CHEST)), 50, "창작 모드가 증기를 씀");
		helper.succeed();
	}

	/** 패킷 수신기가 등록돼 있고, 속도 식은 내려가는 중이면 먼저 줄여 준다. */
	@GameTest
	public void jetpackPacketIsRegistered(GameTestHelper helper) {
		helper.assertTrue(ServerPlayNetworking.getGlobalReceivers().contains(Jetpack.ThrustPayload.TYPE.id()), "제트팩 패킷 수신기가 없음");
		helper.assertValueEqual(Jetpack.liftedSpeed(-1.0), -0.5 + Jetpack.ACCEL, "내려가는 중 속도");
		helper.assertValueEqual(Jetpack.liftedSpeed(5.0), Jetpack.MAX_RISE, "상한");
		helper.assertTrue(new ItemStack(ProductionGear.STEAM_JETPACK).get(net.minecraft.core.component.DataComponents.EQUIPPABLE).slot() == EquipmentSlot.CHEST, "가슴 칸 장비가 아님");
		helper.succeed();
	}

	// ---- 설정 · 레시피 ----

	/** 설정 값을 바꾸면 따라간다: 드릴 블록당 증기. */
	@GameTest
	public void configControlsDrillCost(GameTestHelper helper) {
		try {
			SanupcraftConfig config = SanupcraftConfig.defaults();
			helper.assertValueEqual(config.drillSteamPerBlock, 10, "기본 드릴 증기");
			helper.assertValueEqual(config.chainsawSteamPerBlock, 10, "기본 전기톱 증기");
			helper.assertValueEqual(config.jetpackSteamPerTick, 5, "기본 제트팩 증기");
			config.drillSteamPerBlock = 25;
			SanupcraftConfig.use(config);
			ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
			player.setItemSlot(EquipmentSlot.MAINHAND, chargedDrill());
			BlockPos pos = new BlockPos(3, 2, 3);
			helper.setBlock(pos, Blocks.STONE);
			player.gameMode.destroyBlock(helper.absolutePos(pos));
			helper.assertValueEqual(SteamCharge.get(player.getMainHandItem()), SteamDrillItem.CAPACITY - 25, "설정한 증기");
		} finally {
			SanupcraftConfig.resetToDefaults();
		}
		helper.succeed();
	}

	/** 레시피가 있고, 아이템 · 블록이 등록돼 있다. */
	@GameTest
	public void recipesAndRegistrationExist(GameTestHelper helper) {
		var recipes = helper.getLevel().getServer().getRecipeManager();
		for (String id : new String[] {"crop_harvester", "tree_feller", "steam_drill", "chainsaw", "steam_jetpack"}) {
			helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Sanupcraft.id(id))).isPresent(), "레시피가 없음: " + id);
		}
		helper.assertTrue(new ItemStack(ProductionGear.STEAM_DRILL).is(ProductionGear.STEAM_DRILL), "드릴");
		helper.assertTrue(SteamCharge.holdsSteam(new ItemStack(ProductionGear.CHAINSAW)), "전기톱이 증기 아이템이 아님");
		helper.assertTrue(SteamCharge.holdsSteam(new ItemStack(ProductionGear.STEAM_JETPACK)), "제트팩이 증기 아이템이 아님");
		helper.assertTrue(new ItemStack(Items.OAK_SAPLING).is(ItemTags.SAPLINGS), "묘목 태그");
		helper.succeed();
	}

	private static java.util.List<BlockPos> plane(BlockPos center) {
		java.util.List<BlockPos> list = new java.util.ArrayList<>(io.github.yhyeop.sanupcraft.gear.AreaMining.plane(center, Direction.SOUTH));
		list.add(center);
		return list;
	}
}
