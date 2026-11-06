package io.github.yhyeop.sanupcraft.server;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** {@code /sanupcraft goal set <아이템> <개수>} (OP): 바라보는 공용 목표판의 목표를 바꾼다. */
final class GoalCommand {
	private static final double REACH = 8.0;

	private GoalCommand() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> node(CommandBuildContext context) {
		return Commands.literal("goal")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("set")
						.then(Commands.argument("item", ItemArgument.item(context))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 1_000_000))
										.executes(GoalCommand::set))));
	}

	private static int set(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer player = c.getSource().getPlayerOrException();
		Item item = ItemArgument.getItem(c, "item").item().value();
		int count = IntegerArgumentType.getInteger(c, "count");
		HitResult hit = player.pick(REACH, 1.0F, false);
		if (!(hit instanceof BlockHitResult block) || !(player.level().getBlockEntity(block.getBlockPos()) instanceof GoalBoardBlockEntity board)) {
			c.getSource().sendFailure(Component.translatable("commands.sanupcraft.goal.not_looking"));
			return 0;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		board.setGoal(id, count);
		c.getSource().sendSuccess(() -> Component.translatable("commands.sanupcraft.goal.set", new ItemStack(item).getHoverName(), count), true);
		return 1;
	}
}
