package io.github.yhyeop.sanupcraft.test;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.mining.ModMining;
import io.github.yhyeop.sanupcraft.mining.QuarryBlockEntity;
import io.github.yhyeop.sanupcraft.world.ModWorld;

/**
 * 월드에서 보스를 만나는 길: 녹슨 사당 구조물 · 사당 상자, 네더 요새 · 보루 상자에 더한 전리품.
 * 그리고 내열 드릴을 나중에 끼워도 지나간 층의 흑요석을 캐는지.
 */
public class SanupcraftWorldGameTest {
	/** 구조물 틀에 가운데 제단, 상자(사당 전리품 테이블)가 있다. */
	@GameTest
	public void shrineTemplateHasAltarAndChest(GameTestHelper helper) {
		StructureTemplate template = helper.getLevel().getStructureTemplateManager().get(Sanupcraft.id("rusted_shrine"))
				.orElseThrow(() -> helper.assertionException("rusted_shrine.nbt를 못 읽음"));
		StructurePlaceSettings settings = new StructurePlaceSettings();
		List<StructureTemplate.StructureBlockInfo> altars = template.filterBlocks(BlockPos.ZERO, settings, ModBoss.GUARDIAN_ALTAR);
		helper.assertValueEqual(altars.size(), 1, "제단 수");
		helper.assertValueEqual(altars.get(0).pos(), new BlockPos(4, 1, 4), "제단 위치");
		List<StructureTemplate.StructureBlockInfo> chests = template.filterBlocks(BlockPos.ZERO, settings, Blocks.CHEST);
		helper.assertValueEqual(chests.size(), 1, "상자 수");
		String loot = chests.get(0).nbt() == null ? "" : chests.get(0).nbt().getString("LootTable").orElse("");
		helper.assertValueEqual(loot, ModWorld.RUSTED_SHRINE_CHEST.identifier().toString(), "상자 전리품 테이블");
		helper.succeed();
	}

	/** 구조물 · 구조물 묶음이 등록돼 월드 생성에 쓰인다. */
	@GameTest
	public void shrineIsRegisteredForWorldgen(GameTestHelper helper) {
		var access = helper.getLevel().registryAccess();
		helper.assertTrue(access.lookupOrThrow(Registries.STRUCTURE).get(ModWorld.RUSTED_SHRINE).isPresent(), "구조물이 등록 안 됨");
		helper.assertTrue(access.lookupOrThrow(Registries.STRUCTURE_SET)
				.get(ResourceKey.create(Registries.STRUCTURE_SET, Sanupcraft.id("rusted_shrines"))).isPresent(), "구조물 묶음이 등록 안 됨");
		helper.succeed();
	}

	/** 사당 상자에는 늘 수호자 소환석이 하나 있다. */
	@GameTest
	public void shrineChestAlwaysHasSummoner(GameTestHelper helper) {
		for (int i = 0; i < 20; i++) {
			List<ItemStack> items = roll(helper, ModWorld.RUSTED_SHRINE_CHEST, i);
			helper.assertValueEqual(count(items, ModBoss.GUARDIAN_SUMMONER), 1, "소환석 수 (" + i + "번째)");
			helper.assertTrue(items.size() >= 3, "사당 상자가 너무 빔: " + items);
		}
		helper.succeed();
	}

	/** 네더 요새 상자에서 파수꾼 소환석이 대략 20%로 나온다 (200번 열어 15~80개). */
	@GameTest
	public void fortressChestsCanGiveWardenSummoner(GameTestHelper helper) {
		int summoners = 0;
		int shards = 0;
		for (int i = 0; i < 200; i++) {
			List<ItemStack> items = roll(helper, BuiltInLootTables.NETHER_BRIDGE, i);
			summoners += count(items, ModBoss.WARDEN_SUMMONER);
			shards += count(items, ModItems.CORE_SHARD);
		}
		helper.assertTrue(summoners >= 15 && summoners <= 80, "요새 상자 200개에서 파수꾼 소환석: " + summoners);
		helper.assertTrue(shards > 0, "요새 상자에서 핵 조각이 안 나옴");
		helper.succeed();
	}

	/** 보루 상자에서는 핵 조각이 나온다. */
	@GameTest
	public void bastionChestsCanGiveCoreShards(GameTestHelper helper) {
		int shards = 0;
		for (int i = 0; i < 100; i++) {
			shards += count(roll(helper, BuiltInLootTables.BASTION_OTHER, i), ModItems.CORE_SHARD);
		}
		helper.assertTrue(shards > 0, "보루 상자에서 핵 조각이 안 나옴");
		helper.succeed();
	}

	/** 드릴 없이 흑요석 층을 지나친 굴착기에 드릴을 끼우면, 맨 위부터 다시 훑어 흑요석을 캔다. */
	@GameTest(environment = "sanupcraft-test:alone_quarry", maxTicks = 400)
	public void drillRescansPassedLayers(GameTestHelper helper) {
		BlockPos quarryPos = new BlockPos(3, 3, 3);
		helper.setBlock(quarryPos.below(), Blocks.OBSIDIAN);
		helper.setBlock(quarryPos.below(2), Blocks.STONE);
		helper.setBlock(quarryPos, ModMining.STEAM_QUARRY);
		helper.setBlock(quarryPos.above(), Blocks.CHEST);
		QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
		int obsidianY = helper.absolutePos(quarryPos.below()).getY();
		helper.succeedWhen(() -> {
			quarry.acceptSteam(QuarryBlockEntity.STEAM_CAPACITY);
			if (!quarry.hasDrill()) {
				helper.assertTrue(quarry.layer() < obsidianY - 1, "아직 흑요석 아래층까지 안 내려감: " + quarry.layer());
				helper.assertBlockPresent(Blocks.OBSIDIAN, quarryPos.below());
				helper.assertTrue(quarry.installDrill(), "드릴을 못 끼움");
			}
			helper.assertBlockNotPresent(Blocks.OBSIDIAN, quarryPos.below());
		});
	}

	private static List<ItemStack> roll(GameTestHelper helper, ResourceKey<LootTable> key, long seed) {
		ServerLevel level = helper.getLevel();
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
		LootParams params = new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
				.create(LootContextParamSets.CHEST);
		return table.getRandomItems(params, RandomSource.create(seed));
	}

	private static int count(List<ItemStack> items, Item item) {
		return items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
	}
}
