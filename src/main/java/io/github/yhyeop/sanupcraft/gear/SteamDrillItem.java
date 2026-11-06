package io.github.yhyeop.sanupcraft.gear;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 증기 드릴: 곡괭이 + 삽을 한 자루에, 다이아 등급 (강철보다 단단한 블록까지). 내구도 대신 증기 2000, 블록당 10.
 * 웅크리고 우클릭하면 1x1 / 3x3 모드를 바꾼다 (모드는 아이템에 저장). 3x3은 캔 면에 평평하게 {@link ToolMining}이 처리한다.
 * 증기가 떨어지면 맨손처럼 느리고 도구가 필요한 블록은 드롭도 없다. 웅크리고 캐면 모드와 상관없이 한 칸만.
 */
public class SteamDrillItem extends SteamToolItem {
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int CAPACITY = 2000;
	public static final int STEAM_PER_BLOCK = 10;

	/** 3x3 모드인가 (없으면 1x1). 저장 · 동기화. */
	public static final DataComponentType<Boolean> WIDE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Sanupcraft.id("drill_wide"),
			DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	public SteamDrillItem(Item.Properties properties) {
		super(properties, CAPACITY, state -> SanupcraftConfig.current().drillSteamPerBlock);
	}

	public static boolean isWide(ItemStack stack) {
		return stack.getOrDefault(WIDE, false);
	}

	public static void setWide(ItemStack stack, boolean wide) {
		if (wide) {
			stack.set(WIDE, true);
		} else {
			stack.remove(WIDE);
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			boolean wide = !isWide(stack);
			setWide(stack, wide);
			if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.connection != null) {
				player.sendOverlayMessage(Component.translatable(wide ? "item.sanupcraft.steam_drill.mode_wide" : "item.sanupcraft.steam_drill.mode_single"));
			}
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, wide ? 1.4F : 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, builder, flag);
		builder.accept(Component.translatable(isWide(stack) ? "item.sanupcraft.steam_drill.mode_wide" : "item.sanupcraft.steam_drill.mode_single")
				.withStyle(ChatFormatting.YELLOW));
	}
}
