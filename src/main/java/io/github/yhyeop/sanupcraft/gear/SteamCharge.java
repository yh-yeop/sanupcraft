package io.github.yhyeop.sanupcraft.gear;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 아이템에 담긴 증기. 증기 통 · 증기 부츠 (나중에 잔해 탐지기)가 같은 데이터 컴포넌트 {@code sanupcraft:steam}을 쓴다.
 * 컴포넌트가 없거나 0이면 빈 것이고, 0이 되면 컴포넌트를 지워서 같은 아이템끼리 합쳐질 수 있게 한다.
 * 이 클래스의 도우미만 쓰면 컴포넌트를 직접 만질 일이 없다:
 * <ul>
 * <li>{@link #get} · {@link #set} · {@link #fill} · {@link #take}: 한 아이템의 저장량</li>
 * <li>{@link #drain}: 플레이어 인벤토리의 증기 통들에서 한꺼번에 빼 쓴다 (리벳총 · 겉날개 가속 등)</li>
 * </ul>
 */
public final class SteamCharge {
	/** 증기 통 하나가 담는 최대 증기. */
	public static final int CANISTER_CAPACITY = 1000;

	/** 저장된 증기 (정수). 저장 · 네트워크 동기화 모두 한다. */
	public static final DataComponentType<Integer> STEAM = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Sanupcraft.id("steam"),
			DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 1_000_000)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** 막대 색 (옅은 하늘색). */
	public static final int BAR_COLOR = 0x6FD0F0;

	private SteamCharge() {
	}

	/** 모드 시작 때 한 번: 컴포넌트 등록(클래스 로딩)을 보장한다. */
	public static void init() {
	}

	/** 증기를 담는 아이템인가 (증기 통 · 증기 부츠). */
	public static boolean holdsSteam(ItemStack stack) {
		return stack.getItem() instanceof SteamChargedItem;
	}

	/** 담을 수 있는 최대 증기. 증기를 못 담는 아이템은 0. */
	public static int capacity(ItemStack stack) {
		return stack.getItem() instanceof SteamChargedItem item ? item.steamCapacity() : 0;
	}

	/** 지금 담긴 증기. */
	public static int get(ItemStack stack) {
		return holdsSteam(stack) ? Math.min(stack.getOrDefault(STEAM, 0), capacity(stack)) : 0;
	}

	/** 저장량을 정한다 (0 ~ 최대로 맞춤). 증기를 못 담는 아이템은 그대로. */
	public static void set(ItemStack stack, int amount) {
		if (!holdsSteam(stack)) {
			return;
		}
		int clamped = Mth.clamp(amount, 0, capacity(stack));
		if (clamped == 0) {
			stack.remove(STEAM);
		} else {
			stack.set(STEAM, clamped);
		}
	}

	/** 증기를 최대 {@code amount}만큼 채우고, 실제로 채운 양을 돌려준다. */
	public static int fill(ItemStack stack, int amount) {
		if (amount <= 0) {
			return 0;
		}
		int current = get(stack);
		int accepted = Math.min(amount, capacity(stack) - current);
		if (accepted > 0) {
			set(stack, current + accepted);
		}
		return Math.max(accepted, 0);
	}

	/** 증기를 최대 {@code amount}만큼 꺼내고, 실제로 꺼낸 양을 돌려준다. */
	public static int take(ItemStack stack, int amount) {
		if (amount <= 0) {
			return 0;
		}
		int current = get(stack);
		int taken = Math.min(amount, current);
		if (taken > 0) {
			set(stack, current - taken);
		}
		return taken;
	}

	/** 가득 찼나 (증기를 못 담는 아이템은 거짓). */
	public static boolean isFull(ItemStack stack) {
		return holdsSteam(stack) && get(stack) >= capacity(stack);
	}

	// ---- 증기 통 (인벤토리) ----

	/** 인벤토리에서 증기가 담긴 첫 증기 통. 없으면 null. */
	public static @Nullable ItemStack findCharged(Player player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (isCanister(stack) && get(stack) > 0) {
				return stack;
			}
		}
		return null;
	}

	/** 인벤토리의 증기 통들에 담긴 증기 합계. */
	public static int total(Player player) {
		int sum = 0;
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (isCanister(stack)) {
				sum += get(stack);
			}
		}
		return sum;
	}

	/**
	 * 인벤토리의 증기 통들에서 증기 {@code amount}를 빼 쓴다 (앞쪽 통부터). 합계가 모자라면 아무것도 빼지 않고 false.
	 * 창작 모드도 같이 뺀다 - 면제는 부르는 쪽이 정한다.
	 */
	public static boolean drain(Player player, int amount) {
		if (amount <= 0) {
			return true;
		}
		if (total(player) < amount) {
			return false;
		}
		int left = amount;
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (isCanister(stack)) {
				left -= take(stack, left);
			}
		}
		return true;
	}

	private static boolean isCanister(ItemStack stack) {
		return stack.is(SteamGear.STEAM_CANISTER);
	}
}
