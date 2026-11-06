package io.github.yhyeop.sanupcraft.nether;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteamChargedItem;

/**
 * 잔해 탐지기 (M5): 자기 몸에 증기 300을 담는다 (충전대에서 채움). 우클릭하면 증기 20(설정 {@code debrisSonar.steam})을 쓰고
 * 반경 16칸 정육면체 안에서 가장 가까운 고대 잔해의 방향(8방위) · 거리를 액션바에 띄우고, 그쪽으로 짧은 입자 줄을 그린다.
 * 창작 모드는 증기가 들지 않는다. 증기가 모자라면 안내만 하고 쿨다운도 걸지 않는다.
 */
public class DebrisSonarItem extends SteamChargedItem {
	public static final int CAPACITY = 300;
	/** 기본값. 실제로 쓰는 값은 설정 파일. */
	public static final int STEAM_COST = 20;
	public static final int RADIUS = 16;
	public static final int COOLDOWN_TICKS = 20;
	private static final String[] DIRECTIONS = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};

	/** 써 본 결과. */
	public enum Result {
		FOUND, NOTHING, NO_STEAM, COOLDOWN
	}

	public DebrisSonarItem(Item.Properties properties) {
		super(properties, CAPACITY);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack sonar = player.getItemInHand(hand);
		if (level instanceof ServerLevel serverLevel) {
			return ping(serverLevel, player, sonar) == Result.FOUND || player.getCooldowns().isOnCooldown(sonar)
					? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}
		return player.getCooldowns().isOnCooldown(sonar) ? InteractionResult.FAIL : InteractionResult.SUCCESS;
	}

	/** 한 번 탐지한다 (증기를 쓰고 결과를 알린다). 시험이 직접 부른다. */
	public static Result ping(ServerLevel level, Player player, ItemStack sonar) {
		if (player.getCooldowns().isOnCooldown(sonar)) {
			return Result.COOLDOWN;
		}
		int cost = SanupcraftConfig.current().debrisSonarSteam;
		if (!player.hasInfiniteMaterials()) {
			if (SteamCharge.get(sonar) < cost) {
				player.sendOverlayMessage(Component.translatable("item.sanupcraft.debris_sonar.no_steam", cost));
				return Result.NO_STEAM;
			}
			SteamCharge.take(sonar, cost);
		}
		player.getCooldowns().addCooldown(sonar, COOLDOWN_TICKS);
		BlockPos origin = player.blockPosition();
		BlockPos debris = findNearest(level, origin, RADIUS);
		if (debris == null) {
			player.sendOverlayMessage(Component.translatable("item.sanupcraft.debris_sonar.nothing", RADIUS));
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6F, 0.6F);
			return Result.NOTHING;
		}
		player.sendOverlayMessage(message(origin, debris));
		trail(level, player, debris);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7F, 1.4F);
		return Result.FOUND;
	}

	/** {@code center} 둘레 {@code radius}칸 정육면체에서 가장 가까운 고대 잔해. 없으면 null. */
	public static @Nullable BlockPos findNearest(Level level, BlockPos center, int radius) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
			if (level.getBlockState(pos).is(Blocks.ANCIENT_DEBRIS)) {
				double distance = pos.distSqr(center);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = pos.immutable();
				}
			}
		}
		return best;
	}

	/** 8방위 키 끝말 (n · ne · e …). 북 = -z, 동 = +x. */
	public static String direction(int dx, int dz) {
		double angle = Math.toDegrees(Math.atan2(dx, -dz));
		int index = (int) Math.floorMod(Math.round(angle / 45.0), 8L);
		return DIRECTIONS[index];
	}

	/** "고대 잔해: 북동쪽 12칸 · 아래로 5칸". */
	static Component message(BlockPos from, BlockPos to) {
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		int dz = to.getZ() - from.getZ();
		int distance = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
		Component way = dx == 0 && dz == 0
				? Component.translatable("item.sanupcraft.debris_sonar.dir.here")
				: Component.translatable("item.sanupcraft.debris_sonar.dir." + direction(dx, dz));
		Component height = dy > 0 ? Component.translatable("item.sanupcraft.debris_sonar.up", dy)
				: dy < 0 ? Component.translatable("item.sanupcraft.debris_sonar.down", -dy)
				: Component.translatable("item.sanupcraft.debris_sonar.level");
		return Component.translatable("item.sanupcraft.debris_sonar.found", way, distance, height);
	}

	/** 눈높이에서 잔해 쪽으로 최대 6칸 입자 줄 (쓴 사람에게만). */
	private static void trail(ServerLevel level, Player player, BlockPos target) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		Vec3 from = player.getEyePosition().subtract(0.0, 0.3, 0.0);
		Vec3 to = Vec3.atCenterOf(target);
		Vec3 step = to.subtract(from);
		double length = Math.min(6.0, step.length());
		if (length < 0.5) {
			return;
		}
		step = step.normalize();
		for (double d = 0.8; d <= length; d += 0.4) {
			Vec3 p = from.add(step.scale(d));
			level.sendParticles(serverPlayer, ParticleTypes.SOUL_FIRE_FLAME, true, false, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}
}
