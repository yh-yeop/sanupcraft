package io.github.yhyeop.sanupcraft.module;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 모듈이 매 틱 하는 일 (G3). 아이템 자체의 속성 컴포넌트는 건드리지 않고(올리기 레시피가 컴포넌트를 통째로 옮겨도 공격력이 섞이지 않게),
 * 입은 갑옷 · 든 장비의 모듈을 보고 플레이어 속성에 우리 수정자를 붙였다 뗐다 한다:
 * 효율(캐는 속도 +30%) · 범위(캐는 속도 ×60%) = 든 도구, 밀치기 +1 = 든 무기, 증기 추진(점프 +0.1 · 안전 낙하 +1) = 입은 갑옷.
 * 자석(6칸 안 아이템 끌기)과 야간 투시도 여기서. 녹 · 과열 · 침식 · 가스는 {@code effect.Resistances}가 본다.
 */
public final class ModuleEffects {
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int MAGNET_RANGE = 6;
	public static final int EFFICIENCY_PERCENT = 30;
	public static final int AREA_SPEED_PERCENT = 60;
	public static final double KNOCKBACK = 1.0;
	/** 증기 추진: 점프 강화 I과 같은 값. */
	public static final double JUMP = 0.1;
	public static final double SAFE_FALL = 1.0;
	/** 야간 투시: 이만큼 걸고(깜빡임은 200틱 아래부터), 남은 시간이 REFRESH 아래면 다시 건다. */
	public static final int NIGHT_VISION_TICKS = 400;
	private static final int NIGHT_VISION_REFRESH = 260;
	/** 자석이 당기는 빠르기 (틱당 칸). */
	private static final double MAGNET_SPEED = 0.35;

	public static final Identifier EFFICIENCY_ID = Sanupcraft.id("module_efficiency");
	public static final Identifier AREA_ID = Sanupcraft.id("module_area");
	public static final Identifier KNOCKBACK_ID = Sanupcraft.id("module_knockback");
	public static final Identifier JUMP_ID = Sanupcraft.id("module_steam_jump");
	public static final Identifier SAFE_FALL_ID = Sanupcraft.id("module_steam_jump_fall");

	private ModuleEffects() {
	}

	static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tick = server.getTickCount();
			server.getPlayerList().getPlayers().forEach(player -> tick(player, tick));
		});
	}

	/** 한 틱 (시험이 직접 부른다). 속성은 매 틱, 자석은 2틱마다, 야간 투시는 1초마다. */
	public static void tick(Player player, int tick) {
		updateAttributes(player);
		if (tick % 2 == 0) {
			magnet(player);
		}
		if (tick % 20 == 0) {
			nightVision(player);
		}
	}

	/** 지금 장비에 맞게 우리 속성 수정자를 붙이거나 뗀다. 바뀔 때만 건드린다 (속성 패킷이 매 틱 나가지 않게). */
	public static void updateAttributes(LivingEntity entity) {
		SanupcraftConfig config = SanupcraftConfig.current();
		boolean efficiency = GearModules.heldHas(entity, GearModule.EFFICIENCY);
		boolean area = GearModules.heldHas(entity, GearModule.AREA);
		boolean knockback = GearModules.heldHas(entity, GearModule.KNOCKBACK);
		boolean jump = GearModules.wornHas(entity, GearModule.STEAM_JUMP);
		apply(entity, Attributes.BLOCK_BREAK_SPEED, EFFICIENCY_ID, efficiency, config.moduleEfficiencyPercent / 100.0,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		apply(entity, Attributes.BLOCK_BREAK_SPEED, AREA_ID, area, config.moduleAreaSpeedPercent / 100.0 - 1.0,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		apply(entity, Attributes.ATTACK_KNOCKBACK, KNOCKBACK_ID, knockback, config.moduleKnockback, AttributeModifier.Operation.ADD_VALUE);
		apply(entity, Attributes.JUMP_STRENGTH, JUMP_ID, jump, JUMP, AttributeModifier.Operation.ADD_VALUE);
		apply(entity, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL_ID, jump, SAFE_FALL, AttributeModifier.Operation.ADD_VALUE);
	}

	private static void apply(LivingEntity entity, Holder<Attribute> attribute, Identifier id, boolean on, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (!on || amount == 0.0) {
			if (instance.hasModifier(id)) {
				instance.removeModifier(id);
			}
			return;
		}
		AttributeModifier modifier = new AttributeModifier(id, amount, operation);
		if (!modifier.equals(instance.getModifier(id))) {
			instance.removeModifier(id);
			instance.addTransientModifier(modifier);
		}
	}

	/** 자석 모듈을 입었으면 둘레의 떨어진 아이템을 몸 쪽으로 당긴다 (막 버린 것 · 주울 수 없는 것은 그대로). 당긴 개수. */
	public static int magnet(Player player) {
		if (player.isSpectator() || !GearModules.wornHas(player, GearModule.MAGNET)) {
			return 0;
		}
		int range = SanupcraftConfig.current().moduleMagnetRange;
		Vec3 target = player.position().add(0.0, 0.5, 0.0);
		int pulled = 0;
		for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(range),
				e -> e.isAlive() && !e.hasPickUpDelay())) {
			Vec3 to = target.subtract(item.position());
			double distance = to.length();
			if (distance < 0.5 || distance > range + 1) {
				continue;
			}
			Vec3 pull = to.scale(Math.min(MAGNET_SPEED, distance) / distance);
			item.setDeltaMovement(pull.x, Math.max(item.getDeltaMovement().y, pull.y), pull.z);
			item.needsSync = true;
			pulled++;
		}
		return pulled;
	}

	/** 야간 투시 모듈: 입고 있으면 계속, 벗으면 우리가 건 야간 투시(아무 표시 없는 은은한 효과)를 거둔다. */
	public static void nightVision(LivingEntity entity) {
		MobEffectInstance have = entity.getEffect(MobEffects.NIGHT_VISION);
		if (GearModules.wornHas(entity, GearModule.NIGHT_VISION)) {
			if (have == null || have.getDuration() < NIGHT_VISION_REFRESH) {
				entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_TICKS, 0, true, false, true));
			}
		} else if (have != null && have.isAmbient() && !have.isVisible() && have.getDuration() <= NIGHT_VISION_TICKS) {
			entity.removeEffect(MobEffects.NIGHT_VISION);
		}
	}
}
