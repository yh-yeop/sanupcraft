"""다중 블록 외벽의 이어진 모양 (확장 M15 R5): 블록 상태 · 모델 JSON 생성기.

외벽 블록은 모양이 서면 cx · cy · cz (구조물 안 위치, 월드 축 0 · 1 · 2)를 갖는다 (industry/CasingLook).
바깥 면은 3x3 큰 그림의 한 조각, 안쪽 면(빈칸 쪽) · 밑면은 예전 formed 텍스처를 쓴다.
조각 텍스처는 tools/make_textures.py main_a5가 만든다: block/<이름>_side_<열><줄> (열 0 = 바깥에서 볼 때 왼쪽, 줄 0 = 맨 위),
block/<이름>_top_<cx><cz>.

    python tools/make_casing_models.py
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "sanupcraft")
BLOCKS = ("big_boiler_casing", "heatproof_bricks")


def write(rel, data):
    path = os.path.join(ASSETS, *rel.split("/"))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def faces(name, cx, cy, cz):
    inner = f"sanupcraft:block/{name}_formed"
    row = 2 - cy  # 텍스처 줄: 0 = 맨 위

    def side(col):
        return f"sanupcraft:block/{name}_side_{col}{row}"

    return {
        "particle": inner,
        # 북쪽 면은 남쪽을 보고 서서 본다: 왼쪽이 동(+X)
        "north": side(2 - cx) if cz == 0 else inner,
        "south": side(cx) if cz == 2 else inner,
        # 동쪽 면은 서쪽을 보고: 왼쪽이 남(+Z)
        "east": side(2 - cz) if cx == 2 else inner,
        "west": side(cz) if cx == 0 else inner,
        "up": f"sanupcraft:block/{name}_top_{cx}{cz}" if cy == 2 else inner,
        "down": inner,
    }


def main():
    for name in BLOCKS:
        variants = {"formed=false": {"model": f"sanupcraft:block/{name}"}}
        for cx in range(3):
            for cy in range(3):
                for cz in range(3):
                    model = f"block/{name}_formed_{cx}{cy}{cz}"
                    write(f"models/{model}.json", {"parent": "minecraft:block/cube", "textures": faces(name, cx, cy, cz)})
                    variants[f"cx={cx},cy={cy},cz={cz},formed=true"] = {"model": f"sanupcraft:{model}"}
        write(f"blockstates/{name}.json", {"variants": variants})
    print("ok")


if __name__ == "__main__":
    main()
