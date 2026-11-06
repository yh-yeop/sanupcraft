"""손에 든 3D 아이템 모델 (확장 M15 R3): 모델 · 아이템 정의 JSON 생성기.

증기 드릴 · 전기톱 · 리벳총 · 화염 방사기 (앞을 겨누는 도구)와 증기 해머 · 렌치 (세워 쥐는 도구)를 상자 요소로 만든다.
인벤토리 · 땅 · 액자 · 선반에서는 원래 평면 아이콘(item/<이름>), 손에서는 입체 모델(item/<이름>_held)을 쓴다
(아이템 정의의 minecraft:select · display_context, 바닐라 망원경 · 삼지창과 같은 방식).
증기 드릴은 공격 키를 누르고 있는 동안(손에 든 칸이 선택돼 있을 때) 날이 도는 모델 넷 중 하나를 매 프레임 고른다.

텍스처는 한 장: item/held/steam_tools (64x64, tools/make_textures.py main_r2가 그린다). 아래 REGIONS 자리와 같아야 한다.

    python tools/make_held_models.py
"""
import json
import math
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "sanupcraft")
TEX = "sanupcraft:item/held/steam_tools"
TEX_SIZE = 64

# 텍스처 안 재질 자리 (x, y, 너비, 높이, 텍셀)
REGIONS = {
    "brass": (0, 0, 16, 16), "copper": (16, 0, 16, 16), "steel": (32, 0, 16, 16), "iron": (48, 0, 16, 16),
    "leather": (0, 16, 16, 16), "red": (16, 16, 16, 16), "chrome": (32, 16, 16, 16), "wood": (48, 16, 16, 16),
    "dial": (0, 32, 8, 8), "bit": (8, 32, 16, 16), "chain": (24, 32, 16, 8), "glow": (40, 32, 8, 8),
    "glass": (48, 32, 8, 8), "rivet": (56, 32, 8, 8),
}
DECALS = {"dial", "glow", "rivet"}  # 면 크기와 상관없이 자리 전체를 한 면에


def write(rel, data):
    path = os.path.join(ASSETS, *rel.split("/"))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def face_uv(mat, w, h, seed):
    """면 크기 (모델 단위)에 맞춘 uv: 1단위 = 1텍셀, 재질 자리 안에서 면마다 조금씩 다른 곳을 쓴다."""
    rx, ry, rw, rh = REGIONS[mat]
    k = 16 / TEX_SIZE
    if mat in DECALS:
        return [rx * k, ry * k, (rx + rw) * k, (ry + rh) * k]
    tw, th = min(rw, max(0.5, w)), min(rh, max(0.5, h))
    ox = (seed * 5) % (rw - tw + 1) if rw > tw else 0
    oy = (seed * 3) % (rh - th + 1) if rh > th else 0
    return [round((rx + ox) * k, 4), round((ry + oy) * k, 4), round((rx + ox + tw) * k, 4), round((ry + oy + th) * k, 4)]


def box(frm, to, mat, faces=None, rot=None, light=0, skip=()):
    """상자 하나. faces = {면: 재질} (일부 면만 다른 재질), rot = (축, 각도, 중심), skip = 그리지 않을 면."""
    faces = faces or {}
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    dims = {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}
    seed = int(sum(frm) * 7 + sum(to) * 3)
    el = {"from": list(frm), "to": list(to), "faces": {}}
    for i, (name, (w, h)) in enumerate(dims.items()):
        if name in skip:
            continue
        m = faces.get(name, mat)
        el["faces"][name] = {"uv": face_uv(m, w, h, seed + i), "texture": "#t"}
    if rot:
        axis, angle, origin = rot
        el["rotation"] = {"origin": list(origin), "axis": axis, "angle": angle}
    if light:
        el["light_emission"] = light
    return el


def _norm(v):
    n = math.sqrt(sum(c * c for c in v))
    return [c / n for c in v]


def _cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]


def _ortho(v, base):
    """v에서 base 방향 성분을 뺀 단위 벡터."""
    d = sum(p * q for p, q in zip(v, base))
    return _norm([c - d * bc for c, bc in zip(v, base)])


def _euler_xyz(x_axis, y_axis, y_first=False):
    """모델 +X · +Y가 갈 방향 → display rotation [x, y, z] (Quaternionf.rotationXYZ = Rx · Ry · Rz, Z부터 돈다).
    y_first면 +Y 방향을 그대로 두고 +X를 맞춘다 (아니면 반대)."""
    if y_first:
        y = _norm(y_axis)
        x = _ortho(x_axis, y)
    else:
        x = _norm(x_axis)
        y = _ortho(y_axis, x)
    z = _cross(x, y)
    m = [[x[i], y[i], z[i]] for i in range(3)]  # 열 = 모델 축이 간 곳
    b = math.asin(max(-1.0, min(1.0, m[0][2])))
    if abs(m[0][2]) > 0.9999:  # 짐벌 잠김: Y가 ±90도면 X · Z가 같은 축이라 X = 0으로 둔다
        a, c = 0.0, math.atan2(m[1][0], m[1][1])
    else:
        a = math.atan2(-m[1][2], m[2][2])
        c = math.atan2(-m[0][1], m[0][0])
    return [round(math.degrees(v), 1) for v in (a, b, c)], m


# 손 자세: (모델 +X 방향, 모델 +Y 방향, 크기, 손 위치)
# 3인칭 display 좌표: +Y = 앞 (주먹에서 뻗는 쪽), +Z = 위. 1인칭: 화면 좌표 (+X 오른쪽, +Y 위, -Z 앞).
# 손 위치는 바닐라 handheld 자루 끝이 오는 곳 (픽셀): 3인칭 (0, -2, 1), 1인칭 (1.1, -1.2, -1.0).
POSES = {
    "aim": {
        "thirdperson": ([0, 1, 0.08], [0, 0, 1], 0.85, (0, -2.0, 1.0)),
        "firstperson": ([-0.42, 0.08, -1], [0, 1, 0], 0.68, (0.6, -0.4, -2.6)),
    },
    "tool": {
        "thirdperson": ([0, -0.57, 0.82], [0, 0.82, 0.57], 0.85, (0, -2.0, 1.0)),
        "firstperson": ([0.9, 0.2, 0.35], [-0.25, 0.85, -0.45], 0.68, (1.1, -1.4, -1.6)),
    },
}


def display(kind, grip, size=1.0):
    """손 자세. kind = "aim" (모델 +X가 앞을 겨눔, +Y가 위) 또는 "tool" (모델 +Y가 자루 → 머리, +X가 머리 가로)."""
    g = [grip[i] - 8 for i in range(3)]
    out = {}
    for ctx, (xa, ya, s, target) in POSES[kind].items():
        s = round(s * size, 3)
        r, m = _euler_xyz(xa, ya, y_first=kind == "tool")
        p = [sum(m[i][j] * g[j] for j in range(3)) for i in range(3)]
        t = [round(target[i] - s * p[i], 2) for i in range(3)]
        out[f"{ctx}_righthand"] = {"rotation": r, "translation": t, "scale": [s, s, s]}
        # 왼손은 같은 값: 게임이 왼손일 때 X 이동 · Y · Z 회전을 뒤집어 거울처럼 그린다 (ItemTransform.apply)
        out[f"{ctx}_lefthand"] = {"rotation": r, "translation": t, "scale": [s, s, s]}
    return out


def model(elements, kind, grip, size=1.0):
    return {"textures": {"t": TEX, "particle": TEX}, "elements": elements, "display": display(kind, grip, size)}


# ---- 앞을 겨누는 도구 (모델 +X = 앞) ----
def drill_bit(angle):
    """드릴 날: 가늘어지는 네모 마디 넷, 축 (y 8.5, z 8)을 따라 angle만큼 돌린다."""
    rot = ("x", angle, (12, 8.5, 8)) if angle else None
    segs = ((10.5, 12.5, 2.0), (12.5, 14.5, 1.5), (14.5, 16.0, 1.0), (16.0, 17.0, 0.5))
    return [box((x0, 8.5 - r, 8 - r), (x1, 8.5 + r, 8 + r), "bit", rot=rot) for x0, x1, r in segs]


def steam_drill(angle=0):
    els = [
        box((2, 6, 5.5), (9, 11, 10.5), "brass"),
        box((1.5, 5.5, 5), (2.5, 11.5, 11), "steel"),
        box((8, 5.5, 5), (9, 11.5, 11), "steel"),
        box((4, 7, 10.5), (7, 10, 10.6), "dial", skip=("north",)),       # 계기 (양옆)
        box((4, 7, 5.4), (7, 10, 5.5), "dial", skip=("south",)),
        box((3, 1, 6.5), (5, 6, 9.5), "leather", rot=("z", -12.5, (4, 6, 8))),  # 권총 손잡이
        box((5.5, 4.5, 7.5), (6.25, 6, 8.5), "iron"),                     # 방아쇠
        box((1, 11, 7), (5, 13, 9), "copper"),                            # 증기 통
        box((9, 6.5, 6), (10.5, 10.5, 10), "iron"),                       # 척
    ]
    return els + drill_bit(angle)


def chainsaw():
    els = [
        box((1, 5, 5), (8, 11, 11), "brass"),
        box((3, 11, 6), (6.5, 12.5, 10), "iron"),                         # 냉각 핀
        box((4, 7, 11), (7, 10, 11.1), "dial", skip=("north",)),
        box((4, 7, 4.9), (7, 10, 5), "dial", skip=("south",)),
        box((-1, 4, 7), (1, 10, 9), "leather"),                           # 뒤 손잡이
        box((-1, 10, 7), (2, 11, 9), "leather"),
        box((2, 11, 7.5), (3, 14, 8.5), "iron"),                          # 위 손잡이 고리
        box((6, 11, 7.5), (7, 14, 8.5), "iron"),
        box((2, 13.5, 7.25), (7, 14.5, 8.75), "leather"),
        box((8, 6.5, 7.5), (19, 9.5, 8.5), "steel"),                      # 톱날 판
        box((8, 9.5, 7.25), (19, 10.25, 8.75), "chain"),                  # 사슬 (위 · 아래 · 끝)
        box((8, 5.75, 7.25), (19, 6.5, 8.75), "chain"),
        box((19, 5.75, 7.25), (19.75, 10.25, 8.75), "chain"),
        box((7.5, 6, 6.5), (8.5, 10, 9.5), "iron"),
    ]
    return els


def rivet_gun():
    return [
        box((2, 7, 6.5), (8, 11, 9.5), "steel"),                          # 몸통
        box((8, 8, 6.75), (14, 10.5, 9.25), "steel"),                     # 총열
        box((14, 7.75, 6.5), (15, 10.75, 9.5), "iron"),                   # 총구
        box((14.9, 8.75, 7.5), (15.4, 9.75, 8.5), "rivet"),               # 장전된 리벳
        box((3, 11, 7), (8, 13, 9), "brass"),                             # 압축 공기 통
        box((8, 10.5, 7.5), (11, 11.5, 8.5), "copper"),
        box((2.5, 2, 7), (5, 7, 9), "leather", rot=("z", 15, (3.75, 7, 8))),  # 손잡이
        box((5, 5.25, 7.5), (8, 5.75, 8.5), "iron"),                      # 방아쇠울
        box((7.5, 5.25, 7.5), (8, 7, 8.5), "iron"),
        box((5.75, 5.75, 7.75), (6.25, 7, 8.25), "iron"),                 # 방아쇠
    ]


def flamethrower():
    return [
        box((1, 3.5, 5.5), (8, 7.5, 10.5), "red"),                        # 연료 통
        box((2, 3.25, 5.25), (2.75, 7.75, 10.75), "brass"),
        box((6.25, 3.25, 5.25), (7, 7.75, 10.75), "brass"),
        box((7, 7.5, 7.5), (8, 9, 8.5), "copper"),                        # 관
        box((1, 8.5, 7), (15, 10.5, 9), "steel"),                         # 총열
        box((0, 8, 6.5), (3, 11, 9.5), "iron"),                           # 뒤 마개
        box((15, 8, 6.5), (17, 11, 9.5), "iron"),                         # 노즐
        box((17, 8.75, 7.25), (17.25, 10.25, 8.75), "glow", light=15),    # 노즐 속 불
        box((16, 7, 7.5), (17, 8, 8.5), "glow", light=15),                # 점화 불씨
        box((9, 3.5, 7), (11, 8.5, 9), "leather", rot=("z", 10, (10, 8.5, 8))),  # 앞 손잡이
        box((9.5, 10.5, 7.5), (10.5, 11.5, 8.5), "copper"),
    ]


# ---- 세워 쥐는 도구 (모델 +Y = 자루 → 머리) ----
def steam_hammer():
    return [
        box((7, 0, 7), (9, 11, 9), "steel"),                              # 자루
        box((6.75, 0, 6.75), (9.25, 5, 9.25), "leather"),
        box((6.5, -1, 6.5), (9.5, 0, 9.5), "iron"),
        box((3, 11, 5.5), (13, 16, 10.5), "brass"),                       # 머리
        box((2, 10.5, 5), (4, 16.5, 11), "steel"),
        box((12, 10.5, 5), (14, 16.5, 11), "steel"),
        box((5.5, 10.75, 5.25), (6.25, 16.25, 10.75), "copper"),
        box((9.75, 10.75, 5.25), (10.5, 16.25, 10.75), "copper"),
        box((6.75, 12, 10.5), (9.25, 14.5, 10.6), "dial", skip=("north",)),
        box((6.75, 12, 5.4), (9.25, 14.5, 5.5), "dial", skip=("south",)),
        box((7, 16, 7), (9, 17, 9), "iron"),                              # 증기 구멍
    ]


def wrench():
    return [
        box((7, 0, 7.25), (9, 11, 8.75), "steel"),                        # 자루
        box((6, -2.5, 7), (10, 0, 9), "steel"),                           # 아래 고리
        box((6.5, 10, 6.75), (9.5, 11.5, 9.25), "brass"),                 # 조절 나사
        box((5, 11.5, 7), (11, 15, 9), "steel"),                          # 머리
        box((5, 15, 7), (7, 18.5, 9), "steel"),                           # 고정 턱
        box((9.25, 15, 7), (11, 17.5, 9), "chrome"),                      # 움직이는 턱
    ]


# 이름: (자세, 손 위치 (모델 좌표), 크기 배율)
ITEMS = {
    "steam_drill": ("aim", (4, 3, 8), 1.0),
    "chainsaw": ("aim", (0, 4.5, 8), 0.9),
    "rivet_gun": ("aim", (3.75, 4, 8), 1.0),
    "flamethrower": ("aim", (10, 3.5, 8), 1.0),
    "steam_hammer": ("tool", (8, 2.5, 8), 0.85),
    "wrench": ("tool", (8, 2.5, 8), 1.0),
}
BUILD = {"steam_drill": steam_drill, "chainsaw": chainsaw, "rivet_gun": rivet_gun, "flamethrower": flamethrower,
         "steam_hammer": steam_hammer, "wrench": wrench}
SPIN = (22.5, 45, 67.5)


def flat_or_held(name, held):
    return {
        "type": "minecraft:select",
        "property": "minecraft:display_context",
        "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"],
                   "model": {"type": "minecraft:model", "model": f"sanupcraft:item/{name}"}}],
        "fallback": held,
    }


def main():
    for name, (kind, grip, size) in ITEMS.items():
        write(f"models/item/{name}_held.json", model(BUILD[name](), kind, grip, size))
        held = {"type": "minecraft:model", "model": f"sanupcraft:item/{name}_held"}
        if name == "steam_drill":
            # 날 회전: 선택된 칸 + 공격 키를 누르는 동안, 돌린 모델 넷 중 하나를 매번 무작위로 (빠르게 도는 것처럼 보인다)
            for k, a in enumerate(SPIN, 1):
                write(f"models/item/{name}_held_spin{k}.json", model(steam_drill(a), kind, grip, size))
            spin = {
                "type": "minecraft:range_dispatch",
                "property": "minecraft:time",
                "source": "random",
                "wobble": False,
                "entries": [{"threshold": (k + 1) / 4, "model": {"type": "minecraft:model", "model": f"sanupcraft:item/{name}_held_spin{k + 1}"}}
                            for k in range(3)],
                "fallback": held,
            }
            held = {
                "type": "minecraft:condition",
                "property": "minecraft:selected",
                "on_true": {"type": "minecraft:condition", "property": "minecraft:keybind_down", "keybind": "key.attack",
                            "on_true": spin, "on_false": held},
                "on_false": held,
            }
        write(f"items/{name}.json", {"model": flat_or_held(name, held)})
    print(f"{len(ITEMS)}개 아이템 모델")


if __name__ == "__main__":
    main()
