package io.github.yhyeop.sanupcraft.logistics;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 유령 칸 필터: 벨트 분류기(9칸) · 필터 깔때기(5칸)가 쓴다. 칸에는 아이템이 1개짜리 견본으로만 들어 있고,
 * 아이템 종류가 같으면 맞는 것으로 친다 (개수 · 데이터는 보지 않는다). 칸에 {@link ModLogistics#ORE_MARKER 원석 표지}가 있으면
 * 원석 · 광석 태그({@code c:raw_materials} · {@code c:ores})에 든 아이템이 전부 맞는다.
 */
public class ItemFilter extends SimpleContainer {
	/** 원석(원석 철 · 구리 · 금). Fabric 공용 태그. */
	public static final TagKey<Item> RAW_MATERIALS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "raw_materials"));
	/** 광석 블록(철 광석 · 심층 철 광석 …). Fabric 공용 태그. */
	public static final TagKey<Item> ORES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores"));

	private Runnable onChange = () -> {
	};

	public ItemFilter(int size) {
		super(size);
	}

	/** 칸이 바뀔 때 부를 일 (주인 블록 엔티티의 setChanged). */
	public void onChange(Runnable onChange) {
		this.onChange = onChange;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		onChange.run();
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	/** 필터 칸이 하나라도 차 있는가. 비었으면 필터가 없는 것이다. */
	public boolean hasEntries() {
		return !isEmpty();
	}

	/** {@code stack}이 필터에 맞는가. 빈 필터는 아무것도 맞지 않는다 (쓰는 쪽이 빈 필터를 따로 다룬다). */
	public boolean matches(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		for (ItemStack entry : items) {
			if (entryMatches(entry, stack)) {
				return true;
			}
		}
		return false;
	}

	public static boolean entryMatches(ItemStack entry, ItemStack stack) {
		if (entry.isEmpty()) {
			return false;
		}
		if (entry.is(ModLogistics.ORE_MARKER)) {
			return stack.is(RAW_MATERIALS) || stack.is(ORES);
		}
		return ItemStack.isSameItem(entry, stack);
	}

	/** 같은 견본이 이미 있는가 (Shift 클릭으로 같은 걸 또 넣지 않으려고). */
	public boolean contains(ItemStack stack) {
		for (ItemStack entry : items) {
			if (!entry.isEmpty() && ItemStack.isSameItem(entry, stack)) {
				return true;
			}
		}
		return false;
	}

	public void save(ValueOutput output) {
		ContainerHelper.saveAllItems(output.child("filter"), items);
	}

	public void load(ValueInput input) {
		for (int i = 0; i < items.size(); i++) {
			items.set(i, ItemStack.EMPTY);
		}
		ContainerHelper.loadAllItems(input.childOrEmpty("filter"), items);
	}
}
