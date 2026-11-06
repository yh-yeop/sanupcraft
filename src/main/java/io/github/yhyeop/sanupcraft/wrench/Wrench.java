package io.github.yhyeop.sanupcraft.wrench;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.factory.MachineBlock;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.MachineFaces;
import io.github.yhyeop.sanupcraft.logistics.EnderAnchorBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.EnderLinkBlockEntity;
import io.github.yhyeop.sanupcraft.storage.TerminalBlockEntity;

/**
 * 렌치 동작 (M8 I4). 서버에서만 부른다.
 * <ul>
 *   <li>우클릭 (돌리기 모드): "facing"이 있는 블록을 돌린다. 가로 4방향은 시계 방향으로 90도, 6방향(위 · 아래 포함)은 다음 방향으로.
 *       바닐라 블록은 두 칸짜리(침대 · 문 · 큰 상자)와 늘어난 피스톤은 안 돌리고, 돌린 뒤 붙어 있을 수 없으면(횃불 등) 안 돌린다.</li>
 *   <li>우클릭 (면 설정 모드): 기계의 누른 면의 모드를 기본 → 넣기 → 빼기 → 둘 다 → 막기 → 기본 순으로 바꾼다.</li>
 *   <li>웅크리고 우클릭: 이 모드의 블록 엔티티가 있는 블록을 내용물 · 설정째 아이템으로 집는다 (바닐라 Ctrl+집기와 같은 방식,
 *       {@code minecraft:block_entity_data} + 블록 엔티티의 컴포넌트). 다시 놓으면 그대로 돌아온다.
 *       잠긴 엔더 수신기 · 단말은 관리할 수 있는 사람(주인 · 팀 · OP)만. 엔더 닻은 청크 붙잡기 때문에 안 집는다 (캐서 옮김).</li>
 * </ul>
 * 허공에 우클릭하면 돌리기 ↔ 면 설정 모드를 바꾼다 ({@link WrenchItem}).
 */
public final class Wrench {
	private Wrench() {
	}

	public static InteractionResult use(ServerLevel level, Player player, InteractionHand hand, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		ItemStack wrench = player.getItemInHand(hand);
		if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), wrench)) {
			return InteractionResult.FAIL;
		}
		BlockState state = level.getBlockState(pos);
		boolean done;
		if (player.isSecondaryUseActive()) {
			done = pickUp(level, player, pos, state);
		} else if (WrenchItem.isFaceMode(wrench)) {
			done = cycleFace(level, player, pos, state, hit.getDirection());
		} else {
			done = rotate(level, player, pos, state);
		}
		return done ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	// ---- 돌리기 ----

	/** 돌리고 true. 돌릴 수 없으면 안내하고 false. */
	public static boolean rotate(ServerLevel level, Player player, BlockPos pos, BlockState state) {
		BlockState next = isOurs(state) || safeVanilla(state) ? rotated(state) : null;
		if (next != null && state.getDestroySpeed(level, pos) >= 0.0F) {
			BlockState placed = Block.updateFromNeighbourShapes(next, level, pos);
			if (placed.canSurvive(level, pos)) {
				level.setBlock(pos, placed, Block.UPDATE_ALL);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
				level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.4F, 1.6F);
				return true;
			}
		}
		tell(player, Component.translatable("item.sanupcraft.wrench.cant_rotate").withStyle(ChatFormatting.RED));
		return false;
	}

	/** 한 번 돌린 모습. "facing"(방향 값) 속성이 없으면 null. */
	public static @Nullable BlockState rotated(BlockState state) {
		Property<Direction> facing = facingOf(state);
		if (facing == null) {
			return null;
		}
		Direction current = state.getValue(facing);
		List<Direction> allowed = facing.getPossibleValues();
		if (allowed.size() == 4 && allowed.stream().allMatch(d -> d.getAxis().isHorizontal())) {
			// 블록이 돌리기를 따로 다루면(계단 모양 등) 그 규칙대로
			BlockState turned = state.rotate(Rotation.CLOCKWISE_90);
			return turned != state ? turned : state.setValue(facing, current.getClockWise());
		}
		Direction[] order = Direction.values();
		for (int step = 1; step < order.length; step++) {
			Direction candidate = order[(current.ordinal() + step) % order.length];
			if (allowed.contains(candidate)) {
				return state.setValue(facing, candidate);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static @Nullable Property<Direction> facingOf(BlockState state) {
		for (Property<?> property : state.getProperties()) {
			if (property.getName().equals("facing") && property.getValueClass() == Direction.class) {
				return (Property<Direction>) property;
			}
		}
		return null;
	}

	/** 바닐라 블록 중 돌려도 깨지지 않는 것. 두 칸짜리 · 큰 상자 · 늘어난 피스톤은 뺀다. */
	private static boolean safeVanilla(BlockState state) {
		if (state.hasProperty(BlockStateProperties.BED_PART) || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			return false;
		}
		if (state.hasProperty(BlockStateProperties.CHEST_TYPE) && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE) {
			return false;
		}
		if (state.hasProperty(BlockStateProperties.EXTENDED) && state.getValue(BlockStateProperties.EXTENDED)) {
			return false;
		}
		return !state.is(Blocks.PISTON_HEAD) && !state.is(Blocks.MOVING_PISTON);
	}

	private static boolean isOurs(BlockState state) {
		return Sanupcraft.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
	}

	// ---- 면 설정 ----

	/** 기계의 {@code face} 면 모드를 다음으로. 기계가 아니면 안내하고 false. */
	public static boolean cycleFace(ServerLevel level, Player player, BlockPos pos, BlockState state, Direction face) {
		if (!(state.getBlock() instanceof MachineBlock) || !(level.getBlockEntity(pos) instanceof MachineBlockEntity machine)) {
			tell(player, Component.translatable("item.sanupcraft.wrench.machines_only").withStyle(ChatFormatting.RED));
			return false;
		}
		MachineFaces.Side side = MachineFaces.Side.of(state.getValue(MachineBlock.FACING), face);
		MachineFaces.Mode mode = machine.faces().get(side).next();
		machine.faces().set(side, mode);
		machine.setChanged();
		tell(player, Component.translatable("item.sanupcraft.wrench.face_set", side.displayName(), mode.displayName())
				.withStyle(ChatFormatting.YELLOW)
				.append(Component.literal("  ").append(machine.faces().summary()).withStyle(ChatFormatting.GRAY)));
		level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4F, 1.2F + mode.ordinal() * 0.1F);
		return true;
	}

	// ---- 집어 들기 ----

	/** 내용물째 집어 인벤토리에 넣고 (가득이면 발밑에) true. 못 집으면 안내하고 false. */
	public static boolean pickUp(ServerLevel level, Player player, BlockPos pos, BlockState state) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		ItemStack stack = new ItemStack(state.getBlock());
		if (blockEntity == null || !isOurs(state) || !(stack.getItem() instanceof BlockItem) || state.getDestroySpeed(level, pos) < 0.0F) {
			tell(player, Component.translatable("item.sanupcraft.wrench.cant_pick").withStyle(ChatFormatting.RED));
			return false;
		}
		if (blockEntity instanceof EnderAnchorBlockEntity) {
			tell(player, Component.translatable("item.sanupcraft.wrench.anchor").withStyle(ChatFormatting.RED));
			return false;
		}
		if (!mayPickUp(blockEntity, player)) {
			tell(player, Component.translatable("item.sanupcraft.wrench.locked").withStyle(ChatFormatting.RED));
			return false;
		}
		copyData(level, blockEntity, stack);
		// 내용물은 아이템에 담았으니 블록 엔티티가 부서질 때 떨구는 일(상자 내용물 쏟기 등)은 건너뛴다
		level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
		level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
		level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
		if (!player.getInventory().add(stack) && !stack.isEmpty()) {
			player.spawnAtLocation(level, stack);
		}
		return true;
	}

	/** 잠긴 블록은 관리할 수 있는 사람만 집는다. */
	public static boolean mayPickUp(BlockEntity blockEntity, Player player) {
		if (blockEntity instanceof EnderLinkBlockEntity link) {
			return !link.isLocked() || link.canManage(player);
		}
		if (blockEntity instanceof TerminalBlockEntity terminal) {
			return !terminal.lock().isLocked() || terminal.lock().canManage(player);
		}
		return true;
	}

	/** 바닐라 "데이터째 집기"와 같은 방식으로 블록 엔티티를 아이템에 담는다. */
	private static void copyData(ServerLevel level, BlockEntity blockEntity, ItemStack stack) {
		try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), Sanupcraft.LOGGER)) {
			TagValueOutput output = TagValueOutput.createWithContext(reporter, level.registryAccess());
			blockEntity.saveCustomOnly(output);
			blockEntity.removeComponentsFromTag(output);
			BlockItem.setBlockEntityData(stack, blockEntity.getType(), output);
			stack.applyComponents(blockEntity.collectComponents());
		}
	}

	/** 액션바 안내 (시험용 가짜 플레이어는 연결이 없어 건너뜀). */
	static void tell(Player player, Component message) {
		if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.connection != null) {
			player.sendOverlayMessage(message);
		}
	}
}
