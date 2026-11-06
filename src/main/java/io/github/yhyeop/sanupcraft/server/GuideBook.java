package io.github.yhyeop.sanupcraft.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 설명서 (T4). 쪽 글은 언어 파일의 {@code book.sanupcraft.guide.1 ~ N}에 있고 (한국어가 기본), 아이템에는 번역 키 그대로 들어간다.
 * 처음 접속한 사람에게 한 번 준다 (받은 명단은 {@code data/sanupcraft/guide_given.dat}). 잃어버리면 책 + 철판으로 다시 만든다.
 */
public final class GuideBook {
	/** 쪽 수. 언어 파일의 쪽 키 개수와 같아야 한다 (GameTest가 확인). */
	public static final int PAGES = 14;
	public static final String TITLE = "산업크래프트 안내서";

	private static final class Given extends SavedData {
		static final Codec<Given> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(g -> new ArrayList<>(g.players))).apply(i, Given::new));
		static final SavedDataType<Given> TYPE = new SavedDataType<>(Sanupcraft.id("guide_given"), Given::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

		private final Set<UUID> players = new HashSet<>();

		Given() {
		}

		Given(List<UUID> players) {
			this.players.addAll(players);
		}

		boolean add(UUID id) {
			boolean added = players.add(id);
			if (added) {
				setDirty();
			}
			return added;
		}
	}

	private GuideBook() {
	}

	/** 책 내용: 제목 + 번역 키 쪽들. */
	public static WrittenBookContent content() {
		List<Filterable<Component>> pages = new ArrayList<>();
		for (int page = 1; page <= PAGES; page++) {
			pages.add(Filterable.passThrough(Component.translatable("book.sanupcraft.guide." + page)));
		}
		return new WrittenBookContent(Filterable.passThrough(TITLE), "산업크래프트", 0, pages, true);
	}

	public static ItemStack create() {
		return new ItemStack(ModServer.GUIDE_BOOK);
	}

	/** 이 사람이 처음이면 설명서를 주고 참을 돌려준다. 이미 받았으면 아무것도 안 하고 거짓. */
	public static boolean giveFirstJoin(MinecraftServer server, ServerPlayer player) {
		Given given = server.getDataStorage().computeIfAbsent(Given.TYPE);
		if (!given.add(player.getUUID())) {
			return false;
		}
		ItemStack book = create();
		if (!player.getInventory().add(book) && !book.isEmpty()) {
			player.spawnAtLocation(player.level(), book);
		}
		player.sendSystemMessage(Component.translatable("book.sanupcraft.guide.given"));
		return true;
	}
}
