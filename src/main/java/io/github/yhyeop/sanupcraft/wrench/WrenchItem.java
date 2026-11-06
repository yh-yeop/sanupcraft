package io.github.yhyeop.sanupcraft.wrench;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

/** 렌치 아이템. 블록에 쓰는 건 {@link Wrench} (이벤트), 여기는 허공 우클릭(모드 바꾸기)과 툴팁. */
public class WrenchItem extends Item {
	public WrenchItem(Item.Properties properties) {
		super(properties);
	}

	public static boolean isFaceMode(ItemStack stack) {
		return stack.getOrDefault(ModWrench.FACE_MODE, false);
	}

	public static void setFaceMode(ItemStack stack, boolean faces) {
		if (faces) {
			stack.set(ModWrench.FACE_MODE, true);
		} else {
			stack.remove(ModWrench.FACE_MODE);
		}
	}

	private static Component modeName(ItemStack stack) {
		return Component.translatable(isFaceMode(stack) ? "item.sanupcraft.wrench.mode_faces" : "item.sanupcraft.wrench.mode_rotate");
	}

	/** 허공에 우클릭: 돌리기 ↔ 면 설정. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			setFaceMode(stack, !isFaceMode(stack));
			Wrench.tell(player, modeName(stack));
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F,
					isFaceMode(stack) ? 1.4F : 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		// 사용법 줄(.tooltip ~ .tooltip3)은 클라이언트가 언어 파일에서 붙인다
		builder.accept(modeName(stack).copy().withStyle(ChatFormatting.YELLOW));
	}
}
