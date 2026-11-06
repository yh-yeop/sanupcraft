package io.github.yhyeop.sanupcraft.end;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 차원 창고 블록 엔티티 (M6). 들고 있는 것은 채널(염료 색)과 놓은 사람뿐이고, 내용은 서버 전체 {@link VaultData}에 있다.
 * 깔때기 · 벨트가 넣고 빼도록 {@link Container}로서 채널의 54칸을 그대로 보여 준다 (서버에서만, 잠긴 채널은 주인 · 팀이 놓은 창고만).
 * 부서져도 내용은 떨구지 않는다 (채널에 그대로 남는다).
 */
public class DimensionalVaultBlockEntity extends BlockEntity implements Container {
	private DyeColor channel = DyeColor.WHITE;
	private @Nullable UUID placer;

	public DimensionalVaultBlockEntity(BlockPos pos, BlockState state) {
		super(ModEnd.DIMENSIONAL_VAULT_BLOCK_ENTITY, pos, state);
	}

	public DyeColor channelColor() {
		return channel;
	}

	public void setChannel(DyeColor color) {
		channel = color;
		setChanged();
	}

	public @Nullable UUID placer() {
		return placer;
	}

	public void setPlacer(Player player) {
		placer = player.getUUID();
		setChanged();
	}

	/** 이 창고의 채널 (서버에서만, 아니면 null). */
	public VaultData.@Nullable Channel channel() {
		return level instanceof ServerLevel serverLevel ? VaultData.get(serverLevel.getServer()).channel(channel) : null;
	}

	/** 자동 운반이 보는 칸 묶음. 잠겨서 못 쓰면 null. */
	private VaultData.@Nullable Channel automationChannel() {
		VaultData.Channel shared = channel();
		return shared != null && shared.automationAllowed(level.getServer(), placer) ? shared : null;
	}

	/** 플레이어가 여는 화면. 칸은 채널과 같은 묶음이고, 창고에서 멀어지거나 채널 · 잠금이 바뀌면 닫힌다. */
	public MenuProvider menuProvider(VaultData.Channel shared) {
		Component title = Component.translatable("block.sanupcraft.dimensional_vault.titled", Component.translatable("color.minecraft." + channel.getName()));
		DyeColor opened = channel;
		return new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return title;
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
				return ChestMenu.sixRows(containerId, inventory, new VaultView(shared, opened));
			}
		};
	}

	/** 화면용 칸 묶음: 채널에 그대로 맡기고, 열 수 있는지만 따로 본다. */
	private final class VaultView implements Container {
		private final VaultData.Channel shared;
		private final DyeColor opened;

		VaultView(VaultData.Channel shared, DyeColor opened) {
			this.shared = shared;
			this.opened = opened;
		}

		@Override
		public int getContainerSize() {
			return shared.getContainerSize();
		}

		@Override
		public boolean isEmpty() {
			return shared.isEmpty();
		}

		@Override
		public ItemStack getItem(int slot) {
			return shared.getItem(slot);
		}

		@Override
		public ItemStack removeItem(int slot, int count) {
			return shared.removeItem(slot, count);
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			return shared.removeItemNoUpdate(slot);
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			shared.setItem(slot, stack);
		}

		@Override
		public void setChanged() {
			shared.setChanged();
		}

		@Override
		public boolean stillValid(Player player) {
			return !isRemoved() && channel == opened && shared.canUse(player)
					&& Container.stillValidBlockEntity(DimensionalVaultBlockEntity.this, player);
		}

		@Override
		public void clearContent() {
			shared.clearContent();
		}
	}

	// ---- 깔때기 · 벨트 (서버) ----

	@Override
	public int getContainerSize() {
		VaultData.Channel shared = automationChannel();
		return shared == null ? 0 : shared.getContainerSize();
	}

	@Override
	public boolean isEmpty() {
		VaultData.Channel shared = automationChannel();
		return shared == null || shared.isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		VaultData.Channel shared = automationChannel();
		return shared == null ? ItemStack.EMPTY : shared.getItem(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		VaultData.Channel shared = automationChannel();
		return shared == null ? ItemStack.EMPTY : shared.removeItem(slot, count);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		VaultData.Channel shared = automationChannel();
		return shared == null ? ItemStack.EMPTY : shared.removeItemNoUpdate(slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		VaultData.Channel shared = automationChannel();
		if (shared != null) {
			shared.setItem(slot, stack);
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		// 채널 내용은 블록과 상관없이 남는다
	}

	/** 부서져도 채널 내용을 떨구지 않는다. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		channel = input.read("channel", DyeColor.CODEC).orElse(DyeColor.WHITE);
		placer = input.read("placer", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("channel", DyeColor.CODEC, channel);
		output.storeNullable("placer", UUIDUtil.CODEC, placer);
	}
}
