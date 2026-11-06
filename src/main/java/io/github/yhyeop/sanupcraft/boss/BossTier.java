package io.github.yhyeop.sanupcraft.boss;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.factory.ModMachines;

/**
 * 강화 소환석 (C3): 소환석에 붙는 보스 강화 단계 0 ~ {@link #MAX}. 단계는 아이템 데이터 컴포넌트 {@code sanupcraft:boss_tier}에 담고,
 * 작업대에서 소환석 + 재료로 한 단계씩 올린다 ({@link BossTierRecipe}, 레시피 종류 {@code sanupcraft:crafting_boss_tier}).
 * 제단이 강화 소환석으로 부르면 보스 체력 × (1 + 0.5 · 단계) (인원 배율 위에 곱), 피해 × (1 + 0.25 · 단계), 기여자 보상 = 핵 · 심장
 * {@code 1 + 단계}개 + 기계 업그레이드 하나. 값은 서버 설정 {@code bossTier} 구역. 상세는 {@link BossParty}.
 *
 * <p><b>새 보스를 더하는 법</b> (녹슨 수호자 · 용광로 파수꾼 · 공허 거상이 이렇게 붙어 있다) — 네 곳만 건드린다:
 * <ol>
 *   <li>소환석 아이템을 {@link BossSummonerItem}으로 등록한다 ({@code ModBoss}의 다른 소환석처럼). 이름 · 툴팁의 "+N"이 저절로 붙는다.</li>
 *   <li>{@code GuardianAltarBlock.bossFor}에 소환석 → 보스 종류 한 줄. (강화 단계는 제단이 {@link #of}로 읽어 소환에 넘긴다.)</li>
 *   <li>보스 엔티티는 {@link PartyBoss}를 구현한다: {@code refreshBossBar()}에서 {@link #barName}으로 보스 바 제목을 쓰고,
 *       패턴 피해를 {@code party().outgoingDamageScale()}로 곱하고 (근접은 공격력 속성에 이미 반영됨), 보상은 {@code party().giveKeyItem}으로 준다.
 *       체력 상한 보정은 {@code getDamageAfterMagicAbsorb}에서 {@code party().incomingDamageScale(this)}를 곱한다 ({@link BossParty} 주석).</li>
 *   <li>데이터에 레시피 파일 하나: {@code recipe/<보스>_summoner_upgrade.json}
 *       ({@code "type": "sanupcraft:crafting_boss_tier", "summoner": …, "material": 공허 핵, "count": 1}).</li>
 * </ol>
 */
public final class BossTier {
	/** 최대 강화 단계. */
	public static final int MAX = 3;
	/** 단계당 체력 증가 (기본값. 실제 값은 설정). */
	public static final double HEALTH_PER_TIER = 0.5;
	/** 단계당 피해 증가 (기본값. 실제 값은 설정). */
	public static final double DAMAGE_PER_TIER = 0.25;
	/** 마지막 단계를 불렀을 때 받는 발전 과제. */
	public static final String ADVANCEMENT = "stronger_foe";

	/** 소환석의 강화 단계 (없으면 0). 저장 · 동기화. */
	public static final DataComponentType<Integer> COMPONENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Sanupcraft.id("boss_tier"),
			DataComponentType.<Integer>builder().persistent(Codec.intRange(0, MAX)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	public static final RecipeSerializer<BossTierRecipe> RECIPE_SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
			Sanupcraft.id("crafting_boss_tier"), new RecipeSerializer<>(BossTierRecipe.MAP_CODEC, BossTierRecipe.STREAM_CODEC));

	private BossTier() {
	}

	/** 등록만 한다 (클래스를 읽히게 함). */
	public static void init() {
	}

	/** 소환석의 강화 단계 0 ~ {@link #MAX}. */
	public static int of(ItemStack stack) {
		return clamp(stack.getOrDefault(COMPONENT, 0));
	}

	/** 단계를 정한다 (0이면 컴포넌트를 지워 강화 전과 같이 쌓인다). */
	public static void set(ItemStack stack, int tier) {
		int clamped = clamp(tier);
		if (clamped == 0) {
			stack.remove(COMPONENT);
		} else {
			stack.set(COMPONENT, clamped);
		}
	}

	/** 단계 {@code tier}인 {@code item} 하나. */
	public static ItemStack stack(Item item, int tier) {
		ItemStack stack = new ItemStack(item);
		set(stack, tier);
		return stack;
	}

	/** 체력 배율 (인원 배율과는 따로 곱한다). */
	public static double healthScale(int tier) {
		return 1.0 + SanupcraftConfig.current().bossTierHealthPerTier * clamp(tier);
	}

	/** 피해 배율. */
	public static double damageScale(int tier) {
		return 1.0 + SanupcraftConfig.current().bossTierDamagePerTier * clamp(tier);
	}

	/** 보스 바 · 소환석 이름에 "+N"을 붙인다 (0이면 그대로). */
	public static Component barName(Component name, int tier) {
		return tier > 0 ? Component.translatable("item.sanupcraft.boss_tier.name", name, tier) : name;
	}

	/** 보상으로 줄 기계 업그레이드(속도 · 효율 · 두 배) 하나. */
	public static ItemStack randomUpgrade(RandomSource random) {
		return switch (random.nextInt(3)) {
			case 0 -> new ItemStack(ModMachines.SPEED_UPGRADE);
			case 1 -> new ItemStack(ModMachines.EFFICIENCY_UPGRADE);
			default -> new ItemStack(ModMachines.DOUBLE_UPGRADE);
		};
	}

	/** 마지막 단계를 부른 사람에게 발전 과제를 준다. */
	public static void awardMaxSummon(ServerPlayer player) {
		var advancement = player.level().getServer().getAdvancements().get(Sanupcraft.id(ADVANCEMENT));
		if (advancement != null) {
			player.getAdvancements().award(advancement, "done");
		}
	}

	private static int clamp(int tier) {
		return Math.max(0, Math.min(MAX, tier));
	}
}
