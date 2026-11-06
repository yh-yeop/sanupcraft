"""월드 구조물(.nbt)을 코드로 만든다. 배치를 여기서 고치고 다시 돌린다.

    python tools/make_structures.py

녹슨 사당 (rusted_shrine): 오버월드 평지 · 숲에 생기는 무너진 석재 사당. 가운데 수호자 제단, 상자에 소환석과 재료.
바닥(y=0)은 지면 높이에 묻히도록 놓인다 (structure 의 start_height -1).
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


def main():
    n = rusted_shrine().save(OUT / "rusted_shrine.nbt")
    print(f"rusted_shrine.nbt: 블록 {n}개")


if __name__ == "__main__":
    main()
