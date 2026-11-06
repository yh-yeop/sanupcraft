package io.github.yhyeop.sanupcraft.mining.blast;

import java.util.List;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 확장 3 D3 증기 발파: 발파 장약 (5x5x5를 캐는 폭파) + 원격 기폭기 (장약을 연결해 한꺼번에 터뜨림). */
public final class ModBlast {
	/** 기폭기 하나에 연결할 수 있는 장약 수. */
	public static final int MAX_LINKS = 8;
	/** 기폭기가 닿는 거리 (같은 차원, 블록). */
	public static final int DETONATOR_RANGE = 64;
	/** 기본값. 실제로 쓰는 값은 설정 파일. */
	public static final int FUSE_TICKS = 20;
	public static final double ENTITY_DAMAGE = 4.0;
	/** 발전 과제 "발파" (처음 터뜨릴 때). */
	public static final String ADVANCEMENT = "blast";

	public static final BlastChargeBlock BLAST_CHARGE = ModRegistry.block("blast_charge", BlastChargeBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_RED)
			.strength(1.0F)
			.sound(SoundType.METAL));

	/** 기폭기에 연결된 장약 자리 (최대 {@link #MAX_LINKS}). */
	public static final DataComponentType<List<GlobalPos>> DETONATOR_LINKS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			Sanupcraft.id("detonator_links"),
			DataComponentType.<List<GlobalPos>>builder().persistent(GlobalPos.CODEC.listOf())
					.networkSynchronized(GlobalPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINKS))).build());

	public static final Item REMOTE_DETONATOR = ModRegistry.item("remote_detonator", RemoteDetonatorItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	private ModBlast() {
	}

	public static void init() {
	}

	/** 터진 자리 둘레 플레이어에게 발전 과제 "발파"를 준다. */
	static void awardNearby(ServerLevel level, Vec3 center) {
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceToSqr(center) <= 32.0 * 32.0) {
				award(player);
			}
		}
	}

	/** 발전 과제 "발파"를 준다. */
	public static void award(ServerPlayer player) {
		var advancement = player.level().getServer().getAdvancements().get(Sanupcraft.id(ADVANCEMENT));
		if (advancement != null) {
			player.getAdvancements().award(advancement, "done");
		}
	}
}
