package io.github.yhyeop.sanupcraft.gear.special;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.SteamToolItem;

/**
 * G6 멀티툴: 곡괭이 · 삽 · 도끼 · 괭이를 한 자루에 (다이아 등급, 속도 9). 내구도 대신 증기 4000, 블록당 10.
 * 캐는 쪽은 바닐라 {@code Tool} 컴포넌트에 네 가지 "캘 수 있음" 태그 규칙을 다 넣어서, 블록마다 맞는 도구처럼 캔다.
 * 우클릭은 원목 · 구리 = 도끼(껍질 벗기기 · 녹 벗기기), 흙 · 잔디 = 괭이(밭 갈기), 웅크리고 우클릭 = 삽(흙길).
 * 증기가 떨어지면 맨손처럼 느리다 ({@link SteamToolItem}). 범위 모듈은 못 끼운다 (증기 공구라서).
 */
public class MultitoolItem extends SteamToolItem {
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int CAPACITY = 4000;
	public static final int STEAM_PER_BLOCK = 10;
	/** 캐는 속도 (드릴 8, 네더라이트 9). */
	public static final float SPEED = 9.0F;

	public MultitoolItem(Item.Properties properties) {
		super(properties, CAPACITY, state -> SanupcraftConfig.current().multitoolSteamPerBlock);
	}

	/** 도구 속성: 다이아 등급, 네 가지 태그를 다 캔다. 공격력 7 · 공격 속도 0.9 (도끼 쪽). */
	static Item.Properties properties() {
		HolderGetter<Block> lookup = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
		List<Tool.Rule> rules = new ArrayList<>();
		rules.add(Tool.Rule.deniesDrops(lookup.getOrThrow(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)));
		for (TagKey<Block> tag : List.of(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_SHOVEL, BlockTags.MINEABLE_WITH_AXE,
				BlockTags.MINEABLE_WITH_HOE)) {
			rules.add(Tool.Rule.minesAndDrops(lookup.getOrThrow(tag), SPEED));
		}
		return new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
				.component(DataComponents.TOOL, new Tool(rules, 1.0F, 0, true))
				.attributes(ItemAttributeModifiers.builder()
						.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE),
								EquipmentSlotGroup.MAINHAND)
						.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.1, AttributeModifier.Operation.ADD_VALUE),
								EquipmentSlotGroup.MAINHAND)
						.build());
	}

	/** 우클릭: 웅크리면 삽(흙길), 아니면 도끼(원목 · 구리) → 괭이(밭) 순서로 맞는 것 하나. 증기는 안 쓴다. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Registry<BlockTransformer> transformers = context.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER);
		List<ResourceKey<BlockTransformer>> order = context.isSecondaryUseActive()
				? List.of(BlockTransformers.SHOVEL)
				: List.of(BlockTransformers.AXE, BlockTransformers.HOE);
		for (ResourceKey<BlockTransformer> key : order) {
			var holder = transformers.get(key);
			if (holder.isEmpty()) {
				continue;
			}
			InteractionResult result = holder.get().value().transformBlock(context);
			if (result.consumesAction()) {
				return result;
			}
		}
		return InteractionResult.PASS;
	}
}
