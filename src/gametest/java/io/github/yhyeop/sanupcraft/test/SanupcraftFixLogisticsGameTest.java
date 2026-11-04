package io.github.yhyeop.sanupcraft.test;

import java.util.concurrent.atomic.AtomicBoolean;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.logistics.BeltBlock;
import io.github.yhyeop.sanupcraft.logistics.BeltBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.BeltMotorBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.EnderLinkBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.mining.ModMining;
import io.github.yhyeop.sanupcraft.mining.QuarryBlockEntity;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.power.ModPower;

/**
 * 1.5단계 리팩터링 수정 검사 (REFACTOR.md 2-5, 묶음 Logistics): 벨트 끝 기계 · 빈 줄 모터 · 굴착기 · 엔더 운반기 · 실테스트 꾸러미.
 * 굴착기는 주변 테스트를 파지 않도록 {@code alone_quarry}에서 돌린다.
 */
public class SanupcraftFixLogisticsGameTest {
	// ---- 벨트 · 모터 ----

	/** 벨트 끝이 기계이면 연료(석탄)는 옆면의 연료 칸으로, 연료가 아닌 것(원석)은 위의 재료 칸으로 들어간다. */
	@GameTest(maxTicks = 200)
	public void beltFeedsFuelToSideAndOreToInput(GameTestHelper helper) {
		BlockPos belt = new BlockPos(2, 1, 2);
		helper.setBlock(belt, ModLogistics.BELT.defaultBlockState().setValue(BeltBlock.FACING, Direction.EAST));
		BlockPos crusherPos = new BlockPos(3, 1, 2);
		helper.setBlock(crusherPos, ModMachines.CRUSHER);
		MachineBlockEntity crusher = helper.getBlockEntity(crusherPos, MachineBlockEntity.class);
		helper.getBlockEntity(belt, BeltBlockEntity.class).power(helper.getLevel().getGameTime() + 1000);
		// 벨트 끝 쪽(앞 0.35칸)에 두면 바로 넘겨준다
		dropOnBelt(helper, belt, 0.35, new ItemStack(Items.COAL, 3));
		dropOnBelt(helper, belt, 0.35, new ItemStack(Items.RAW_IRON, 1));
		helper.succeedWhen(() -> {
			helper.assertTrue(crusher.getItem(MachineBlockEntity.SLOT_FUEL).is(Items.COAL), "연료 칸에 석탄이 없음: " + crusher.getItem(MachineBlockEntity.SLOT_FUEL));
			boolean ore = crusher.getItem(MachineBlockEntity.SLOT_INPUT).is(Items.RAW_IRON) || !crusher.result().isEmpty();
			helper.assertTrue(ore, "재료 칸에 원석이 없음: " + crusher.getItem(MachineBlockEntity.SLOT_INPUT));
		});
	}

	/** 줄 위에 아무것도 없으면 모터는 증기를 쓰지 않고 벨트도 돌리지 않는다. 아이템이 올라오면 다시 쓴다. */
	@GameTest(maxTicks = 200)
	public void motorSavesSteamOnEmptyLine(GameTestHelper helper) {
		BlockPos motorPos = new BlockPos(3, 1, 1);
		helper.setBlock(motorPos, ModLogistics.BELT_MOTOR);
		for (int z = 2; z <= 4; z++) {
			helper.setBlock(new BlockPos(3, 1, z), ModLogistics.BELT.defaultBlockState().setValue(BeltBlock.FACING, Direction.SOUTH));
		}
		BeltMotorBlockEntity motor = helper.getBlockEntity(motorPos, BeltMotorBlockEntity.class);
		motor.acceptSteam(BeltMotorBlockEntity.STEAM_CAPACITY);
		AtomicBoolean loaded = new AtomicBoolean();
		helper.runAfterDelay(40, () -> {
			helper.assertValueEqual(motor.steam(), BeltMotorBlockEntity.STEAM_CAPACITY, "빈 줄에서 쓴 증기");
			helper.assertFalse(helper.getBlockState(new BlockPos(3, 1, 2)).getValue(BeltBlock.POWERED), "빈 줄인데 벨트가 돎");
			helper.assertValueEqual(motor.poweredBelts(), 3, "이어진 벨트 칸 수");
			dropOnBelt(helper, new BlockPos(3, 1, 2), 0.0, new ItemStack(Items.COAL));
			loaded.set(true);
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(loaded.get(), "아직 아이템을 올리지 않음");
			helper.assertTrue(motor.steam() < BeltMotorBlockEntity.STEAM_CAPACITY, "아이템이 있는데 증기를 안 씀");
			helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 2)).getValue(BeltBlock.POWERED), "아이템이 있는데 벨트가 안 돎");
		});
	}

	// ---- 굴착기 ----

	/** 철 곡괭이로 못 캐는 흑요석은 남기고, 돌은 캔다 (층을 한 번 다 지나간 뒤에 본다). */
	@GameTest(environment = "sanupcraft-test:alone_quarry", maxTicks = 300)
	public void quarryLeavesObsidian(GameTestHelper helper) {
		BlockPos quarryPos = new BlockPos(3, 3, 3);
		helper.setBlock(quarryPos.below(), Blocks.OBSIDIAN);
		helper.setBlock(quarryPos.below().west(), Blocks.STONE);
		helper.setBlock(quarryPos, ModMining.STEAM_QUARRY);
		helper.setBlock(quarryPos.above(), Blocks.CHEST);
		QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
		quarry.acceptSteam(QuarryBlockEntity.STEAM_CAPACITY);
		int firstLayer = helper.absolutePos(quarryPos).getY() - 1;
		helper.succeedWhen(() -> {
			helper.assertTrue(quarry.layer() < firstLayer, "아직 첫 층을 다 지나가지 않음: " + quarry.layer());
			helper.assertBlockNotPresent(Blocks.STONE, quarryPos.below().west());
			helper.assertBlockPresent(Blocks.OBSIDIAN, quarryPos.below());
		});
	}

	/** 못 보낸 것(위에 상자가 없을 때)이 남은 채로 굴착기를 부수면 그 자리에 떨어진다. */
	@GameTest(environment = "sanupcraft-test:alone_quarry", maxTicks = 200)
	public void quarryDropsPendingWhenRemoved(GameTestHelper helper) {
		BlockPos quarryPos = new BlockPos(3, 3, 3);
		helper.setBlock(quarryPos.below(), Blocks.STONE);
		helper.setBlock(quarryPos, ModMining.STEAM_QUARRY);
		QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
		quarry.acceptSteam(QuarryBlockEntity.STEAM_CAPACITY);
		AtomicBoolean removed = new AtomicBoolean();
		helper.succeedWhen(() -> {
			if (!removed.get()) {
				helper.assertTrue(quarry.mined() >= 1, "아직 아무것도 캐지 않음");
				helper.setBlock(quarryPos, Blocks.AIR);
				removed.set(true);
			}
			AABB around = new AABB(helper.absolutePos(quarryPos)).inflate(2.0);
			helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).isEmpty(), "남은 아이템이 떨어지지 않음");
		});
	}

	/** 시작 전에는 NOT_STARTED, 위에 상자가 없으면 BLOCKED. */
	@GameTest
	public void quarryStatusBeforeStartAndWithoutChest(GameTestHelper helper) {
		BlockPos quarryPos = new BlockPos(3, 3, 3);
		helper.setBlock(quarryPos, ModMining.STEAM_QUARRY);
		QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
		helper.assertValueEqual(quarry.status(), QuarryBlockEntity.Status.BLOCKED, "위가 빈 굴착기 상태");
		helper.setBlock(quarryPos.above(), Blocks.CHEST);
		helper.assertValueEqual(quarry.status(), QuarryBlockEntity.Status.NOT_STARTED, "상자만 얹은 굴착기 상태");
		helper.succeed();
	}

	/** 증기를 한 블록치만 주면 한 블록 캔 뒤 NO_STEAM. */
	@GameTest(environment = "sanupcraft-test:alone_quarry", maxTicks = 200)
	public void quarryStatusNoSteam(GameTestHelper helper) {
		BlockPos quarryPos = new BlockPos(3, 3, 3);
		helper.setBlock(quarryPos.below(), Blocks.STONE);
		helper.setBlock(quarryPos, ModMining.STEAM_QUARRY);
		helper.setBlock(quarryPos.above(), Blocks.CHEST);
		QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
		quarry.acceptSteam(QuarryBlockEntity.STEAM_PER_BLOCK);
		helper.succeedWhen(() -> {
			helper.assertTrue(quarry.mined() >= 1, "아직 아무것도 캐지 않음");
			helper.assertValueEqual(quarry.status(), QuarryBlockEntity.Status.NO_STEAM, "증기가 떨어진 굴착기 상태");
		});
	}

	// ---- 엔더 운반기 ----

	/** 수신기 자리가 4개뿐이면 송신기는 16개를 거부하고 4개만 받는다. 채널에 이어진 수신기 수도 센다. */
	@GameTest
	public void senderRefusesWhatReceiversCannotHold(GameTestHelper helper) {
		BlockPos senderPos = new BlockPos(1, 1, 1);
		BlockPos receiverPos = new BlockPos(3, 1, 1);
		EnderLinkBlockEntity sender = link(helper, senderPos, ModLogistics.ENDER_SENDER, DyeColor.LIME);
		helper.assertValueEqual(sender.linkedReceivers(), 0, "수신기를 놓기 전 연결 수");
		EnderLinkBlockEntity receiver = link(helper, receiverPos, ModLogistics.ENDER_RECEIVER, DyeColor.LIME);
		helper.assertValueEqual(sender.linkedReceivers(), 1, "수신기를 놓은 뒤 연결 수");
		fillLeavingRoom(receiver, 4);

		helper.assertFalse(sender.canPlaceItem(0, new ItemStack(Items.COBBLESTONE, 16)), "자리가 4인데 16개를 받음");
		helper.assertTrue(sender.canPlaceItem(0, new ItemStack(Items.COBBLESTONE, 4)), "자리가 4인데 4개를 거부");

		sender.setItem(0, new ItemStack(Items.COBBLESTONE, 4));
		helper.assertValueEqual(receiver.countItem(Items.COBBLESTONE), 64, "수신기의 조약돌");
		helper.assertItemEntityCountIs(Items.COBBLESTONE, senderPos, 3.0, 0);

		// 자리가 없는데 억지로 넣으면 그때만 송신기 위에 떨어진다
		sender.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
		helper.assertItemEntityCountIs(Items.COBBLESTONE, senderPos, 3.0, 5);
		helper.succeed();
	}

	/** 한 수신기에 다 안 들어가면 다음 수신기에 나눠 담는다. 두 곳의 자리를 합쳐도 모자라면 거부한다. */
	@GameTest
	public void senderSplitsAcrossReceivers(GameTestHelper helper) {
		BlockPos senderPos = new BlockPos(1, 1, 1);
		EnderLinkBlockEntity sender = link(helper, senderPos, ModLogistics.ENDER_SENDER, DyeColor.PINK);
		EnderLinkBlockEntity near = link(helper, new BlockPos(3, 1, 1), ModLogistics.ENDER_RECEIVER, DyeColor.PINK);
		EnderLinkBlockEntity far = link(helper, new BlockPos(5, 1, 1), ModLogistics.ENDER_RECEIVER, DyeColor.PINK);
		fillLeavingRoom(near, 4);
		fillLeavingRoom(far, 4);

		helper.assertFalse(sender.canPlaceItem(0, new ItemStack(Items.COBBLESTONE, 9)), "두 곳을 합쳐 8인데 9개를 받음");
		helper.assertTrue(sender.canPlaceItem(0, new ItemStack(Items.COBBLESTONE, 8)), "두 곳을 합쳐 8인데 8개를 거부");

		sender.setItem(0, new ItemStack(Items.COBBLESTONE, 8));
		helper.assertValueEqual(near.countItem(Items.COBBLESTONE), 64, "가까운 수신기의 조약돌");
		helper.assertValueEqual(far.countItem(Items.COBBLESTONE), 64, "먼 수신기의 조약돌");
		helper.assertItemEntityCountIs(Items.COBBLESTONE, senderPos, 6.0, 0);
		helper.succeed();
	}

	/** 수신기 제목에 채널 색이 붙는다 (기본 흰색은 그대로 블록 이름). */
	@GameTest
	public void receiverTitleShowsChannel(GameTestHelper helper) {
		EnderLinkBlockEntity receiver = link(helper, new BlockPos(2, 1, 2), ModLogistics.ENDER_RECEIVER, DyeColor.WHITE);
		helper.assertFalse("block.sanupcraft.ender_receiver.titled".equals(key(receiver.getDisplayName())), "흰색 채널인데 제목에 채널이 붙음");
		receiver.setChannel(DyeColor.RED);
		helper.assertValueEqual(key(receiver.getDisplayName()), "block.sanupcraft.ender_receiver.titled", "빨강 채널 수신기 제목 키");
		EnderLinkBlockEntity sender = link(helper, new BlockPos(4, 1, 2), ModLogistics.ENDER_SENDER, DyeColor.RED);
		helper.assertFalse("block.sanupcraft.ender_receiver.titled".equals(key(sender.getDisplayName())), "송신기 제목에 수신기 형식이 붙음");
		helper.succeed();
	}

	// ---- 실테스트 꾸러미 ----

	/** steam 꾸러미는 보일러 2개, start 꾸러미에는 레드스톤 4 · 참나무 판자 16이 들어 있다. */
	@GameTest
	public void playtestKitsMatchPlan(GameTestHelper helper) {
		ServerPlayer steam = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
		PlaytestCommand.apply(steam, PlaytestCommand.Stage.STEAM);
		helper.assertValueEqual(steam.getInventory().countItem(ModPower.BOILER.asItem()), 2, "steam 꾸러미의 보일러");

		ServerPlayer start = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
		PlaytestCommand.apply(start, PlaytestCommand.Stage.START);
		helper.assertValueEqual(start.getInventory().countItem(Items.REDSTONE), 4, "start 꾸러미의 레드스톤");
		helper.assertValueEqual(start.getInventory().countItem(Items.OAK_PLANKS), 16, "start 꾸러미의 참나무 판자");
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

	private static EnderLinkBlockEntity link(GameTestHelper helper, BlockPos pos, Block block, DyeColor channel) {
		helper.setBlock(pos, block);
		EnderLinkBlockEntity link = helper.getBlockEntity(pos, EnderLinkBlockEntity.class);
		link.setChannel(channel);
		return link;
	}

	/** 수신기 아홉 칸 중 여덟 칸을 돌로 채우고, 마지막 칸에 조약돌을 채워 조약돌이 {@code room}개만 더 들어가게 한다. */
	private static void fillLeavingRoom(EnderLinkBlockEntity receiver, int room) {
		for (int i = 0; i < EnderLinkBlockEntity.RECEIVER_SLOTS - 1; i++) {
			receiver.setItem(i, new ItemStack(Items.STONE, 64));
		}
		receiver.setItem(EnderLinkBlockEntity.RECEIVER_SLOTS - 1, new ItemStack(Items.COBBLESTONE, 64 - room));
	}

	private static String key(Component component) {
		return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
	}
}
