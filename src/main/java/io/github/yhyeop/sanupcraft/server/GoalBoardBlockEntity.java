package io.github.yhyeop.sanupcraft.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import org.jspecify.annotations.Nullable;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 공용 목표판 (T3). 목표 하나(아이템 + 개수)를 들고, 사람들이 넣은 수로 진행도를 센다.
 * <ul>
 *   <li>넣는 법: 목표 아이템을 들고 우클릭 (든 것 전부) · 깔때기 · 벨트 (한 칸짜리 상자처럼 보이고 받자마자 센다).</li>
 *   <li>다 차면: 서버 전체에 알림 → 기여자(접속 중)에게 발전 과제 → 위 칸에 보상 상자({@code sanupcraft:goal_reward} 전리품) → 다음 기본 목표.</li>
 *   <li>관리자가 {@code /sanupcraft goal set}으로 목표를 바꾸면 진행도는 0부터. 그 목표를 마치면 기본 목표 목록을 이어서 간다.</li>
 *   <li>부수면 진행도는 사라진다 (블록이 단단해서 쉽게 부서지지 않는다).</li>
 * </ul>
 */
public class GoalBoardBlockEntity extends BlockEntity implements Container {
	public static final ResourceKey<LootTable> REWARD_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Sanupcraft.id("goal_reward"));
	public static final String ADVANCEMENT = "goal_done";
	private static final Codec<Map<String, Integer>> CONTRIBUTORS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

	private Identifier goalItem;
	private int goalCount;
	private int progress;
	/** 다음에 올 기본 목표의 번호. */
	private int nextDefault = 1;
	/** 지금까지 채운 목표 수. */
	private int completed;
	/** 이번 목표에 넣은 사람별 개수 (손으로 넣은 것만). */
	private final Map<String, Integer> contributors = new LinkedHashMap<>();

	public GoalBoardBlockEntity(BlockPos pos, BlockState state) {
		super(ModServer.GOAL_BOARD_BLOCK_ENTITY, pos, state);
		Goals.Goal first = Goals.defaultGoal(0);
		this.goalItem = first.item();
		this.goalCount = first.count();
	}

	// ---- 상태 ----

	public Identifier goalItemId() {
		return goalItem;
	}

	public int goalCount() {
		return goalCount;
	}

	public int progress() {
		return progress;
	}

	public int completedGoals() {
		return completed;
	}

	public boolean isGoalItem(ItemStack stack) {
		return !stack.isEmpty() && goalItem.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
	}

	/** 목표 아이템의 이름 (등록에 없는 id면 id 그대로). */
	public Component goalName() {
		Item item = BuiltInRegistries.ITEM.getOptional(goalItem).orElse(null);
		return item == null ? Component.literal(goalItem.toString()) : new ItemStack(item).getHoverName();
	}

	public int percent() {
		return goalCount <= 0 ? 100 : (int) Math.min(100L, 100L * progress / goalCount);
	}

	/** 많이 넣은 순 상위 {@code n}명 "이름 개수". */
	public List<Map.Entry<String, Integer>> topContributors(int n) {
		List<Map.Entry<String, Integer>> sorted = new ArrayList<>(contributors.entrySet());
		sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
		return sorted.subList(0, Math.min(n, sorted.size()));
	}

	public Component status() {
		return Component.translatable("block.sanupcraft.goal_board.status", goalName(), progress, goalCount, percent());
	}

	// ---- 바꾸기 ----

	/** 관리자 명령: 새 목표로 바꾸고 진행도를 0으로. */
	public void setGoal(Identifier item, int count) {
		goalItem = item;
		goalCount = Math.max(1, count);
		progress = 0;
		contributors.clear();
		setChanged();
	}

	/**
	 * 목표 아이템 {@code stack}을 센다 (스택은 줄이지 않는다). 모자란 만큼만 받고, 받은 개수를 돌려준다.
	 * 다 차면 바로 마무리한다.
	 */
	public int insert(@Nullable Player who, ItemStack stack) {
		if (!isGoalItem(stack) || progress >= goalCount) {
			return 0;
		}
		int accepted = Math.min(stack.getCount(), goalCount - progress);
		progress += accepted;
		if (who != null) {
			contributors.merge(who.getGameProfile().name(), accepted, Integer::sum);
		}
		setChanged();
		if (progress >= goalCount && level instanceof ServerLevel serverLevel) {
			complete(serverLevel);
		}
		return accepted;
	}

	private void complete(ServerLevel serverLevel) {
		MinecraftServer server = serverLevel.getServer();
		List<String> names = new ArrayList<>(contributors.keySet());
		Component goal = Component.translatable("block.sanupcraft.goal_board.goal_text", goalName(), goalCount);
		server.getPlayerList().broadcastSystemMessage(names.isEmpty()
				? Component.translatable("block.sanupcraft.goal_board.done", goal)
				: Component.translatable("block.sanupcraft.goal_board.done_with", goal, String.join(", ", names)), false);

		AdvancementHolder advancement = server.getAdvancements().get(Sanupcraft.id(ADVANCEMENT));
		if (advancement != null) {
			for (String name : names) {
				ServerPlayer player = server.getPlayerList().getPlayerByName(name);
				if (player != null) {
					player.getAdvancements().award(advancement, "done");
				}
			}
		}
		spawnReward(serverLevel);

		completed++;
		Goals.Goal next = Goals.defaultGoal(nextDefault++);
		goalItem = next.item();
		goalCount = next.count();
		progress = 0;
		contributors.clear();
		setChanged();
	}

	/** 위 칸(없으면 옆 칸)에 전리품 상자를 놓는다. 둘 곳이 없으면 전리품을 바로 떨군다. */
	private void spawnReward(ServerLevel serverLevel) {
		List<BlockPos> candidates = new ArrayList<>();
		candidates.add(worldPosition.above());
		for (Direction side : Direction.Plane.HORIZONTAL) {
			candidates.add(worldPosition.relative(side));
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			candidates.add(worldPosition.above().relative(side));
		}
		for (BlockPos pos : candidates) {
			if (serverLevel.getBlockState(pos).isAir()) {
				serverLevel.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
				RandomizableContainer.setBlockEntityLootTable(serverLevel, serverLevel.getRandom(), pos, REWARD_LOOT);
				return;
			}
		}
		LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(REWARD_LOOT);
		LootParams params = new LootParams.Builder(serverLevel)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
				.create(LootContextParamSets.CHEST);
		table.getRandomItems(params, stack -> Containers.dropItemStack(serverLevel, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
				worldPosition.getZ() + 0.5, stack));
	}

	// ---- 깔때기 · 벨트가 넣는 길 (한 칸짜리 상자, 받자마자 센다) ----

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		// 깔때기 · 벨트 · 일꾼은 스택째 넘긴다 (26.3 tryMoveInItem): 남는 몫은 판 위에 떨군다
		int accepted = insert(null, stack);
		int rest = stack.getCount() - accepted;
		if (rest > 0 && accepted > 0 && isGoalItem(stack) && progress < goalCount) {
			// 한 목표를 채우고 같은 아이템의 다음 목표(/goal set 연속 지정)로 넘어간 경우
			int more = insert(null, stack.copyWithCount(rest));
			rest -= more;
		}
		if (rest > 0 && level != null) {
			Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
					stack.copyWithCount(rest));
		}
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return progress < goalCount && isGoalItem(stack);
	}

	@Override
	public boolean canTakeItem(Container into, int slot, ItemStack stack) {
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
	}

	// ---- 저장 ----

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		Goals.Goal first = Goals.defaultGoal(0);
		goalItem = input.read("goal_item", Identifier.CODEC).orElse(first.item());
		goalCount = Math.max(1, input.getIntOr("goal_count", first.count()));
		progress = Math.max(0, input.getIntOr("progress", 0));
		nextDefault = input.getIntOr("next_default", 1);
		completed = input.getIntOr("completed", 0);
		contributors.clear();
		input.read("contributors", CONTRIBUTORS_CODEC).ifPresent(contributors::putAll);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("goal_item", Identifier.CODEC, goalItem);
		output.putInt("goal_count", goalCount);
		output.putInt("progress", progress);
		output.putInt("next_default", nextDefault);
		output.putInt("completed", completed);
		output.store("contributors", CONTRIBUTORS_CODEC, contributors);
	}
}
