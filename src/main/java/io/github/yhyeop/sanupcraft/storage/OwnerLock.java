package io.github.yhyeop.sanupcraft.storage;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 주인 잠금 (엔더 운반기와 같은 규칙). 놓은 사람이 주인. 주인 · OP가 웅크리고 빈손 우클릭으로 잠그면 주인 · OP만 쓴다 (기본 열림).
 * 주인이 없으면 처음 잠그는 사람이 주인이 된다. 블록 엔티티가 하나씩 갖고, 저장도 대신 해 준다.
 */
public final class OwnerLock {
	private @Nullable UUID owner;
	private String ownerName = "";
	private boolean locked;

	public @Nullable UUID owner() {
		return owner;
	}

	public String ownerName() {
		return ownerName;
	}

	public void setOwner(Player player) {
		owner = player.getUUID();
		ownerName = player.getGameProfile().name();
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean value) {
		locked = value;
	}

	public boolean isOwner(Player player) {
		return owner != null && owner.equals(player.getUUID());
	}

	/** 잠금을 켜고 끌 수 있는가: 주인, OP, 또는 주인이 없을 때. */
	public boolean canManage(Player player) {
		return owner == null || isOwner(player) || io.github.yhyeop.sanupcraft.server.Teams.sameTeam(player.level(), owner, player) // M10 팀
				|| player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	/** 쓸 수 있는가. 잠기지 않았으면 누구나. */
	public boolean canUse(Player player) {
		return !locked || canManage(player);
	}

	/**
	 * 웅크리고 빈손 우클릭: 관리할 수 있으면 잠금을 뒤집고 true. 주인이 없었으면 누른 사람이 주인이 된다.
	 * 관리할 수 없으면 아무것도 안 하고 false.
	 */
	public boolean toggle(Player player) {
		if (!canManage(player)) {
			return false;
		}
		if (owner == null) {
			setOwner(player);
		}
		locked = !locked;
		return true;
	}

	public void load(ValueInput input) {
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		ownerName = input.getStringOr("owner_name", "");
		locked = input.getBooleanOr("locked", false);
	}

	public void save(ValueOutput output) {
		output.storeNullable("owner", UUIDUtil.CODEC, owner);
		output.putString("owner_name", ownerName);
		output.putBoolean("locked", locked);
	}
}
