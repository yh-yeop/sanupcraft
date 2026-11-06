package io.github.yhyeop.sanupcraft.test;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.hazard.LooseRockFeature;
import io.github.yhyeop.sanupcraft.hazard.ModHazard;
import io.github.yhyeop.sanupcraft.hazard.ToxicGasBlock;
import io.github.yhyeop.sanupcraft.hazard.ToxicGasPocketFeature;
import io.github.yhyeop.sanupcraft.mining.blast.BlastChargeBlock;
import io.github.yhyeop.sanupcraft.mining.blast.BlastCharges;
import io.github.yhyeop.sanupcraft.mining.blast.ModBlast;
import io.github.yhyeop.sanupcraft.mining.blast.RemoteDetonatorItem;

/** 확장 3: D3 증기 발파 (발파 장약 · 원격 기폭기)와 D5 위험 요소 (유독 가스 · 무른 바위). */
public class SanupcraftBlastHazardGameTest {
	private static final String ALONE = "sanupcraft-test:alone";
	private static final String ALONE_HAZARD = "sanupcraft-test:alone_hazard";
	private static final BlockPos CENTER = new BlockPos(3, 3, 3);

	// ---- 도우미 ----

	private static void fill(GameTestHelper helper, BlockPos from, BlockPos to, Block block) {
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			helper.setBlock(pos.immutable(), block);
		}
	}

	/** 0..6 칸을 돌로 채운다 (가운데 5x5x5 둘레에 돌 한 겹이 남게). */
	private static void stoneBlock(GameTestHelper helper) {
		fill(helper, new BlockPos(0, 0, 0), new BlockPos(6, 6, 6), Blocks.STONE);
	}

	/** 장소 둘레의 아이템 개수 (종류별). */
	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(CENTER)).inflate(8.0);
		int sum = 0;
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area)) {
			if (entity.getItem().is(item)) {
				sum += entity.getItem().getCount();
			}
		}
		return sum;
	}

	private static int airCount(GameTestHelper helper, BlockPos from, BlockPos to) {
		int air = 0;
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			if (helper.getBlockState(pos.immutable()).isAir()) {
				air++;
			}
		}
		return air;
	}

	// ---- D3 발파 장약 ----

	/** 5x5x5를 캐서 아이템으로 만든다. 상자 · 우리 기계 · 기반암은 그대로, 둘레 한 겹도 그대로. */
	@GameTest(environment = ALONE, maxTicks = 100)
	public void chargeMinesCubeIntoItems(GameTestHelper helper) {
		stoneBlock(helper);
		helper.setBlock(CENTER, ModBlast.BLAST_CHARGE);
		helper.setBlock(new BlockPos(3, 4, 3), Blocks.CHEST);
		helper.setBlock(new BlockPos(2, 3, 2), ModMachines.CRUSHER);
		helper.setBlock(new BlockPos(4, 4, 4), Blocks.BEDROCK);
		helper.setBlock(new BlockPos(2, 4, 2), Blocks.OBSIDIAN);
		ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(3, 4, 3), ChestBlockEntity.class);
		chest.setItem(0, new ItemStack(Items.DIAMOND, 3));

		int mined = BlastCharges.detonate(helper.getLevel(), helper.absolutePos(CENTER));
		// 125칸 - 장약 자신 - 상자 - 분쇄기 - 기반암 = 121칸을 캠 (그 중 흑요석 1)
		helper.assertValueEqual(mined, 121, "캔 블록 수");
		helper.assertValueEqual(dropped(helper, Items.COBBLESTONE), 120, "조약돌 아이템 수");
		helper.assertValueEqual(dropped(helper, Items.OBSIDIAN), 1, "흑요석 아이템 수");
		helper.assertBlockPresent(Blocks.CHEST, new BlockPos(3, 4, 3));
		helper.assertBlockPresent(ModMachines.CRUSHER, new BlockPos(2, 3, 2));
		helper.assertBlockPresent(Blocks.BEDROCK, new BlockPos(4, 4, 4));
		helper.assertTrue(chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 3, "상자 내용물이 바뀜");
		helper.assertTrue(helper.getBlockState(CENTER).isAir(), "장약 자리가 안 비었다");
		helper.assertBlockPresent(Blocks.STONE, new BlockPos(0, 3, 3));
		helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 3, 3));
		helper.assertBlockPresent(Blocks.STONE, new BlockPos(3, 6, 3));
		helper.assertBlockPresent(Blocks.STONE, new BlockPos(3, 0, 3));
		// 5x5x5 안에서 빈 칸: 125 - 상자 - 분쇄기 - 기반암
		helper.assertValueEqual(airCount(helper, new BlockPos(1, 1, 1), new BlockPos(5, 5, 5)), 122, "빈 칸 수");
		helper.succeed();
	}

	/** 둘레 생물은 피해 4와 밀려남을 받는다. 멀리 있는 것은 그대로. */
	@GameTest(environment = ALONE, maxTicks = 100)
	public void chargeHurtsAndPushesMobs(GameTestHelper helper) {
		fill(helper, new BlockPos(0, 0, 0), new BlockPos(7, 0, 7), Blocks.STONE);
		helper.setBlock(new BlockPos(3, 1, 3), ModBlast.BLAST_CHARGE);
		Cow near = helper.spawn(EntityTypes.COW, new BlockPos(6, 1, 3));
		near.setNoAi(true);
		float before = near.getHealth();
		BlastCharges.detonate(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)));
		helper.assertValueEqual(near.getHealth(), before - 4.0F, "가까운 소가 입은 피해");
		helper.assertTrue(near.getDeltaMovement().x > 0.1, "밀려나지 않음: " + near.getDeltaMovement());
		helper.succeed();
	}

	/** 레드스톤 신호를 받으면 불이 붙고 (lit), 심지(20틱) 뒤 터진다. */
	@GameTest(environment = ALONE, maxTicks = 100)
	public void redstoneTriggersAfterFuse(GameTestHelper helper) {
		stoneBlock(helper);
		helper.setBlock(CENTER, ModBlast.BLAST_CHARGE);
		helper.setBlock(new BlockPos(3, 3, 4), Blocks.REDSTONE_BLOCK); // 장약 옆에서 신호를 준다
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(helper.getBlockState(CENTER).is(ModBlast.BLAST_CHARGE), "너무 일찍 터짐");
			helper.assertTrue(helper.getBlockState(CENTER).getValue(BlastChargeBlock.LIT), "불이 안 붙음");
		});
		helper.runAfterDelay(10, () -> helper.assertTrue(helper.getBlockState(CENTER).is(ModBlast.BLAST_CHARGE), "심지가 다 타기 전에 터짐"));
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(helper.getBlockState(CENTER).isAir(), "심지 뒤에도 안 터짐");
			helper.assertTrue(helper.getBlockState(new BlockPos(1, 1, 1)).isAir(), "모서리를 안 캠");
			helper.assertBlockPresent(Blocks.STONE, new BlockPos(0, 0, 0));
			helper.assertTrue(dropped(helper, Items.COBBLESTONE) > 100, "조약돌이 안 떨어짐");
			helper.succeed();
		});
	}

	/** 터지면 옆 장약은 부서지지 않고 짧은 심지로 이어 터진다. */
	@GameTest(environment = ALONE, maxTicks = 100)
	public void chargeChainsToNeighbourCharge(GameTestHelper helper) {
		stoneBlock(helper);
		helper.setBlock(CENTER, ModBlast.BLAST_CHARGE);
		helper.setBlock(new BlockPos(4, 3, 3), ModBlast.BLAST_CHARGE);
		BlastCharges.detonate(helper.getLevel(), helper.absolutePos(CENTER));
		helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 3)).is(ModBlast.BLAST_CHARGE), "옆 장약이 부서짐");
		helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 3)).getValue(BlastChargeBlock.LIT), "옆 장약에 불이 안 붙음");
		helper.runAfterDelay(BlastCharges.CHAIN_FUSE + 4, () -> {
			helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 3)).isAir(), "옆 장약이 안 터짐");
			helper.succeed();
		});
	}

	/** 원격 기폭기: 웅크리고 우클릭하면 연결, 허공 우클릭하면 연결된 장약만 불이 붙는다. 처음 터뜨리면 발전 과제. */
	@GameTest(environment = ALONE, maxTicks = 100)
	public void detonatorLinksAndDetonates(GameTestHelper helper) {
		stoneBlock(helper);
		BlockPos linked = new BlockPos(3, 3, 3);
		BlockPos other = new BlockPos(1, 5, 1);
		helper.setBlock(linked, ModBlast.BLAST_CHARGE);
		helper.setBlock(other, ModBlast.BLAST_CHARGE);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL); // 서버 플레이어 모형은 연결이 없어 액션바를 못 보낸다
		player.setPos(helper.absoluteVec(new Vec3(3.5, 7.0, 3.5)));
		ItemStack detonator = new ItemStack(ModBlast.REMOTE_DETONATOR);
		player.setItemInHand(InteractionHand.MAIN_HAND, detonator);

		// 웅크리고 연결
		player.setShiftKeyDown(true);
		BlockPos abs = helper.absolutePos(linked);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
		InteractionResult linkResult = detonator.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		helper.assertTrue(linkResult.consumesAction(), "웅크리고 우클릭이 연결을 안 함");
		helper.assertValueEqual(RemoteDetonatorItem.links(detonator).size(), 1, "연결 수");
		// 웅크리지 않은 우클릭은 연결하지 않는다 (PASS → 허공 우클릭과 같은 기폭)
		player.setShiftKeyDown(false);
		helper.assertTrue(!detonator.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "웅크리지 않았는데 소비함");
		helper.assertValueEqual(RemoteDetonatorItem.links(detonator).size(), 1, "연결 수 (웅크리지 않음)");

		// 허공 우클릭 = 기폭: 연결된 것만 불이 붙는다
		InteractionResult fire = detonator.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(fire.consumesAction(), "기폭 실패");
		helper.assertTrue(helper.getBlockState(linked).getValue(BlastChargeBlock.LIT), "연결된 장약에 불이 안 붙음");
		helper.assertFalse(helper.getBlockState(other).getValue(BlastChargeBlock.LIT), "연결 안 된 장약에 불이 붙음");
		helper.runAfterDelay(SanupcraftConfig.current().blastFuseTicks + 5, () -> {
			helper.assertTrue(helper.getBlockState(linked).isAir(), "연결된 장약이 안 터짐");
			helper.assertTrue(helper.getBlockState(other).is(ModBlast.BLAST_CHARGE) || helper.getBlockState(other).isAir(), "이상한 상태");
			helper.succeed();
		});
	}

	/** 처음 터뜨릴 때 주는 발전 과제 "발파" (근처 플레이어 판정은 모형이 레벨 명단에 없어 `award`만 직접 시험). */
	@GameTest(environment = ALONE, maxTicks = 100)
	public void firstBlastAwardsAdvancement(GameTestHelper helper) {
		stoneBlock(helper);
		helper.setBlock(CENTER, ModBlast.BLAST_CHARGE);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(3.5, 7.0, 3.5)));
		var advancement = helper.getLevel().getServer().getAdvancements().get(Sanupcraft.id(ModBlast.ADVANCEMENT));
		helper.assertTrue(advancement != null, "발전 과제 '발파'가 없음");
		helper.assertFalse(player.getAdvancements().getOrStartProgress(advancement).isDone(), "터뜨리기 전에 받음");
		BlastCharges.detonate(helper.getLevel(), helper.absolutePos(CENTER));
		ModBlast.award(player); // 모형 플레이어는 레벨 명단에 없어서 근처 판정을 안 탄다
		helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "발전 과제를 못 받음");
		helper.succeed();
	}

	/** 연결은 최대 8개, 같은 장약을 다시 누르면 해제. 먼 장약 · 사라진 장약은 불이 안 붙고, 사라진 것은 연결에서 지워진다. */
	@GameTest(environment = ALONE, maxTicks = 100)
	public void detonatorLimitsAndRange(GameTestHelper helper) {
		ItemStack detonator = new ItemStack(ModBlast.REMOTE_DETONATOR);
		ServerLevel level = helper.getLevel();
		for (int i = 0; i < ModBlast.MAX_LINKS; i++) {
			BlockPos pos = helper.absolutePos(new BlockPos(i, 1, 1));
			level.setBlock(pos, ModBlast.BLAST_CHARGE.defaultBlockState(), Block.UPDATE_ALL);
			helper.assertTrue(RemoteDetonatorItem.toggleLink(detonator, GlobalPos.of(level.dimension(), pos)) == RemoteDetonatorItem.LinkResult.ADDED, "연결 " + i);
		}
		GlobalPos ninth = GlobalPos.of(level.dimension(), helper.absolutePos(new BlockPos(0, 2, 1)));
		helper.assertTrue(RemoteDetonatorItem.toggleLink(detonator, ninth) == RemoteDetonatorItem.LinkResult.FULL, "9번째가 연결됨");
		GlobalPos first = RemoteDetonatorItem.links(detonator).get(0);
		helper.assertTrue(RemoteDetonatorItem.toggleLink(detonator, first) == RemoteDetonatorItem.LinkResult.REMOVED, "해제 실패");
		helper.assertValueEqual(RemoteDetonatorItem.links(detonator).size(), ModBlast.MAX_LINKS - 1, "해제 뒤 연결 수");

		// 먼 곳에서 누르면 하나도 안 붙는다
		Vec3 far = helper.absoluteVec(new Vec3(0.5, 1.5, 0.5)).add(ModBlast.DETONATOR_RANGE + 10, 0, 0);
		helper.assertValueEqual(RemoteDetonatorItem.detonate(level, detonator, far), 0, "범위 밖에서 불이 붙음");
		// 장약이 없어진 자리는 연결에서 지워진다
		GlobalPos second = RemoteDetonatorItem.links(detonator).get(0);
		level.setBlock(second.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		int lit = RemoteDetonatorItem.detonate(level, detonator, helper.absoluteVec(new Vec3(4.5, 1.5, 1.5)));
		helper.assertValueEqual(lit, ModBlast.MAX_LINKS - 2, "불이 붙은 장약 수");
		helper.assertValueEqual(RemoteDetonatorItem.links(detonator).size(), ModBlast.MAX_LINKS - 2, "사라진 장약이 연결에서 안 지워짐");
		helper.succeed();
	}

	@GameTest
	public void blastRecipesLoad(GameTestHelper helper) {
		for (String id : new String[] {"blast_charge", "remote_detonator"}) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Sanupcraft.id(id));
			helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(key).isPresent(), "레시피가 없음: " + id);
		}
		helper.succeed();
	}

	// ---- D5 유독 가스 ----

	/** 가스는 보이지 않고, 지나갈 수 있고, 다른 블록이 놓이면 밀려난다. */
	@GameTest
	public void gasIsInvisibleAndPassable(GameTestHelper helper) {
		BlockState gas = ModHazard.TOXIC_GAS.defaultBlockState();
		helper.assertTrue(gas.getRenderShape() == RenderShape.INVISIBLE, "보인다");
		helper.assertTrue(gas.getCollisionShape(helper.getLevel(), helper.absolutePos(CENTER)).isEmpty(), "부딪힌다");
		helper.assertTrue(gas.getShape(helper.getLevel(), helper.absolutePos(CENTER)) == Shapes.empty(), "윤곽이 있다");
		helper.assertTrue(gas.canBeReplaced(), "다른 블록이 못 놓인다");
		helper.succeed();
	}

	/** 가스 안에 있는 생물은 독 + 구역질. */
	@GameTest(maxTicks = 100)
	public void gasPoisonsEntityInside(GameTestHelper helper) {
		fill(helper, new BlockPos(0, 0, 0), new BlockPos(6, 0, 6), Blocks.STONE);
		helper.setBlock(new BlockPos(3, 1, 3), ModHazard.TOXIC_GAS);
		helper.setBlock(new BlockPos(3, 2, 3), ModHazard.TOXIC_GAS);
		Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(3, 1, 3));
		cow.setNoAi(true);
		helper.succeedWhen(() -> {
			helper.assertTrue(cow.hasEffect(MobEffects.POISON), "독이 안 걸림");
			helper.assertTrue(cow.hasEffect(MobEffects.NAUSEA), "구역질이 안 걸림");
		});
	}

	/** 불이 닿으면 터진다: 가스가 사라지고, 둘레 생물이 피해 4, 블록 피해 없음, 이어진 가스도 따라 터진다. 횃불 · 용암도 불씨다. */
	@GameTest(maxTicks = 100)
	public void fireNextToGasExplodesWithoutBlockDamage(GameTestHelper helper) {
		fill(helper, new BlockPos(0, 0, 0), new BlockPos(7, 0, 7), Blocks.STONE);
		helper.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
		helper.setBlock(new BlockPos(3, 1, 3), ModHazard.TOXIC_GAS);
		helper.setBlock(new BlockPos(3, 1, 2), ModHazard.TOXIC_GAS); // 이어진 가스
		helper.setBlock(new BlockPos(3, 1, 1), ModHazard.TOXIC_GAS);
		Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(3, 1, 5));
		cow.setNoAi(true);
		float before = cow.getHealth();
		// 따로 떨어진 가스: 횃불과 용암
		helper.setBlock(new BlockPos(6, 1, 6), ModHazard.TOXIC_GAS);
		helper.setBlock(new BlockPos(0, 1, 6), ModHazard.TOXIC_GAS);
		helper.setBlock(new BlockPos(2, 1, 3), Blocks.FIRE);
		helper.setBlock(new BlockPos(6, 1, 5), Blocks.TORCH);
		helper.setBlock(new BlockPos(0, 1, 5), Blocks.LAVA);
		helper.runAfterDelay(10, () -> {
			for (BlockPos pos : List.of(new BlockPos(3, 1, 3), new BlockPos(3, 1, 2), new BlockPos(3, 1, 1), new BlockPos(6, 1, 6))) {
				helper.assertFalse(helper.getBlockState(pos).is(ModHazard.TOXIC_GAS), "가스가 남음: " + pos);
			}
			helper.assertFalse(helper.getBlockState(new BlockPos(0, 1, 6)).is(ModHazard.TOXIC_GAS), "용암 옆 가스가 남음");
			helper.assertBlockPresent(Blocks.STONE, new BlockPos(4, 1, 3));
			helper.assertBlockPresent(Blocks.STONE, new BlockPos(3, 0, 3));
			helper.assertTrue(cow.getHealth() < before, "소가 피해를 안 입음");
			helper.assertTrue(cow.getHealth() >= before - ToxicGasBlock.BURST_DAMAGE * 3, "피해가 너무 큼: " + cow.getHealth());
			helper.succeed();
		});
	}

	// ---- D5 무른 바위 ----

	/** 아래가 비면 떨어져 아래 바닥 위에 쌓인다. */
	@GameTest(maxTicks = 100)
	public void looseRockFallsWhenUnsupported(GameTestHelper helper) {
		fill(helper, new BlockPos(0, 0, 0), new BlockPos(6, 0, 6), Blocks.STONE);
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
		helper.setBlock(new BlockPos(3, 2, 3), ModHazard.LOOSE_ROCK);
		helper.runAfterDelay(10, () -> helper.assertBlockPresent(ModHazard.LOOSE_ROCK, new BlockPos(3, 2, 3))); // 받쳐 있으면 그대로
		helper.runAfterDelay(12, () -> helper.setBlock(new BlockPos(3, 1, 3), Blocks.AIR));
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(ModHazard.LOOSE_ROCK, new BlockPos(3, 1, 3));
			helper.assertTrue(helper.getBlockState(new BlockPos(3, 2, 3)).isAir(), "떨어진 자리가 안 비었다");
		});
	}

	/** 캐면 조약 심층암을 준다 (곡괭이가 있어야). */
	@GameTest
	public void looseRockDropsCobbledDeepslate(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(CENTER);
		BlockState rock = ModHazard.LOOSE_ROCK.defaultBlockState();
		List<ItemStack> withPick = Block.getDrops(rock, helper.getLevel(), pos, null, null, new ItemStack(Items.IRON_PICKAXE));
		helper.assertTrue(withPick.size() == 1 && withPick.get(0).is(Items.COBBLED_DEEPSLATE), "곡괭이로 캤는데 조약 심층암이 아님: " + withPick);
		helper.assertTrue(rock.requiresCorrectToolForDrops(), "곡괭이 없이도 캐짐");
		helper.succeed();
	}

	// ---- 월드 생성 ----

	/** 지형지물이 등록돼 있고, 동굴 공기에 가스를, 천장에 무른 바위를 깐다 (높이 한계를 시험 높이로 풀어서). */
	@GameTest(maxTicks = 100)
	public void featuresPlaceHazards(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
		helper.assertTrue(placed.get(ModHazard.TOXIC_GAS_PLACED).isPresent(), "가스 placed_feature가 없음");
		helper.assertTrue(placed.get(ModHazard.LOOSE_ROCK_PLACED).isPresent(), "바위 placed_feature가 없음");

		// 속이 빈 돌 방 (7x7x7 중 안쪽 5x5x5가 공기)
		stoneBlock(helper);
		fill(helper, new BlockPos(1, 1, 1), new BlockPos(5, 5, 5), Blocks.AIR);
		BlockPos origin = helper.absolutePos(new BlockPos(3, 2, 3));
		int maxY = origin.getY() + 100;
		boolean gas = new ToxicGasPocketFeature(10, maxY).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1L), origin);
		helper.assertTrue(gas, "가스를 못 깜");
		int gasCount = 0;
		for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(5, 5, 5))) {
			if (helper.getBlockState(pos.immutable()).is(ModHazard.TOXIC_GAS)) {
				gasCount++;
			}
		}
		helper.assertValueEqual(gasCount, 10, "깐 가스 칸 수");
		boolean rock = new LooseRockFeature(4, maxY).place(level, level.getChunkSource().getGenerator(), RandomSource.create(2L), helper.absolutePos(new BlockPos(1, 1, 1)));
		helper.assertTrue(rock, "무른 바위를 못 깜");
		int rockCount = 0;
		for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(6, 6, 6))) {
			if (helper.getBlockState(pos.immutable()).is(ModHazard.LOOSE_ROCK)) {
				rockCount++;
				helper.assertTrue(helper.getBlockState(pos.below()).isAir() || helper.getBlockState(pos.below()).is(ModHazard.TOXIC_GAS), "천장이 아닌 곳에 깔림");
			}
		}
		helper.assertTrue(rockCount >= 1 && rockCount <= 4, "바위 칸 수: " + rockCount);
		helper.succeed();
	}

	/** 높이 한계 위(= Y 0 이상)에서는 아무것도 안 깐다. */
	@GameTest
	public void featuresStayBelowHeightLimit(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		stoneBlock(helper);
		fill(helper, new BlockPos(1, 1, 1), new BlockPos(5, 5, 5), Blocks.AIR);
		BlockPos origin = helper.absolutePos(new BlockPos(3, 2, 3));
		int maxY = origin.getY(); // 원점이 한계 위 (Y >= maxY)
		helper.assertFalse(new ToxicGasPocketFeature(10, maxY).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1L), origin), "한계 위에 가스를 깖");
		helper.assertFalse(new LooseRockFeature(4, maxY).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1L), origin), "한계 위에 바위를 깖");
		helper.succeed();
	}

	// ---- 설정 ----

	/** 설정을 끄면 새로 깔리지 않고, 있는 가스는 독도 폭발도 없고, 있는 바위는 떨어지지 않는다. */
	@GameTest(environment = ALONE_HAZARD, maxTicks = 200)
	public void configTogglesDisableHazards(GameTestHelper helper) {
		SanupcraftConfig config = SanupcraftConfig.current();
		boolean gasBefore = config.hazardToxicGas;
		boolean rockBefore = config.hazardLooseRock;
		config.hazardToxicGas = false;
		config.hazardLooseRock = false;
		try {
			ServerLevel level = helper.getLevel();
			fill(helper, new BlockPos(0, 0, 0), new BlockPos(7, 0, 7), Blocks.STONE);
			// 있는 가스: 독도 폭발도 없다
			helper.setBlock(new BlockPos(3, 1, 3), ModHazard.TOXIC_GAS);
			helper.setBlock(new BlockPos(1, 1, 5), ModHazard.TOXIC_GAS);
			Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(3, 1, 3));
			cow.setNoAi(true);
			helper.setBlock(new BlockPos(1, 1, 4), Blocks.FIRE);
			// 있는 바위: 떨어지지 않는다
			helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
			helper.setBlock(new BlockPos(6, 2, 6), ModHazard.LOOSE_ROCK);
			helper.runAfterDelay(5, () -> helper.setBlock(new BlockPos(6, 1, 6), Blocks.AIR));
			// 새로 깔기: 아무것도 안 깐다
			BlockPos origin = helper.absolutePos(new BlockPos(5, 2, 2));
			int maxY = origin.getY() + 100;
			helper.assertFalse(new ToxicGasPocketFeature(10, maxY).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1L), origin), "꺼졌는데 가스를 깖");
			helper.assertFalse(new LooseRockFeature(4, maxY).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1L), origin), "꺼졌는데 바위를 깖");
			helper.runAfterDelay(40, () -> {
				try {
					helper.assertFalse(cow.hasEffect(MobEffects.POISON), "꺼졌는데 독이 걸림");
					helper.assertFalse(cow.hasEffect(MobEffects.NAUSEA), "꺼졌는데 구역질이 걸림");
					helper.assertBlockPresent(ModHazard.TOXIC_GAS, new BlockPos(1, 1, 5));
					helper.assertBlockPresent(ModHazard.LOOSE_ROCK, new BlockPos(6, 2, 6));
					helper.succeed();
				} finally {
					config.hazardToxicGas = gasBefore;
					config.hazardLooseRock = rockBefore;
				}
			});
		} catch (RuntimeException | Error e) {
			config.hazardToxicGas = gasBefore;
			config.hazardLooseRock = rockBefore;
			throw e;
		}
	}

	/** 설정 파일 키: 발파 · 위험 요소 값이 읽히고 (1/0) 범위를 벗어나면 기본값 + 경고. */
	@GameTest
	public void configKeysParse(GameTestHelper helper) {
		var ok = SanupcraftConfig.parse("{\"hazards\":{\"toxicGas\":0,\"looseRock\":1},\"blast\":{\"fuseTicks\":40,\"entityDamage\":2.5}}");
		helper.assertFalse(ok.config().hazardToxicGas, "toxicGas 0이 안 읽힘");
		helper.assertTrue(ok.config().hazardLooseRock, "looseRock 1이 안 읽힘");
		helper.assertValueEqual(ok.config().blastFuseTicks, 40, "심지");
		helper.assertValueEqual(ok.config().blastEntityDamage, 2.5, "피해");
		var bad = SanupcraftConfig.parse("{\"hazards\":{\"toxicGas\":5}}");
		helper.assertTrue(bad.config().hazardToxicGas, "범위 밖 값이 기본값으로 안 돌아옴");
		helper.assertFalse(bad.warnings().isEmpty(), "경고가 없음");
		helper.assertTrue(SanupcraftConfig.defaults().hazardToxicGas && SanupcraftConfig.defaults().hazardLooseRock, "기본값은 켬");
		helper.succeed();
	}
}
