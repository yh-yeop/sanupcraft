package io.github.yhyeop.sanupcraft.boss;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** 수호자 제단. 수호자 소환석을 들고 우클릭하면 제단 위에 녹슨 수호자가 나온다. */
public class GuardianAltarBlock extends Block {
	/** 이 거리 안에 수호자가 이미 있으면 소환하지 않는다. */
	public static final double ONE_PER_RADIUS = 48.0;

	public GuardianAltarBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.is(ModBoss.GUARDIAN_SUMMONER)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		if (summon(serverLevel, pos) == null) {
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.guardian_altar.busy"));
			return InteractionResult.FAIL;
		}
		stack.consume(1, player);
		return InteractionResult.SUCCESS_SERVER;
	}

	/** 제단 위에 수호자를 부른다. 근처에 이미 있으면 null. */
	public static @Nullable RustyGuardian summon(ServerLevel level, BlockPos altarPos) {
		AABB area = new AABB(altarPos).inflate(ONE_PER_RADIUS);
		if (!level.getEntitiesOfClass(RustyGuardian.class, area).isEmpty()) {
			return null;
		}
		RustyGuardian guardian = ModBoss.RUSTY_GUARDIAN.create(level, EntitySpawnReason.TRIGGERED);
		if (guardian == null) {
			return null;
		}
		guardian.snapTo(altarPos.getX() + 0.5, altarPos.getY() + 1.0, altarPos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(guardian);
		level.playSound(null, altarPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.2F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, guardian.getX(), guardian.getY() + 1.0, guardian.getZ(), 40, 0.8, 1.2, 0.8, 0.02);
		return guardian;
	}
}
