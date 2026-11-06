package io.github.yhyeop.sanupcraft.wrench;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** M8 I4 렌치: 돌리기 · 집어 들기 · 기계 면 설정. 동작은 {@link Wrench}. */
public final class ModWrench {
	/** 면 설정 모드인가 (없으면 돌리기 모드). 허공에 우클릭으로 바꾼다. */
	public static final DataComponentType<Boolean> FACE_MODE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Sanupcraft.id("wrench_faces"),
			DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	public static final Item WRENCH = ModRegistry.item("wrench", WrenchItem::new, new Item.Properties().stacksTo(1));

	private ModWrench() {
	}

	public static void init() {
		// 블록의 우클릭(기계 화면 열기 등)보다 먼저 받아야 하므로 아이템의 useOn이 아니라 이벤트로 처리한다
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player.isSpectator() || !(player.getItemInHand(hand).getItem() instanceof WrenchItem)) {
				return InteractionResult.PASS;
			}
			if (!(level instanceof ServerLevel serverLevel)) {
				// 클라이언트는 서버로 보내기만 한다 (판단 · 안내는 서버)
				return InteractionResult.SUCCESS;
			}
			return Wrench.use(serverLevel, player, hand, hit);
		});
	}
}
