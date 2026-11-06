package io.github.yhyeop.sanupcraft.combat;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteamChargedItem;

/**
 * 화염 방사기 (C1). 자기 몸에 증기 2000, 연료는 인벤토리의 블레이즈 가루(불꽃 40틱) · 마그마 크림(20틱).
 * 우클릭을 누르고 있는 동안 2틱마다 앞 6칸 원뿔(반각 30도)에 불을 뿜는다: 맞은 생물은 4초 불 · 피해 2.
 * 한 번 뿜을 때 증기 4, 연료 2틱. 넣어 둔 연료가 다 타면 다음 가루를 하나 넣는다. 창작 모드는 증기 · 연료가 들지 않는다.
 * 블록에 불을 붙이는 것은 설정({@code flamethrower.lightsBlocks}, 기본 끔).
 */
public class FlamethrowerItem extends SteamChargedItem {
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int CAPACITY = 2000;
	public static final int STEAM_PER_PULSE = 4;
	public static final float DAMAGE = 2.0F;
	public static final int RANGE = 6;
	public static final int BURN_TICKS = 80;
	/** 뿜는 간격(틱). 연료도 한 번에 이만큼 탄다. */
	public static final int PULSE_INTERVAL = 2;
	public static final int BLAZE_POWDER_TICKS = 40;
	public static final int MAGMA_CREAM_TICKS = 20;
	/** 원뿔 반각 30도. */
	private static final double CONE_COS = Math.cos(Math.toRadians(30.0));

	/** 넣어 둔 연료가 남은 틱. 저장 · 동기화. */
	public static final DataComponentType<Integer> FUEL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Sanupcraft.id("flame_fuel"),
			DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 10_000)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** 뿜어 본 결과. */
	public enum Result {
		FIRED, NO_STEAM, NO_FUEL
	}

	public FlamethrowerItem(Item.Properties properties) {
		super(properties, CAPACITY);
	}

	/** 컴포넌트 등록(클래스 로딩)을 보장한다. */
	static void init() {
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Result ready = ready(player, stack);
		if (ready != Result.FIRED) {
			if (!level.isClientSide()) {
				tell(player, ready);
			}
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.NONE;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
		if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
			return;
		}
		if ((getUseDuration(stack, entity) - ticksRemaining) % PULSE_INTERVAL != 0) {
			return;
		}
		Result result = emit(serverLevel, player, stack);
		if (result != Result.FIRED) {
			tell(player, result);
			player.stopUsingItem();
		}
	}

	/** 지금 한 번 뿜을 수 있나 (증기 · 연료가 있나). 아무것도 쓰지 않는다. */
	public static Result ready(Player player, ItemStack stack) {
		if (player.hasInfiniteMaterials()) {
			return Result.FIRED;
		}
		if (SteamCharge.get(stack) < SanupcraftConfig.current().flamethrowerSteamPerPulse) {
			return Result.NO_STEAM;
		}
		if (fuel(stack) <= 0 && findFuel(player).isEmpty()) {
			return Result.NO_FUEL;
		}
		return Result.FIRED;
	}

	/** 한 번 뿜는다: 증기 · 연료를 쓰고, 원뿔 안의 생물에 불을 붙인다. 시험이 직접 부른다. */
	public static Result emit(ServerLevel level, Player player, ItemStack stack) {
		SanupcraftConfig config = SanupcraftConfig.current();
		if (!player.hasInfiniteMaterials()) {
			Result ready = ready(player, stack);
			if (ready != Result.FIRED) {
				return ready;
			}
			int fuel = fuel(stack);
			if (fuel <= 0) {
				ItemStack item = findFuel(player);
				fuel = fuelTicks(item);
				item.shrink(1);
			}
			setFuel(stack, fuel - PULSE_INTERVAL);
			SteamCharge.take(stack, config.flamethrowerSteamPerPulse);
		}
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		int range = config.flamethrowerRange;
		if (player.isUnderWater()) {
			level.sendParticles(ParticleTypes.BUBBLE, eye.x + look.x, eye.y + look.y - 0.2, eye.z + look.z, 6, 0.2, 0.2, 0.2, 0.05);
			return Result.FIRED;
		}
		for (int i = 1; i <= range; i++) {
			Vec3 at = eye.add(look.scale(i)).add(0.0, -0.2, 0.0);
			double spread = 0.12 * i;
			level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 2, spread, spread, spread, 0.01);
		}
		for (LivingEntity target : targets(level, player, eye, look, range)) {
			burn(level, player, target, config.flamethrowerDamage);
		}
		if (config.flamethrowerLightsBlocks != 0 && player.tickCount % 10 == 0) {
			lightBlock(level, player, eye, look, range);
		}
		if (player.tickCount % 8 == 0) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.4F, 0.6F);
		}
		return Result.FIRED;
	}

	/** 원뿔(눈에서 {@code range}칸, 반각 30도) 안에서 보이는 생물. 쏜 사람은 뺀다. */
	public static List<LivingEntity> targets(ServerLevel level, Player player, Vec3 eye, Vec3 look, double range) {
		AABB box = new AABB(eye, eye).inflate(range);
		return level.getEntitiesOfClass(LivingEntity.class, box, e -> {
			if (e == player || !e.isAlive() || e.isSpectator()) {
				return false;
			}
			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
			double distance = to.length();
			if (distance > range + e.getBbWidth() * 0.5) {
				return false;
			}
			return (distance < 0.5 || to.scale(1.0 / distance).dot(look) >= CONE_COS) && player.hasLineOfSight(e);
		});
	}

	/**
	 * 맞은 생물 하나에 불 · 피해. 플레이어는 피해가 들어갈 때만 불도 붙는다 (같은 팀 · PvP 끔).
	 * 과열 효과(C3)도 함께 건다.
	 */
	static void burn(ServerLevel level, Player user, LivingEntity target, double damage) {
		boolean hurt = target.hurtServer(level, user.damageSources().source(DamageTypes.IN_FIRE, user), (float) damage);
		if (target.fireImmune() || (!hurt && target instanceof Player)) {
			return;
		}
		if (target.getRemainingFireTicks() < BURN_TICKS) {
			target.igniteForTicks(BURN_TICKS);
		}
		// 과열 I (C3) — 내열 한 벌이면 효과 쪽(Resistances)에서 막는다
		io.github.yhyeop.sanupcraft.effect.ModEffects.inflict(target, io.github.yhyeop.sanupcraft.effect.ModEffects.OVERHEAT, BURN_TICKS, 1, user);
	}

	/** 바라보는 블록 면 앞이 비었으면 불을 놓는다 (설정이 켜졌을 때만). */
	private static void lightBlock(ServerLevel level, Player player, Vec3 eye, Vec3 look, int range) {
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
		if (level.getBlockState(pos).isAir() && BaseFireBlock.canBePlacedAt(level, pos, hit.getDirection())) {
			level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
		}
	}

	// ---- 연료 ----

	public static int fuel(ItemStack stack) {
		return stack.getOrDefault(FUEL, 0);
	}

	public static void setFuel(ItemStack stack, int ticks) {
		if (ticks <= 0) {
			stack.remove(FUEL);
		} else {
			stack.set(FUEL, ticks);
		}
	}

	/** 연료 아이템 하나가 태우는 틱 (연료가 아니면 0). */
	public static int fuelTicks(ItemStack stack) {
		if (stack.is(Items.BLAZE_POWDER)) {
			return BLAZE_POWDER_TICKS;
		}
		return stack.is(Items.MAGMA_CREAM) ? MAGMA_CREAM_TICKS : 0;
	}

	/** 인벤토리의 첫 연료 (블레이즈 가루 · 마그마 크림). 없으면 빈 것. */
	public static ItemStack findFuel(Player player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (fuelTicks(stack) > 0) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static void tell(Player player, Result result) {
		if (result == Result.NO_STEAM) {
			player.sendOverlayMessage(Component.translatable("item.sanupcraft.flamethrower.no_steam"));
		} else if (result == Result.NO_FUEL) {
			player.sendOverlayMessage(Component.translatable("item.sanupcraft.flamethrower.no_fuel"));
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, builder, flag);
		builder.accept(Component.translatable("item.sanupcraft.flamethrower.fuel", fuel(stack)).withStyle(ChatFormatting.GOLD));
	}
}
