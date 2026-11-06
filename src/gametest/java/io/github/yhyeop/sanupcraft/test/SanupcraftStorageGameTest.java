package io.github.yhyeop.sanupcraft.test;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.storage.DrawerBlock;
import io.github.yhyeop.sanupcraft.storage.DrawerBlockEntity;
import io.github.yhyeop.sanupcraft.storage.DrawerContents;
import io.github.yhyeop.sanupcraft.storage.ModStorage;
import io.github.yhyeop.sanupcraft.storage.StorageNetwork;
import io.github.yhyeop.sanupcraft.storage.TerminalBlockEntity;
import io.github.yhyeop.sanupcraft.storage.TerminalMenu;

/** M7 보관 · 제작 검사: 저장 서랍 (S1), 창고 네트워크 · 단말 · 제작 단말 (S2 · S3). */
public class SanupcraftStorageGameTest {
	// ---- S1 서랍 ----

	private static DrawerBlockEntity drawer(GameTestHelper helper, BlockPos pos, Block block) {
		helper.setBlock(pos, block.defaultBlockState().setValue(DrawerBlock.FACING, Direction.NORTH));
		return helper.getBlockEntity(pos, DrawerBlockEntity.class);
	}

	/** 손으로 넣고 하나 · 한 더미 꺼내기, 다른 아이템은 안 받음, 빠른 두 번 우클릭 = 인벤토리의 같은 것 전부. */
	@GameTest
	public void drawerInsertExtract(GameTestHelper helper) {
		DrawerBlockEntity drawer = drawer(helper, new BlockPos(1, 1, 1), ModStorage.IRON_DRAWER);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack held = new ItemStack(Items.COBBLESTONE, 40);
		helper.assertValueEqual(drawer.insertFromHand(player, held), 40, "넣은 수");
		helper.assertTrue(held.isEmpty(), "손에 남음");
		helper.assertValueEqual(drawer.insert(new ItemStack(Items.DIRT), 5, false), 0, "다른 아이템을 받음");
		helper.assertValueEqual(drawer.extract(1).getCount(), 1, "하나 꺼내기");
		helper.assertValueEqual(drawer.extract(64).getCount(), 39, "한 더미 꺼내기 (남은 만큼)");
		helper.assertTrue(drawer.template().isEmpty(), "비었는데 견본이 남음");

		// 두 번 우클릭: 인벤토리의 조약돌을 전부 (흙은 그대로)
		player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 64));
		player.getInventory().setItem(5, new ItemStack(Items.COBBLESTONE, 30));
		player.getInventory().setItem(6, new ItemStack(Items.DIRT, 10));
		drawer.insert(new ItemStack(Items.COBBLESTONE), 1, false);
		long now = helper.getLevel().getGameTime();
		helper.assertFalse(drawer.registerClick(player, now), "첫 클릭이 두 번으로 잡힘");
		helper.assertTrue(drawer.registerClick(player, now + 5), "빠른 두 번째 클릭을 못 잡음");
		helper.assertValueEqual(drawer.insertAllFrom(player.getInventory()), 94, "인벤토리에서 넣은 수");
		helper.assertValueEqual(drawer.count(), 95, "서랍 개수");
		helper.assertValueEqual(player.getInventory().countItem(Items.DIRT), 10, "흙이 들어감");
		helper.assertFalse(drawer.registerClick(player, now + 40), "늦은 클릭이 두 번으로 잡힘");
		helper.succeed();
	}

	/** 철 서랍 32더미, 업그레이드 하나에 ×2 (최대 2개), 넘침 버림은 가득 찬 뒤 버린다. 넘칠 업그레이드는 못 뺀다. */
	@GameTest
	public void drawerCapacityAndUpgrades(GameTestHelper helper) {
		DrawerBlockEntity drawer = drawer(helper, new BlockPos(1, 1, 1), ModStorage.IRON_DRAWER);
		ItemStack stone = new ItemStack(Items.COBBLESTONE);
		helper.assertValueEqual(drawer.capacityFor(stone), 32 * 64, "철 서랍 용량");
		helper.assertValueEqual(drawer.insert(stone, 3000, false), 2048, "넘게 받음");
		helper.assertFalse(drawer.canPlaceItem(DrawerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 1)), "가득 찼는데 깔때기로 받음");
		helper.assertTrue(drawer.addCapacityUpgrade() && drawer.addCapacityUpgrade(), "업그레이드를 못 끼움");
		helper.assertFalse(drawer.addCapacityUpgrade(), "세 번째 업그레이드가 들어감");
		helper.assertValueEqual(drawer.capacityFor(stone), 2048 * 4, "업그레이드 2개 용량");
		helper.assertValueEqual(drawer.capacityFor(new ItemStack(Items.ENDER_PEARL)), 32 * 16 * 4, "16개 더미 아이템 용량");
		drawer.insert(stone, 8192, false);
		helper.assertValueEqual(drawer.count(), 8192, "가득 채운 수");
		helper.assertTrue(drawer.removeUpgrade().isEmpty(), "넘치는데 업그레이드가 빠짐");
		helper.assertTrue(drawer.addVoidUpgrade(), "넘침 버림을 못 끼움");
		helper.assertValueEqual(drawer.insert(stone, 500, false), 500, "넘침 버림이 안 받음");
		helper.assertTrue(drawer.canPlaceItem(DrawerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 64)), "넘침 버림인데 깔때기를 거부");
		helper.assertValueEqual(drawer.count(), 8192, "버린 만큼 늘어남");
		helper.assertTrue(drawer.removeUpgrade().is(ModStorage.DRAWER_VOID_UPGRADE), "넘침 버림이 먼저 빠지지 않음");
		helper.assertValueEqual(drawer(helper, new BlockPos(3, 1, 1), ModStorage.STEEL_DRAWER).capacityFor(stone), 128 * 64, "강철 서랍 용량");
		helper.succeed();
	}

	/** 깔때기: 위에서 넣고, 아래 깔때기가 뽑아 간다. 개수가 맞는다 (사라지거나 늘지 않음). */
	@GameTest(maxTicks = 300)
	public void drawerHopperIO(GameTestHelper helper) {
		BlockPos top = new BlockPos(1, 3, 1);
		BlockPos drawerPos = new BlockPos(1, 2, 1);
		BlockPos bottom = new BlockPos(1, 1, 1);
		DrawerBlockEntity drawer = drawer(helper, drawerPos, ModStorage.IRON_DRAWER);
		drawer.insert(new ItemStack(Items.IRON_INGOT), 100, false);
		helper.setBlock(top, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.setBlock(bottom, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN).setValue(HopperBlock.ENABLED, true));
		helper.setBlock(bottom.below(), Blocks.BARRIER);
		HopperBlockEntity in = helper.getBlockEntity(top, HopperBlockEntity.class);
		in.setItem(0, new ItemStack(Items.IRON_INGOT, 6));
		in.setItem(1, new ItemStack(Items.GOLD_INGOT, 2));
		helper.succeedWhen(() -> {
			HopperBlockEntity out = helper.getBlockEntity(bottom, HopperBlockEntity.class);
			helper.assertValueEqual(in.countItem(Items.IRON_INGOT), 0, "위 깔때기의 철");
			helper.assertValueEqual(in.countItem(Items.GOLD_INGOT), 2, "다른 아이템(금)이 서랍에 들어감");
			int pulled = out.countItem(Items.IRON_INGOT);
			helper.assertTrue(pulled >= 10, "아래 깔때기가 덜 뽑음: " + pulled);
			helper.assertValueEqual(pulled + drawer.count(), 106, "합계");
		});
	}

	/** 부순 서랍의 아이템에 내용물 · 업그레이드가 남고, 다시 놓으면 돌아온다. 저장 · 읽기도 같다. */
	@GameTest
	public void drawerKeepsContents(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		DrawerBlockEntity drawer = drawer(helper, pos, ModStorage.STEEL_DRAWER);
		drawer.insert(new ItemStack(Items.REDSTONE), 5000, false);
		drawer.addCapacityUpgrade();
		var registries = helper.getLevel().registryAccess();
		BlockEntity copy = BlockEntity.loadStatic(drawer.getBlockPos(), drawer.getBlockState(), drawer.saveWithFullMetadata(registries), registries);
		helper.assertTrue(copy instanceof DrawerBlockEntity d && d.count() == 5000 && d.upgrades() == 1 && d.template().is(Items.REDSTONE), "저장 · 읽기가 다름");

		List<ItemStack> drops = Block.getDrops(drawer.getBlockState(), helper.getLevel(), helper.absolutePos(pos), drawer);
		helper.assertValueEqual(drops.size(), 1, "떨어진 수");
		DrawerContents contents = drops.get(0).get(ModStorage.DRAWER_CONTENTS);
		helper.assertTrue(contents != null && contents.count() == 5000 && contents.item().is(Items.REDSTONE) && contents.upgrades() == 1, "아이템에 내용물이 없음: " + contents);
		helper.assertFalse(drawer.accepts(drops.get(0)), "내용물 든 서랍을 서랍에 넣을 수 있음");

		BlockPos other = new BlockPos(3, 1, 1);
		DrawerBlockEntity placed = drawer(helper, other, ModStorage.STEEL_DRAWER);
		placed.applyComponentsFromItemStack(drops.get(0));
		helper.assertValueEqual(placed.count(), 5000, "다시 놓은 서랍 개수");
		helper.assertValueEqual(placed.upgrades(), 1, "다시 놓은 서랍 업그레이드");
		helper.succeed();
	}

	// ---- S2 · S3 창고 네트워크 · 단말 ----

	private static final BlockPos TERMINAL = new BlockPos(1, 1, 1);

	/** 단말 (1,1,1) + 관 (2~4,1,1). 관 남쪽(z=2)에 큰 상자(2~3) · 서랍(4), 북쪽(z=0)에 빈 상자(3). */
	private static TerminalBlockEntity network(GameTestHelper helper, Block terminalBlock) {
		helper.setBlock(TERMINAL, terminalBlock);
		for (int x = 2; x <= 4; x++) {
			helper.setBlock(new BlockPos(x, 1, 1), ModStorage.STORAGE_CABLE);
		}
		helper.setBlock(new BlockPos(2, 1, 2), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
		helper.setBlock(new BlockPos(3, 1, 2), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
		helper.setBlock(new BlockPos(3, 1, 0), Blocks.CHEST);
		helper.getBlockEntity(new BlockPos(3, 1, 2), ChestBlockEntity.class).setItem(4, new ItemStack(Items.DIRT, 10));
		drawer(helper, new BlockPos(4, 1, 2), ModStorage.IRON_DRAWER).insert(new ItemStack(Items.COBBLESTONE), 100, false);
		return helper.getBlockEntity(TERMINAL, TerminalBlockEntity.class);
	}

	/** 네트워크는 큰 상자(한 번만) · 서랍 · 빈 상자를 본다. 넣기는 같은 아이템이 든 곳부터, 꺼내기는 있는 만큼. */
	@GameTest
	public void networkSeesChestsAndDrawers(GameTestHelper helper) {
		TerminalBlockEntity terminal = network(helper, ModStorage.STORAGE_TERMINAL);
		StorageNetwork net = StorageNetwork.discover(helper.getLevel(), terminal.getBlockPos());
		helper.assertValueEqual(net.storeCount(), 3, "보관함 수 (큰 상자는 하나)");
		helper.assertValueEqual(net.count(new ItemStack(Items.DIRT)), 10L, "흙 (큰 상자를 두 번 셈?)");
		helper.assertValueEqual(net.count(new ItemStack(Items.COBBLESTONE)), 100L, "서랍의 조약돌");

		helper.assertTrue(net.insert(new ItemStack(Items.COBBLESTONE, 50)).isEmpty(), "조약돌이 다 안 들어감");
		DrawerBlockEntity drawer = helper.getBlockEntity(new BlockPos(4, 1, 2), DrawerBlockEntity.class);
		helper.assertValueEqual(drawer.count(), 150, "조약돌이 서랍으로 가지 않음");
		net.insert(new ItemStack(Items.DIRT, 5));
		ChestBlockEntity right = helper.getBlockEntity(new BlockPos(3, 1, 2), ChestBlockEntity.class);
		ChestBlockEntity lone = helper.getBlockEntity(new BlockPos(3, 1, 0), ChestBlockEntity.class);
		helper.assertValueEqual(right.getItem(4).getCount(), 15, "흙이 흙 칸에 합쳐지지 않음");
		helper.assertTrue(lone.isEmpty(), "빈 상자에 먼저 들어감");

		ItemStack got = net.extract(new ItemStack(Items.COBBLESTONE), 120);
		helper.assertValueEqual(got.getCount(), 120, "꺼낸 조약돌");
		helper.assertValueEqual(drawer.count(), 30, "서랍에 남은 조약돌");
		helper.assertTrue(net.extract(new ItemStack(Items.DIAMOND), 5).isEmpty(), "없는 것을 꺼냄");
		helper.succeed();
	}

	/** 관 수 한도 (설정): 한도 밖의 상자는 안 보인다. 캐시는 관이 바뀌면 다시 찾는다. */
	@GameTest
	public void networkNodeLimitAndCache(GameTestHelper helper) {
		TerminalBlockEntity terminal = network(helper, ModStorage.STORAGE_TERMINAL);
		SanupcraftConfig config = SanupcraftConfig.defaults();
		config.storageMaxNodes = 2;
		SanupcraftConfig.use(config);
		try {
			StorageNetwork small = StorageNetwork.discover(helper.getLevel(), terminal.getBlockPos());
			helper.assertValueEqual(small.count(new ItemStack(Items.COBBLESTONE)), 0L, "한도 밖 서랍이 보임");
		} finally {
			SanupcraftConfig.resetToDefaults();
		}
		helper.assertValueEqual(terminal.network().count(new ItemStack(Items.COBBLESTONE)), 100L, "캐시된 네트워크");
		helper.setBlock(new BlockPos(4, 1, 1), Blocks.AIR);
		helper.assertValueEqual(terminal.network().count(new ItemStack(Items.COBBLESTONE)), 0L, "관을 부숴도 캐시가 그대로");
		helper.succeed();
	}

	/** 단말 화면 (서버): 목록에서 한 더미 꺼내 들기 · 든 것 넣기 · 인벤토리 Shift 클릭으로 넣기 · 인벤토리로 꺼내기. */
	@GameTest
	public void terminalMenuMovesItems(GameTestHelper helper) {
		TerminalBlockEntity terminal = network(helper, ModStorage.STORAGE_TERMINAL);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		TerminalMenu menu = new TerminalMenu(1, player.getInventory(), terminal, p -> true);
		helper.assertValueEqual(terminal.users(), 1, "사용자 수");
		menu.handleAction(player, TerminalMenu.ACTION_TAKE_STACK, new ItemStack(Items.COBBLESTONE));
		helper.assertValueEqual(menu.getCarried().getCount(), 64, "든 조약돌");
		menu.handleAction(player, TerminalMenu.ACTION_INSERT_ONE, ItemStack.EMPTY);
		helper.assertValueEqual(menu.getCarried().getCount(), 63, "하나 넣은 뒤 든 것");
		menu.handleAction(player, TerminalMenu.ACTION_INSERT_CARRIED, ItemStack.EMPTY);
		helper.assertTrue(menu.getCarried().isEmpty(), "다 넣었는데 남음");
		helper.assertValueEqual(terminal.network().count(new ItemStack(Items.COBBLESTONE)), 100L, "조약돌 합계");

		player.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 20));
		int slot = menu.inventoryStart();
		menu.quickMoveStack(player, slot);
		helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "Shift 클릭이 넣지 않음");
		helper.assertValueEqual(terminal.network().count(new ItemStack(Items.IRON_INGOT)), 20L, "넣은 철");
		menu.handleAction(player, TerminalMenu.ACTION_TAKE_TO_INVENTORY, new ItemStack(Items.IRON_INGOT, 1));
		helper.assertValueEqual(player.getInventory().countItem(Items.IRON_INGOT), 20, "인벤토리로 꺼낸 철");
		helper.assertValueEqual(terminal.network().count(new ItemStack(Items.IRON_INGOT)), 0L, "네트워크에 남은 철");
		menu.removed(player);
		helper.assertValueEqual(terminal.users(), 0, "닫은 뒤 사용자 수");
		helper.succeed();
	}

	/** 제작 단말: JEI 옮기기(서버 쪽)로 판자 4개를 놓고, Shift 클릭으로 재료가 떨어질 때까지 작업대를 만든다 (빈 칸은 네트워크에서 다시 채움). */
	@GameTest
	public void craftingTerminalCraftsFromNetwork(GameTestHelper helper) {
		TerminalBlockEntity terminal = network(helper, ModStorage.CRAFTING_TERMINAL);
		terminal.network().insert(new ItemStack(Items.OAK_PLANKS, 9));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		TerminalMenu menu = new TerminalMenu(1, player.getInventory(), terminal, p -> true);
		List<ItemStack> planks = List.of(new ItemStack(Items.OAK_PLANKS));
		menu.fillGrid(player, List.of(planks, planks, List.of(), planks, planks, List.of(), List.of(), List.of(), List.of()));
		helper.assertTrue(menu.craftingView().result().getItem(0).is(Items.CRAFTING_TABLE), "결과가 작업대가 아님: " + menu.craftingView().result().getItem(0));
		helper.assertValueEqual(terminal.network().count(new ItemStack(Items.OAK_PLANKS)), 5L, "칸에 놓고 남은 판자");
		menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(player.getInventory().countItem(Items.CRAFTING_TABLE), 2, "만든 작업대");
		helper.assertValueEqual(terminal.network().count(new ItemStack(Items.OAK_PLANKS)), 0L, "네트워크 판자");
		// 남은 판자 1개는 칸에 있고, 닫으면 네트워크로 돌아간다
		menu.removed(player);
		helper.assertValueEqual(terminal.network().count(new ItemStack(Items.OAK_PLANKS)), 1L, "닫은 뒤 돌아온 판자");
		helper.assertTrue(menu.craftingView().grid().isEmpty(), "닫은 뒤 칸이 안 비었음");
		helper.succeed();
	}

	/** 단말은 증기가 없으면 안 열리고, 잠그면 주인만 쓴다. 증기 · 잠금은 저장된다. */
	@GameTest
	public void terminalNeedsSteamAndLock(GameTestHelper helper) {
		helper.setBlock(TERMINAL, ModStorage.STORAGE_TERMINAL);
		TerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL, TerminalBlockEntity.class);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertFalse(terminal.hasPower(), "증기 없이 켜짐");
		helper.assertFalse(terminal.tryOpen(owner, terminal), "증기 없이 열림");
		helper.assertValueEqual(terminal.acceptSteam(5000), TerminalBlockEntity.STEAM_CAPACITY, "받은 증기");
		helper.assertTrue(terminal.hasPower(), "증기가 있는데 꺼짐");
		terminal.lock().setOwner(owner);
		helper.assertTrue(terminal.lock().toggle(owner), "주인이 못 잠금");
		Player other = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertFalse(terminal.lock().canUse(other), "남이 잠긴 단말을 씀");
		helper.assertFalse(terminal.lock().toggle(other), "남이 잠금을 풂");
		var registries = helper.getLevel().registryAccess();
		BlockEntity copy = BlockEntity.loadStatic(terminal.getBlockPos(), terminal.getBlockState(), terminal.saveWithFullMetadata(registries), registries);
		helper.assertTrue(copy instanceof TerminalBlockEntity t && t.steamStored() == TerminalBlockEntity.STEAM_CAPACITY && t.lock().isLocked()
				&& t.lock().isOwner(owner), "증기 · 잠금이 저장되지 않음");
		helper.succeed();
	}
}
