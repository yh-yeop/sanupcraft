package io.github.yhyeop.sanupcraft.combat;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.power.SteamConsumer;
import io.github.yhyeop.sanupcraft.power.SteamFlowMeter;
import io.github.yhyeop.sanupcraft.power.SteamStorage;
import io.github.yhyeop.sanupcraft.server.TeamData;
import io.github.yhyeop.sanupcraft.server.Teams;

/**
 * 전투 신호기 (C5). 증기 파이프로 증기를 받아 1000까지 모은다.
 * 40틱마다 반경 16 안의 플레이어에게 힘 I · 저항 I (3초)을 건다. 받는 사람: 놓은 사람의 팀(M10) 전원, 놓은 사람이 팀이 없으면(또는 주인이 없으면) 모두.
 * 한 번 걸 때 증기 20 (받는 사람이 없으면 쓰지 않는다).
 */
public class CombatBeaconBlockEntity extends BlockEntity implements SteamConsumer, SteamStorage {
	public static final int CAPACITY = 1000;
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int STEAM_PER_PULSE = 20;
	public static final int RADIUS = 16;
	public static final int PULSE_INTERVAL = 40;
	public static final int BUFF_TICKS = 60;

	private int steam;
	private int cooldown;
	private @Nullable UUID owner;
	private String ownerName = "";
	private final SteamFlowMeter flow = new SteamFlowMeter();

	public CombatBeaconBlockEntity(BlockPos pos, BlockState state) {
		super(ModCombat.COMBAT_BEACON_BLOCK_ENTITY, pos, state);
	}

	void serverTick(ServerLevel level, BlockPos pos) {
		if (--cooldown > 0) {
			return;
		}
		cooldown = PULSE_INTERVAL;
		int radius = SanupcraftConfig.current().combatBeaconRadius;
		pulse(level, pos, level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(radius)));
	}

	/** 한 번 건다. 버프를 받은 사람 수 (증기가 모자라거나 받을 사람이 없으면 0). 시험이 후보를 직접 넘긴다. */
	public int pulse(ServerLevel level, BlockPos pos, List<? extends Player> candidates) {
		SanupcraftConfig config = SanupcraftConfig.current();
		if (steam < config.combatBeaconSteamPerPulse) {
			return 0;
		}
		Vec3 center = Vec3.atCenterOf(pos);
		double radius = config.combatBeaconRadius;
		List<? extends Player> targets = candidates.stream()
				.filter(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(center) <= radius * radius && allows(level, p))
				.toList();
		if (targets.isEmpty()) {
			return 0;
		}
		steam -= config.combatBeaconSteamPerPulse;
		setChanged();
		for (Player player : targets) {
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, BUFF_TICKS, 0, true, true));
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, BUFF_TICKS, 0, true, true));
		}
		level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.8, center.z, 6, 0.2, 0.3, 0.2, 0.02);
		level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.4F);
		return targets.size();
	}

	/** 이 사람이 버프를 받나: 주인이 없거나, 주인이 팀이 없으면 모두. 아니면 주인의 팀원만 (주인 포함). */
	public boolean allows(ServerLevel level, Player player) {
		if (owner == null) {
			return true;
		}
		TeamData.Team team = Teams.teamOf(level.getServer(), owner);
		return team == null || team.has(player.getUUID());
	}

	public void setOwner(Player player) {
		owner = player.getUUID();
		ownerName = player.getGameProfile().name();
		setChanged();
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public String ownerName() {
		return ownerName;
	}

	Component status() {
		return Component.translatable("block.sanupcraft.combat_beacon.status", steam, CAPACITY, CombatBeaconBlock.ownerText(this),
				SanupcraftConfig.current().combatBeaconRadius);
	}

	// ---- 증기망 ----

	@Override
	public int acceptSteam(int amount) {
		int accepted = Math.max(0, Math.min(amount, CAPACITY - steam));
		steam += accepted;
		if (accepted > 0) {
			flow.add(gameTime(), accepted);
			setChanged();
		}
		return accepted;
	}

	@Override
	public int steamStored() {
		return steam;
	}

	@Override
	public int steamCapacity() {
		return CAPACITY;
	}

	@Override
	public int steamReceivedLastSecond() {
		return flow.lastSecond(gameTime());
	}

	private long gameTime() {
		return level != null ? level.getGameTime() : 0L;
	}

	// ---- 저장 ----

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		steam = Math.max(0, Math.min(CAPACITY, input.getIntOr("steam", 0)));
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		ownerName = input.getStringOr("owner_name", "");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("steam", steam);
		output.storeNullable("owner", UUIDUtil.CODEC, owner);
		output.putString("owner_name", ownerName);
	}
}
