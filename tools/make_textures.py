"""임시 도트 텍스처 생성기.

글자 그림(16x16)과 간단한 도형으로 아이템 · 블록 · 갑옷 텍스처를 만든다.
직접 그린 도트로 바꾸려면 해당 png를 덮어쓰고 이 스크립트에서 그 항목을 지우면 된다.

    python tools/make_textures.py          # src/main/resources/assets/sanupcraft/textures 에 쓴다
    python tools/make_textures.py --sheet  # 미리보기 한 장(build/texture_sheet.png)도 만든다
"""
import math
import os
import random
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "sanupcraft", "textures")

PALETTE = {
    ".": None,
    # 강철
    "a": "#2c3138", "b": "#5f6873", "c": "#8f99a5", "d": "#bcc6d0", "e": "#eaf0f5",
    # 녹 · 주황
    "r": "#6e3219", "s": "#a85628", "t": "#de8442", "u": "#ffc27a",
    # 수호자의 핵 (푸른 빛)
    "k": "#16577a", "g": "#2fb8ef", "h": "#a6efff",
    # 나무 · 돌 · 기타
    "w": "#5c3f24", "x": "#8f6a40", "m": "#4a4a4a", "n": "#6e6e6e", "o": "#929292",
    "R": "#a3262a", "Q": "#e04848",
}


def px(ch):
    c = PALETTE[ch]
    if c is None:
        return (0, 0, 0, 0)
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def from_art(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    im = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            im.putpixel((x, y), px(ch))
    return im


ITEMS = {
    "iron_dust": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".......de.......",
        "......dcce......",
        ".....cdcbcd.....",
        "....bccdcbcc....",
        "...bcbcdcccbb...",
        "..abcbbcbcbcba..",
        "..aabbbbbbbbaa..",
        "...aaaaaaaaaa...",
        "................",
        "................",
    ],
    "iron_plate": [
        "................",
        "................",
        "................",
        "...aaaaaaaaaa...",
        "..adeeeeeeeeca..",
        "..aeddddddddca..",
        "..aedcddddddca..",
        "..aeddddddddca..",
        "..aeddddddddca..",
        "..aeddddddcdca..",
        "..aeddddddddca..",
        "..acccccccccba..",
        "..abbbbbbbbbba..",
        "...aaaaaaaaaa...",
        "................",
        "................",
    ],
    "guardian_summoner": [
        "................",
        "......aaaa......",
        ".....abccba.....",
        "....abcddcba....",
        "...abcdRRdcba...",
        "..abcdRQQRdcba..",
        "..abcRQuuQRcba..",
        "..abcRQuuQRcba..",
        "..abcdRQQRdcba..",
        "...abcdRRdcba...",
        "....abcddcba....",
        ".....abccba.....",
        "......aaaa......",
        "................",
        "................",
        "................",
    ],
    "steel_helmet": [
        "................",
        "................",
        "................",
        "....aaaaaaaa....",
        "...adeeeeeedca..",
        "..adeddddddddca.",
        "..aedddddddddca.",
        "..aedcaaaaaadca.",
        "..aeca......aca.",
        "..abba......aba.",
        "...aa........a..",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "steel_chestplate": [
        "................",
        "..aaa......aaa..",
        ".adea......adca.",
        ".adeeaaaaaaedca.",
        ".adeeddddddedca.",
        "..aaeddddddcaa..",
        "...aeddddddca...",
        "...aedddcddca...",
        "...aeddddddca...",
        "...aedcddddca...",
        "...aeddddddca...",
        "...acccccccba...",
        "...abbbbbbbba...",
        "....aaaaaaaa....",
        "................",
        "................",
    ],
    "steel_leggings": [
        "................",
        "................",
        "...aaaaaaaaaa...",
        "...aeeeeeeeca...",
        "...aeddddddca...",
        "...aeddaaddca...",
        "...aedda.adca...",
        "...aedca.adca...",
        "...aedca.adca...",
        "...aedca.adca...",
        "...aedca.adca...",
        "...acbba.abba...",
        "...aaaa..aaaa...",
        "................",
        "................",
        "................",
    ],
    "steel_boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "...aaaa..aaaa...",
        "...aeca..aeca...",
        "...aeca..aeca...",
        "...aeda..aeda...",
        "..aedca..aedca..",
        ".aeddca..aeddca.",
        ".abbbba..abbbba.",
        ".aaaaaa..aaaaaa.",
        "................",
        "................",
    ],
    "steel_sword": [
        "................",
        ".............aa.",
        "............aea.",
        "...........aeda.",
        "..........aedca.",
        ".........aedca..",
        "........aedca...",
        ".......aedca....",
        "..aa..aedca.....",
        "..aea.adca......",
        "...aesaca.......",
        "....asta........",
        "...axwasa.......",
        "..axwa..aa......",
        ".awwa...........",
        ".aaa............",
    ],
}


def cog(teeth_color="b", body="c", light="d", hole=True):
    im = Image.new("RGBA", (16, 16))
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            tooth = math.cos(a * 8) > 0.25
            ch = None
            if hole and r < 1.8:
                ch = None
            elif r < 4.6:
                ch = light if (dx + dy) < -1.5 else body
            elif r < 5.4:
                ch = teeth_color
            elif r < 7.2 and tooth:
                ch = teeth_color
            elif r < 7.8 and tooth:
                ch = "a"
            if ch:
                im.putpixel((x, y), px(ch))
    # 테두리
    out = im.copy()
    for y in range(16):
        for x in range(16):
            if im.getpixel((x, y))[3] == 0 and math.hypot(x - cx, y - cy) > 2.2:
                for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                    if 0 <= nx < 16 and 0 <= ny < 16 and im.getpixel((nx, ny))[3]:
                        out.putpixel((x, y), px("a"))
                        break
    return out


def orb():
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            hl = math.hypot(x - 5.5, y - 5.5)
            if r < 3.0:
                ch = "h" if hl < 2.0 else "g"
            elif r < 5.0:
                ch = "g" if hl < 3.5 else "k"
            elif r < 6.0:
                ch = "r"
            elif r < 7.0 and (x + y) % 2 == 0:
                ch = "s"
            else:
                continue
            im.putpixel((x, y), px(ch))
    return im


def frame():
    im = Image.new("RGBA", (16, 16))
    for y in range(1, 15):
        for x in range(1, 15):
            edge = x in (1, 14) or y in (1, 14)
            inner = x in (3, 12) or y in (3, 12)
            if edge:
                ch = "a"
            elif x in (2, 13) or y in (2, 13):
                ch = "d" if x < 8 and y < 8 else "c"
            elif inner:
                ch = "b"
            elif abs(x - y) <= 0 or abs(x + y - 15) <= 0:
                ch = "s"
            else:
                continue
            im.putpixel((x, y), px(ch))
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        im.putpixel((x, y), px("g"))
    return im


# ---------- 블록 ----------
def noise_fill(im, chars, seed, box=(0, 0, 16, 16)):
    rnd = random.Random(seed)
    for y in range(box[1], box[3]):
        for x in range(box[0], box[2]):
            im.putpixel((x, y), px(rnd.choice(chars)))


def panel(seed=1):
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, "cccd", seed)
    for i in range(16):
        im.putpixel((i, 0), px("d"))
        im.putpixel((0, i), px("d"))
        im.putpixel((i, 15), px("a"))
        im.putpixel((15, i), px("a"))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        im.putpixel((x, y), px("e"))
        im.putpixel((x + 1, y + 1), px("b"))
    return im


def machine_front(kind, lit):
    im = panel(seed={"crusher": 11, "press": 12, "assembler": 13}[kind])
    glow = "u" if lit else "m"
    if kind == "crusher":
        # 맞물린 톱니 롤러 두 개
        for y in range(4, 12):
            for x in range(3, 13):
                im.putpixel((x, y), px("a"))
        for y in range(5, 11):
            for x in (4, 5, 6, 7):
                im.putpixel((x, y), px("o" if (y + x) % 3 else "n"))
            for x in (8, 9, 10, 11):
                im.putpixel((x, y), px("o" if (y - x) % 3 else "n"))
        for y in range(12, 14):
            for x in range(4, 12):
                im.putpixel((x, y), px(glow))
    elif kind == "press":
        # 위에서 내려오는 압착판
        for y in range(3, 13):
            for x in range(3, 13):
                im.putpixel((x, y), px("a"))
        for y in range(4, 7):
            for x in range(4, 12):
                im.putpixel((x, y), px("d" if y == 4 else "c"))
        for y in range(7, 9):
            for x in (7, 8):
                im.putpixel((x, y), px("b"))
        for y in range(10, 12):
            for x in range(4, 12):
                im.putpixel((x, y), px(glow if y == 11 else "n"))
    else:
        # 조립대: 집게 팔 두 개 + 작업판
        for y in range(3, 13):
            for x in range(3, 13):
                im.putpixel((x, y), px("a"))
        for x, y in ((5, 4), (5, 5), (5, 6), (6, 7), (10, 4), (10, 5), (10, 6), (9, 7)):
            im.putpixel((x, y), px("t"))
        for y in range(9, 12):
            for x in range(4, 12):
                im.putpixel((x, y), px("x" if y < 11 else "w"))
        im.putpixel((7, 8), px(glow))
        im.putpixel((8, 8), px(glow))
    return im


def machine_top(kind):
    im = panel(seed={"crusher": 21, "press": 22, "assembler": 23}[kind])
    for y in range(5, 11):
        for x in range(5, 11):
            im.putpixel((x, y), px("a" if x in (5, 10) or y in (5, 10) else "b"))
    mark = {"crusher": "n", "press": "d", "assembler": "t"}[kind]
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        im.putpixel((x, y), px(mark))
    return im


def altar(face):
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, "nnoom", 42 + len(face))
    for y in (0, 7, 8, 15):
        for x in range(16):
            im.putpixel((x, y), px("m"))
    for x in (0, 15):
        for y in range(16):
            im.putpixel((x, y), px("m"))
    if face == "top":
        for y in range(3, 13):
            for x in range(3, 13):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 2.2:
                    im.putpixel((x, y), px("u"))
                elif d < 3.5:
                    im.putpixel((x, y), px("t"))
                elif d < 4.8:
                    im.putpixel((x, y), px("r"))
    elif face == "side":
        for x in range(2, 14):
            im.putpixel((x, 2), px("s"))
            im.putpixel((x, 13), px("s"))
        for y in range(5, 11):
            im.putpixel((7, y), px("t"))
            im.putpixel((8, y), px("t"))
    return im


# ---------- 갑옷 (64x32, 플레이어 스킨 옛 배치) ----------
def armor_layer(leggings):
    im = Image.new("RGBA", (64, 32))
    rnd = random.Random(7 if leggings else 3)

    def face(x0, y0, w, h, rows=None):
        for y in range(y0, y0 + h):
            if rows and (y - y0) not in rows:
                continue
            for x in range(x0, x0 + w):
                edge_dark = x == x0 + w - 1 or y == y0 + h - 1
                edge_light = x == x0 or y == y0
                ch = "a" if edge_dark else "d" if edge_light else rnd.choice("ccb" if leggings else "ccd")
                im.putpixel((x, y), px(ch))

    def box(u, v, w, h, d, rows=None):
        # 위 · 아래 · 오른쪽 · 앞 · 왼쪽 · 뒤
        if rows is None:
            face(u + d, v, w, d)
            face(u + d + w, v, w, d)
        face(u, v + d, d, h, rows)
        face(u + d, v + d, w, h, rows)
        face(u + d + w, v + d, d, h, rows)
        face(u + d + w + d, v + d, w, h, rows)

    if leggings:
        box(16, 16, 8, 12, 4, rows=set(range(0, 5)))   # 허리
        box(0, 16, 4, 12, 4, rows=set(range(0, 9)))    # 다리
        face(4, 16, 4, 4)
    else:
        box(0, 0, 8, 8, 8)                              # 머리
        for y in range(12, 15):                         # 얼굴 앞 틈
            for x in range(9, 15):
                im.putpixel((x, y), (0, 0, 0, 0))
        box(16, 16, 8, 12, 4)                           # 몸통
        box(40, 16, 4, 12, 4)                           # 팔
        box(0, 16, 4, 12, 4, rows=set(range(8, 12)))   # 부츠
        face(8, 16, 4, 4)
    return im


# ---------- M2 ----------
HEAT = {"a": "#2a1410", "b": "#6e2c18", "c": "#a8481f", "d": "#e0782e", "e": "#ffc27a"}
COPPER = {"a": "#3a1f12", "b": "#7a4423", "c": "#b06a35", "d": "#d99a5c", "e": "#f2c58f"}


def hex_px(color):
    return tuple(int(color[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def recolor(im, mapping):
    """강철 팔레트(a~e 등)를 다른 색으로 바꾼다."""
    table = {px(k)[:3]: hex_px(v)[:3] for k, v in mapping.items()}
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = im.getpixel((x, y))
            if a and (r, g, b) in table:
                out.putpixel((x, y), table[(r, g, b)] + (a,))
    return out


SHARD = [
    "................",
    "................",
    "................",
    "........ka......",
    ".......kgk......",
    "......kghgk.....",
    ".....kgghgk.....",
    ".....kghggk.....",
    "....kgghggk.....",
    "....kghgggk.....",
    "....kgggggk.....",
    ".....kggggk.....",
    "......kkkk......",
    "................",
    "................",
    "................",
]

HEART = [
    "................",
    "................",
    "...rrr....rrr...",
    "..rsttr..rttsr..",
    ".rsttutrrtuttsr.",
    ".rsttuutttuttsr.",
    ".rstttuuuuttssr.",
    ".rssttttttttssr.",
    "..rssttttttssr..",
    "...rsstttttsr...",
    "....rssttssr....",
    ".....rssssr.....",
    "......rssr......",
    ".......rr.......",
    "................",
    "................",
]


def boiler_front(lit):
    im = recolor(panel(seed=31), COPPER)
    for y in range(6, 13):
        for x in range(4, 12):
            im.putpixel((x, y), px("a"))
    for x in range(5, 11):
        im.putpixel((x, 7), px("b"))
    for y in range(9, 12):
        for x in range(5, 11):
            im.putpixel((x, y), px("u" if lit and (x + y) % 2 else ("t" if lit else "m")))
    for x in range(3, 13):
        im.putpixel((x, 3), px("b"))
    return im


def boiler_top():
    im = recolor(panel(seed=32), COPPER)
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.2:
                im.putpixel((x, y), px("a"))
            elif d < 3.6:
                im.putpixel((x, y), px("b"))
    return im


def boiler_side():
    im = recolor(panel(seed=33), COPPER)
    for y in (4, 11):
        for x in range(16):
            im.putpixel((x, y), px("b"))
            if x % 3 == 1:
                im.putpixel((x, y), px("e"))
    return im


def belt_motor_front(on):
    im = panel(seed=41)
    gear = cog(teeth_color="b", body="n", light="o")
    small = gear.resize((12, 12), Image.NEAREST)
    im.alpha_composite(small, (2, 2))
    if on:
        for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
            im.putpixel((x, y), px("u"))
    return im


def belt_motor_top():
    im = panel(seed=42)
    for y in range(5, 11):
        for x in range(5, 11):
            im.putpixel((x, y), px("a" if x in (5, 10) or y in (5, 10) else "s"))
    return im


def pipe_texture():
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, "ccd", 51)
    im = recolor(im, COPPER)
    for i in range(16):
        im.putpixel((i, 0), hex_px(COPPER["a"]))
        im.putpixel((i, 15), hex_px(COPPER["a"]))
    for x in (4, 11):
        for y in range(16):
            im.putpixel((x, y), hex_px(COPPER["e"]))
    return im


BELT_PERIOD = 8


def belt_frame(shift):
    """벨트 윗면: 고무 + 북쪽(위)을 가리키는 화살 무늬, 양옆 쇠 난간. shift만큼 위로 밀린 그림."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x in (0, 15):
                ch = "b"
            elif x in (1, 14):
                ch = "c"
            else:
                yy = (y + shift) % BELT_PERIOD
                # 화살 ^ : 가운데로 갈수록 위
                arrow = abs(x - 7.5) // 1.5
                ch = "o" if (yy - arrow) % BELT_PERIOD in (0, 1) and 3 <= x <= 12 else ("m" if (x + y) % 5 else "n")
            im.putpixel((x, y), px(ch))
    return im


def belt_moving():
    frames = BELT_PERIOD // 2
    im = Image.new("RGBA", (16, 16 * frames))
    for i in range(frames):
        im.paste(belt_frame(i * 2), (0, 16 * i))
    return im


def belt_side():
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, "bc", 61)
    for x in range(16):
        im.putpixel((x, 12), px("d"))
        im.putpixel((x, 15), px("a"))
        if x % 4 == 1:
            im.putpixel((x, 13), px("e"))
    return im


def main_m2(made):
    made.append(save(from_art(SHARD), "item/core_shard.png"))
    made.append(save(from_art(HEART), "item/warden_heart.png"))
    summoner = from_art(ITEMS["guardian_summoner"])
    made.append(save(recolor(summoner, {**HEAT, "R": "#c2410c", "Q": "#fb923c", "u": "#fff1c1"}), "item/warden_summoner.png"))
    for part in ("helmet", "chestplate", "leggings", "boots", "sword"):
        made.append(save(recolor(from_art(ITEMS[f"steel_{part}"]), HEAT), f"item/heatproof_{part}.png"))

    made.append(save(boiler_front(False), "block/boiler_front.png"))
    made.append(save(boiler_front(True), "block/boiler_front_on.png"))
    made.append(save(boiler_top(), "block/boiler_top.png"))
    made.append(save(boiler_side(), "block/boiler_side.png"))
    made.append(save(belt_motor_front(False), "block/belt_motor_front.png"))
    made.append(save(belt_motor_front(True), "block/belt_motor_front_on.png"))
    made.append(save(belt_motor_top(), "block/belt_motor_top.png"))
    made.append(save(pipe_texture(), "block/steam_pipe.png"))
    made.append(save(belt_frame(0), "block/conveyor_belt_top.png"))
    moving = save(belt_moving(), "block/conveyor_belt_top_moving.png")
    with open(moving + ".mcmeta", "w", encoding="utf-8") as f:
        f.write('{"animation": {"frametime": 1}}')
    made.append(moving)
    made.append(save(belt_side(), "block/conveyor_belt_side.png"))

    made.append(save(recolor(armor_layer(False), HEAT), "entity/equipment/humanoid/heatproof.png"))
    made.append(save(recolor(armor_layer(True), HEAT), "entity/equipment/humanoid_leggings/heatproof.png"))


def save(im, rel, scale=1):
    path = os.path.join(OUT, *rel.split("/"))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)
    return path


def main():
    made = []
    for name, art in ITEMS.items():
        made.append(save(from_art(art), f"item/{name}.png"))
    made.append(save(cog(), "item/gear_part.png"))
    made.append(save(orb(), "item/guardian_core.png"))
    made.append(save(frame(), "item/reinforced_machine_frame.png"))

    made.append(save(panel(), "block/machine_side.png"))
    for kind in ("crusher", "press", "assembler"):
        made.append(save(machine_front(kind, False), f"block/{kind}_front.png"))
        made.append(save(machine_front(kind, True), f"block/{kind}_front_on.png"))
        made.append(save(machine_top(kind), f"block/{kind}_top.png"))
    for f in ("top", "side", "bottom"):
        made.append(save(altar(f), f"block/guardian_altar_{f}.png"))

    made.append(save(armor_layer(False), "entity/equipment/humanoid/steel.png"))
    made.append(save(armor_layer(True), "entity/equipment/humanoid_leggings/steel.png"))
    main_m2(made)

    if "--sheet" in sys.argv:
        tiles = [Image.open(p) for p in made]
        cols = 8
        cell = 72
        sheet = Image.new("RGBA", (cols * cell, ((len(tiles) + cols - 1) // cols) * cell), (40, 44, 52, 255))
        for i, t in enumerate(tiles):
            w = 64 if t.width > 16 else 64
            h = w * t.height // t.width
            sheet.paste(t.resize((w, h), Image.NEAREST), ((i % cols) * cell + 4, (i // cols) * cell + 4), t.resize((w, h), Image.NEAREST))
        os.makedirs(os.path.join(ROOT, "build"), exist_ok=True)
        sheet.save(os.path.join(ROOT, "build", "texture_sheet.png"))
    print(f"{len(made)}개 생성")


if __name__ == "__main__":
    main()
