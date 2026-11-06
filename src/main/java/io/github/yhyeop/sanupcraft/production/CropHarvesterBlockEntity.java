package io.github.yhyeop.sanupcraft.production;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Inventory;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 작물 수확기: 앞쪽 9x9 (옆 -4~+4, 앞 1~9칸, 수확기와 같은 높이)를 한 칸씩 돌며 다 자란 작물을 거두고 씨앗 하나로 다시 심는다.
 * 거두는 것: 밀 · 당근 · 감자 · 비트(CropBlock), 네더 사마귀, 코코아, 달콤한 열매.
 * 다시 심기: 거둔 것 중 그 작물의 씨앗(밀 → 밀 씨앗, 당근 → 당근 …) 하나를 쓰고 나이 0으로 되돌린다 (코코아는 방향 그대로).
 * 씨앗이 안 나왔으면 거두기만 하고 빈 자리로 둔다. 달콤한 열매 덤불은 씨앗 없이 나이 1로 되돌린다.
 * 증기는 거둘 때만 든다 (훑는 것은 공짜). 증기가 모자라면 멈춘다.
 */
public class CropHarvesterBlockEntity extends SteamWorkerBlockEntity {
	public static final int RADIUS = 4;
	public static final int DEPTH = 9;
	public static final int STEAM_CAPACITY = 200;
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int TICKS_PER_SCAN = 2;
	public static final int STEAM_PER_CROP = 3;
	private static final ItemStack TOOL = new ItemStack(Items.IRON_HOE);

	private int cursor;

	public CropHarvesterBlockEntity(BlockPos pos, BlockState state) {
		super(ModProduction.CROP_HARVESTER_BLOCK_ENTITY, pos, state);
	}

	@Override
	protected boolean work(ServerLevel level, BlockPos pos, BlockState state) {
		if (!flushPending()) {
			return false;
		}
		SanupcraftConfig config = SanupcraftConfig.current();
		if (steam < config.harvesterSteamPerCrop) {
			return false;
		}
		if (level.getGameTime() % config.harvesterTicksPerScan != 0) {
			return true;
		}
		BlockPos target = target(pos);
		cursor = (cursor + 1) % (DEPTH * (RADIUS * 2 + 1));
		return harvest(level, target);
	}

	/** 앞 9x9의 {@code index}번째 칸 (앞쪽 줄부터, 줄 안에서는 왼쪽부터). */
	public BlockPos cell(BlockPos pos, int index) {
		Direction forward = facing();
		Direction right = forward.getClockWise();
		int side = RADIUS * 2 + 1;
		int row = index / side + 1;
		int col = index % side - RADIUS;
		return pos.relative(forward, row).relative(right, col);
	}

	private BlockPos target(BlockPos pos) {
		return cell(pos, cursor);
	}

	/** 다 자란 작물인가. */
	public static boolean isMature(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof CropBlock crop) {
			return crop.isMaxAge(state);
		}
		if (block instanceof NetherWartBlock) {
			return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
		}
		if (block instanceof CocoaBlock) {
			return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
		}
		if (block instanceof SweetBerryBushBlock) {
			return state.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE;
		}
		return false;
	}

	private static IntegerProperty ageProperty(BlockState state) {
		for (Property<?> property : state.getProperties()) {
			if (property instanceof IntegerProperty integer && property.getName().equals("age")) {
				return integer;
			}
		}
		return null;
	}

	/** 한 칸을 거둔다. 거뒀으면 true. 시험이 직접 부른다. */
	public boolean harvest(ServerLevel level, BlockPos target) {
		BlockState state = level.getBlockState(target);
		IntegerProperty age = ageProperty(state);
		if (age == null || !isMature(state)) {
			return false;
		}
		int cost = SanupcraftConfig.current().harvesterSteamPerCrop;
		if (steam < cost) {
			return false;
		}
		List<ItemStack> drops = Block.getDrops(state, level, target, level.getBlockEntity(target), null, TOOL);
		BlockState replanted;
		if (state.getBlock() instanceof SweetBerryBushBlock) {
			replanted = state.setValue(age, 1);
		} else {
			ItemStack seed = state.getCloneItemStack(level, target, false);
			boolean paid = false;
			for (ItemStack drop : drops) {
				if (!drop.isEmpty() && drop.is(seed.getItem())) {
					drop.shrink(1);
					paid = true;
					break;
				}
			}
			replanted = paid ? state.setValue(age, 0) : Blocks.AIR.defaultBlockState();
		}
		steam -= cost;
		level.setBlock(target, replanted, Block.UPDATE_ALL);
		level.playSound(null, target, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 3, 0.25, 0.25, 0.25, 0.0);
		for (ItemStack drop : drops) {
			if (!drop.isEmpty()) {
				stash(drop);
			}
		}
		setChanged();
		return true;
	}

	@Override
	public int steamCapacity() {
		return STEAM_CAPACITY;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.sanupcraft.crop_harvester");
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, this, 1);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		cursor = Math.floorMod(input.getIntOr("cursor", 0), DEPTH * (RADIUS * 2 + 1));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("cursor", cursor);
	}
}
