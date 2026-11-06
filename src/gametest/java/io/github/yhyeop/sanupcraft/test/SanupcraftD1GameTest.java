package io.github.yhyeop.sanupcraft.test;

import java.util.Collections;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.MachineInput;
import io.github.yhyeop.sanupcraft.factory.MachineKind;
import io.github.yhyeop.sanupcraft.factory.MachineRecipe;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;
import io.github.yhyeop.sanupcraft.industry.ModFoundry;
import io.github.yhyeop.sanupcraft.logistics.ItemFilter;
import io.github.yhyeop.sanupcraft.mining.ModMining;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.resource.ModResources;

/** 확장 3 D1 검사: 새 광석 2종 (크롬철석 · 보크사이트) — 가공 사슬 · 광석 배치 · 태그 · 채굴. */
public class SanupcraftD1GameTest {
	private record Metal(Item raw, Item dust, Item refined, Item ingot, Block ore, Block rawBlock, Block metalBlock, String tagName) {
	}

	private static final Metal[] METALS = {
			new Metal(ModResources.RAW_CHROMIUM, ModResources.CHROMIUM_DUST, ModResources.REFINED_CHROMIUM_DUST, ModResources.CHROMIUM_INGOT,
					ModResources.DEEPSLATE_CHROMITE_ORE, ModResources.RAW_CHROMIUM_BLOCK, ModResources.CHROMIUM_BLOCK, "chromium"),
			new Metal(ModResources.RAW_ALUMINIUM, ModResources.ALUMINIUM_DUST, ModResources.REFINED_ALUMINIUM_DUST, ModResources.ALUMINIUM_INGOT,
					ModResources.BAUXITE_ORE, ModResources.RAW_ALUMINIUM_BLOCK, ModResources.ALUMINIUM_BLOCK, "aluminum")};

	private static TagKey<Item> itemTag(String path) {
		return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
	}

	private static TagKey<Block> blockTag(String path) {
		return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", path));
	}

	/** 분쇄기: 원석 → 가루 2, 광석 → 가루 3. */
	@GameTest
	public void crusherMakesDustFromRawAndOre(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (Metal m : METALS) {
			MachineRecipe raw = recipe(level, MachineKind.CRUSHER, new ItemStack(m.raw));
			helper.assertTrue(raw != null && raw.output().create().is(m.dust) && raw.output().create().getCount() == 2, m.tagName + " 원석 분쇄 레시피");
			MachineRecipe ore = recipe(level, MachineKind.CRUSHER, new ItemStack(m.ore));
			helper.assertTrue(ore != null && ore.output().create().is(m.dust) && ore.output().create().getCount() == 3, m.tagName + " 광석 분쇄 레시피");
		}
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModMachines.CRUSHER);
		MachineBlockEntity crusher = helper.getBlockEntity(pos, MachineBlockEntity.class);
		crusher.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(ModResources.RAW_CHROMIUM));
		tick(helper, pos, crusher, 400, false);
		helper.assertTrue(crusher.result().is(ModResources.CHROMIUM_DUST), "크롬가루가 아님: " + crusher.result());
		helper.assertValueEqual(crusher.result().getCount(), 2, "원석 하나의 가루");
		helper.succeed();
	}

	/** 세척기: 원석 → 정제 가루 3 (물 필요) + 희귀 가루 부산물. */
	@GameTest
	public void washerMakesThreeRefinedDust(GameTestHelper helper) {
		for (Metal m : METALS) {
			MachineRecipe recipe = recipe(helper.getLevel(), MachineKind.WASHER, new ItemStack(m.raw));
			helper.assertTrue(recipe != null && recipe.output().create().is(m.refined) && recipe.output().create().getCount() == 3, m.tagName + " 세척 레시피");
			helper.assertTrue(recipe.byproduct().map(b -> b.item().create().is(ModFoundry.RARE_DUST)).orElse(false), m.tagName + " 세척 부산물");
		}
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModMachines.WASHER);
		MachineBlockEntity washer = helper.getBlockEntity(pos, MachineBlockEntity.class);
		washer.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(ModResources.RAW_ALUMINIUM));
		washer.fill(FluidKind.WATER, 100, false);
		tick(helper, pos, washer, 400, false);
		helper.assertTrue(washer.result().is(ModResources.REFINED_ALUMINIUM_DUST), "정제 알루미늄가루가 아님: " + washer.result());
		helper.assertValueEqual(washer.result().getCount(), 3, "원석 하나의 정제 가루");
		helper.succeed();
	}

	/** 고압 정련기: 원석 → 정제 가루 4. */
	@GameTest
	public void refinerMakesFourRefinedDust(GameTestHelper helper) {
		for (Metal m : METALS) {
			MachineRecipe recipe = recipe(helper.getLevel(), MachineKind.REFINER, new ItemStack(m.raw));
			helper.assertTrue(recipe != null && recipe.output().create().is(m.refined) && recipe.output().create().getCount() == 4, m.tagName + " 정련 레시피");
		}
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModMachines.HP_REFINER);
		MachineBlockEntity refiner = helper.getBlockEntity(pos, MachineBlockEntity.class);
		refiner.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(ModResources.RAW_CHROMIUM));
		tick(helper, pos, refiner, 400, true);
		helper.assertTrue(refiner.result().is(ModResources.REFINED_CHROMIUM_DUST), "정제 크롬가루가 아님: " + refiner.result());
		helper.assertValueEqual(refiner.result().getCount(), 4, "원석 하나의 정제 가루");
		helper.succeed();
	}

	/** 가루 · 정제 가루 · 원석 · 광석이 화로와 용광로에서 주괴 하나가 된다. */
	@GameTest
	public void everythingSmeltsToIngot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var manager = level.recipeAccess();
		for (Metal m : METALS) {
			for (Item source : new Item[] {m.dust, m.refined, m.raw, m.ore.asItem()}) {
				SingleRecipeInput input = new SingleRecipeInput(new ItemStack(source));
				var smelt = manager.getRecipeFor(RecipeType.SMELTING, input, level);
				helper.assertTrue(smelt.isPresent() && smelt.get().value().assemble(input).is(m.ingot), "화로 레시피가 없음: " + source);
				var blast = manager.getRecipeFor(RecipeType.BLASTING, input, level);
				helper.assertTrue(blast.isPresent() && blast.get().value().assemble(input).is(m.ingot), "용광로 레시피가 없음: " + source);
			}
		}
		helper.succeed();
	}

	/** 원석 블록 · 금속 블록은 9개 ↔ 1개로 오간다. */
	@GameTest
	public void blockRecipesRoundTrip(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var manager = level.recipeAccess();
		for (Metal m : METALS) {
			for (Item[] pair : new Item[][] {{m.raw, m.rawBlock.asItem()}, {m.ingot, m.metalBlock.asItem()}}) {
				List<ItemStack> nine = Collections.nCopies(9, new ItemStack(pair[0]));
				CraftingInput pack = CraftingInput.of(3, 3, nine);
				var packed = manager.getRecipeFor(RecipeType.CRAFTING, pack, level);
				helper.assertTrue(packed.isPresent() && packed.get().value().assemble(pack).is(pair[1]), "블록 합치기 레시피가 없음: " + pair[1]);
				CraftingInput unpack = CraftingInput.of(1, 1, List.of(new ItemStack(pair[1])));
				var unpacked = manager.getRecipeFor(RecipeType.CRAFTING, unpack, level);
				ItemStack out = unpacked.isPresent() ? unpacked.get().value().assemble(unpack) : ItemStack.EMPTY;
				helper.assertTrue(out.is(pair[0]) && out.getCount() == 9, "블록 풀기 레시피가 없음: " + pair[0]);
			}
		}
		helper.succeed();
	}

	/** 광석 배치가 레지스트리에 있고, 바이옴에 붙어 있다 (크롬철석: 오버월드 전부, 보크사이트: 사바나 · 메사 · 사막만). */
	@GameTest
	public void placedFeaturesAttachedToBiomes(GameTestHelper helper) {
		var access = helper.getLevel().registryAccess();
		var placed = access.lookupOrThrow(Registries.PLACED_FEATURE);
		var feature = access.lookupOrThrow(Registries.FEATURE);
		for (String name : new String[] {"ore_chromite", "ore_bauxite"}) {
			helper.assertTrue(placed.get(ResourceKey.create(Registries.PLACED_FEATURE, Sanupcraft.id(name))).isPresent(), "배치 기능이 없음: " + name);
			helper.assertTrue(feature.get(ResourceKey.create(Registries.FEATURE, Sanupcraft.id(name))).isPresent(), "광석 기능이 없음: " + name);
		}
		PlacedFeature chromite = placed.getOrThrow(ModResources.CHROMITE_PLACED).value();
		PlacedFeature bauxite = placed.getOrThrow(ModResources.BAUXITE_PLACED).value();
		var biomes = access.lookupOrThrow(Registries.BIOME);
		List<ResourceKey<Biome>> overworld = List.of(Biomes.PLAINS, Biomes.DESERT, Biomes.SAVANNA, Biomes.BADLANDS, Biomes.DEEP_DARK, Biomes.OCEAN);
		for (ResourceKey<Biome> key : overworld) {
			Biome biome = biomes.getOrThrow(key).value();
			helper.assertTrue(biome.getGenerationSettings().hasFeature(chromite), "크롬철석이 안 붙음: " + key.identifier());
			boolean wantsBauxite = key == Biomes.DESERT || key == Biomes.SAVANNA || key == Biomes.BADLANDS;
			helper.assertValueEqual(biome.getGenerationSettings().hasFeature(bauxite), wantsBauxite, "보크사이트 붙음 여부: " + key.identifier());
		}
		Biome nether = biomes.getOrThrow(Biomes.NETHER_WASTES).value();
		helper.assertFalse(nether.getGenerationSettings().hasFeature(chromite), "네더에 크롬철석이 붙음");
		helper.succeed();
	}

	/** 광석 기능을 심층암 · 돌 · 흙에 직접 돌려 본다: 크롬철석은 심층암에만, 보크사이트는 돌에만 생기고 흙은 그대로. */
	@GameTest(maxTicks = 40)
	public void oreFeaturesPlaceIntoRightRock(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var feature = level.registryAccess().lookupOrThrow(Registries.FEATURE);
		var generator = level.getChunkSource().getGenerator();
		var random = level.getRandom();
		// x 0..5 심층암, x 8..13 돌 (z 0..5), z 8..13 흙
		for (int x = 0; x < 6; x++) {
			for (int y = 1; y < 7; y++) {
				for (int z = 0; z < 6; z++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.DEEPSLATE);
					helper.setBlock(new BlockPos(x + 8, y, z), Blocks.STONE);
					helper.setBlock(new BlockPos(x, y, z + 8), Blocks.DIRT);
				}
			}
		}
		var chromiteFeature = feature.getOrThrow(ResourceKey.create(Registries.FEATURE, Sanupcraft.id("ore_chromite"))).value();
		var bauxiteFeature = feature.getOrThrow(ResourceKey.create(Registries.FEATURE, Sanupcraft.id("ore_bauxite"))).value();
		for (int i = 0; i < 20; i++) {
			chromiteFeature.place(level, generator, random, helper.absolutePos(new BlockPos(3, 3, 3)));
			bauxiteFeature.place(level, generator, random, helper.absolutePos(new BlockPos(11, 3, 3)));
			// 흙 속에서는 어느 쪽도 생기지 않아야 한다
			chromiteFeature.place(level, generator, random, helper.absolutePos(new BlockPos(3, 3, 11)));
			bauxiteFeature.place(level, generator, random, helper.absolutePos(new BlockPos(3, 4, 11)));
		}
		int chromite = 0;
		int bauxite = 0;
		int stray = 0;
		for (int x = 0; x < 6; x++) {
			for (int y = 1; y < 7; y++) {
				for (int z = 0; z < 6; z++) {
					BlockState deep = helper.getBlockState(new BlockPos(x, y, z));
					BlockState stone = helper.getBlockState(new BlockPos(x + 8, y, z));
					BlockState dirt = helper.getBlockState(new BlockPos(x, y, z + 8));
					if (deep.is(ModResources.DEEPSLATE_CHROMITE_ORE)) {
						chromite++;
					}
					if (deep.is(ModResources.BAUXITE_ORE) || stone.is(ModResources.DEEPSLATE_CHROMITE_ORE)) {
						stray++;
					}
					if (stone.is(ModResources.BAUXITE_ORE)) {
						bauxite++;
					}
					if (dirt.is(ModResources.BAUXITE_ORE) || dirt.is(ModResources.DEEPSLATE_CHROMITE_ORE)) {
						stray++;
					}
				}
			}
		}
		helper.assertTrue(chromite > 0 && chromite <= 6 * 6 * 6, "심층암에 크롬철석이 안 생김: " + chromite);
		helper.assertTrue(bauxite > 0, "돌에 보크사이트가 안 생김");
		helper.assertValueEqual(stray, 0, "엉뚱한 돌 · 흙에 생긴 광석");
		helper.succeed();
	}

	/** 태그: 광석 · 원석 · 주괴 · 가루 · 블록 규약 태그, 곡괭이 등급, 굴착기 쓰레기에 없음, 원석 표지 대상. */
	@GameTest
	public void tagsAreSet(GameTestHelper helper) {
		for (Metal m : METALS) {
			helper.assertTrue(m.ore.defaultBlockState().is(blockTag("ores/" + m.tagName)), m.tagName + " 광석 블록 태그");
			helper.assertTrue(m.ore.defaultBlockState().is(blockTag("ores")), m.tagName + " c:ores 블록 태그");
			helper.assertTrue(new ItemStack(m.ore).is(itemTag("ores/" + m.tagName)), m.tagName + " 광석 아이템 태그");
			helper.assertTrue(new ItemStack(m.ore).is(ItemFilter.ORES), m.tagName + " 원석 표지가 광석을 못 알아봄");
			helper.assertTrue(new ItemStack(m.raw).is(itemTag("raw_materials/" + m.tagName)), m.tagName + " 원석 태그");
			helper.assertTrue(new ItemStack(m.raw).is(ItemFilter.RAW_MATERIALS), m.tagName + " 원석 표지가 원석을 못 알아봄");
			helper.assertTrue(new ItemStack(m.ingot).is(itemTag("ingots/" + m.tagName)), m.tagName + " 주괴 태그");
			helper.assertTrue(new ItemStack(m.dust).is(itemTag("dusts/" + m.tagName)), m.tagName + " 가루 태그");
			helper.assertTrue(new ItemStack(m.refined).is(itemTag("dusts/" + m.tagName)), m.tagName + " 정제 가루 태그");
			helper.assertTrue(m.rawBlock.defaultBlockState().is(blockTag("storage_blocks/raw_" + m.tagName)), m.tagName + " 원석 블록 태그");
			helper.assertTrue(m.metalBlock.defaultBlockState().is(blockTag("storage_blocks/" + m.tagName)), m.tagName + " 금속 블록 태그");
			for (Block block : new Block[] {m.ore, m.rawBlock, m.metalBlock}) {
				helper.assertTrue(block.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), "곡괭이로 캐지 않음: " + block);
			}
			for (Item item : new Item[] {m.raw, m.dust, m.refined, m.ingot, m.ore.asItem()}) {
				helper.assertFalse(new ItemStack(item).is(ModMining.QUARRY_JUNK), "굴착기가 버리는 쓰레기 태그에 있음: " + item);
			}
		}
		helper.assertTrue(ModResources.DEEPSLATE_CHROMITE_ORE.defaultBlockState().is(BlockTags.NEEDS_IRON_TOOL), "크롬철석은 철 곡괭이부터");
		helper.assertTrue(ModResources.BAUXITE_ORE.defaultBlockState().is(BlockTags.NEEDS_STONE_TOOL), "보크사이트는 돌 곡괭이부터");
		helper.succeed();
	}

	/** 광석을 알맞은 곡괭이로 캐면 원석이 나오고, 비단 손길이면 광석이 나온다. 도구 등급도 맞다. */
	@GameTest
	public void oreDrops(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
		BlockState chromiteState = ModResources.DEEPSLATE_CHROMITE_ORE.defaultBlockState();
		BlockState bauxiteState = ModResources.BAUXITE_ORE.defaultBlockState();
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
		var chromiteDrops = Block.getDrops(chromiteState, level, pos, null, player, player.getMainHandItem());
		helper.assertTrue(chromiteDrops.size() == 1 && chromiteDrops.get(0).is(ModResources.RAW_CHROMIUM), "크롬철석에서 원석 크롬이 안 나옴: " + chromiteDrops);
		helper.assertTrue(player.hasCorrectToolForDrops(chromiteState), "철 곡괭이로 크롬철석이 안 캐짐");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_PICKAXE));
		helper.assertFalse(player.hasCorrectToolForDrops(chromiteState), "돌 곡괭이로 크롬철석이 캐짐");
		var bauxiteDrops = Block.getDrops(bauxiteState, level, pos, null, player, player.getMainHandItem());
		helper.assertTrue(bauxiteDrops.size() == 1 && bauxiteDrops.get(0).is(ModResources.RAW_ALUMINIUM), "보크사이트에서 원석 알루미늄이 안 나옴: " + bauxiteDrops);
		helper.assertTrue(player.hasCorrectToolForDrops(bauxiteState), "돌 곡괭이로 보크사이트가 안 캐짐");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
		helper.assertFalse(player.hasCorrectToolForDrops(bauxiteState), "나무 곡괭이로 보크사이트가 캐짐");
		// 금속 블록은 자기 자신이 나온다
		for (Block block : new Block[] {ModResources.RAW_CHROMIUM_BLOCK, ModResources.CHROMIUM_BLOCK, ModResources.RAW_ALUMINIUM_BLOCK, ModResources.ALUMINIUM_BLOCK}) {
			var drops = Block.getDrops(block.defaultBlockState(), level, pos, null, player, new ItemStack(Items.IRON_PICKAXE));
			helper.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()), "블록이 자기 자신을 떨구지 않음: " + block);
		}
		helper.succeed();
	}

	/** 실테스트 꾸러미 `ores`가 새 광석 · 원석 · 기계를 준다. */
	@GameTest
	public void playtestOresKit(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		PlaytestCommand.apply(player, PlaytestCommand.Stage.ORES);
		helper.assertTrue(player.getInventory().countItem(ModResources.RAW_CHROMIUM) > 0, "크롬 원석을 주지 않음");
		helper.assertTrue(player.getInventory().countItem(ModResources.DEEPSLATE_CHROMITE_ORE.asItem()) > 0, "크롬철석을 주지 않음");
		helper.assertTrue(player.getInventory().countItem(ModMachines.WASHER.asItem()) > 0, "세척기를 주지 않음");
		helper.succeed();
	}

	private static MachineRecipe recipe(ServerLevel level, MachineKind kind, ItemStack input) {
		return level.recipeAccess().getRecipeFor(kind.recipeType(), new MachineInput(input, ItemStack.EMPTY), level).map(RecipeHolder::value).orElse(null);
	}

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
}
