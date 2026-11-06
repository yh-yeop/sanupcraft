package io.github.yhyeop.sanupcraft.mining.blast;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 원격 기폭기. 웅크리고 발파 장약을 우클릭하면 그 장약을 연결(다시 하면 연결 해제, 최대 8개)하고,
 * 허공에 우클릭하면 같은 차원 64칸 안의 연결된 장약 심지에 한꺼번에 불을 붙인다. 기폭기는 닳지 않는다.
 */
public class RemoteDetonatorItem extends Item {
	public static final int COOLDOWN_TICKS = 20;

	/** 연결을 눌러 본 결과. */
	public enum LinkResult {
		ADDED, REMOVED, FULL
	}

	public RemoteDetonatorItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		if (player == null || !player.isShiftKeyDown() || !level.getBlockState(pos).is(ModBlast.BLAST_CHARGE)) {
			return InteractionResult.PASS; // 허공 우클릭(use)과 같게 기폭
		}
		if (!level.isClientSide()) {
			LinkResult result = toggleLink(context.getItemInHand(), GlobalPos.of(level.dimension(), pos.immutable()));
			player.sendOverlayMessage(switch (result) {
				case ADDED -> Component.translatable("item.sanupcraft.remote_detonator.linked", pos.getX(), pos.getY(), pos.getZ(),
						links(context.getItemInHand()).size(), ModBlast.MAX_LINKS);
				case REMOVED -> Component.translatable("item.sanupcraft.remote_detonator.unlinked", pos.getX(), pos.getY(), pos.getZ());
				case FULL -> Component.translatable("item.sanupcraft.remote_detonator.full", ModBlast.MAX_LINKS);
			});
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel serverLevel) {
			int count = detonate(serverLevel, stack, player.position());
			if (count == 0) {
				player.sendOverlayMessage(Component.translatable("item.sanupcraft.remote_detonator.none", ModBlast.DETONATOR_RANGE));
				return InteractionResult.FAIL;
			}
			player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
			player.sendOverlayMessage(Component.translatable("item.sanupcraft.remote_detonator.fired", count));
		}
		return InteractionResult.SUCCESS;
	}

	/** 연결된 장약 자리들. */
	public static List<GlobalPos> links(ItemStack stack) {
		return stack.getOrDefault(ModBlast.DETONATOR_LINKS, List.of());
	}

	/** 이 장약을 연결하거나 (이미 연결돼 있으면) 연결을 푼다. */
	public static LinkResult toggleLink(ItemStack stack, GlobalPos target) {
		List<GlobalPos> list = new ArrayList<>(links(stack));
		if (list.remove(target)) {
			setLinks(stack, list);
			return LinkResult.REMOVED;
		}
		if (list.size() >= ModBlast.MAX_LINKS) {
			return LinkResult.FULL;
		}
		list.add(target);
		setLinks(stack, list);
		return LinkResult.ADDED;
	}

	private static void setLinks(ItemStack stack, List<GlobalPos> list) {
		if (list.isEmpty()) {
			stack.remove(ModBlast.DETONATOR_LINKS);
		} else {
			stack.set(ModBlast.DETONATOR_LINKS, List.copyOf(list));
		}
	}

	/**
	 * 연결된 장약 중 {@code from}에서 {@link ModBlast#DETONATOR_RANGE}칸 안, 같은 차원, 불러온 청크의 것에 불을 붙인다.
	 * 이제 장약이 아닌 자리는 연결에서 지운다. 불을 붙인 장약 수를 돌려준다.
	 */
	public static int detonate(ServerLevel level, ItemStack stack, Vec3 from) {
		List<GlobalPos> list = new ArrayList<>(links(stack));
		int lit = 0;
		boolean changed = false;
		for (GlobalPos link : new ArrayList<>(list)) {
			if (link.dimension() != level.dimension() || !level.isLoaded(link.pos())) {
				continue;
			}
			if (!level.getBlockState(link.pos()).is(ModBlast.BLAST_CHARGE)) {
				list.remove(link);
				changed = true;
				continue;
			}
			if (Vec3.atCenterOf(link.pos()).distanceToSqr(from) > (double) ModBlast.DETONATOR_RANGE * ModBlast.DETONATOR_RANGE) {
				continue;
			}
			if (BlastChargeBlock.ignite(level, link.pos(), SanupcraftConfig.current().blastFuseTicks)) {
				lit++;
			}
		}
		if (changed) {
			setLinks(stack, list);
		}
		return lit;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, builder, flag);
		int count = links(stack).size();
		builder.accept(Component.translatable("item.sanupcraft.remote_detonator.links", count, ModBlast.MAX_LINKS)
				.withStyle(count == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA));
	}
}
