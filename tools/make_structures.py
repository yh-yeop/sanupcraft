"""월드 구조물(.nbt)을 코드로 만든다. 배치를 여기서 고치고 다시 돌린다.

    python tools/make_structures.py

녹슨 사당 (rusted_shrine): 오버월드 평지 · 숲에 생기는 무너진 석재 사당. 가운데 수호자 제단, 상자에 소환석과 재료.
바닥(y=0)은 지면 높이에 묻히도록 놓인다 (structure 의 start_height -1).
버려진 용광로 (abandoned_furnace, M5): 네더 바닥의 무너진 공방. 수호자 제단 · 부서진 심장 제련로 · 상자.
버려진 정거장 (abandoned_station, M6): 엔드 바깥 섬 위에 뜬 무너진 승강장. 수호자 제단 · 상자 · 끊긴 선로.
"""
import gzip
import io
import random
import struct
from pathlib import Path

DATA_VERSION = 5023  # Minecraft 26.3 (SharedConstants.WORLD_VERSION)
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/sanupcraft/structure"


# ---- 아주 작은 NBT 쓰기 ----

def _name(buf, s):
    b = s.encode("utf-8")
    buf.write(struct.pack(">H", len(b)))
    buf.write(b)


def _tag_id(v):
    if isinstance(v, dict):
        return 10
    if isinstance(v, list):
        return 9
    if isinstance(v, str):
        return 8
    if isinstance(v, int):
        return 3
    raise TypeError(v)


def _payload(buf, v):
    t = _tag_id(v)
    if t == 3:
        buf.write(struct.pack(">i", v))
    elif t == 8:
        _name(buf, v)
    elif t == 9:
        inner = _tag_id(v[0]) if v else 0
        buf.write(struct.pack(">bi", inner, len(v)))
        for x in v:
            _payload(buf, x)
    elif t == 10:
        for k, x in v.items():
            buf.write(struct.pack(">b", _tag_id(x)))
            _name(buf, k)
            _payload(buf, x)
        buf.write(b"\x00")


def write_nbt(path: Path, root: dict) -> None:
    buf = io.BytesIO()
    buf.write(b"\x0a")
    _name(buf, "")
    _payload(buf, root)
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.GzipFile(path, "wb", mtime=0) as f:
        f.write(buf.getvalue())


# ---- 구조물 ----

class Template:
    def __init__(self, size):
        self.size = size
        self.blocks = {}  # (x, y, z) -> (name, props, nbt)

    def set(self, x, y, z, name, props=None, nbt=None):
        self.blocks[(x, y, z)] = ("minecraft:" + name if ":" not in name else name, props or {}, nbt)

    def save(self, path: Path):
        palette, index, blocks = [], {}, []
        for pos in sorted(self.blocks, key=lambda p: (p[1], p[2], p[0])):
            name, props, nbt = self.blocks[pos]
            key = (name, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                entry = {"id": name}  # 26.x: 블록 상태 NBT 키가 id · properties (예전 Name · Properties)
                if props:
                    entry["properties"] = dict(props)
                palette.append(entry)
            block = {"pos": list(pos), "state": index[key]}
            if nbt:
                block["nbt"] = nbt
            blocks.append(block)
        write_nbt(path, {
            "DataVersion": DATA_VERSION,
            "size": list(self.size),
            "palette": palette,
            "blocks": blocks,
            "entities": [],
        })
        return len(blocks)


def rusted_shrine() -> Template:
    rnd = random.Random(20261106)
    t = Template((9, 6, 9))
    # 안쪽 공간은 비운다 (풀 · 꽃 · 나뭇잎 걷어 내기)
    for x in range(9):
        for z in range(9):
            for y in range(1, 6):
                t.set(x, y, z, "air")
    # 바닥: 금 간 · 이끼 낀 석재 벽돌, 가운데 3x3은 매끈한 안산암
    for x in range(9):
        for z in range(9):
            if 3 <= x <= 5 and 3 <= z <= 5:
                t.set(x, 0, z, "polished_andesite")
                continue
            r = rnd.random()
            name = "stone_bricks" if r < 0.45 else "cracked_stone_bricks" if r < 0.7 else "mossy_stone_bricks" if r < 0.9 else "gravel"
            t.set(x, 0, z, name)
    # 무너진 네 기둥 (높이가 제각각), 꼭대기에 반 블록
    for (x, z), h in {(0, 0): 4, (8, 0): 2, (0, 8): 3, (8, 8): 5}.items():
        for y in range(1, h + 1):
            t.set(x, y, z, "stone_bricks" if y % 2 else "cracked_stone_bricks")
        if h < 5:
            t.set(x, h + 1, z, "stone_brick_slab", {"type": "bottom", "waterlogged": "false"})
    # 가장자리 낮은 담: 군데군데 빠짐
    for i in range(1, 8):
        for (x, z) in ((i, 0), (i, 8), (0, i), (8, i)):
            if rnd.random() < 0.55:
                t.set(x, 1, z, "stone_brick_wall")
    # 녹슨 쇠창살 · 사슬 조각
    for (x, z) in ((3, 0), (5, 8), (0, 5)):
        t.set(x, 1, z, "iron_bars")
    t.set(8, 4, 8, "iron_chain", {"axis": "y", "waterlogged": "false"})
    # 가운데 수호자 제단, 뒤쪽 상자, 옆에 금 간 모루
    t.set(4, 1, 4, "sanupcraft:guardian_altar")
    t.set(4, 1, 7, "chest", {"facing": "north", "type": "single", "waterlogged": "false"},
          {"id": "minecraft:chest", "LootTable": "sanupcraft:chests/rusted_shrine"})
    t.set(2, 1, 6, "damaged_anvil", {"facing": "east"})
    t.set(6, 1, 2, "cobweb")
    t.set(1, 1, 2, "cobweb")
    return t


def abandoned_furnace() -> Template:
    """버려진 용광로 (M5, 네더): 무너진 흑암 벽돌 공방. 가운데 수호자 제단, 안쪽에 부서진(꺼진) 심장 제련로와 굴뚝, 상자.

    바닥(y=0)은 네더 바닥 블록 자리에 놓인다 (구조물 종류 sanupcraft:nether_floor). 크기 11 x 8 x 11.
    """
    rnd = random.Random(20261107)
    t = Template((11, 8, 11))
    for x in range(11):
        for z in range(11):
            for y in range(1, 8):
                t.set(x, y, z, "air")
    # 바닥: 네더 벽돌 · 금 간 네더 벽돌 · 흑암 · 마그마, 가운데 3x3은 매끈한 현무암
    for x in range(11):
        for z in range(11):
            if 4 <= x <= 6 and 4 <= z <= 6:
                t.set(x, 0, z, "polished_basalt", {"axis": "y"})
                continue
            r = rnd.random()
            name = ("nether_bricks" if r < 0.4 else "cracked_nether_bricks" if r < 0.65
                    else "polished_blackstone_bricks" if r < 0.85 else "cracked_polished_blackstone_bricks" if r < 0.95 else "magma_block")
            t.set(x, 0, z, name)
    # 무너진 벽: 둘레, 높이가 제각각 (앞쪽 z=0은 입구로 낮게)
    for i in range(11):
        for (x, z) in ((i, 0), (i, 10), (0, i), (10, i)):
            front = z == 0 and 3 <= x <= 7
            h = 0 if front else rnd.choice((1, 1, 2, 2, 3, 4))
            for y in range(1, h + 1):
                t.set(x, y, z, "polished_blackstone_bricks" if rnd.random() < 0.6 else "cracked_polished_blackstone_bricks")
            if h and rnd.random() < 0.35:
                t.set(x, h + 1, z, "polished_blackstone_brick_slab", {"type": "bottom", "waterlogged": "false"})
    # 뒤쪽 왼편 굴뚝 (벽돌, 위에 꺼지지 않는 영혼 모닥불 연기)
    for y in range(1, 7):
        for (x, z) in ((1, 8), (2, 8), (1, 9), (2, 9)):
            if y < 6 or (x, z) == (1, 9):
                t.set(x, y, z, "bricks" if (x + y + z) % 3 else "nether_bricks")
    t.set(2, 6, 8, "soul_campfire", {"facing": "south", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    # 부서진 심장 제련로: 굴뚝 앞, 꺼진 채 둘레에 금 간 벽돌 · 거미줄
    t.set(2, 1, 7, "sanupcraft:heart_forge", {"facing": "south", "lit": "false"})
    t.set(3, 1, 7, "blast_furnace", {"facing": "south", "lit": "false"})
    t.set(3, 2, 7, "cracked_nether_bricks")
    t.set(1, 1, 7, "magma_block")
    t.set(2, 2, 7, "cobweb")
    # 가운데 수호자 제단, 뒤쪽 오른편 상자
    t.set(5, 1, 5, "sanupcraft:guardian_altar")
    t.set(8, 1, 9, "chest", {"facing": "north", "type": "single", "waterlogged": "false"},
          {"id": "minecraft:chest", "LootTable": "sanupcraft:chests/abandoned_furnace"})
    # 녹슨 쇠창살 · 사슬 · 금 블록 부스러기
    for (x, z) in ((10, 4), (0, 6), (6, 10)):
        t.set(x, 2, z, "iron_bars")
    t.set(9, 3, 9, "iron_chain", {"axis": "y", "waterlogged": "false"})
    t.set(9, 2, 9, "iron_chain", {"axis": "y", "waterlogged": "false"})
    t.set(7, 1, 8, "gilded_blackstone")
    t.set(8, 1, 3, "cobweb")
    return t


def abandoned_station() -> Template:
    """버려진 정거장 (M6, 엔드 바깥 섬): 다리 넷 위에 뜬 무너진 퍼퍼 승강장. 가운데 수호자 제단, 상자, 끊긴 선로 · 고압 파이프 · 돛대.

    구조물 종류 sanupcraft:end_island 가 틀의 y=0을 섬 지면 바로 위에 놓는다 (hover 1). 다리(y 0~4)가 땅에 닿고 승강장은 y=5.
    다리 사이는 비우지 않는다 (섬 지형을 깎지 않게). 크기 11 x 11 x 11. 한쪽 다리에 사다리, 바닥에 구멍.
    """
    rnd = random.Random(20261108)
    t = Template((11, 11, 11))
    for x in range(11):
        for z in range(11):
            for y in range(6, 11):
                t.set(x, y, z, "air")
    # 다리: 퍼퍼 기둥 (하나는 반쯤 무너짐 → 엔드 돌 벽돌로 괴어 둠)
    for (x, z) in ((1, 1), (9, 1), (1, 9), (9, 9)):
        for y in range(0, 5):
            if (x, z) == (9, 9) and y >= 2:
                t.set(x, y, z, "end_stone_bricks")
            else:
                t.set(x, y, z, "purpur_pillar", {"axis": "y"})
    # 승강장 바닥 (y=5): 퍼퍼 · 엔드 돌 벽돌, 가장자리는 퍼퍼 기둥(눕힘), 군데군데 구멍
    keep = {(5, 5), (4, 5), (6, 5), (5, 4), (5, 6), (8, 2), (1, 2)}
    for x in range(11):
        for z in range(11):
            if (x, z) == (1, 2):
                continue  # 사다리 구멍
            if x in (0, 10):
                t.set(x, 5, z, "purpur_pillar", {"axis": "z"})
                continue
            if z in (0, 10):
                t.set(x, 5, z, "purpur_pillar", {"axis": "x"})
                continue
            if (x, z) not in keep and rnd.random() < 0.08:
                continue
            if 4 <= x <= 6 and 4 <= z <= 6:
                t.set(x, 5, z, "obsidian" if (x, z) != (5, 5) else "crying_obsidian")
                continue
            t.set(x, 5, z, "purpur_block" if rnd.random() < 0.6 else "end_stone_bricks")
    # 사다리 (다리 (1,1)의 남쪽 면, 바닥 구멍까지)
    for y in range(0, 6):
        t.set(1, y, 2, "ladder", {"facing": "south", "waterlogged": "false"})
    # 난간: 쇠창살, 군데군데 빠짐
    for i in range(11):
        for (x, z) in ((i, 0), (i, 10), (0, i), (10, i)):
            if rnd.random() < 0.6:
                t.set(x, 6, z, "iron_bars", {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})
    # 가운데 수호자 제단, 동쪽 위 상자
    t.set(5, 6, 5, "sanupcraft:guardian_altar")
    t.set(8, 6, 2, "chest", {"facing": "south", "type": "single", "waterlogged": "false"},
          {"id": "minecraft:chest", "LootTable": "sanupcraft:chests/abandoned_station"})
    # 끊긴 선로 (서쪽 줄)
    for z in range(2, 9):
        if z == 6:
            continue
        t.set(3, 6, z, "powered_rail" if z == 4 else "rail",
              {"shape": "north_south", "waterlogged": "false", **({"powered": "false"} if z == 4 else {})})
    # 고압 파이프 잔해 (남쪽)
    t.set(6, 6, 8, "sanupcraft:high_pressure_pipe", {"east": "true", "west": "false", "north": "false", "south": "false", "up": "false", "down": "false"})
    t.set(7, 6, 8, "sanupcraft:high_pressure_pipe", {"east": "false", "west": "true", "north": "false", "south": "false", "up": "false", "down": "false"})
    # 돛대: 퍼퍼 기둥 + 꼭대기 엔드 막대, 반쯤 걸린 사슬
    for y in range(6, 10):
        t.set(1, y, 8, "purpur_pillar", {"axis": "y"})
    t.set(1, 10, 8, "end_rod", {"facing": "up"})
    t.set(2, 9, 8, "purpur_slab", {"type": "top", "waterlogged": "false"})
    t.set(3, 9, 8, "purpur_slab", {"type": "top", "waterlogged": "false"})
    t.set(3, 8, 8, "iron_chain", {"axis": "y", "waterlogged": "false"})
    # 상자 위 무너진 차양 (동쪽 위)
    for y in range(6, 9):
        t.set(9, y, 1, "purpur_pillar", {"axis": "y"})
    for x in range(6, 10):
        for z in range(1, 4):
            if rnd.random() < 0.7:
                t.set(x, 9, z, "purpur_slab", {"type": "bottom", "waterlogged": "false"})
    t.set(7, 6, 3, "end_rod", {"facing": "up"})
    return t


def main():
    n = rusted_shrine().save(OUT / "rusted_shrine.nbt")
    print(f"rusted_shrine.nbt: 블록 {n}개")
    n = abandoned_furnace().save(OUT / "abandoned_furnace.nbt")
    print(f"abandoned_furnace.nbt: 블록 {n}개")
    n = abandoned_station().save(OUT / "abandoned_station.nbt")
    print(f"abandoned_station.nbt: 블록 {n}개")


if __name__ == "__main__":
    main()
