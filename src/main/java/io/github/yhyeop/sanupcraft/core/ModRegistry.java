package io.github.yhyeop.sanupcraft.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/** 아이템 · 블록 등록 도우미. 등록한 순서대로 크리에이티브 탭에 들어간다. */
public final class ModRegistry {
	static final List<Item> TAB_ITEMS = new ArrayList<>();

	private ModRegistry() {
	}

	public static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Sanupcraft.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		TAB_ITEMS.add(item);
		return item;
	}

	public static Item item(String name, Item.Properties properties) {
		return item(name, Item::new, properties);
	}

	public static Item item(String name) {
		return item(name, new Item.Properties());
	}

	/** 블록과 그 블록 아이템을 함께 등록한다. */
	public static <B extends Block> B block(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Sanupcraft.id(name));
		B block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		item(name, p -> new BlockItem(block, p), new Item.Properties().useBlockDescriptionPrefix());
		return block;
	}
}
