package io.github.yhyeop.sanupcraft.resource.vein;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteamChargedItem;

/**
 * 광맥 탐사기 (확장 3 D2): 자기 몸에 증기 500을 담는다 (충전대에서 채움). 우클릭하면 증기 50(설정 {@code veins.prospectorSteam})을 쓰고
 * 둘레 5x5 청크의 광맥 지도를 채팅에, 지금 청크의 요약을 액션바에 띄운다. 쿨다운 1초. 창작 모드는 증기가 들지 않는다.
 * 증기가 모자라거나 광맥이 없는 차원(엔드 등)이면 안내만 하고 증기도 쿨다운도 쓰지 않는다.
 */
public class ProspectorItem extends SteamChargedItem {
	public static final int CAPACITY = 500;
	public static final int COOLDOWN_TICKS = 20;
	/** 지도 반지름 (청크). 2 = 5x5. */
	public static final int MAP_RADIUS = 2;

	/** 써 본 결과. */
	public enum Result {
		SCANNED, NO_VEINS_HERE, NO_STEAM, COOLDOWN
	}

	public ProspectorItem(Item.Properties properties) {
		super(properties, CAPACITY);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel serverLevel) {
			return scan(serverLevel, player, stack) == Result.SCANNED || player.getCooldowns().isOnCooldown(stack)
					? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}
		return player.getCooldowns().isOnCooldown(stack) ? InteractionResult.FAIL : InteractionResult.SUCCESS;
	}

	/** 한 번 탐사한다 (증기를 쓰고 지도를 보낸다). 시험이 직접 부른다. */
	public static Result scan(ServerLevel level, Player player, ItemStack prospector) {
		if (player.getCooldowns().isOnCooldown(prospector)) {
			return Result.COOLDOWN;
		}
		if (!Veins.hasVeins(level.dimension())) {
			player.sendOverlayMessage(Component.translatable("item.sanupcraft.vein_prospector.none_here"));
			return Result.NO_VEINS_HERE;
		}
		int cost = SanupcraftConfig.current().prospectorSteam;
		if (!player.hasInfiniteMaterials()) {
			if (SteamCharge.get(prospector) < cost) {
				player.sendOverlayMessage(Component.translatable("item.sanupcraft.vein_prospector.no_steam", cost));
				return Result.NO_STEAM;
			}
			SteamCharge.take(prospector, cost);
		}
		player.getCooldowns().addCooldown(prospector, COOLDOWN_TICKS);
		ChunkPos center = ChunkPos.containing(player.blockPosition());
		for (Component line : mapLines(level, center)) {
			player.sendSystemMessage(line);
		}
		player.sendOverlayMessage(summary(level, center));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7F, 1.2F);
		return Result.SCANNED;
	}

	/** 지도 머리글 + 5줄 (북쪽이 위, 가운데가 {@code center}). 칸마다 "철87"처럼 한 글자 + 남은 %, 광맥이 없으면 "·". */
	public static List<Component> mapLines(ServerLevel level, ChunkPos center) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("item.sanupcraft.vein_prospector.map", center.x(), center.z()).withStyle(ChatFormatting.AQUA));
		for (int dz = -MAP_RADIUS; dz <= MAP_RADIUS; dz++) {
			MutableComponent row = Component.empty();
			for (int dx = -MAP_RADIUS; dx <= MAP_RADIUS; dx++) {
				if (dx > -MAP_RADIUS) {
					row.append(" ");
				}
				row.append(cell(Veins.get(level, new ChunkPos(center.x() + dx, center.z() + dz)), dx == 0 && dz == 0));
			}
			lines.add(row);
		}
		return lines;
	}

	private static Component cell(@Nullable Vein vein, boolean here) {
		MutableComponent text;
		if (vein == null) {
			text = Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY);
		} else {
			text = Component.empty()
					.append(Component.translatable(vein.type().shortKey()))
					.append(Integer.toString(vein.percent()))
					.withStyle(Style.EMPTY.withColor(vein.depleted() ? 0x555555 : vein.type().color()));
		}
		return here ? text.withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE) : text;
	}

	/** 지금 청크 한 줄 요약 (액션바). */
	public static Component summary(ServerLevel level, ChunkPos chunk) {
		Vein vein = Veins.get(level, chunk);
		if (vein == null) {
			return Component.translatable("item.sanupcraft.vein_prospector.here_none");
		}
		Component name = Component.translatable(vein.type().nameKey()).withStyle(Style.EMPTY.withColor(vein.type().color()));
		return vein.depleted()
				? Component.translatable("item.sanupcraft.vein_prospector.here_empty", name)
				: Component.translatable("item.sanupcraft.vein_prospector.here", name, vein.remaining(), vein.total(), vein.percent());
	}
}
