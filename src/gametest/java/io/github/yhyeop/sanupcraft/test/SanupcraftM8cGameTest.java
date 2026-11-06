package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.MachineBlock;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.MachineFaces;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.logistics.BeltBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.EnderLinkBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.wrench.ModWrench;
import io.github.yhyeop.sanupcraft.wrench.Wrench;
import io.github.yhyeop.sanupcraft.wrench.WrenchItem;

/** M8 I4 검사: 렌치 돌리기 · 내용물째 집기 · 잠금, 기계 면 설정 (깔때기 · 벨트). */
public class SanupcraftM8cGameTest {
	private static final BlockPos POS = new BlockPos(2, 2, 2);

	/** 렌치 우클릭으로 기계가 시계 방향으로 돌고, 면 설정은 기계 기준이라 같이 돈다. 면 설정 모드에서는 누른 면이 바뀐다. */
	@GameTest
	public void wrenchRotatesMachine(GameTestHelper helper) {
		helper.setBlock(POS, ModMachines.CRUSHER.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		MachineBlockEntity crusher = helper.getBlockEntity(POS, MachineBlockEntity.class);
		crusher.faces().set(MachineFaces.Side.FRONT, MachineFaces.Mode.NONE);
		helper.assertValueEqual(crusher.faceMode(Direction.NORTH), MachineFaces.Mode.NONE, "앞(북쪽) 면");

		Player player = wrenchPlayer(helper);
		helper.assertTrue(use(helper, player, POS, Direction.UP).consumesAction(), "돌리기가 안 됨");
		helper.assertValueEqual(helper.getBlockState(POS).getValue(MachineBlock.FACING), Direction.EAST, "돌린 방향");
		MachineBlockEntity same = helper.getBlockEntity(POS, MachineBlockEntity.class);
		helper.assertTrue(same == crusher, "돌리니 블록 엔티티가 바뀜");
		helper.assertValueEqual(crusher.faceMode(Direction.EAST), MachineFaces.Mode.NONE, "돌린 뒤 앞(동쪽) 면");
		helper.assertValueEqual(crusher.faceMode(Direction.NORTH), MachineFaces.Mode.DEFAULT, "돌린 뒤 북쪽 면");

		// 바닐라: 깔때기(아래 + 가로 4방향)는 위를 건너뛰고 다음 방향으로, 돌 · 큰 상자는 안 돈다
		BlockPos hopperPos = new BlockPos(0, 2, 0);
		helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.assertTrue(use(helper, player, hopperPos, Direction.UP).consumesAction(), "깔때기 돌리기");
		helper.assertValueEqual(helper.getBlockState(hopperPos).getValue(HopperBlock.FACING), Direction.NORTH, "깔때기 다음 방향");
		BlockPos stonePos = new BlockPos(4, 2, 0);
		helper.setBlock(stonePos, Blocks.STONE);
		helper.assertFalse(use(helper, player, stonePos, Direction.UP).consumesAction(), "돌이 돌아감");

		// 면 설정 모드: 위 면을 누르면 기본 → 넣기
		WrenchItem.setFaceMode(player.getMainHandItem(), true);
		use(helper, player, POS, Direction.UP);
		helper.assertValueEqual(crusher.faces().get(MachineFaces.Side.UP), MachineFaces.Mode.IN, "위 면 모드");
		helper.assertValueEqual(helper.getBlockState(POS).getValue(MachineBlock.FACING), Direction.EAST, "면 설정 모드에서 돌아감");
		helper.succeed();
	}

	/** 웅크리고 렌치: 분쇄기가 내용물 · 증기 · 면 설정째 아이템이 되고 (아무것도 안 쏟음), 다시 놓으면 그대로다. */
	@GameTest
	public void sneakWrenchKeepsContents(GameTestHelper helper) {
		helper.setBlock(POS.below(), Blocks.STONE);
		helper.setBlock(POS, ModMachines.CRUSHER);
		MachineBlockEntity crusher = helper.getBlockEntity(POS, MachineBlockEntity.class);
		crusher.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 5));
		crusher.setItem(MachineBlockEntity.SLOT_RESULT, new ItemStack(ModItems.IRON_DUST, 7));
		crusher.setItem(MachineBlockEntity.SLOT_UPGRADE, new ItemStack(ModMachines.SPEED_UPGRADE));
		crusher.acceptSteam(150);
		crusher.faces().set(MachineFaces.Side.DOWN, MachineFaces.Mode.OUT);

		Player player = wrenchPlayer(helper);
		player.setShiftKeyDown(true);
		helper.assertTrue(use(helper, player, POS, Direction.UP).consumesAction(), "집기가 안 됨");
		helper.assertBlockPresent(Blocks.AIR, POS);
		AABB around = new AABB(helper.absolutePos(POS)).inflate(3.0);
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).isEmpty(), "내용물이 쏟아짐");

		ItemStack picked = ItemStack.EMPTY;
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(ModMachines.CRUSHER.asItem())) {
				picked = stack;
			}
		}
		helper.assertFalse(picked.isEmpty(), "분쇄기 아이템을 받지 않음");
		helper.assertTrue(picked.has(DataComponents.CONTAINER), "내용물 컴포넌트가 없음");
		helper.assertTrue(picked.has(DataComponents.BLOCK_ENTITY_DATA), "블록 엔티티 데이터가 없음");

		// 다시 놓기 (돌 위에)
		player.setShiftKeyDown(false);
		player.setItemInHand(InteractionHand.MAIN_HAND, picked.copy());
		BlockPos below = helper.absolutePos(POS.below());
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		helper.assertBlockPresent(ModMachines.CRUSHER, POS);
		MachineBlockEntity placed = helper.getBlockEntity(POS, MachineBlockEntity.class);
		helper.assertTrue(placed.getItem(MachineBlockEntity.SLOT_INPUT).is(Items.RAW_IRON), "재료가 안 돌아옴");
		helper.assertValueEqual(placed.getItem(MachineBlockEntity.SLOT_INPUT).getCount(), 5, "재료 개수");
		helper.assertValueEqual(placed.result().getCount(), 7, "결과 개수");
		helper.assertTrue(placed.upgrade(0).is(ModMachines.SPEED_UPGRADE), "업그레이드가 안 돌아옴");
		helper.assertValueEqual(placed.steam(), 150, "증기");
		helper.assertValueEqual(placed.faces().get(MachineFaces.Side.DOWN), MachineFaces.Mode.OUT, "면 설정");
		helper.succeed();
	}

	/** 잠긴 엔더 수신기는 남이 렌치로 못 집고, 주인은 집는다. 바닐라 블록 · 블록 엔티티 없는 블록도 못 집는다. */
	@GameTest
	public void lockedReceiverNotPickable(GameTestHelper helper) {
		helper.setBlock(POS, ModLogistics.ENDER_RECEIVER);
		EnderLinkBlockEntity receiver = helper.getBlockEntity(POS, EnderLinkBlockEntity.class);
		Player owner = wrenchPlayer(helper);
		Player stranger = wrenchPlayer(helper);
		receiver.setOwner(owner);
		receiver.setLocked(true);
		receiver.setItem(0, new ItemStack(Items.DIAMOND, 3));
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(POS);

		helper.assertFalse(Wrench.pickUp(level, stranger, abs, level.getBlockState(abs)), "남이 잠긴 수신기를 집음");
		helper.assertBlockPresent(ModLogistics.ENDER_RECEIVER, POS);
		helper.assertTrue(Wrench.pickUp(level, owner, abs, level.getBlockState(abs)), "주인이 못 집음");
		helper.assertBlockPresent(Blocks.AIR, POS);
		helper.assertTrue(owner.getInventory().countItem(ModLogistics.ENDER_RECEIVER.asItem()) == 1, "주인이 수신기를 받지 않음");

		BlockPos chestPos = new BlockPos(0, 2, 0);
		helper.setBlock(chestPos, Blocks.CHEST);
		BlockPos chestAbs = helper.absolutePos(chestPos);
		helper.assertFalse(Wrench.pickUp(level, owner, chestAbs, level.getBlockState(chestAbs)), "바닐라 상자를 집음");
		helper.succeed();
	}

	/** 위 면을 막으면 위 깔때기가 못 넣고 (벨트도 못 넣음), 기본으로 돌리면 넣는다. */
	@GameTest(maxTicks = 100)
	public void noneFaceBlocksHopper(GameTestHelper helper) {
		helper.setBlock(POS, ModMachines.CRUSHER);
		MachineBlockEntity crusher = helper.getBlockEntity(POS, MachineBlockEntity.class);
		crusher.faces().set(MachineFaces.Side.UP, MachineFaces.Mode.NONE);
		ItemStack belted = BeltBlockEntity.insert(crusher, new ItemStack(Items.RAW_IRON, 2), Direction.DOWN);
		helper.assertValueEqual(belted.getCount(), 2, "막은 면으로 벨트가 넣음");
		helper.setBlock(POS.above(), Blocks.HOPPER);
		HopperBlockEntity hopper = helper.getBlockEntity(POS.above(), HopperBlockEntity.class);
		hopper.setItem(0, new ItemStack(Items.RAW_IRON, 4));
		helper.runAfterDelay(30, () -> {
			helper.assertValueEqual(hopper.getItem(0).getCount(), 4, "막은 면으로 깔때기가 넣음");
			helper.assertTrue(crusher.getItem(MachineBlockEntity.SLOT_INPUT).isEmpty(), "재료 칸이 참");
			crusher.faces().set(MachineFaces.Side.UP, MachineFaces.Mode.DEFAULT);
			helper.succeedWhen(() -> helper.assertFalse(crusher.getItem(MachineBlockEntity.SLOT_INPUT).isEmpty(), "기본으로 돌렸는데 안 들어감"));
		});
	}

	/** 빼기 면: 위 깔때기는 못 넣고, 아래 깔때기는 결과를 뺀다. 옆 면을 빼기로 하면 그 면에 결과 칸이 보인다. */
	@GameTest(maxTicks = 100)
	public void outFaceExtractsOnly(GameTestHelper helper) {
		helper.setBlock(POS, ModMachines.CRUSHER);
		MachineBlockEntity crusher = helper.getBlockEntity(POS, MachineBlockEntity.class);
		crusher.faces().set(MachineFaces.Side.UP, MachineFaces.Mode.OUT);
		crusher.faces().set(MachineFaces.Side.DOWN, MachineFaces.Mode.OUT);
		crusher.setItem(MachineBlockEntity.SLOT_RESULT, new ItemStack(ModItems.IRON_DUST, 3));
		helper.assertFalse(crusher.canPlaceItemThroughFace(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON), Direction.UP), "빼기 면으로 넣기");
		helper.assertTrue(crusher.canTakeItemThroughFace(MachineBlockEntity.SLOT_RESULT, crusher.result(), Direction.UP), "빼기 면으로 빼기");
		BlockState state = helper.getBlockState(POS);
		Direction side = state.getValue(MachineBlock.FACING).getClockWise();
		helper.assertValueEqual(crusher.getSlotsForFace(side).length, 1, "기본 옆 면 칸 수");
		crusher.faces().set(MachineFaces.Side.LEFT, MachineFaces.Mode.OUT);
		helper.assertTrue(java.util.Arrays.stream(crusher.getSlotsForFace(side)).anyMatch(s -> s == MachineBlockEntity.SLOT_RESULT), "빼기 옆 면에 결과 칸이 없음");
		helper.assertFalse(crusher.canPlaceItemThroughFace(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL), side), "빼기 옆 면으로 연료가 들어감");

		helper.setBlock(POS.above(), Blocks.HOPPER);
		HopperBlockEntity above = helper.getBlockEntity(POS.above(), HopperBlockEntity.class);
		above.setItem(0, new ItemStack(Items.RAW_IRON, 4));
		helper.setBlock(POS.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
		HopperBlockEntity below = helper.getBlockEntity(POS.below(), HopperBlockEntity.class);
		helper.runAfterDelay(60, () -> {
			helper.assertValueEqual(above.getItem(0).getCount(), 4, "빼기 면으로 깔때기가 넣음");
			int pulled = 0;
			for (int i = 0; i < below.getContainerSize(); i++) {
				if (below.getItem(i).is(ModItems.IRON_DUST)) {
					pulled += below.getItem(i).getCount();
				}
			}
			helper.assertTrue(pulled > 0, "아래 깔때기가 결과를 못 뺌");
			helper.succeed();
		});
	}

	// ---- 도우미 ----

	private static Player wrenchPlayer(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModWrench.WRENCH));
		return player;
	}

	private static net.minecraft.world.InteractionResult use(GameTestHelper helper, Player player, BlockPos pos, Direction face) {
		BlockPos abs = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), face, abs, false);
		return Wrench.use(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
	}
}
