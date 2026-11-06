package io.github.yhyeop.sanupcraft.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.fluid.FluidContents;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;
import io.github.yhyeop.sanupcraft.fluid.ModFluid;
import io.github.yhyeop.sanupcraft.fluid.TankBlockEntity;
import io.github.yhyeop.sanupcraft.gear.backpack.BackpackContainer;
import io.github.yhyeop.sanupcraft.gear.backpack.BackpackItem;
import io.github.yhyeop.sanupcraft.gear.backpack.BackpackMenu;
import io.github.yhyeop.sanupcraft.gear.backpack.ModBackpack;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.power.SteamConsumer;
import io.github.yhyeop.sanupcraft.production.grinder.ExperienceTankBlock;
import io.github.yhyeop.sanupcraft.production.grinder.ExperienceTankBlockEntity;
import io.github.yhyeop.sanupcraft.production.grinder.MobGrinderBlockEntity;
import io.github.yhyeop.sanupcraft.production.grinder.ModGrinder;

/** M9 part B 검사: 몹 분쇄기 · 경험치 탱크 (P2), 배낭 (P5). */
public class SanupcraftM9bGameTest {
	private static final BlockPos GRINDER = new BlockPos(2, 1, 2);
	private static final BlockPos ABOVE = new BlockPos(2, 2, 2);

	// ---- P2 몹 분쇄기 ----

	/** 증기가 있어야 벤다. 피해는 가짜 플레이어가 준 것으로 쳐서 플레이어 처치 기록(전리품 · 경험치 조건)이 남는다. */
	@GameTest
	public void grinderDamagesZombieOnlyWithSteam(GameTestHelper helper) {
		helper.setBlock(GRINDER, ModGrinder.MOB_GRINDER);
		MobGrinderBlockEntity grinder = helper.getBlockEntity(GRINDER, MobGrinderBlockEntity.class);
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(GRINDER);
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, ABOVE);
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		zombie.setPersistenceRequired();
		float full = zombie.getHealth();

		helper.assertValueEqual(grinder.grind(level, abs), 0, "증기 없이 벰");
		helper.assertTrue(zombie.getHealth() == full, "증기 없는데 피해: " + zombie.getHealth());
		helper.assertTrue(grinder.acceptSteam(MobGrinderBlockEntity.STEAM_CAPACITY) > 0, "증기를 못 받음");
		helper.assertValueEqual(grinder.grind(level, abs), 1, "벤 몹 수");
		helper.assertTrue(zombie.getHealth() < full && zombie.getHealth() > 0, "피해: " + zombie.getHealth());
		helper.assertValueEqual(grinder.steamStored(), MobGrinderBlockEntity.STEAM_CAPACITY - MobGrinderBlockEntity.STEAM_PER_HIT, "쓴 증기");
		helper.assertTrue(zombie.getLastHurtByPlayerMemoryTime() > 0, "플레이어 처치 기록이 없음");
		helper.assertTrue(zombie.getLastHurtByPlayer() instanceof FakePlayer, "가짜 플레이어가 아님: " + zombie.getLastHurtByPlayer());
		helper.assertTrue(zombie.getTarget() == null, "가짜 플레이어를 쫓음");
		helper.assertTrue(grinder instanceof SteamConsumer, "증기를 받는 블록이 아님");
		helper.succeed();
	}

	/** 플레이어 · 주민 · 길들인 동물 · 이름표 붙은 몹은 건드리지 않는다. */
	@GameTest
	public void grinderSparesPlayersVillagersPetsAndNamed(GameTestHelper helper) {
		helper.setBlock(GRINDER, ModGrinder.MOB_GRINDER);
		MobGrinderBlockEntity grinder = helper.getBlockEntity(GRINDER, MobGrinderBlockEntity.class);
		grinder.acceptSteam(MobGrinderBlockEntity.STEAM_CAPACITY);
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(GRINDER);

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setPos(Vec3.atBottomCenterOf(abs.above()));
		Villager villager = helper.spawn(EntityTypes.VILLAGER, ABOVE);
		villager.setNoAi(true);
		villager.setNoGravity(true);
		Wolf wolf = helper.spawn(EntityTypes.WOLF, ABOVE);
		wolf.setNoAi(true);
		wolf.setNoGravity(true);
		wolf.tame(player);
		Zombie named = helper.spawn(EntityTypes.ZOMBIE, ABOVE);
		named.setNoAi(true);
		named.setNoGravity(true);
		named.setCustomName(Component.literal("친구"));
		float playerHealth = player.getHealth();
		float villagerHealth = villager.getHealth();
		float wolfHealth = wolf.getHealth();
		float namedHealth = named.getHealth();

		helper.assertFalse(MobGrinderBlockEntity.canGrind(player), "플레이어를 벨 수 있음");
		helper.assertValueEqual(grinder.grind(level, abs), 0, "벤 몹 수");
		helper.assertTrue(player.getHealth() == playerHealth, "플레이어가 맞음");
		helper.assertTrue(villager.getHealth() == villagerHealth, "주민이 맞음");
		helper.assertTrue(wolf.getHealth() == wolfHealth, "길들인 늑대가 맞음");
		helper.assertTrue(named.getHealth() == namedHealth, "이름표 붙은 좀비가 맞음");
		helper.assertValueEqual(grinder.steamStored(), MobGrinderBlockEntity.STEAM_CAPACITY, "증기를 씀");
		helper.succeed();
	}

	/** 죽이면 (플레이어가 잡은 것처럼) 경험치 구슬이 나오고, 붙은 경험치 탱크로 모인다. */
	@GameTest
	public void grinderKillFeedsExperienceTank(GameTestHelper helper) {
		helper.setBlock(GRINDER, ModGrinder.MOB_GRINDER);
		BlockPos tankPos = GRINDER.east();
		helper.setBlock(tankPos, ModGrinder.EXPERIENCE_TANK);
		MobGrinderBlockEntity grinder = helper.getBlockEntity(GRINDER, MobGrinderBlockEntity.class);
		ExperienceTankBlockEntity tank = helper.getBlockEntity(tankPos, ExperienceTankBlockEntity.class);
		grinder.acceptSteam(MobGrinderBlockEntity.STEAM_CAPACITY);
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(GRINDER);
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, ABOVE);
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		zombie.setHealth(1.0F);

		helper.assertValueEqual(grinder.grind(level, abs), 1, "벤 몹 수");
		helper.assertTrue(zombie.isDeadOrDying(), "좀비가 안 죽음");
		helper.runAfterDelay(2, () -> {
			int collected = grinder.collectExperience(level, abs);
			helper.assertTrue(collected >= 5, "모은 경험치: " + collected);
			helper.assertValueEqual(tank.points(), collected, "탱크의 경험치");
			helper.assertValueEqual(grinder.bufferPoints(), 0, "분쇄기 안에 쌓임");
			helper.assertValueEqual(tank.fluidAmount(), collected * FluidKind.MB_PER_XP, "탱크의 mB");
			helper.succeed();
		});
	}

	/** 탱크가 없으면 안 저장고에 모으고, 탱크를 붙이면 옮겨 간다. 빈손 우클릭으로 받는다. */
	@GameTest
	public void grinderBufferMovesToTankAndPlayer(GameTestHelper helper) {
		helper.setBlock(GRINDER, ModGrinder.MOB_GRINDER);
		MobGrinderBlockEntity grinder = helper.getBlockEntity(GRINDER, MobGrinderBlockEntity.class);
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(GRINDER);
		ExperienceOrb.award(level, Vec3.atCenterOf(abs.above()), 12);
		helper.runAfterDelay(2, () -> {
			helper.assertValueEqual(grinder.collectExperience(level, abs), 12, "모은 경험치");
			helper.assertValueEqual(grinder.bufferPoints(), 12, "저장고");
			helper.setBlock(GRINDER.west(), ModGrinder.EXPERIENCE_TANK);
			ExperienceTankBlockEntity tank = helper.getBlockEntity(GRINDER.west(), ExperienceTankBlockEntity.class);
			grinder.collectExperience(level, abs);
			helper.assertValueEqual(tank.points(), 12, "탱크로 옮겨 감");
			helper.assertValueEqual(grinder.bufferPoints(), 0, "저장고에 남음");

			ExperienceOrb.award(level, Vec3.atCenterOf(abs.above()), 4);
			helper.setBlock(GRINDER.west(), net.minecraft.world.level.block.Blocks.AIR);
			helper.runAfterDelay(2, () -> {
				grinder.collectExperience(level, abs);
				helper.assertValueEqual(grinder.bufferPoints(), 4, "탱크를 떼고 다시 저장고");
				Player player = helper.makeMockPlayer(GameType.SURVIVAL);
				BlockState state = helper.getBlockState(GRINDER);
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
				helper.assertValueEqual(state.useWithoutItem(level, player, hit), InteractionResult.SUCCESS, "빈손 우클릭");
				helper.assertValueEqual(grinder.bufferPoints(), 0, "받고 남음");
				helper.assertValueEqual(player.totalExperience, 4, "받은 경험치");
				helper.succeed();
			});
		});
	}

	// ---- P2 경험치 탱크 ----

	/** 빈손 우클릭 = 레벨 하나 몫, 웅크리고 = 전부. 부족하면 있는 만큼. */
	@GameTest
	public void experienceTankGivesLevels(GameTestHelper helper) {
		helper.setBlock(GRINDER, ModGrinder.EXPERIENCE_TANK);
		ExperienceTankBlockEntity tank = helper.getBlockEntity(GRINDER, ExperienceTankBlockEntity.class);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		tank.setPoints(1000);

		helper.assertValueEqual(ExperienceTankBlock.giveTo(tank, player, false), 7, "0레벨에서 한 레벨 몫");
		helper.assertValueEqual(player.experienceLevel, 1, "레벨");
		helper.assertValueEqual(ExperienceTankBlock.giveTo(tank, player, false), 9, "1레벨에서 한 레벨 몫");
		helper.assertValueEqual(player.experienceLevel, 2, "레벨");
		player.giveExperiencePoints(3);
		int expected = ExperienceTankBlock.pointsToNextLevel(player);
		helper.assertValueEqual(ExperienceTankBlock.giveTo(tank, player, false), expected, "중간부터 한 레벨 몫");
		helper.assertValueEqual(player.experienceLevel, 3, "레벨");
		int rest = tank.points();
		helper.assertValueEqual(ExperienceTankBlock.giveTo(tank, player, true), rest, "전부 받기");
		helper.assertValueEqual(tank.points(), 0, "다 줬는데 남음");
		helper.assertTrue(tank.fluid() == null, "비었는데 유체가 남음");
		tank.setPoints(3);
		helper.assertValueEqual(ExperienceTankBlock.giveTo(tank, player, false), 3, "모자라면 있는 만큼");

		// 우클릭 경로 (빈손 · 웅크리기)
		tank.setPoints(500);
		player.experienceLevel = 0;
		player.experienceProgress = 0.0F;
		player.totalExperience = 0;
		BlockPos abs = helper.absolutePos(GRINDER);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
		BlockState state = helper.getBlockState(GRINDER);
		helper.assertValueEqual(state.useWithoutItem(helper.getLevel(), player, hit), InteractionResult.SUCCESS, "빈손 우클릭");
		helper.assertValueEqual(tank.points(), 493, "한 레벨 몫(7)을 줌");
		player.setShiftKeyDown(true);
		state.useWithoutItem(helper.getLevel(), player, hit);
		helper.assertValueEqual(tank.points(), 0, "웅크리고 우클릭은 전부");
		helper.succeed();
	}

	/** 유리병 우클릭 = 경험치 10점으로 경험의 병. 모자라면 거절. 경험치만 받고, 양동이 퍼 내기는 하지 않는다. */
	@GameTest
	public void experienceTankBottlesAndRejectsOtherFluids(GameTestHelper helper) {
		helper.setBlock(GRINDER, ModGrinder.EXPERIENCE_TANK);
		ExperienceTankBlockEntity tank = helper.getBlockEntity(GRINDER, ExperienceTankBlockEntity.class);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockPos abs = helper.absolutePos(GRINDER);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
		BlockState state = helper.getBlockState(GRINDER);

		tank.setPoints(9);
		helper.assertValueEqual(use(helper, state, player, hit, new ItemStack(Items.GLASS_BOTTLE, 2)), InteractionResult.FAIL, "모자란데 병을 채움");
		tank.setPoints(25);
		helper.assertValueEqual(use(helper, state, player, hit, new ItemStack(Items.GLASS_BOTTLE, 2)), InteractionResult.SUCCESS, "병 채우기");
		helper.assertValueEqual(tank.points(), 15, "병 하나의 값");
		helper.assertTrue(player.getInventory().countItem(Items.EXPERIENCE_BOTTLE) == 1, "경험의 병을 못 받음");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "남은 유리병");

		helper.assertValueEqual(tank.fill(FluidKind.WATER, 1000, false), 0, "경험치 탱크에 물");
		helper.assertValueEqual(tank.fill(FluidKind.EXPERIENCE, 1000, true), 1000, "경험치 넣기 셈");
		helper.assertValueEqual(use(helper, state, player, hit, new ItemStack(Items.BUCKET)), InteractionResult.TRY_WITH_EMPTY_HAND, "양동이는 쓰지 않음");
		helper.succeed();
	}

	/** 비교기 신호, 부수면 내용이 아이템 컴포넌트로 남고 다시 놓으면 그대로. 보통 탱크는 경험치 양동이로 새지 않는다. */
	@GameTest
	public void experienceTankSignalAndComponent(GameTestHelper helper) {
		helper.setBlock(GRINDER, ModGrinder.EXPERIENCE_TANK);
		ExperienceTankBlockEntity tank = helper.getBlockEntity(GRINDER, ExperienceTankBlockEntity.class);
		BlockState state = helper.getBlockState(GRINDER);
		BlockPos abs = helper.absolutePos(GRINDER);
		helper.assertValueEqual(tank.signal(), 0, "빈 신호");
		tank.setPoints(1);
		helper.assertTrue(tank.signal() >= 1, "조금 있을 때 신호");
		tank.setPoints(ExperienceTankBlockEntity.CAPACITY_POINTS);
		helper.assertValueEqual(tank.signal(), 15, "가득 신호");
		helper.assertValueEqual(tank.addPoints(10), 0, "가득인데 들어감");

		tank.setPoints(321);
		List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), abs, tank);
		helper.assertValueEqual(drops.size(), 1, "전리품 수");
		FluidContents contents = drops.getFirst().get(ModFluid.FLUID);
		helper.assertTrue(contents != null && contents.kind() == FluidKind.EXPERIENCE && contents.amount() == 321 * FluidKind.MB_PER_XP, "아이템에 남은 경험치: " + contents);
		BlockPos againPos = new BlockPos(4, 1, 2);
		helper.setBlock(againPos, ModGrinder.EXPERIENCE_TANK);
		ExperienceTankBlockEntity again = helper.getBlockEntity(againPos, ExperienceTankBlockEntity.class);
		again.applyComponentsFromItemStack(drops.getFirst());
		helper.assertValueEqual(again.points(), 321, "다시 놓은 탱크");

		// 경험치는 양동이가 없는 유체
		helper.assertTrue(FluidKind.EXPERIENCE.bucket() == null, "경험치 양동이");
		helper.assertTrue(FluidKind.ofBucket(ItemStack.EMPTY) == null && FluidKind.ofBucket(new ItemStack(Items.EXPERIENCE_BOTTLE)) == null, "양동이로 센 것");
		BlockPos plainPos = new BlockPos(1, 1, 4);
		helper.setBlock(plainPos, ModFluid.TANK);
		TankBlockEntity plain = helper.getBlockEntity(plainPos, TankBlockEntity.class);
		plain.set(FluidKind.EXPERIENCE, 5000);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockPos plainAbs = helper.absolutePos(plainPos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(plainAbs), Direction.UP, plainAbs, false);
		helper.assertValueEqual(use(helper, helper.getBlockState(plainPos), player, hit, new ItemStack(Items.BUCKET)), InteractionResult.FAIL, "보통 탱크의 경험치를 양동이로 뜸");
		helper.succeed();
	}

	// ---- P5 배낭 ----

	/** 내용이 아이템 컴포넌트에 남는다: 복사 · 저장해 불러와도 그대로. */
	@GameTest
	public void backpackKeepsItemsInComponent(GameTestHelper helper) {
		ItemStack pack = new ItemStack(ModBackpack.BACKPACK);
		BackpackContainer container = new BackpackContainer(pack, 27);
		container.setItem(0, new ItemStack(Items.DIAMOND, 5));
		container.setItem(26, new ItemStack(Items.APPLE, 12));
		ItemContainerContents stored = pack.get(DataComponents.CONTAINER);
		helper.assertTrue(stored != null && stored.nonEmptyItemCopyStream().count() == 2, "컴포넌트에 안 담김: " + stored);

		BackpackContainer copy = new BackpackContainer(pack.copy(), 27);
		helper.assertTrue(copy.getItem(0).is(Items.DIAMOND) && copy.getItem(0).getCount() == 5, "복사본 0번: " + copy.getItem(0));
		helper.assertTrue(copy.getItem(26).is(Items.APPLE) && copy.getItem(26).getCount() == 12, "복사본 26번: " + copy.getItem(26));

		var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
		var encoded = ItemStack.CODEC.encodeStart(ops, pack).getOrThrow();
		ItemStack loaded = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
		BackpackContainer reloaded = new BackpackContainer(loaded, 27);
		helper.assertTrue(reloaded.getItem(0).is(Items.DIAMOND) && reloaded.getItem(26).is(Items.APPLE), "저장해 불러온 배낭의 내용");

		container.setItem(0, ItemStack.EMPTY);
		helper.assertTrue(pack.get(DataComponents.CONTAINER).nonEmptyItemCopyStream().count() == 1, "비운 것이 반영 안 됨");
		helper.assertValueEqual(((BackpackItem) ModBackpack.BACKPACK).size(), 27, "배낭 칸 수");
		helper.assertValueEqual(((BackpackItem) ModBackpack.STEEL_BACKPACK).size(), 54, "강철 배낭 칸 수");
		helper.succeed();
	}

	/** 배낭은 배낭 안(자기 자신 포함)에 못 들어가고, 열려 있는 배낭 자신은 집을 수 없다. */
	@GameTest
	public void backpackCannotContainBackpack(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack pack = new ItemStack(ModBackpack.BACKPACK);
		ItemStack other = new ItemStack(ModBackpack.BACKPACK);
		ItemStack steel = new ItemStack(ModBackpack.STEEL_BACKPACK);
		player.getInventory().setItem(0, pack);
		BackpackContainer container = new BackpackContainer(pack, 27);
		BackpackMenu menu = new BackpackMenu(1, player.getInventory(), container, 3);

		helper.assertFalse(container.canPlaceItem(0, other), "배낭이 들어감");
		helper.assertFalse(container.canPlaceItem(0, steel), "강철 배낭이 들어감");
		helper.assertFalse(container.canPlaceItem(0, pack), "자기 자신이 들어감");
		helper.assertTrue(container.canPlaceItem(0, new ItemStack(Items.DIAMOND)), "다이아가 안 들어감");
		helper.assertFalse(pack.getItem().canFitInsideContainerItems(), "셜커 상자에 들어감");
		helper.assertValueEqual(menu.slots.size(), 27 + 36, "창의 칸 수");
		helper.assertFalse(menu.getSlot(0).mayPlace(other), "칸이 배낭을 받음");

		// 배낭을 든 채 칸을 눌러도 안 들어간다
		menu.setCarried(other);
		menu.clicked(0, 0, ContainerInput.PICKUP, player);
		helper.assertTrue(container.getItem(0).isEmpty(), "누르니 배낭이 들어감");
		helper.assertTrue(menu.getCarried() == other, "든 배낭이 사라짐");
		menu.setCarried(ItemStack.EMPTY);

		// 열린 배낭 자신 (핫바 0번 = 창의 54번 칸)은 집을 수도, 옮길 수도 없다
		menu.clicked(54, 0, ContainerInput.PICKUP, player);
		helper.assertTrue(menu.getCarried().isEmpty(), "열린 배낭을 집음");
		menu.clicked(54, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(player.getInventory().getItem(0) == pack, "열린 배낭을 Shift 클릭으로 옮김");
		container.setItem(3, new ItemStack(Items.APPLE));
		menu.clicked(3, 0, ContainerInput.SWAP, player);
		helper.assertTrue(player.getInventory().getItem(0) == pack, "숫자 키로 열린 배낭과 바꿈");
		helper.assertTrue(container.getItem(3).is(Items.APPLE), "숫자 키 바꾸기가 칸 내용을 건드림");
		helper.assertTrue(container.stillValid(player), "가방에 있는데 창이 닫힘");
		player.getInventory().setItem(0, ItemStack.EMPTY);
		helper.assertFalse(container.stillValid(player), "배낭이 사라졌는데 창이 안 닫힘");
		helper.succeed();
	}

	/** 우클릭으로 창이 열린다 (27 / 54칸). */
	@GameTest
	public void backpackOpensMenu(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack pack = new ItemStack(ModBackpack.BACKPACK);
		player.setItemInHand(InteractionHand.MAIN_HAND, pack);
		helper.assertTrue(pack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND) instanceof InteractionResult.Success, "우클릭");
		helper.assertTrue(player.containerMenu instanceof BackpackMenu, "창이 안 열림: " + player.containerMenu);
		helper.assertValueEqual(player.containerMenu.slots.size(), 27 + 36, "배낭 창 칸");
		player.containerMenu.getSlot(0).set(new ItemStack(Items.GOLD_INGOT, 3));
		player.closeContainer();
		helper.assertTrue(new BackpackContainer(pack, 27).getItem(0).is(Items.GOLD_INGOT), "닫은 뒤 내용");

		ItemStack steel = new ItemStack(ModBackpack.STEEL_BACKPACK);
		player.setItemInHand(InteractionHand.MAIN_HAND, steel);
		steel.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.containerMenu instanceof BackpackMenu, "강철 배낭 창이 안 열림");
		helper.assertValueEqual(player.containerMenu.slots.size(), 54 + 36, "강철 배낭 창 칸");
		helper.succeed();
	}

	/** 배낭 + 강철판 4개 = 강철 배낭, 내용 그대로. */
	@GameTest
	public void steelBackpackUpgradeKeepsContents(GameTestHelper helper) {
		ItemStack pack = new ItemStack(ModBackpack.BACKPACK);
		new BackpackContainer(pack, 27).setItem(5, new ItemStack(Items.EMERALD, 7));
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Sanupcraft.id("steel_backpack"));
		var holder = helper.getLevel().getServer().getRecipeManager().byKey(key);
		helper.assertTrue(holder.isPresent(), "레시피가 없음");
		CraftingRecipe recipe = (CraftingRecipe) holder.get().value();
		List<ItemStack> grid = new ArrayList<>();
		grid.add(pack);
		for (int i = 0; i < 4; i++) {
			grid.add(new ItemStack(ModItems.STEEL_PLATE));
		}
		for (int i = grid.size(); i < 9; i++) {
			grid.add(ItemStack.EMPTY);
		}
		CraftingInput input = CraftingInput.of(3, 3, grid);
		helper.assertTrue(recipe.matches(input, helper.getLevel()), "강철 배낭 제작법이 안 맞음");
		ItemStack result = recipe.assemble(input);
		helper.assertTrue(result.is(ModBackpack.STEEL_BACKPACK), "결과: " + result);
		BackpackContainer upgraded = new BackpackContainer(result, 54);
		helper.assertTrue(upgraded.getItem(5).is(Items.EMERALD) && upgraded.getItem(5).getCount() == 7, "내용이 안 넘어옴: " + upgraded.getItem(5));
		// 강철판 3개로는 안 된다
		List<ItemStack> few = new ArrayList<>(grid);
		few.set(4, ItemStack.EMPTY);
		helper.assertFalse(recipe.matches(CraftingInput.of(3, 3, few), helper.getLevel()), "강철판 3개로 만들어짐");
		helper.succeed();
	}

	// ---- 레시피 · 문구 · 길잡이 · 실테스트 · 설정 ----

	@GameTest
	public void m9bRecipesAndAdvancementsLoad(GameTestHelper helper) {
		var recipes = helper.getLevel().getServer().getRecipeManager();
		for (String id : new String[] {"mob_grinder", "experience_tank", "backpack", "steel_backpack"}) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Sanupcraft.id(id));
			helper.assertTrue(recipes.byKey(key).isPresent(), "레시피가 없음: " + id);
		}
		var advancements = helper.getLevel().getServer().getAdvancements();
		for (String id : new String[] {"mob_grinder", "backpack"}) {
			helper.assertTrue(advancements.get(Sanupcraft.id(id)) != null, "발전 과제가 없음: " + id);
		}
		helper.succeed();
	}

	@GameTest
	public void m9bLangKeysExistInBothLanguages(GameTestHelper helper) {
		String[] keys = {"fluid.sanupcraft.experience", "block.sanupcraft.mob_grinder", "block.sanupcraft.experience_tank", "item.sanupcraft.backpack",
				"item.sanupcraft.steel_backpack", "block.sanupcraft.mob_grinder.status", "block.sanupcraft.experience_tank.status",
				"block.sanupcraft.experience_tank.contents", "block.sanupcraft.experience_tank.bottle_short",
				"block.sanupcraft.mob_grinder.tooltip", "block.sanupcraft.mob_grinder.tooltip2", "block.sanupcraft.mob_grinder.tooltip3",
				"block.sanupcraft.experience_tank.tooltip", "block.sanupcraft.experience_tank.tooltip2", "block.sanupcraft.experience_tank.tooltip3",
				"item.sanupcraft.backpack.tooltip", "item.sanupcraft.backpack.tooltip2", "item.sanupcraft.steel_backpack.tooltip",
				"item.sanupcraft.steel_backpack.tooltip2", "advancements.sanupcraft.mob_grinder.title", "advancements.sanupcraft.mob_grinder.description",
				"advancements.sanupcraft.backpack.title", "advancements.sanupcraft.backpack.description"};
		for (String file : new String[] {"ko_kr.json", "en_us.json"}) {
			JsonObject lang = readLang(file);
			for (String key : keys) {
				helper.assertTrue(lang.has(key), file + "에 문구가 없음: " + key);
			}
			// 코드가 넘기는 인수 수: 분쇄기 상태 4개, 탱크 상태 2개, 병 1개
			helper.assertTrue(lang.get("block.sanupcraft.mob_grinder.status").getAsString().contains("%4$s"), file + " 분쇄기 상태 인수");
			helper.assertTrue(lang.get("block.sanupcraft.experience_tank.bottle_short").getAsString().contains("%s"), file + " 병 문구 인수");
		}
		helper.succeed();
	}

	@GameTest
	public void playtestProduction2KitGivesGear(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		PlaytestCommand.apply(player, PlaytestCommand.Stage.PRODUCTION2);
		helper.assertTrue(player.getInventory().countItem(ModGrinder.MOB_GRINDER.asItem()) > 0, "몹 분쇄기를 안 줌");
		helper.assertTrue(player.getInventory().countItem(ModGrinder.EXPERIENCE_TANK.asItem()) > 0, "경험치 탱크를 안 줌");
		helper.assertTrue(player.getInventory().countItem(ModBackpack.BACKPACK) > 0, "배낭을 안 줌");
		helper.succeed();
	}

	@GameTest
	public void grinderConfigDefaults(GameTestHelper helper) {
		SanupcraftConfig config = SanupcraftConfig.defaults();
		helper.assertTrue(config.grinderDamage == 6.0, "분쇄기 피해: " + config.grinderDamage);
		helper.assertValueEqual(config.grinderSteamPerHit, MobGrinderBlockEntity.STEAM_PER_HIT, "분쇄기 증기");
		helper.assertValueEqual(config.grinderTicksPerHit, MobGrinderBlockEntity.TICKS_PER_HIT, "분쇄기 간격");
		helper.succeed();
	}

	// ---- 도우미 ----

	private static InteractionResult use(GameTestHelper helper, BlockState state, Player player, BlockHitResult hit, ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return state.useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
	}

	private static JsonObject readLang(String file) {
		try (InputStream in = SanupcraftM9bGameTest.class.getResourceAsStream("/assets/sanupcraft/lang/" + file)) {
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
