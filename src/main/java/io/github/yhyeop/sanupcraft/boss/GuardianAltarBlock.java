package io.github.yhyeop.sanupcraft.boss;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 수호자 제단. 소환석을 들고 우클릭하면 제단 위에 보스가 나온다.
 * 수호자 소환석 → 녹슨 수호자, 파수꾼 소환석 → 용광로 파수꾼. 근처에 보스가 있거나, 위 공간이 막혔거나,
 * 평화로움이면 부르지 않고 소환석도 쓰지 않는다.
 * 용광로 파수꾼은 네더의 열기에서만 깨어난다 (제단을 네더에 지어야 함).
 * 부를 때 주변 인원수로 보스 체력을 늘린다 ({@link BossParty}).
 */
public class GuardianAltarBlock extends Block {
	/** 이 거리 안에 보스가 이미 있으면 소환하지 않는다. */
	public static final double ONE_PER_RADIUS = 48.0;

	public GuardianAltarBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		EntityType<? extends Monster> boss = bossFor(stack);
		if (boss == null) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		if (!canWakeHere(serverLevel, boss)) {
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.guardian_altar.too_cold"));
			return InteractionResult.FAIL;
		}
		if (serverLevel.getDifficulty() == Difficulty.PEACEFUL) {
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.guardian_altar.peaceful"));
			return InteractionResult.FAIL;
		}
		if (hasBossNearby(serverLevel, pos)) {
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.guardian_altar.busy"));
			return InteractionResult.FAIL;
		}
		if (!hasRoom(serverLevel, pos, boss)) {
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.guardian_altar.no_room"));
			return InteractionResult.FAIL;
		}
		Monster summoned = summon(serverLevel, pos, boss);
		if (summoned == null) {
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.guardian_altar.busy"));
			return InteractionResult.FAIL;
		}
		if (summoned instanceof PartyBoss partyBoss && partyBoss.party().size() > 1) {
			int size = partyBoss.party().size();
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.guardian_altar.party",
					size, String.format(Locale.ROOT, "%.1f", BossParty.healthScale(size))));
		}
		stack.consume(1, player);
		return InteractionResult.SUCCESS_SERVER;
	}

	/** 제단 위에 보스가 서 있을 공간이 있는가. 블록만 본다 (엔티티는 무시). */
	public static boolean hasRoom(ServerLevel level, BlockPos altarPos, EntityType<?> type) {
		return level.noBlockCollision(null, type.getSpawnAABB(altarPos.getX() + 0.5, altarPos.getY() + 1.0, altarPos.getZ() + 0.5));
	}

	/** 이 거리 안에 보스가 이미 있는가. */
	public static boolean hasBossNearby(ServerLevel level, BlockPos altarPos) {
		AABB area = new AABB(altarPos).inflate(ONE_PER_RADIUS);
		return !level.getEntitiesOfClass(Monster.class, area, GuardianAltarBlock::isBoss).isEmpty();
	}

	private static @Nullable EntityType<? extends Monster> bossFor(ItemStack stack) {
		if (stack.is(ModBoss.GUARDIAN_SUMMONER)) {
			return ModBoss.RUSTY_GUARDIAN;
		}
		if (stack.is(ModBoss.WARDEN_SUMMONER)) {
			return ModBoss.FURNACE_WARDEN;
		}
		return null;
	}

	/** 제단 위에 녹슨 수호자를 부른다. 근처에 보스가 있으면 null. */
	public static @Nullable RustyGuardian summon(ServerLevel level, BlockPos altarPos) {
		return summon(level, altarPos, ModBoss.RUSTY_GUARDIAN);
	}

	/** 이 차원에서 깨어나는 보스인가. 파수꾼은 네더에서만. */
	public static boolean canWakeHere(ServerLevel level, EntityType<?> type) {
		return type != ModBoss.FURNACE_WARDEN || level.dimension() == Level.NETHER;
	}

	/** 제단 위에 보스를 부른다. 깨어날 수 없는 차원이거나 근처에 보스가 있으면 null. */
	public static <T extends Monster> @Nullable T summon(ServerLevel level, BlockPos altarPos, EntityType<T> type) {
		if (!canWakeHere(level, type)) {
			return null;
		}
		if (hasBossNearby(level, altarPos)) {
			return null;
		}
		T boss = type.create(level, EntitySpawnReason.TRIGGERED);
		if (boss == null) {
			return null;
		}
		boss.snapTo(altarPos.getX() + 0.5, altarPos.getY() + 1.0, altarPos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
		if (boss instanceof PartyBoss partyBoss) {
			partyBoss.party().scale(boss, BossParty.countPlayers(level, boss.position()));
		}
		level.addFreshEntity(boss);
		level.playSound(null, altarPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.2F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY() + 1.0, boss.getZ(), 40, 0.8, 1.2, 0.8, 0.02);
		return boss;
	}

	private static boolean isBoss(Monster monster) {
		return monster instanceof RustyGuardian || monster instanceof FurnaceWarden;
	}
}
