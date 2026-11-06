package io.github.yhyeop.sanupcraft.end;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.server.Teams;

/**
 * 차원 창고 (M6)의 내용. 서버 전체 저장 데이터 {@code data/sanupcraft/dimensional_vaults.dat}라서 어느 차원의 창고든 같은 채널이면 같은 54칸이다.
 * 채널(염료 16색)마다 {@link Channel} 하나: 54칸 + 주인 · 잠금. 잠금은 블록이 아니라 채널에 걸린다 (잠긴 채널은 어느 창고로도 주인 · 팀 · OP만 연다).
 * 칸이 바뀌면 바로 저장 표시(setDirty)를 한다. 여러 사람이 같은 채널을 동시에 열어도 같은 칸 묶음 하나를 본다 (상자를 같이 여는 것과 같다).
 */
public final class VaultData extends SavedData {
	public static final int SLOTS = 54;

	private record SlotEntry(int slot, ItemStack item) {
		static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.intRange(0, SLOTS - 1).fieldOf("slot").forGetter(SlotEntry::slot),
				ItemStack.CODEC.fieldOf("item").forGetter(SlotEntry::item)).apply(i, SlotEntry::new));
	}

	private record Saved(List<SlotEntry> items, Optional<UUID> owner, String ownerName, boolean locked) {
		static final Codec<Saved> CODEC = RecordCodecBuilder.create(i -> i.group(
				SlotEntry.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(Saved::items),
				UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Saved::owner),
				Codec.STRING.optionalFieldOf("owner_name", "").forGetter(Saved::ownerName),
				Codec.BOOL.optionalFieldOf("locked", false).forGetter(Saved::locked)).apply(i, Saved::new));
	}

	public static final Codec<VaultData> CODEC = Codec.unboundedMap(DyeColor.CODEC, Saved.CODEC)
			.xmap(VaultData::new, VaultData::snapshot);

	public static final SavedDataType<VaultData> TYPE = new SavedDataType<>(Sanupcraft.id("dimensional_vaults"), VaultData::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	/** 채널 하나: 54칸 + 주인 · 잠금. 칸이 바뀌면 저장 데이터에 표시한다. */
	public final class Channel extends SimpleContainer {
		private @Nullable UUID owner;
		private String ownerName = "";
		private boolean locked;

		Channel() {
			super(SLOTS);
		}

		@Override
		public void setChanged() {
			super.setChanged();
			setDirty();
		}

		public @Nullable UUID owner() {
			return owner;
		}

		public String ownerName() {
			return ownerName;
		}

		public boolean isLocked() {
			return locked;
		}

		/** 잠금을 바꿀 수 있는가: 주인 없음, 주인, 같은 팀, OP. */
		public boolean canManage(Player player) {
			return owner == null || owner.equals(player.getUUID()) || Teams.sameTeam(player.level(), owner, player)
					|| player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
		}

		public boolean canUse(Player player) {
			return !locked || canManage(player);
		}

		/** 잠금을 뒤집는다 (주인이 없으면 누른 사람이 주인). 못 바꾸면 false. */
		public boolean toggleLock(Player player) {
			if (!canManage(player)) {
				return false;
			}
			if (owner == null) {
				owner = player.getUUID();
				ownerName = player.getGameProfile().name();
			}
			locked = !locked;
			setDirty();
			return true;
		}

		/** 주인 · 잠금을 지운다 (시험 · 관리용). */
		public void release() {
			owner = null;
			ownerName = "";
			locked = false;
			setDirty();
		}

		/** 이 플레이어(UUID, 오프라인일 수 있음)의 블록이 자동 운반(깔때기)으로 쓸 수 있는가. 잠기면 주인 · 같은 팀이 놓은 창고만. */
		public boolean automationAllowed(@Nullable MinecraftServer server, @Nullable UUID placer) {
			if (!locked || owner == null) {
				return true;
			}
			if (placer == null) {
				return false;
			}
			if (owner.equals(placer)) {
				return true;
			}
			if (server == null) {
				return false;
			}
			var ownerTeam = Teams.teamOf(server, owner);
			return ownerTeam != null && ownerTeam == Teams.teamOf(server, placer);
		}
	}

	private final Map<DyeColor, Channel> channels = new EnumMap<>(DyeColor.class);

	public VaultData() {
	}

	private VaultData(Map<DyeColor, Saved> loaded) {
		loaded.forEach((color, saved) -> {
			Channel channel = new Channel();
			for (SlotEntry entry : saved.items()) {
				channel.items.set(entry.slot(), entry.item());
			}
			channel.owner = saved.owner().orElse(null);
			channel.ownerName = saved.ownerName();
			channel.locked = saved.locked();
			channels.put(color, channel);
		});
	}

	private Map<DyeColor, Saved> snapshot() {
		Map<DyeColor, Saved> out = new EnumMap<>(DyeColor.class);
		channels.forEach((color, channel) -> {
			List<SlotEntry> items = new ArrayList<>();
			for (int i = 0; i < SLOTS; i++) {
				ItemStack stack = channel.getItem(i);
				if (!stack.isEmpty()) {
					items.add(new SlotEntry(i, stack.copy()));
				}
			}
			if (!items.isEmpty() || channel.owner != null || channel.locked) {
				out.put(color, new Saved(items, Optional.ofNullable(channel.owner), channel.ownerName, channel.locked));
			}
		});
		return out;
	}

	/** 서버 전체 창고 데이터 (차원과 상관없이 하나). */
	public static VaultData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	/** 그 채널 (없으면 빈 채널을 만든다). */
	public Channel channel(DyeColor color) {
		return channels.computeIfAbsent(color, c -> new Channel());
	}
}
