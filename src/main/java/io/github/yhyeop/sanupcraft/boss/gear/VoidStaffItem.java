package io.github.yhyeop.sanupcraft.boss.gear;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 공허 지팡이 (공허 거상 전용 장비, 확장 3 C4): 우클릭하면 바라보는 쪽으로 공허 탄을 쏜다 ({@link VoidBolt}: 피해 6 + 공허 침식 I).
 * 쿨다운 {@link #COOLDOWN_TICKS}틱, 한 발에 내구도 1. 탄약은 없다 (창작 모드는 내구도도 안 든다).
 */
public class VoidStaffItem extends Item {
	public static final int COOLDOWN_TICKS = 30;
	public static final int DURABILITY = 384;

	public VoidStaffItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack staff = player.getItemInHand(hand);
		if (level instanceof ServerLevel serverLevel) {
			return fire(serverLevel, player, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}
		return player.getCooldowns().isOnCooldown(staff) ? InteractionResult.FAIL : InteractionResult.SUCCESS;
	}

	/** 한 발 쏜다 (쿨다운 중이면 false). 시험이 직접 부른다. */
	public static boolean fire(ServerLevel level, Player player, InteractionHand hand) {
		ItemStack staff = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(staff)) {
			return false;
		}
		VoidBolt bolt = new VoidBolt(level, player);
		bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, VoidBolt.SPEED, 0.2F);
		level.addFreshEntity(bolt);
		Vec3 tip = player.getEyePosition().add(player.getLookAngle().scale(0.9));
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, tip.x, tip.y - 0.1, tip.z, 8, 0.1, 0.1, 0.1, 0.02);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), io.github.yhyeop.sanupcraft.sound.ModSounds.VOID_STAFF_SHOOT, SoundSource.PLAYERS, 0.8F, 1.0F);
		player.getCooldowns().addCooldown(staff, COOLDOWN_TICKS);
		staff.hurtAndBreak(1, player, hand);
		return true;
	}
}
