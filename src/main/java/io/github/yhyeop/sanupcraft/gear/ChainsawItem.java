package io.github.yhyeop.sanupcraft.gear;

import net.minecraft.world.item.Item;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.production.TreeFelling;

/**
 * 전기톱: 도끼 + 나무 통째 베기. 내구도 대신 증기 2000, 원목 한 개에 10 (잎은 그 1/4).
 * 원목을 캐면 이어진 나무의 나머지 원목 · 잎을 {@link ToolMining}이 함께 벤다 (벌목기와 같은 {@link TreeFelling} 규칙).
 * 웅크리고 캐면 그 원목 하나만. 증기가 떨어지면 맨손처럼 느리다.
 */
public class ChainsawItem extends SteamToolItem {
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int CAPACITY = 2000;
	public static final int STEAM_PER_BLOCK = 10;

	public ChainsawItem(Item.Properties properties) {
		super(properties, CAPACITY, state -> {
			int log = SanupcraftConfig.current().chainsawSteamPerBlock;
			return TreeFelling.isLog(state) ? log : state.is(net.minecraft.tags.BlockTags.LEAVES) ? TreeFelling.leafCost(log) : log;
		});
	}
}
