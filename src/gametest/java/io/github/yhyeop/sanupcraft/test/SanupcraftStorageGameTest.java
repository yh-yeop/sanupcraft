package io.github.yhyeop.sanupcraft.test;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import io.github.yhyeop.sanupcraft.storage.DrawerBlock;
import io.github.yhyeop.sanupcraft.storage.DrawerBlockEntity;
import io.github.yhyeop.sanupcraft.storage.DrawerContents;
import io.github.yhyeop.sanupcraft.storage.ModStorage;

/** M7 보관 · 제작 검사: 저장 서랍 (S1). */
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
}
