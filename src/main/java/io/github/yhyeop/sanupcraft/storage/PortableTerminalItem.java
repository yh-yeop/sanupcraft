package io.github.yhyeop.sanupcraft.storage;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 휴대 단말. 창고 단말(블록)에 우클릭하면 그 단말에 연결되고, 들고 우클릭하면 그 단말 화면을 연다.
 * 기본판은 같은 차원 64칸 안(설정), 셜커 껍데기판은 어느 차원에서나. 단말이 있는 청크가 불러와져 있어야 한다
 * (멀리 있으면 엔더 닻으로 붙잡아 둔다). 단말의 잠금 · 증기 규칙은 그대로 따른다.
 */
public class PortableTerminalItem extends Item {
	public static final int RANGE = 64;

	/** 여는 조건을 따져 본 결과. */
	public enum Check {
		OK, NOT_LINKED, OTHER_DIMENSION, TOO_FAR, NOT_LOADED, MISSING
	}

	private final boolean ender;

	public PortableTerminalItem(boolean ender, Item.Properties properties) {
		super(properties);
		this.ender = ender;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!(level.getBlockEntity(pos) instanceof TerminalBlockEntity terminal)) {
			return InteractionResult.PASS;
		}
		Player player = context.getPlayer();
		if (!level.isClientSide() && player != null) {
			if (!terminal.lock().canUse(player)) {
				player.sendOverlayMessage(Component.translatable("block.sanupcraft.storage_terminal.locked", terminal.lock().ownerName()));
				return InteractionResult.FAIL;
			}
			context.getItemInHand().set(ModStorage.TERMINAL_LINK, GlobalPos.of(level.dimension(), pos.immutable()));
			player.sendOverlayMessage(Component.translatable("item.sanupcraft.portable_terminal.linked", pos.getX(), pos.getY(), pos.getZ()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack stack = player.getItemInHand(hand);
		GlobalPos link = stack.get(ModStorage.TERMINAL_LINK);
		Check check = check(serverLevel, player, link);
		if (check != Check.OK) {
			player.sendOverlayMessage(Component.translatable("item.sanupcraft.portable_terminal." + check.name().toLowerCase(Locale.ROOT), rangeText()));
			return InteractionResult.FAIL;
		}
		ServerLevel target = serverLevel.getServer().getLevel(link.dimension());
		TerminalBlockEntity terminal = (TerminalBlockEntity) target.getBlockEntity(link.pos());
		return terminal.tryOpen(serverPlayer, provider(terminal, link)) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	private static int rangeText() {
		return SanupcraftConfig.current().portableTerminalRange;
	}

	/** 연결된 단말을 지금 열 수 있는가 (거리 · 차원 · 불러온 청크 · 단말이 남아 있는지). */
	public Check check(ServerLevel level, Player player, @Nullable GlobalPos link) {
		if (link == null) {
			return Check.NOT_LINKED;
		}
		if (!inRange(ender, player.level().dimension(), player.position(), link, SanupcraftConfig.current().portableTerminalRange)) {
			return link.dimension() != player.level().dimension() ? Check.OTHER_DIMENSION : Check.TOO_FAR;
		}
		ServerLevel target = level.getServer().getLevel(link.dimension());
		if (target == null || !target.isLoaded(link.pos())) {
			return Check.NOT_LOADED;
		}
		return target.getBlockEntity(link.pos()) instanceof TerminalBlockEntity ? Check.OK : Check.MISSING;
	}

	/** 거리 규칙: 셜커판은 어디서나, 기본판은 같은 차원 range칸 안 (블록 가운데까지). */
	public static boolean inRange(boolean ender, ResourceKey<Level> playerDimension, Vec3 playerPos, GlobalPos link, int range) {
		if (ender) {
			return true;
		}
		return playerDimension == link.dimension() && playerPos.distanceToSqr(Vec3.atCenterOf(link.pos())) <= (double) range * range;
	}

	private ExtendedMenuProvider<TerminalMenu.OpenData> provider(TerminalBlockEntity terminal, GlobalPos link) {
		Predicate<Player> validator = p -> p instanceof ServerPlayer sp && check(sp.level(), sp, link) == Check.OK
				&& terminal.hasPower() && terminal.lock().canUse(p);
		return new ExtendedMenuProvider<>() {
			@Override
			public TerminalMenu.OpenData getScreenOpeningData(ServerPlayer player) {
				return new TerminalMenu.OpenData(terminal.isCrafting());
			}

			@Override
			public Component getDisplayName() {
				return terminal.getDisplayName();
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
				return new TerminalMenu(containerId, inventory, terminal, validator);
			}
		};
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, builder, flag);
		GlobalPos link = stack.get(ModStorage.TERMINAL_LINK);
		if (link == null) {
			builder.accept(Component.translatable("item.sanupcraft.portable_terminal.unlinked").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			builder.accept(Component.translatable("item.sanupcraft.portable_terminal.link", link.pos().getX(), link.pos().getY(), link.pos().getZ(),
					link.dimension().identifier().toString()).withStyle(ChatFormatting.AQUA));
		}
	}
}
