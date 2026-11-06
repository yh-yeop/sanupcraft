package io.github.yhyeop.sanupcraft.production.grinder;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import io.github.yhyeop.sanupcraft.fluid.FluidContents;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;
import io.github.yhyeop.sanupcraft.fluid.ModFluid;

/** 경험치 탱크 아이템: 부술 때 남은 경험치(컴포넌트)를 툴팁에 보인다. */
public class ExperienceTankItem extends BlockItem {
	public ExperienceTankItem(Block block, Item.Properties properties) {
		super(block, properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, builder, flag);
		FluidContents contents = stack.get(ModFluid.FLUID);
		if (contents != null && contents.kind() == FluidKind.EXPERIENCE) {
			builder.accept(Component.translatable("block.sanupcraft.experience_tank.contents", contents.amount() / FluidKind.MB_PER_XP,
					ExperienceTankBlockEntity.CAPACITY_POINTS).withStyle(ChatFormatting.GREEN));
		}
	}
}
