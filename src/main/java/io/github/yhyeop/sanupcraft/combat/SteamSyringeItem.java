package io.github.yhyeop.sanupcraft.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteamChargedItem;

/**
 * 응급 증기 주사기 (C5). 쓰러진 동료를 우클릭하면 체력 4 + 재생으로 일으킨다. 한 번에 증기 200.
 * 증기는 주사기 자신(600, 증기 충전대)에서 먼저 쓰고, 모자란 몫만 인벤토리의 증기 통에서 쓴다 (합쳐도 모자라면 아무것도 안 쓴다). 창작 모드는 공짜.
 * 쓰러진 사람은 아이템을 못 쓰므로 스스로는 못 일어난다.
 */
public class SteamSyringeItem extends SteamChargedItem {
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int CAPACITY = 600;
	public static final int STEAM_PER_USE = 200;
	public static final int REVIVE_HEALTH = 4;

	/** 써 본 결과. */
	public enum Result {
		REVIVED, NOT_DOWNED, SELF, NO_STEAM
	}

	public SteamSyringeItem(Item.Properties properties) {
		super(properties, CAPACITY);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!(target instanceof Player downed) || !Downed.isDowned(downed)) {
			return InteractionResult.PASS;
		}
		if (!(player.level() instanceof ServerLevel level) || !(downed instanceof ServerPlayer serverTarget)) {
			return InteractionResult.SUCCESS;
		}
		Result result = inject(level, player, stack, serverTarget);
		if (result == Result.NO_STEAM) {
			player.sendOverlayMessage(Component.translatable("item.sanupcraft.steam_syringe.no_steam", SanupcraftConfig.current().syringeSteam));
		}
		return result == Result.REVIVED ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	/** 주사한다: 증기를 쓰고 일으킨다. 시험이 직접 부른다. */
	public static Result inject(ServerLevel level, Player user, ItemStack syringe, ServerPlayer target) {
		if (target == user) {
			return Result.SELF;
		}
		if (!Downed.isDowned(target)) {
			return Result.NOT_DOWNED;
		}
		int cost = SanupcraftConfig.current().syringeSteam;
		if (!user.hasInfiniteMaterials()) {
			int have = Math.min(SteamCharge.get(syringe), cost);
			int need = cost - have;
			// 통에서 먼저 빼 보고(모자라면 아무것도 안 빠진다) 성공했을 때만 주사기 몫을 뺀다
			if (need > 0 && !SteamCharge.drain(user, need)) {
				return Result.NO_STEAM;
			}
			SteamCharge.take(syringe, have);
		}
		Downed.revive(target, user);
		return Result.REVIVED;
	}
}
