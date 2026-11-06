package io.github.yhyeop.sanupcraft.storage;

import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 창고 단말 패킷. 네트워크 내용은 바닐라 칸 동기화로 보낼 수 없어서 따로 보낸다.
 * <ul>
 *   <li>서버 → 클라이언트 {@link Contents}: 화면이 열려 있는 동안 내용이 바뀌면 목록 전체.</li>
 *   <li>클라이언트 → 서버 {@link Action}: 목록 칸 클릭 (꺼내기 · 넣기). 서버가 열린 화면 · 개수를 다시 확인한다.</li>
 *   <li>클라이언트 → 서버 {@link Fill}: JEI 레시피 옮기기 (제작 칸 9개 각각에 쓸 수 있는 아이템 후보).</li>
 * </ul>
 */
public final class StoragePayloads {
	/** 목록 최대 종류 수 (패킷 크기 보호). */
	public static final int MAX_ENTRIES = 8192;
	public static final int MAX_CHOICES = 32;

	private StoragePayloads() {
	}

	private static final StreamCodec<RegistryFriendlyByteBuf, StorageNetwork.Entry> ENTRY_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC, StorageNetwork.Entry::stack,
			ByteBufCodecs.VAR_LONG, StorageNetwork.Entry::count,
			StorageNetwork.Entry::new);

	public record Contents(int containerId, List<StorageNetwork.Entry> entries) implements CustomPacketPayload {
		public static final Type<Contents> TYPE = new Type<>(Sanupcraft.id("terminal_contents"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Contents> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Contents::containerId,
				ENTRY_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), Contents::entries,
				Contents::new);

		@Override
		public Type<Contents> type() {
			return TYPE;
		}
	}

	public record Action(int containerId, int action, ItemStack template) implements CustomPacketPayload {
		public static final Type<Action> TYPE = new Type<>(Sanupcraft.id("terminal_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Action::containerId,
				ByteBufCodecs.VAR_INT, Action::action,
				ItemStack.OPTIONAL_UNTRUSTED_STREAM_CODEC, Action::template,
				Action::new);

		@Override
		public Type<Action> type() {
			return TYPE;
		}
	}

	public record Fill(int containerId, List<List<ItemStack>> slots) implements CustomPacketPayload {
		public static final Type<Fill> TYPE = new Type<>(Sanupcraft.id("terminal_fill"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Fill> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Fill::containerId,
				ItemStack.OPTIONAL_UNTRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CHOICES)).apply(ByteBufCodecs.list(TerminalMenu.GRID_SIZE)), Fill::slots,
				Fill::new);

		@Override
		public Type<Fill> type() {
			return TYPE;
		}
	}

	static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Contents.TYPE, Contents.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Fill.TYPE, Fill.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (player.containerMenu instanceof TerminalMenu menu && menu.containerId == payload.containerId() && menu.stillValid(player)) {
				menu.handleAction(player, payload.action(), payload.template());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Fill.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (player.containerMenu instanceof TerminalMenu menu && menu.containerId == payload.containerId() && menu.stillValid(player)) {
				menu.fillGrid(player, payload.slots());
			}
		});
	}
}
