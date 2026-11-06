package io.github.yhyeop.sanupcraft.fluid;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** 탱크 아이템: 부술 때 남은 유체(컴포넌트)를 툴팁에 보인다. */
public class TankItem extends BlockItem {
	public TankItem(Block block, Item.Properties properties) {
		super(block, properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, builder, flag);
		FluidContents contents = stack.get(ModFluid.FLUID);
		if (contents != null) {
			builder.accept(Component.translatable("block.sanupcraft.tank.contents", Component.translatable(contents.kind().translationKey()),
					contents.amount(), TankBlockEntity.CAPACITY).withStyle(ChatFormatting.AQUA));
		}
	}
}
