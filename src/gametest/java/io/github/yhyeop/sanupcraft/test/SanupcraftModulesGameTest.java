package io.github.yhyeop.sanupcraft.test;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.combat.ModCombat;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.effect.ModEffects;
import io.github.yhyeop.sanupcraft.effect.Resistances;
import io.github.yhyeop.sanupcraft.end.ModEnd;
import io.github.yhyeop.sanupcraft.end.VoidGear;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.AreaMining;
import io.github.yhyeop.sanupcraft.gear.BronzeGear;
import io.github.yhyeop.sanupcraft.gear.ChromeSteelGear;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.ProductionGear;
import io.github.yhyeop.sanupcraft.gear.RivetGunItem;
import io.github.yhyeop.sanupcraft.gear.RivetProjectile;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteamGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.hazard.ToxicGasBlock;
import io.github.yhyeop.sanupcraft.module.GearModule;
import io.github.yhyeop.sanupcraft.module.GearModules;
import io.github.yhyeop.sanupcraft.module.GearWorkbenchMenu;
import io.github.yhyeop.sanupcraft.module.ModModules;
import io.github.yhyeop.sanupcraft.module.ModuleEffects;
import io.github.yhyeop.sanupcraft.module.ModuleSlots;
import io.github.yhyeop.sanupcraft.module.RepairStationBlockEntity;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.resource.ModResources;

/** 확장 3 G3 장비 모듈 · G4 수리 · 계승 검사. */
public class SanupcraftModulesGameTest {
	/** 칸 수: 청동 · 철 0, 강철 · 다이아 1, 크롬강 · 내열 · 증기 장비 · 리벳총 2, 네더라이트 · 공허 3. 종류 · 끼우기 규칙. */
	@GameTest
	public void slotCapacityPerTier(GameTestHelper helper) {
		assertSlots(helper, BronzeGear.BRONZE_CHESTPLATE, 0);
		assertSlots(helper, Items.IRON_PICKAXE, 0);
		assertSlots(helper, SteelGear.STEEL_HELMET, 1);
		assertSlots(helper, ModCombat.STEEL_SPEAR, 1);
		assertSlots(helper, Items.DIAMOND_PICKAXE, 1);
		assertSlots(helper, ChromeSteelGear.CHROME_STEEL_SWORD, 2);
		assertSlots(helper, HeatproofGear.HEATPROOF_BOOTS, 2);
		assertSlots(helper, ProductionGear.STEAM_DRILL, 2);
		assertSlots(helper, SteamGear.RIVET_GUN, 2);
		assertSlots(helper, ModCombat.STEAM_HAMMER, 2);
		assertSlots(helper, Items.NETHERITE_CHESTPLATE, 3);
		assertSlots(helper, VoidGear.VOID_SWORD, 3);
		assertSlots(helper, ModModules.item(GearModule.MAGNET), 0);

		ItemStack sword = new ItemStack(ChromeSteelGear.CHROME_STEEL_SWORD);
		helper.assertValueEqual(GearModules.check(sword, GearModule.MAGNET), GearModules.Check.WRONG_KIND, "검에 갑옷 모듈");
		helper.assertValueEqual(GearModules.check(sword, GearModule.EFFICIENCY), GearModules.Check.WRONG_KIND, "검에 도구 모듈");
		helper.assertTrue(GearModules.install(sword, GearModule.KNOCKBACK), "검에 넉백이 안 끼워짐");
		helper.assertValueEqual(GearModules.check(sword, GearModule.KNOCKBACK), GearModules.Check.DUPLICATE, "같은 모듈 두 개");
		helper.assertValueEqual(GearModules.check(new ItemStack(BronzeGear.BRONZE_PICKAXE), GearModule.AREA), GearModules.Check.NO_SLOTS, "청동에 모듈");
		helper.assertValueEqual(GearModules.check(new ItemStack(SteelGear.STEEL_HAMMER), GearModule.AREA), GearModules.Check.WRONG_KIND, "해머에 범위");
		helper.assertValueEqual(GearModules.check(new ItemStack(ProductionGear.STEAM_DRILL), GearModule.AREA), GearModules.Check.WRONG_KIND, "드릴에 범위");
		helper.assertValueEqual(GearModules.check(new ItemStack(SteamGear.RIVET_GUN), GearModule.SCATTER), GearModules.Check.OK, "리벳총에 산탄");
		helper.assertValueEqual(GearModules.check(new ItemStack(SteamGear.RIVET_GUN), GearModule.KNOCKBACK), GearModules.Check.WRONG_KIND, "리벳총에 넉백");

		ItemStack pick = new ItemStack(Items.NETHERITE_PICKAXE);
		helper.assertTrue(GearModules.install(pick, GearModule.LUCK), "행운");
		helper.assertValueEqual(GearModules.check(pick, GearModule.SILK_TOUCH), GearModules.Check.CONFLICT, "행운 + 섬세한 손길");
		helper.assertTrue(GearModules.install(pick, GearModule.AREA) && GearModules.install(pick, GearModule.EFFICIENCY), "세 칸 채우기");
		helper.assertValueEqual(GearModules.check(pick, GearModule.AUTO_SMELT), GearModules.Check.FULL, "네 번째 모듈");
		helper.assertTrue(AreaMining.isAreaTool(pick), "범위 모듈 곡괭이가 3x3 도구가 아님");
		helper.assertTrue(GearModules.remove(pick, GearModule.LUCK) && GearModules.install(pick, GearModule.AUTO_SMELT), "빼고 다시 끼우기");
		// 다른 묶음 장비용 한 줄 등록
		ModuleSlots.register(Items.IRON_SWORD, 1);
		helper.assertValueEqual(ModuleSlots.capacity(new ItemStack(Items.IRON_SWORD)), 1, "register 한 줄");
		helper.succeed();
	}

	/** 장비 작업대 화면: Shift 클릭으로 장비 · 모듈 넣기, 맞지 않는 모듈 · 같은 모듈은 안 들어가고, 꺼내면 장비에서 빠진다. */
	@GameTest
	public void workbenchInstallsAndRemoves(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, ModModules.GEAR_WORKBENCH);
		GearWorkbenchMenu menu = new GearWorkbenchMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(pos)));
		int hotbar = GearWorkbenchMenu.INVENTORY_START + 27;
		player.getInventory().setItem(0, new ItemStack(ChromeSteelGear.CHROME_STEEL_SWORD));
		player.getInventory().setItem(1, new ItemStack(ModModules.item(GearModule.KNOCKBACK), 2));
		player.getInventory().setItem(2, new ItemStack(ModModules.item(GearModule.MAGNET)));
		player.getInventory().setItem(3, new ItemStack(Items.STICK));

		menu.quickMoveStack(player, hotbar + 3);
		helper.assertTrue(menu.gear().isEmpty(), "막대기가 장비 칸에 들어감");
		menu.quickMoveStack(player, hotbar);
		helper.assertTrue(menu.gear().is(ChromeSteelGear.CHROME_STEEL_SWORD), "검이 장비 칸에 안 들어감");
		helper.assertValueEqual(menu.capacity(), 2, "크롬강 검 칸 수");
		helper.assertTrue(menu.getSlot(GearWorkbenchMenu.FIRST_MODULE_SLOT + 1).isActive(), "둘째 모듈 칸이 닫힘");
		helper.assertFalse(menu.getSlot(GearWorkbenchMenu.FIRST_MODULE_SLOT + 2).isActive(), "셋째 모듈 칸이 열림");

		menu.quickMoveStack(player, hotbar + 1);
		helper.assertTrue(GearModules.has(menu.gear(), GearModule.KNOCKBACK), "넉백이 안 끼워짐");
		helper.assertValueEqual(player.getInventory().getItem(1).getCount(), 1, "넉백 모듈 하나만 써야 함");
		menu.quickMoveStack(player, hotbar + 1);
		helper.assertValueEqual(player.getInventory().getItem(1).getCount(), 1, "같은 모듈이 두 번 들어감");
		menu.quickMoveStack(player, hotbar + 2);
		helper.assertValueEqual(player.getInventory().getItem(2).getCount(), 1, "검에 자석이 들어감");
		helper.assertFalse(menu.getSlot(GearWorkbenchMenu.FIRST_MODULE_SLOT + 2).mayPlace(new ItemStack(ModModules.item(GearModule.KNOCKBACK))),
				"닫힌 칸이 모듈을 받음");

		// 모듈 칸에서 꺼내면 장비에서 빠지고 아이템으로 돌아온다
		menu.quickMoveStack(player, GearWorkbenchMenu.FIRST_MODULE_SLOT);
		helper.assertFalse(GearModules.has(menu.gear(), GearModule.KNOCKBACK), "꺼냈는데 장비에 남음");
		helper.assertFalse(menu.gear().has(ModModules.MODULES), "빈 모듈 목록 컴포넌트가 남음");
		helper.assertValueEqual(player.getInventory().countItem(ModModules.item(GearModule.KNOCKBACK)), 2, "꺼낸 모듈이 인벤토리로 안 옴");

		// 다시 끼우고 장비를 꺼내면 모듈은 장비에 남고 모듈 칸은 빈다
		menu.quickMoveStack(player, hotbar + 1);
		menu.quickMoveStack(player, GearWorkbenchMenu.GEAR_SLOT);
		helper.assertTrue(menu.gear().isEmpty(), "장비가 안 나옴");
		helper.assertFalse(menu.getSlot(GearWorkbenchMenu.FIRST_MODULE_SLOT).hasItem(), "장비를 뺐는데 모듈 칸에 남음");
		ItemStack out = ItemStack.EMPTY;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(ChromeSteelGear.CHROME_STEEL_SWORD)) {
				out = stack;
			}
		}
		helper.assertTrue(GearModules.has(out, GearModule.KNOCKBACK), "꺼낸 검에 모듈이 없음");
		helper.assertValueEqual(player.getInventory().countItem(ModModules.item(GearModule.KNOCKBACK)), 1, "모듈 수가 늘거나 줄음");
		// 다시 올리면 모듈 칸에 보인다
		int swordSlot = player.getInventory().findSlotMatchingItem(out);
		menu.quickMoveStack(player, swordSlot < 9 ? hotbar + swordSlot : GearWorkbenchMenu.INVENTORY_START + swordSlot - 9);
		helper.assertTrue(menu.getSlot(GearWorkbenchMenu.FIRST_MODULE_SLOT).getItem().is(ModModules.item(GearModule.KNOCKBACK)), "다시 올린 검의 모듈이 안 보임");
		helper.succeed();
	}

	/** 자석: 갑옷에 끼우면 6칸 안 아이템이 몸 쪽으로 온다. */
	@GameTest(maxTicks = 120)
	public void magnetPullsItems(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
		ItemStack chest = new ItemStack(SteelGear.STEEL_CHESTPLATE);
		GearModules.install(chest, GearModule.MAGNET);
		player.setItemSlot(EquipmentSlot.CHEST, chest);
		Vec3 start = helper.absoluteVec(new Vec3(6.5, 1.2, 1.5));
		ItemEntity item = new ItemEntity(helper.getLevel(), start.x, start.y, start.z, new ItemStack(Items.COBBLESTONE), 0.0, 0.0, 0.0);
		item.setNoPickUpDelay();
		helper.getLevel().addFreshEntity(item);
		helper.assertValueEqual(ModuleEffects.magnet(helper.makeMockPlayer(GameType.SURVIVAL)), 0, "모듈 없는 사람이 끌어당김");
		helper.onEachTick(() -> ModuleEffects.tick(player, 0));
		helper.succeedWhen(() -> helper.assertTrue(item.position().distanceTo(player.position()) < 1.6,
				"아이템이 안 끌려옴: " + item.position().distanceTo(player.position())));
	}

	/** 효율 · 범위 · 넉백 · 증기 추진: 든 장비 · 입은 갑옷에 따라 플레이어 속성이 붙고 떨어진다. */
	@GameTest
	public void attributeModules(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		double base = player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
		ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
		BlockState stone = Blocks.STONE.defaultBlockState();
		player.setItemInHand(InteractionHand.MAIN_HAND, pick.copy());
		ModuleEffects.updateAttributes(player);
		float plainSpeed = player.getDestroySpeed(stone);
		GearModules.install(pick, GearModule.EFFICIENCY);
		player.setItemInHand(InteractionHand.MAIN_HAND, pick);
		ModuleEffects.updateAttributes(player);
		helper.assertValueEqual(player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), base * 1.3, "효율 모듈 캐는 속도 속성");
		helper.assertTrue(Math.abs(player.getDestroySpeed(stone) / plainSpeed - 1.3F) < 0.01F, "효율 모듈로 실제 캐는 속도가 1.3배가 아님");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
		ModuleEffects.updateAttributes(player);
		helper.assertValueEqual(player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), base, "바꿔 들었는데 효율이 남음");
		ItemStack area = new ItemStack(Items.DIAMOND_PICKAXE);
		GearModules.install(area, GearModule.AREA);
		player.setItemInHand(InteractionHand.MAIN_HAND, area);
		ModuleEffects.updateAttributes(player);
		helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED) - base * 0.6) < 1e-6, "범위 모듈 속도 60%");

		ItemStack sword = new ItemStack(ChromeSteelGear.CHROME_STEEL_SWORD);
		GearModules.install(sword, GearModule.KNOCKBACK);
		player.setItemInHand(InteractionHand.MAIN_HAND, sword);
		double jump = player.getAttributeValue(Attributes.JUMP_STRENGTH);
		ItemStack boots = new ItemStack(SteelGear.STEEL_BOOTS);
		GearModules.install(boots, GearModule.STEAM_JUMP);
		player.setItemSlot(EquipmentSlot.FEET, boots);
		ModuleEffects.updateAttributes(player);
		helper.assertValueEqual(player.getAttributeValue(Attributes.ATTACK_KNOCKBACK), 1.0, "넉백 모듈");
		helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.JUMP_STRENGTH) - (jump + 0.1)) < 1e-6, "증기 추진 점프");
		helper.succeed();
	}

	/** 자동 제련 · 섬세한 손길 · 행운: 블록 드롭이 바뀐다. */
	@GameTest
	public void dropModules(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack smelter = new ItemStack(Items.NETHERITE_PICKAXE);
		GearModules.install(smelter, GearModule.AUTO_SMELT);
		List<ItemStack> iron = Block.getDrops(Blocks.IRON_ORE.defaultBlockState(), level, pos, null, player, smelter);
		helper.assertTrue(iron.size() == 1 && iron.getFirst().is(Items.IRON_INGOT), "철광석 자동 제련이 철 주괴가 아님: " + iron);
		List<ItemStack> chrome = Block.getDrops(ModResources.DEEPSLATE_CHROMITE_ORE.defaultBlockState(), level, pos, null, player, smelter);
		helper.assertTrue(chrome.stream().allMatch(s -> s.is(ModResources.CHROMIUM_INGOT)) && !chrome.isEmpty(), "크롬철석 자동 제련: " + chrome);
		List<ItemStack> stone = Block.getDrops(Blocks.STONE.defaultBlockState(), level, pos, null, player, smelter);
		helper.assertTrue(stone.size() == 1 && stone.getFirst().is(Items.STONE), "돌(조약돌 → 돌) 자동 제련: " + stone);

		ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
		GearModules.install(silk, GearModule.SILK_TOUCH);
		helper.assertTrue(Block.getDrops(Blocks.IRON_ORE.defaultBlockState(), level, pos, null, player, silk).getFirst().is(Items.IRON_ORE), "섬세한 손길 철광석");
		helper.assertTrue(Block.getDrops(Blocks.GLASS.defaultBlockState(), level, pos, null, player, silk).getFirst().is(Items.GLASS), "섬세한 손길 유리");
		helper.assertTrue(silk.getEnchantments().isEmpty(), "섬세한 손길 모듈이 도구에 마법을 붙임");

		ItemStack lucky = new ItemStack(Items.DIAMOND_PICKAXE);
		GearModules.install(lucky, GearModule.LUCK);
		ItemStack plain = new ItemStack(Items.DIAMOND_PICKAXE);
		int luckyTotal = 0;
		int plainTotal = 0;
		for (int i = 0; i < 300; i++) {
			luckyTotal += count(Block.getDrops(Blocks.IRON_ORE.defaultBlockState(), level, pos, null, player, lucky), Items.RAW_IRON);
			plainTotal += count(Block.getDrops(Blocks.IRON_ORE.defaultBlockState(), level, pos, null, player, plain), Items.RAW_IRON);
		}
		helper.assertValueEqual(plainTotal, 300, "모듈 없는 곡괭이의 원석 철");
		helper.assertTrue(luckyTotal > 340 && luckyTotal < 470, "행운 모듈 원석 철 300번 (행운 I이면 약 400): " + luckyTotal);
		helper.assertTrue(lucky.getEnchantments().isEmpty(), "행운 모듈이 도구에 마법을 붙임");
		helper.succeed();
	}

	/** 방지 모듈: 강철이 아닌 갑옷 한 부위에 녹 방지 → 녹 면역. 과열 · 침식 방지, 가스 필터. */
	@GameTest
	public void resistanceModules(GameTestHelper helper) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		helper.assertFalse(Resistances.rustImmune(zombie), "맨몸이 녹 면역");
		ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
		helper.assertTrue(GearModules.install(chest, GearModule.RUST_PROOF), "다이아 흉갑에 녹 방지가 안 끼워짐");
		zombie.setItemSlot(EquipmentSlot.CHEST, chest);
		helper.assertTrue(Resistances.rustImmune(zombie), "녹 방지 모듈인데 녹 면역이 아님");
		helper.assertFalse(ModEffects.inflict(zombie, ModEffects.RUST, 100, 1, null), "녹 방지 모듈에 녹이 걸림");
		helper.assertFalse(Resistances.heatResistant(zombie), "녹 방지가 과열도 막음");

		ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
		GearModules.install(helmet, GearModule.HEAT_PROOF);
		GearModules.install(helmet, GearModule.VOID_PROOF);
		GearModules.install(helmet, GearModule.GAS_FILTER);
		zombie.setItemSlot(EquipmentSlot.HEAD, helmet);
		helper.assertTrue(Resistances.heatResistant(zombie), "과열 방지 모듈");
		helper.assertTrue(Resistances.voidResistant(zombie), "침식 방지 모듈");
		helper.assertFalse(ModEffects.inflict(zombie, ModEffects.OVERHEAT, 100, 1, null), "과열 방지인데 과열이 걸림");
		// 가스: 좀비는 원래 독에 안 걸려서 돼지로
		net.minecraft.world.entity.animal.pig.Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 1));
		ItemStack mask = new ItemStack(Items.NETHERITE_HELMET);
		GearModules.install(mask, GearModule.GAS_FILTER);
		pig.setItemSlot(EquipmentSlot.HEAD, mask);
		helper.assertTrue(Resistances.toxicGasImmune(pig), "가스 필터 판정");
		ToxicGasBlock.poison(pig);
		helper.assertFalse(pig.hasEffect(MobEffects.POISON), "가스 필터인데 독이 걸림");
		pig.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
		ToxicGasBlock.poison(pig);
		helper.assertTrue(pig.hasEffect(MobEffects.POISON), "가스 필터를 벗었는데 독이 안 걸림");
		helper.succeed();
	}

	/** 리벳총 연발: 쿨다운 반. 산탄: 리벳 1개로 3발, 한 발 피해 반. */
	@GameTest
	public void rivetGunModules(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
		ItemStack rivets = new ItemStack(SteamGear.RIVET, 10);
		ItemStack canister = new ItemStack(SteamGear.STEAM_CANISTER);
		SteamCharge.set(canister, 500);
		player.getInventory().setItem(1, rivets);
		player.getInventory().setItem(2, canister);
		int base = SanupcraftConfig.current().rivetGunCooldown;

		ItemStack rapid = new ItemStack(SteamGear.RIVET_GUN);
		GearModules.install(rapid, GearModule.RAPID_FIRE);
		helper.assertValueEqual(RivetGunItem.cooldownTicks(new ItemStack(SteamGear.RIVET_GUN)), base, "모듈 없는 쿨다운");
		helper.assertValueEqual(RivetGunItem.cooldownTicks(rapid), base / 2, "연발 쿨다운");
		player.setItemInHand(InteractionHand.MAIN_HAND, rapid);
		helper.assertTrue(RivetGunItem.fire(helper.getLevel(), player, rapid) == RivetGunItem.Result.FIRED, "연발 리벳총이 못 쏨");
		for (int i = 0; i < base / 2; i++) {
			player.getCooldowns().tick();
		}
		helper.assertFalse(player.getCooldowns().isOnCooldown(rapid), "연발인데 쿨다운이 반에 안 풀림");

		ItemStack scatter = new ItemStack(SteamGear.RIVET_GUN);
		GearModules.install(scatter, GearModule.SCATTER);
		player.setItemInHand(InteractionHand.MAIN_HAND, scatter);
		helper.getLevel().getEntitiesOfClass(RivetProjectile.class, player.getBoundingBox().inflate(8.0)).forEach(RivetProjectile::discard);
		int before = rivets.getCount();
		helper.assertTrue(RivetGunItem.fire(helper.getLevel(), player, scatter) == RivetGunItem.Result.FIRED, "산탄 리벳총이 못 쏨");
		helper.assertValueEqual(rivets.getCount(), before - 1, "산탄이 리벳을 하나만 써야 함");
		List<RivetProjectile> shots = helper.getLevel().getEntitiesOfClass(RivetProjectile.class, player.getBoundingBox().inflate(8.0));
		helper.assertValueEqual(shots.size(), 3, "산탄 발 수");
		float pellet = (float) SanupcraftConfig.current().rivetGunDamage * 0.5F;
		for (RivetProjectile shot : shots) {
			helper.assertValueEqual(shot.damage(), pellet, "산탄 한 발 피해");
		}
		helper.succeed();
	}

	/** 산탄 세 발이 한 몹에 동시에 맞으면 무적 시간 없이 셋 다 들어간다. */
	@GameTest(maxTicks = 100)
	public void scatterPelletsAllHit(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
		Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(6, 1, 3));
		cow.setNoAi(true);
		float before = cow.getHealth();
		for (int i = 0; i < 3; i++) {
			RivetProjectile rivet = new RivetProjectile(helper.getLevel(), player);
			rivet.setPos(helper.absoluteVec(new Vec3(2.5, 1.5, 3.5)));
			rivet.setPellet(2.0F);
			rivet.shoot(1.0, 0.0, 0.0, RivetProjectile.SPEED, 0.0F);
			helper.getLevel().addFreshEntity(rivet);
		}
		helper.succeedWhen(() -> helper.assertValueEqual(cow.getHealth(), before - 6.0F, "산탄 세 발 피해"));
	}

	/** 증기 수리대: 재료 + 증기로 내구도를 채우고 마법 부여 · 모듈 · 이름은 그대로. 맞지 않는 재료 · 증기 없음은 안 고친다. */
	@GameTest
	public void repairStationRestoresDurability(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, ModModules.STEAM_REPAIR_STATION);
		RepairStationBlockEntity station = helper.getBlockEntity(pos, RepairStationBlockEntity.class);
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ItemStack sword = new ItemStack(SteelGear.STEEL_SWORD);
		sword.enchant(sharpness, 3);
		GearModules.install(sword, GearModule.KNOCKBACK);
		sword.set(DataComponents.CUSTOM_NAME, Component.literal("고칠 검"));
		sword.setDamageValue(400);
		helper.assertTrue(station.canPlaceItem(RepairStationBlockEntity.SLOT_GEAR, sword), "강철 검을 안 받음");
		helper.assertFalse(station.canPlaceItem(RepairStationBlockEntity.SLOT_GEAR, new ItemStack(Items.STICK)), "막대기를 장비로 받음");
		station.setItem(RepairStationBlockEntity.SLOT_GEAR, sword);
		helper.assertFalse(station.canPlaceItem(RepairStationBlockEntity.SLOT_MATERIAL, new ItemStack(Items.IRON_INGOT)), "철 주괴를 강철 검 재료로 받음");

		// 증기는 있는데 재료가 없으면 안 고침
		station.acceptSteam(RepairStationBlockEntity.STEAM_CAPACITY);
		helper.assertValueEqual(station.repairTick(), 0, "재료 없이 고침");
		station.setItem(RepairStationBlockEntity.SLOT_MATERIAL, new ItemStack(ModItems.STEEL_INGOT, 2));
		int repaired = 0;
		for (int i = 0; i < 100; i++) {
			repaired += station.repairTick();
		}
		ItemStack fixed = station.gear();
		int perIngot = fixed.getMaxDamage() * SanupcraftConfig.current().repairMaterialPercent / 100;
		helper.assertValueEqual(repaired, perIngot * 2, "강철 주괴 2개만큼 고침");
		helper.assertValueEqual(fixed.getDamageValue(), 400 - perIngot * 2, "닳은 정도");
		helper.assertTrue(station.material().isEmpty(), "재료를 다 안 씀");
		helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(sharpness, fixed), 3, "날카로움이 사라짐");
		helper.assertTrue(GearModules.has(fixed, GearModule.KNOCKBACK), "모듈이 사라짐");
		helper.assertValueEqual(fixed.getHoverName().getString(), "고칠 검", "이름이 사라짐");
		helper.assertTrue(station.steamStored() < RepairStationBlockEntity.STEAM_CAPACITY, "증기를 안 씀");

		// 증기가 떨어지면 멈춤: 새 수리대에 증기 20만 (4틱치)
		BlockPos pos2 = new BlockPos(3, 1, 1);
		helper.setBlock(pos2, ModModules.STEAM_REPAIR_STATION);
		RepairStationBlockEntity small = helper.getBlockEntity(pos2, RepairStationBlockEntity.class);
		ItemStack worn = new ItemStack(SteelGear.STEEL_SWORD);
		worn.setDamageValue(400);
		small.setItem(RepairStationBlockEntity.SLOT_GEAR, worn);
		small.setItem(RepairStationBlockEntity.SLOT_MATERIAL, new ItemStack(ModItems.STEEL_INGOT, 4));
		small.acceptSteam(SanupcraftConfig.current().repairSteamPerTick * 4);
		int total = 0;
		for (int i = 0; i < 20; i++) {
			total += small.repairTick();
		}
		int rate = (worn.getMaxDamage() + SanupcraftConfig.current().repairTicksForFull - 1) / SanupcraftConfig.current().repairTicksForFull;
		helper.assertValueEqual(total, rate * 4, "증기 4틱치만큼만 고침");
		helper.assertValueEqual(small.repairTick(), 0, "증기 없이 고침");
		helper.assertTrue(station.canTakeItemThroughFace(RepairStationBlockEntity.SLOT_GEAR, new ItemStack(SteelGear.STEEL_SWORD), net.minecraft.core.Direction.DOWN),
				"다 고친 장비가 아래로 안 빠짐");
		helper.assertFalse(small.canTakeItemThroughFace(RepairStationBlockEntity.SLOT_GEAR, small.gear(), net.minecraft.core.Direction.DOWN), "덜 고친 장비가 아래로 빠짐");
		helper.succeed();
	}

	/** 계승: 강철 → 내열(제작대 모양), 강철 → 크롬강(변환), 강철 부츠 → 증기 부츠(무형)에 마법 부여 · 모듈 · 이름 · 닳은 정도가 간다. */
	@GameTest
	public void craftingUpgradesKeepEverything(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Holder<Enchantment> protection = enchantment(helper, Enchantments.PROTECTION);
		ItemStack helmet = new ItemStack(SteelGear.STEEL_HELMET);
		helmet.enchant(protection, 2);
		GearModules.install(helmet, GearModule.RUST_PROOF);
		helmet.set(DataComponents.CUSTOM_NAME, Component.literal("물려줄 투구"));
		helmet.setDamageValue(30);
		ItemStack g = new ItemStack(ModItems.GEAR_PART);
		ItemStack shard = new ItemStack(ModItems.CORE_SHARD);
		ItemStack heatproof = craft(level, 3, 2, List.of(g, shard, g, g, helmet, g));
		helper.assertTrue(heatproof.is(HeatproofGear.HEATPROOF_HELMET), "강철 투구 → 내열 투구가 아님: " + heatproof);
		assertCarried(helper, heatproof, protection, GearModule.RUST_PROOF, "물려줄 투구", 30);

		ItemStack chromeHelmet = new ItemStack(ChromeSteelGear.CHROME_STEEL_HELMET);
		GearModules.install(chromeHelmet, GearModule.MAGNET);
		GearModules.install(chromeHelmet, GearModule.NIGHT_VISION);
		ItemStack fromChrome = craft(level, 3, 2, List.of(g, shard, g, g, chromeHelmet, g));
		helper.assertTrue(GearModules.has(fromChrome, GearModule.MAGNET) && GearModules.has(fromChrome, GearModule.NIGHT_VISION), "크롬강 → 내열에서 모듈 둘");

		ItemStack steelSword = new ItemStack(SteelGear.STEEL_SWORD);
		GearModules.install(steelSword, GearModule.KNOCKBACK);
		ItemStack chromeSword = craft(level, 3, 1, List.of(steelSword, new ItemStack(ModResources.CHROMIUM_INGOT), new ItemStack(ModResources.CHROMIUM_INGOT)));
		helper.assertTrue(chromeSword.is(ChromeSteelGear.CHROME_STEEL_SWORD) && GearModules.has(chromeSword, GearModule.KNOCKBACK), "강철 → 크롬강 검 모듈");

		ItemStack boots = new ItemStack(SteelGear.STEEL_BOOTS);
		boots.enchant(protection, 1);
		GearModules.install(boots, GearModule.STEAM_JUMP);
		ItemStack steamBoots = craft(level, 2, 2, List.of(boots, g, g, new ItemStack(io.github.yhyeop.sanupcraft.power.ModPower.STEAM_PIPE)));
		helper.assertTrue(steamBoots.is(SteamGear.STEAM_BOOTS), "증기 부츠가 아님: " + steamBoots);
		helper.assertTrue(GearModules.has(steamBoots, GearModule.STEAM_JUMP), "증기 부츠에 모듈이 안 감");
		helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(protection, steamBoots), 1, "증기 부츠 마법 부여");
		helper.succeed();
	}

	/** 계승 (기계): 정밀 조립기에서 네더라이트 투구 + 공허 핵 → 공허 투구에 마법 부여 · 모듈 · 이름 · 닳은 정도가 간다. */
	@GameTest(maxTicks = 100)
	public void precisionUpgradeKeepsEverything(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModMachines.PRECISION_ASSEMBLER);
		MachineBlockEntity machine = helper.getBlockEntity(pos, MachineBlockEntity.class);
		Holder<Enchantment> protection = enchantment(helper, Enchantments.PROTECTION);
		ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
		helmet.enchant(protection, 4);
		GearModules.install(helmet, GearModule.NIGHT_VISION);
		GearModules.install(helmet, GearModule.VOID_PROOF);
		helmet.set(DataComponents.CUSTOM_NAME, Component.literal("공허로"));
		helmet.setDamageValue(50);
		machine.setItem(MachineBlockEntity.SLOT_INPUT, helmet);
		machine.setItem(MachineBlockEntity.SLOT_INPUT2, new ItemStack(ModEnd.VOID_CORE));
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(pos);
		for (int i = 0; i < 1200 && machine.result().isEmpty(); i++) {
			machine.acceptHighPressureSteam(MachineBlockEntity.STEAM_CAPACITY);
			MachineBlockEntity.serverTick(level, abs, level.getBlockState(abs), machine);
		}
		ItemStack result = machine.result();
		helper.assertTrue(result.is(VoidGear.VOID_HELMET), "공허 투구가 안 나옴: " + result);
		assertCarried(helper, result, protection, GearModule.VOID_PROOF, "공허로", 50);
		helper.assertTrue(GearModules.has(result, GearModule.NIGHT_VISION), "둘째 모듈이 안 감");
		helper.succeed();
	}

	/** 실테스트 꾸러미 `modules` · 발전 과제 · 설정. */
	@GameTest
	public void playtestModulesKit(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		PlaytestCommand.apply(player, PlaytestCommand.Stage.MODULES);
		helper.assertTrue(player.getInventory().countItem(ModModules.GEAR_WORKBENCH.asItem()) > 0, "장비 작업대를 주지 않음");
		helper.assertTrue(player.getInventory().countItem(ModModules.STEAM_REPAIR_STATION.asItem()) > 0, "증기 수리대를 주지 않음");
		for (GearModule module : GearModule.values()) {
			helper.assertTrue(player.getInventory().countItem(module.item()) > 0, "모듈을 주지 않음: " + module.id());
		}
		boolean moduleHelmet = false;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			moduleHelmet |= stack.is(SteelGear.STEEL_HELMET) && GearModules.has(stack, GearModule.RUST_PROOF);
		}
		helper.assertTrue(moduleHelmet, "모듈 낀 강철 투구를 주지 않음");
		var advancements = helper.getLevel().getServer().getAdvancements();
		helper.assertTrue(advancements.get(Sanupcraft.id("gear_workbench")) != null && advancements.get(Sanupcraft.id("steam_repair_station")) != null,
				"발전 과제");
		String json = SanupcraftConfig.defaults().toJson();
		helper.assertTrue(json.contains("\"modules\"") && json.contains("\"repairStation\"") && json.contains("scatterPellets"), "설정 구역");
		helper.succeed();
	}

	private static void assertCarried(GameTestHelper helper, ItemStack out, Holder<Enchantment> enchantment, GearModule module, String name, int damage) {
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(enchantment, out) > 0, "마법 부여가 안 옮겨짐: " + out);
		helper.assertTrue(GearModules.has(out, module), "모듈이 안 옮겨짐: " + module.id());
		helper.assertValueEqual(out.getHoverName().getString(), name, "이름이 안 옮겨짐");
		helper.assertValueEqual(out.getDamageValue(), damage, "닳은 정도가 안 옮겨짐");
	}

	private static void assertSlots(GameTestHelper helper, Item item, int slots) {
		helper.assertValueEqual(ModuleSlots.capacity(new ItemStack(item)), slots, "모듈 칸: " + item);
	}

	private static int count(List<ItemStack> drops, Item item) {
		int total = 0;
		for (ItemStack stack : drops) {
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static Holder<Enchantment> enchantment(GameTestHelper helper, net.minecraft.resources.ResourceKey<Enchantment> key) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	private static ItemStack craft(ServerLevel level, int width, int height, List<ItemStack> items) {
		CraftingInput input = CraftingInput.of(width, height, items);
		return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).map(h -> h.value().assemble(input)).orElse(ItemStack.EMPTY);
	}
}
