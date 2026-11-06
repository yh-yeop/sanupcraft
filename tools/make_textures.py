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


# ---------- M3 ----------
NETHER = {"a": "#1c0b0e", "b": "#3b1418", "c": "#5a1e22", "d": "#7a2b2e", "e": "#a14040"}
ENDER = {"a": "#0e0a16", "b": "#2a1d3d", "c": "#45305f", "d": "#6b4c8c", "e": "#a98bd0"}


def heart_forge_front(lit):
    im = recolor(panel(seed=71), NETHER)
    for y in range(5, 13):
        for x in range(4, 12):
            im.putpixel((x, y), px("a"))
    heart = from_art(HEART).resize((8, 8), Image.NEAREST)
    if not lit:
        heart = recolor(heart, {"r": "#2b1010", "s": "#3d1a14", "t": "#4a2216", "u": "#5c2b1a"})
    im.alpha_composite(heart, (4, 5))
    return im


def heart_forge_top():
    im = recolor(panel(seed=72), NETHER)
    for y in range(5, 11):
        for x in range(5, 11):
            im.putpixel((x, y), px("t" if 6 <= x <= 9 and 6 <= y <= 9 else "a"))
    return im


def ender_face(kind, part):
    im = recolor(panel(seed=81 if kind == "sender" else 82), ENDER)
    if part == "top":
        for y in range(4, 12):
            for x in range(4, 12):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 2.0:
                    im.putpixel((x, y), hex_px("#2fd3b4"))
                elif d < 3.6:
                    im.putpixel((x, y), hex_px("#0f6d5e"))
    elif part == "side":
        # 송신기: 위쪽 화살표, 수신기: 아래쪽 화살표
        arrow = [(7, 4), (8, 4), (6, 5), (9, 5), (5, 6), (10, 6), (7, 5), (8, 5), (7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10)]
        for x, y in arrow:
            yy = y if kind == "sender" else 15 - y
            im.putpixel((x, yy), hex_px("#2fd3b4"))
    return im


def main_m3(made):
    made.append(save(heart_forge_front(False), "block/heart_forge_front.png"))
    made.append(save(heart_forge_front(True), "block/heart_forge_front_on.png"))
    made.append(save(recolor(panel(seed=73), NETHER), "block/heart_forge_side.png"))
    made.append(save(heart_forge_top(), "block/heart_forge_top.png"))
    for kind in ("sender", "receiver"):
        made.append(save(ender_face(kind, "top"), f"block/ender_{kind}_top.png"))
        made.append(save(ender_face(kind, "side"), f"block/ender_{kind}_side.png"))
    made.append(save(recolor(panel(seed=83), ENDER), "block/ender_link_bottom.png"))


# ---------- 기계 화면 (176x166, 바닐라 GUI 색) ----------
GUI_BG, GUI_LIGHT, GUI_DARK, GUI_SLOT, GUI_EDGE = "#C6C6C6", "#FFFFFF", "#555555", "#8B8B8B", "#373737"


def gui_machine(two_inputs):
    from PIL import ImageDraw
    im = Image.new("RGBA", (256, 256))
    d = ImageDraw.Draw(im)
    w, h = 176, 166
    d.rectangle([0, 0, w - 1, h - 1], fill=GUI_BG, outline="#000000")
    d.line([(1, 1), (w - 3, 1)], fill=GUI_LIGHT, width=2)
    d.line([(1, 1), (1, h - 3)], fill=GUI_LIGHT, width=2)
    d.line([(2, h - 2), (w - 2, h - 2)], fill=GUI_DARK, width=2)
    d.line([(w - 2, 2), (w - 2, h - 2)], fill=GUI_DARK, width=2)

    def slot(x, y, size=18):
        d.rectangle([x, y, x + size - 1, y + size - 1], fill=GUI_SLOT)
        d.line([(x, y), (x + size - 2, y)], fill=GUI_EDGE)
        d.line([(x, y), (x, y + size - 2)], fill=GUI_EDGE)
        d.line([(x + 1, y + size - 1), (x + size - 1, y + size - 1)], fill=GUI_LIGHT)
        d.line([(x + size - 1, y + 1), (x + size - 1, y + size - 1)], fill=GUI_LIGHT)

    slot(55, 16)                     # 재료
    if two_inputs:
        slot(33, 16)                 # 둘째 재료
    slot(55, 52)                     # 연료
    slot(111, 30, 26)                # 결과 (큰 칸)
    slot(143, 34)                    # 부산물
    # 업그레이드 2칸 (왼쪽 끝): 빈 칸에 옅은 위쪽 화살표
    for uy in (25, 43):
        slot(7, uy)
        for i, (x0, x1) in enumerate(((15, 15), (14, 16), (13, 17))):
            d.line([(x0, uy + 6 + i), (x1, uy + 6 + i)], fill="#9C9C9C")
        d.line([(15, uy + 9), (15, uy + 12)], fill="#9C9C9C")
    # 불꽃 · 화살표 자리 (진행 그림이 위에 덮인다)
    d.polygon([(57, 49), (69, 49), (69, 42), (63, 37), (57, 42)], fill=GUI_SLOT)
    d.rectangle([80, 39, 95, 44], fill=GUI_SLOT)
    d.polygon([(95, 34), (103, 41), (95, 49)], fill=GUI_SLOT)
    # 부산물 표시: 결과와 부산물 사이 작은 +
    d.line([(139, 43), (141, 43)], fill=GUI_DARK)
    d.line([(140, 42), (140, 44)], fill=GUI_DARK)
    for row in range(3):
        for col in range(9):
            slot(7 + col * 18, 83 + row * 18)
    for col in range(9):
        slot(7 + col * 18, 141)
    return im


def main_gui(made):
    made.append(save(gui_machine(False), "gui/machine.png"))
    made.append(save(gui_machine(True), "gui/alloy_smelter.png"))


# ---------- 공장 개편: 가루 · 강철 · 해머 · 합금로 · 굴착기 ----------
COPPER_DUST = {"a": "#5a2a14", "b": "#9c4e2a", "c": "#c8693b", "d": "#e8935f", "e": "#f6c09a"}
GOLD_DUST = {"a": "#6b4a07", "b": "#b88a10", "c": "#e0b52a", "d": "#f6d85a", "e": "#fff4b0"}
COAL_DUST = {"a": "#0c0c0c", "b": "#1f1f1f", "c": "#333333", "d": "#4a4a4a", "e": "#6a6a6a"}
STEEL = {"a": "#1e2633", "b": "#3c4a5e", "c": "#5f708a", "d": "#8a9bb5", "e": "#c5d2e6"}

INGOT = [
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....aaaaaaaa...",
    "....adeeeeeeda..",
    "...adddddddddca.",
    "..adddddddddcba.",
    "..acccccccccbba.",
    "..abbbbbbbbbba..",
    "...aaaaaaaaaa...",
    "................",
    "................",
    "................",
    "................",
]

HAMMER = [
    "................",
    "...aaaaaaaa.....",
    "..adeeeeedda....",
    "..adddddddca....",
    "..accccccbba....",
    "...aaaawaaa.....",
    ".......wx.......",
    "......wx........",
    ".....wx.........",
    "....wx..........",
    "...wx...........",
    "..wx............",
    ".wx.............",
    "wx..............",
    "................",
    "................",
]

EXCAVATOR = [
    "................",
    "..........aaa...",
    ".........adeea..",
    "........addddea.",
    "........acddda..",
    ".......awaccba..",
    "......wx.aaa....",
    ".....wx.........",
    "....wx..........",
    "...wx...........",
    "..wx............",
    ".wx.............",
    "wx..............",
    "................",
    "................",
    "................",
]


def alloy_front(lit):
    im = panel(seed=91)
    for y in range(4, 13):
        for x in range(3, 13):
            im.putpixel((x, y), px("a"))
    # 도가니 두 개
    for cx in (5, 9):
        for y in range(6, 10):
            for x in range(cx, cx + 3):
                im.putpixel((x, y), px("n" if y < 9 else "m"))
    for x in range(4, 12):
        im.putpixel((x, 11), px("u" if lit else "m"))
        im.putpixel((x, 12), px("t" if lit else "m"))
    return im


def alloy_top():
    im = panel(seed=92)
    for x, y in ((5, 7), (6, 7), (9, 7), (10, 7), (5, 8), (6, 8), (9, 8), (10, 8)):
        im.putpixel((x, y), px("a"))
    return im


def quarry_side(on):
    im = recolor(panel(seed=93), STEEL)
    for y in range(3, 13):
        im.putpixel((7, y), px("b"))
        im.putpixel((8, y), px("b"))
    for x, y in ((6, 10), (9, 10), (7, 12), (8, 12), (6, 11), (9, 11)):
        im.putpixel((x, y), px("d"))
    if on:
        for x, y in ((4, 4), (11, 4)):
            im.putpixel((x, y), px("u"))
    return im


def quarry_top():
    im = recolor(panel(seed=94), STEEL)
    for y in range(5, 11):
        for x in range(5, 11):
            im.putpixel((x, y), px("a" if x in (5, 10) or y in (5, 10) else "m"))
    return im


def quarry_bottom():
    im = recolor(panel(seed=95), STEEL)
    for y in range(4, 12):
        for x in range(4, 12):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d < 4:
                im.putpixel((x, y), px("e" if d < 1.5 else "c"))
    return im


def main_redesign(made):
    dust = from_art(ITEMS["iron_dust"])
    made.append(save(recolor(dust, COPPER_DUST), "item/copper_dust.png"))
    made.append(save(recolor(dust, GOLD_DUST), "item/gold_dust.png"))
    made.append(save(recolor(dust, COAL_DUST), "item/coal_dust.png"))
    made.append(save(recolor(from_art(INGOT), STEEL), "item/steel_ingot.png"))
    made.append(save(recolor(from_art(ITEMS["iron_plate"]), STEEL), "item/steel_plate.png"))
    made.append(save(recolor(from_art(HAMMER), STEEL), "item/steel_hammer.png"))
    made.append(save(recolor(from_art(EXCAVATOR), STEEL), "item/steel_excavator.png"))
    made.append(save(alloy_front(False), "block/alloy_smelter_front.png"))
    made.append(save(alloy_front(True), "block/alloy_smelter_front_on.png"))
    made.append(save(alloy_top(), "block/alloy_smelter_top.png"))
    made.append(save(quarry_side(False), "block/steam_quarry_side.png"))
    made.append(save(quarry_side(True), "block/steam_quarry_side_on.png"))
    made.append(save(quarry_top(), "block/steam_quarry_top.png"))
    made.append(save(quarry_bottom(), "block/steam_quarry_bottom.png"))


# ---------- M4 · 네더 · 엔드 심화 ----------
DRILL = [
    "................",
    "...........aa...",
    "..........acda..",
    ".........acdeda.",
    "........acdeda..",
    ".......acdeda...",
    "......abcdda....",
    ".....abbcda.....",
    "....mabbca......",
    "...mnmaba.......",
    "..mnom.a........",
    ".mnom...........",
    "mnom............",
    "nom.............",
    "om..............",
    "................",
]


def vent_top(lit):
    im = recolor(panel(seed=101), NETHER)
    for y in range(3, 13):
        for x in range(3, 13):
            if y % 3 == 0:
                continue
            glow = lit and (x + y) % 2 == 0
            im.putpixel((x, y), hex_px("#ffb347") if glow else (hex_px("#d85a1a") if lit else px("a")))
    return im


def vent_side():
    im = recolor(panel(seed=102), NETHER)
    rnd = random.Random(103)
    for _ in range(9):
        x, y = rnd.randrange(2, 14), rnd.randrange(2, 14)
        im.putpixel((x, y), hex_px("#e0782e"))
    for x in range(16):
        im.putpixel((x, 15), hex_px("#a8481f"))
    return im


def anchor_face(part, on=False):
    im = recolor(panel(seed=111 if part == "side" else 112), ENDER)
    teal = hex_px("#c95cff" if on else "#2fd3b4")
    if part == "top":
        for y in range(3, 13):
            for x in range(3, 13):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 1.6:
                    im.putpixel((x, y), teal)
                elif 3.2 < d < 4.4:
                    im.putpixel((x, y), hex_px("#6b4c8c" if not on else "#a98bd0"))
    else:
        # 닻 모양
        for y in range(3, 12):
            im.putpixel((7, y), teal)
            im.putpixel((8, y), teal)
        for x in range(5, 11):
            im.putpixel((x, 4), teal)
        for x, y in ((4, 10), (5, 11), (6, 12), (11, 10), (10, 11), (9, 12), (7, 12), (8, 12)):
            im.putpixel((x, y), teal)
    return im


def main_m4(made):
    made.append(save(recolor(from_art(DRILL), HEAT), "item/heatproof_drill.png"))
    made.append(save(vent_top(False), "block/geothermal_vent_top.png"))
    made.append(save(vent_top(True), "block/geothermal_vent_top_on.png"))
    made.append(save(vent_side(), "block/geothermal_vent_side.png"))
    made.append(save(anchor_face("top"), "block/ender_anchor_top.png"))
    made.append(save(anchor_face("top", True), "block/ender_anchor_top_on.png"))
    made.append(save(anchor_face("side"), "block/ender_anchor_side.png"))


def gauge_front():
    """증기 계기판 눈금판: 구리 판에 반달 눈금과 바늘."""
    im = recolor(panel(seed=121), COPPER)
    for y in range(2, 14):
        for x in range(2, 14):
            d = math.hypot(x - 7.5, y - 9.5)
            if d < 5.6:
                im.putpixel((x, y), px("e") if d > 4.6 else px("u") if y < 9 else px("d"))
    for x, y in ((3, 9), (4, 6), (6, 4), (9, 4), (11, 6), (12, 9)):
        im.putpixel((x, y), px("a"))
    for x, y in ((8, 8), (9, 7), (10, 6), (7, 9), (8, 9)):
        im.putpixel((x, y), px("R"))
    return im


def gauge_side():
    im = recolor(panel(seed=122), COPPER)
    for x in range(16):
        im.putpixel((x, 0), px("b"))
        im.putpixel((x, 15), px("b"))
    return im


def main_bundle1(made):
    made.append(save(gauge_front(), "block/steam_gauge_front.png"))
    made.append(save(gauge_side(), "block/steam_gauge_side.png"))


FILTER = {"a": "#142a33", "b": "#2f5566", "c": "#4f8a9e", "d": "#7bbccd", "e": "#bfe8f2"}

FILTER_HOPPER_ICON = [
    "................",
    ".aaaaaaaaaaaaaa.",
    ".adeeeeeeeeeeca.",
    ".acddddddddddba.",
    ".abccccccccccba.",
    "..abcccccccbba..",
    "..abbcccccbbba..",
    "...abbcccbbba...",
    "...abcbbbbcba...",
    "....abbbbbba....",
    ".....abbbba.....",
    ".....abbcba.....",
    ".....abbcba.....",
    ".....abbbba.....",
    ".....aaaaaa.....",
    "................",
]

ORE_MARKER_ICON = [
    "................",
    "................",
    "....aaaaaaaa....",
    "...aeeeeeeeea...",
    "..aeddddddddca..",
    "..aedrrrrrrdca..",
    "..aedrsttsrdca..",
    "..aedrtuutrdca..",
    "..aedrtuutrdca..",
    "..aedrsttsrdca..",
    "..aedrrrrrrdca..",
    "..aeddddddddca..",
    "..abcccccccbba..",
    "...aabbbbbbaa...",
    "................",
    "................",
]


def sorter_top():
    """벨트 분류기 윗면: 벨트 줄 화살(앞=위)과 왼쪽으로 꺾이는 주황 화살."""
    im = belt_frame(0)
    for y in range(16):
        for x in range(16):
            if 2 <= x <= 13 and 2 <= y <= 13 and (x + y) % 2 == 0:
                im.putpixel((x, y), px("m"))
    # 앞(위)으로 가는 화살
    for y in range(1, 9):
        im.putpixel((8, y), px("e"))
    for i in range(1, 4):
        im.putpixel((8 - i, 1 + i), px("e"))
        im.putpixel((8 + i, 1 + i), px("e"))
    # 왼쪽으로 가는 주황 화살
    for x in range(1, 7):
        im.putpixel((x, 10), px("u"))
    for i in range(1, 3):
        im.putpixel((1 + i, 10 - i), px("t"))
        im.putpixel((1 + i, 10 + i), px("t"))
    return im


def sorter_side():
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, "bcc", 201)
    for x in range(16):
        im.putpixel((x, 10), px("d"))
        im.putpixel((x, 15), px("a"))
        if x % 5 == 2:
            im.putpixel((x, 12), px("e"))
    return im


def filter_hopper_outside():
    return recolor(panel(seed=211), FILTER)


def filter_hopper_top():
    im = recolor(panel(seed=212), FILTER)
    for y in range(4, 12):
        for x in range(4, 12):
            im.putpixel((x, y), hex_px(FILTER["a"]))
    return im


def filter_hopper_inside():
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, "aab", 213)
    return recolor(im, FILTER)


def main_bundle2(made):
    made.append(save(sorter_top(), "block/belt_sorter_top.png"))
    made.append(save(sorter_side(), "block/belt_sorter_side.png"))
    made.append(save(filter_hopper_outside(), "block/filter_hopper_outside.png"))
    made.append(save(filter_hopper_top(), "block/filter_hopper_top.png"))
    made.append(save(filter_hopper_inside(), "block/filter_hopper_inside.png"))
    made.append(save(recolor(from_art(FILTER_HOPPER_ICON), FILTER), "item/filter_hopper.png"))
    made.append(save(from_art(ORE_MARKER_ICON), "item/ore_marker.png"))


CANISTER_ICON = [
    "................",
    "......aaaa......",
    ".....abccba.....",
    "......abba......",
    "....aaaaaaaa....",
    "...adeeeeeeca...",
    "...aedddddcca...",
    "...aedkkkkdca...",
    "...aedghhgdca...",
    "...aedghhgdca...",
    "...aedkkkkdca...",
    "...aedddddcba...",
    "...abccccccba...",
    "....aaaaaaaa....",
    "................",
    "................",
]

RIVET_ICON = [
    "................",
    "................",
    "................",
    "................",
    "......aaaa......",
    ".....adeeca.....",
    "....adeddcca....",
    "....abcccbba....",
    ".....aaaaaa.....",
    "......abca......",
    "......abca......",
    "......abca......",
    "......abca......",
    ".......aa.......",
    "................",
    "................",
]

RIVET_GUN_ICON = [
    "................",
    "................",
    "................",
    "..aaaaaaaaaaaa..",
    ".adeeeeeeeeedca.",
    ".aedddddddddcba.",
    ".abccccccgcbbaa.",
    ".aabbbbbbbbaa...",
    "...aeda.........",
    "...adca.........",
    "...adcaa........",
    "...abcca........",
    "....abba........",
    ".....aa.........",
    "................",
    "................",
]

CHARGER_TOP_SLOT = 6


def charger_top():
    """충전대 윗면: 철판 가운데에 증기 통이 서는 둥근 칸."""
    im = panel(seed=301)
    for y in range(3, 13):
        for x in range(3, 13):
            im.putpixel((x, y), px("a") if min(x - 3, 12 - x, y - 3, 12 - y) == 0 else px("b"))
    for y in range(5, 11):
        for x in range(5, 11):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 3.2:
                im.putpixel((x, y), px("k") if d > 2.0 else px("g"))
    return im


def charger_side():
    """충전대 옆면: 철판에 하늘색 증기 눈금 띠."""
    im = panel(seed=302)
    for x in range(2, 14):
        im.putpixel((x, 5), px("a"))
        im.putpixel((x, 10), px("a"))
        for y in (6, 7, 8, 9):
            im.putpixel((x, y), px("k") if y in (6, 9) else px("g"))
    for x in (4, 7, 10):
        im.putpixel((x, 7), px("h"))
    return im


def charger_bottom():
    im = panel(seed=303)
    for y in range(1, 15):
        for x in range(1, 15):
            r, g, b, a = im.getpixel((x, y))
            im.putpixel((x, y), (max(0, r - 40), max(0, g - 40), max(0, b - 40), a))
    return im


def steam_boots_icon():
    """증기 부츠: 강철 부츠에 구리 띠와 하늘색 밸브."""
    im = from_art(ITEMS["steel_boots"])
    for x in (3, 4, 5, 6, 9, 10, 11, 12):
        for y in (7, 8):
            if im.getpixel((x, y))[3]:
                im.putpixel((x, y), px("t") if y == 7 else px("s"))
    for x in (4, 10):
        im.putpixel((x, 9), px("h"))
    return im


def main_bundle3(made):
    made.append(save(from_art(CANISTER_ICON), "item/steam_canister.png"))
    made.append(save(from_art(RIVET_ICON), "item/rivet.png"))
    made.append(save(from_art(RIVET_GUN_ICON), "item/rivet_gun.png"))
    made.append(save(steam_boots_icon(), "item/steam_boots.png"))
    made.append(save(charger_top(), "block/steam_charger_top.png"))
    made.append(save(charger_side(), "block/steam_charger_side.png"))
    made.append(save(charger_bottom(), "block/steam_charger_bottom.png"))

# ---- M7 보관 · 제작 ----
IRON = {"a": "#3a3a3a", "b": "#6b6b6b", "c": "#9a9a9a", "d": "#c4c4c4", "e": "#ececec"}
WOOD = {"a": "#3b2814", "b": "#5c3f24", "c": "#7a5634", "d": "#9c7448", "e": "#c49a64"}


def drawer_front(steel):
    """서랍 앞면: 테두리 안쪽에 어두운 칸 (아이템 · 개수가 그려질 자리)과 손잡이."""
    im = panel(seed=701 if steel else 702)
    if not steel:
        im = recolor(im, IRON)
    for y in range(2, 14):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (2, 13)
            im.putpixel((x, y), px("a") if edge else px("m" if steel else "w"))
    for x in range(6, 10):
        im.putpixel((x, 3), px("d"))
    return im


def drawer_side(steel):
    im = panel(seed=703 if steel else 704)
    if not steel:
        im = recolor(im, IRON)
    for x in range(1, 15):
        im.putpixel((x, 7), px("b"))
        im.putpixel((x, 8), px("d"))
    return im


def drawer_upgrade_icon(void):
    """서랍 업그레이드: 강철판 위에 위 화살표 (넘침 버림은 검은 구멍)."""
    im = Image.new("RGBA", (16, 16))
    for y in range(3, 13):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (3, 12)
            im.putpixel((x, y), px("a") if edge else px("c" if (x + y) % 5 else "d"))
    if void:
        for y in range(5, 11):
            for x in range(5, 11):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 3.0:
                    im.putpixel((x, y), (20, 10, 30, 255) if d < 2.0 else px("R"))
    else:
        for y in range(5, 11):
            im.putpixel((7, y), px("u"))
            im.putpixel((8, y), px("u"))
        for i in range(3):
            im.putpixel((7 - i, 5 + i), px("t"))
            im.putpixel((8 + i, 5 + i), px("t"))
    return im


def cable_tex():
    """창고 관: 어두운 철 관에 초록 신호 줄."""
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, "abb", 711)
    for x in range(16):
        im.putpixel((x, 7), (60, 170, 90, 255))
        im.putpixel((x, 8), (40, 120, 60, 255))
    return im


def terminal_front(crafting):
    """창고 단말 앞면: 초록 화면 (제작 단말은 화면 아래에 3x3 칸)."""
    im = panel(seed=721 if crafting else 722)
    for y in range(2, 10 if crafting else 13):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (2, 9 if crafting else 12)
            im.putpixel((x, y), px("a") if edge else (25, 60 + ((x * 7 + y * 3) % 3) * 12, 35, 255))
    for x in (4, 6, 8, 10):
        im.putpixel((x, 4), (110, 230, 140, 255))
    for x in (4, 5, 7, 9, 11):
        im.putpixel((x, 6), (80, 200, 110, 255))
    if crafting:
        for gy in range(3):
            for gx in range(3):
                x0, y0 = 4 + gx * 3, 10 + gy * 2
                im.putpixel((x0, y0), recolor_px(WOOD["d"]))
                im.putpixel((x0 + 1, y0), recolor_px(WOOD["c"]))
    return im


def recolor_px(color):
    return hex_px(color)


def terminal_top(crafting):
    im = panel(seed=723)
    if crafting:
        for y in range(3, 13):
            for x in range(3, 13):
                im.putpixel((x, y), hex_px(WOOD["b" if (x in (3, 12) or y in (3, 12)) else ("c" if (x // 3 + y // 3) % 2 else "d")]))
    else:
        for x in range(3, 13):
            im.putpixel((x, 7), (60, 170, 90, 255))
    return im


def autocrafter_front():
    """자동 제작대 앞면: 3x3 나무 칸과 톱니."""
    im = panel(seed=731)
    for y in range(2, 14):
        for x in range(2, 14):
            if x in (2, 13) or y in (2, 13):
                im.putpixel((x, y), px("a"))
            else:
                cell = (x - 3) % 4 == 3 or (y - 3) % 4 == 3
                im.putpixel((x, y), hex_px(WOOD["a"] if cell else WOOD["c" if (x + y) % 3 else "d"]))
    return im


def autocrafter_top():
    im = panel(seed=732)
    gear = cog()
    for y in range(16):
        for x in range(16):
            p = gear.getpixel((x, y))
            if p[3]:
                im.putpixel((x, y), p)
    return im


def stocker_front():
    """재고 유지기 앞면: 눈금 막대 셋 (빨강 · 노랑 · 초록)과 파수꾼 심장 빛."""
    im = panel(seed=741)
    for y in range(2, 14):
        for x in range(2, 14):
            if x in (2, 13) or y in (2, 13):
                im.putpixel((x, y), px("a"))
            else:
                im.putpixel((x, y), px("m"))
    bars = [(4, 9, (200, 60, 60, 255)), (7, 6, (220, 190, 60, 255)), (10, 4, (80, 200, 100, 255))]
    for x0, top, color in bars:
        for y in range(top, 12):
            im.putpixel((x0, y), color)
            im.putpixel((x0 + 1, y), color)
    im.putpixel((7, 3), px("Q"))
    im.putpixel((8, 3), px("Q"))
    return im


def portable_icon(ender):
    """휴대 단말: 손에 드는 판 + 초록 화면 (셜커판은 보라 테두리)."""
    im = Image.new("RGBA", (16, 16))
    rim = (120, 70, 140, 255) if ender else px("b")
    for y in range(1, 15):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (1, 14)
            im.putpixel((x, y), px("a") if edge else rim)
    for y in range(3, 10):
        for x in range(5, 11):
            im.putpixel((x, y), (25, 70 + ((x + y) % 2) * 15, 40, 255))
    for x in (6, 8):
        im.putpixel((x, 5), (110, 230, 140, 255))
    for x in range(5, 11, 2):
        im.putpixel((x, 12), px("d"))
    return im


def main_m7(made):
    for steel in (False, True):
        name = "steel_drawer" if steel else "iron_drawer"
        made.append(save(drawer_front(steel), f"block/{name}_front.png"))
        made.append(save(drawer_side(steel), f"block/{name}_side.png"))
    made.append(save(drawer_upgrade_icon(False), "item/drawer_upgrade.png"))
    made.append(save(drawer_upgrade_icon(True), "item/drawer_void_upgrade.png"))
    made.append(save(cable_tex(), "block/storage_cable.png"))
    made.append(save(terminal_front(False), "block/storage_terminal_front.png"))
    made.append(save(terminal_front(True), "block/crafting_terminal_front.png"))
    made.append(save(terminal_top(False), "block/storage_terminal_top.png"))
    made.append(save(terminal_top(True), "block/crafting_terminal_top.png"))
    made.append(save(autocrafter_front(), "block/autocrafter_front.png"))
    made.append(save(autocrafter_top(), "block/autocrafter_top.png"))
    made.append(save(stocker_front(), "block/stocker_front.png"))
    made.append(save(portable_icon(False), "item/portable_terminal.png"))
    made.append(save(portable_icon(True), "item/ender_portable_terminal.png"))


# ---------- 묶음 4: 기계 업그레이드 · 펌프 · 탱크 ----------
UPGRADE_CARD = [
    "................",
    "..aaaaaaaaaaaa..",
    ".abddddddddddba.",
    ".adccccccccccda.",
    ".adccccccccccda.",
    ".adccccccccccda.",
    ".adccccccccccda.",
    ".adccccccccccda.",
    ".adccccccccccda.",
    ".adccccccccccda.",
    ".adccccccccccda.",
    ".abddddddddddba.",
    "..aaaaaaaaaaaa..",
    "...t.t.t.t.t....",
    "...s.s.s.s.s....",
    "................",
]


def upgrade_icon(symbol, color, light):
    """업그레이드: 강철 회로판(아래 구리 다리) 위에 기호 하나."""
    im = from_art(UPGRADE_CARD)
    c = tuple(int(color[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
    l = tuple(int(light[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
    for y, row in enumerate(symbol):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((3 + x, 3 + y), l if ch == "L" else c)
    return im


SPEED_SYMBOL = [    # 겹 화살 >>
    "..........",
    ".C...C....",
    ".LC..LC...",
    "..LC..LC..",
    "...LC..LC.",
    "..LC..LC..",
    ".LC..LC...",
    ".C...C....",
]
EFFICIENCY_SYMBOL = [   # 잎 (아끼기)
    "......CC..",
    "....CCLC..",
    "...CLLC...",
    "..CLLCC...",
    "..CLCC....",
    ".CCC......",
    ".C........",
    "C.........",
]
DOUBLE_SYMBOL = [   # x2
    "..........",
    "......LCC.",
    "C...C....C",
    ".C.C.....C",
    "..C.....C.",
    ".C.C...C..",
    "C...C.CCCC",
    "..........",
]


def pump_side():
    """펌프 옆면: 철판에 가운데 하늘색 관과 띠."""
    im = panel(seed=401)
    for y in range(1, 15):
        for x in (6, 7, 8, 9):
            im.putpixel((x, y), px("k") if x in (6, 9) else px("g"))
    for x in range(1, 15):
        im.putpixel((x, 4), px("a"))
        im.putpixel((x, 11), px("a"))
    for x in (7, 8):
        im.putpixel((x, 7), px("h"))
    return im


def pump_top():
    """펌프 윗면: 톱니 모양 모터 덮개."""
    im = panel(seed=402)
    for y in range(3, 13):
        for x in range(3, 13):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 4.8:
                im.putpixel((x, y), px("a") if d > 3.8 else px("b") if d > 1.6 else px("t"))
    return im


def pump_bottom():
    """펌프 밑면: 빨아들이는 구멍 (격자)."""
    im = panel(seed=403)
    for y in range(3, 13):
        for x in range(3, 13):
            im.putpixel((x, y), px("m") if (x + y) % 3 else px("a"))
    for y in range(3, 13):
        im.putpixel((3, y), px("a"))
        im.putpixel((12, y), px("a"))
    return im


def tank_side():
    """탱크 옆면: 철판 테두리에 세로 유리창 (안은 어두운 물빛)."""
    im = panel(seed=411)
    for y in range(2, 14):
        for x in range(4, 12):
            edge = x in (4, 11) or y in (2, 13)
            im.putpixel((x, y), px("a") if edge else (px("h") if x == 5 and y < 9 else px("k")))
    for y in (5, 8, 11):
        im.putpixel((10, y), px("e"))
    return im


def tank_top():
    """탱크 윗면: 둥근 뚜껑과 손잡이."""
    im = panel(seed=412)
    for y in range(2, 14):
        for x in range(2, 14):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 5.8:
                im.putpixel((x, y), px("a") if d > 4.9 else px("d") if d > 4.0 else px("c"))
    for x in range(6, 10):
        im.putpixel((x, 7), px("b"))
        im.putpixel((x, 8), px("e"))
    return im


def main_bundle4(made):
    made.append(save(upgrade_icon(SPEED_SYMBOL, "#de8442", "#ffc27a"), "item/speed_upgrade.png"))
    made.append(save(upgrade_icon(EFFICIENCY_SYMBOL, "#3f9a3a", "#9fe08a"), "item/efficiency_upgrade.png"))
    made.append(save(upgrade_icon(DOUBLE_SYMBOL, "#8a3fc4", "#d6a6ff"), "item/double_upgrade.png"))
    made.append(save(pump_side(), "block/pump_side.png"))
    made.append(save(pump_top(), "block/pump_top.png"))
    made.append(save(pump_bottom(), "block/pump_bottom.png"))
    made.append(save(tank_side(), "block/tank_side.png"))
    made.append(save(tank_top(), "block/tank_top.png"))
BOARD_GOLD = {"a": "#4a3306", "b": "#8a6410", "c": "#c9962a", "d": "#f0c64a", "e": "#fff1a8"}


def fill_rect(im, x0, y0, x1, y1, color):
    """(x0,y0)~(x1,y1) 포함 범위를 #rrggbb 한 색으로 칠한다."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), hex_px(color))


def boss_board_side():
    """보스 기록판 옆면: 금빛 판에 시상대 (2위 · 1위 · 3위)."""
    im = recolor(panel(seed=201), BOARD_GOLD)
    for x0, x1, top, color in ((2, 5, 8, "#9aa4b0"), (6, 9, 5, "#f6d85a"), (10, 13, 10, "#b8733c")):
        fill_rect(im, x0, top, x1, 13, color)
        fill_rect(im, x0, top, x1, top, "#ffffff")
        fill_rect(im, x0, 13, x1, 13, "#3a2a10")
    for x, y in ((7, 3), (8, 3), (6, 4), (7, 4), (8, 4), (9, 4)):
        im.putpixel((x, y), hex_px("#fff1a8"))
    return im


def boss_board_top():
    im = recolor(panel(seed=202), BOARD_GOLD)
    for d in range(-3, 4):
        im.putpixel((8 + d, 8), hex_px("#fff1a8"))
        im.putpixel((8, 8 + d), hex_px("#fff1a8"))
    return im


def goal_board_side():
    """공용 목표판 옆면: 강철 판에 깃발과 진행 막대 (절반 참)."""
    im = recolor(panel(seed=203), STEEL)
    fill_rect(im, 5, 2, 5, 8, "#e8edf5")
    fill_rect(im, 6, 2, 10, 4, "#e04848")
    fill_rect(im, 2, 10, 13, 13, "#1e2633")
    fill_rect(im, 3, 11, 12, 12, "#3c4a5e")
    fill_rect(im, 3, 11, 8, 12, "#3ac24b")
    return im


def goal_board_top():
    im = recolor(panel(seed=204), STEEL)
    fill_rect(im, 4, 7, 11, 8, "#3ac24b")
    fill_rect(im, 7, 4, 8, 11, "#3ac24b")
    return im


def main_m10(made):
    made.append(save(boss_board_side(), "block/boss_record_board_side.png"))
    made.append(save(boss_board_top(), "block/boss_record_board_top.png"))
    made.append(save(goal_board_side(), "block/goal_board_side.png"))
    made.append(save(goal_board_top(), "block/goal_board_top.png"))
M9A_COLORS = {"G": "#3f9b3a", "F": "#8fe063", "Y": "#e8c040"}


def m9a_px(ch):
    c = M9A_COLORS.get(ch)
    if c is None:
        return px(ch)
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def m9a_put(im, x, y, ch):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), m9a_px(ch))


def m9a_rect(im, x0, y0, x1, y1, ch):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            m9a_put(im, x, y, ch)


def m9a_line(im, x0, y0, x1, y1, ch, width=1):
    steps = max(abs(x1 - x0), abs(y1 - y0), 1)
    for i in range(steps + 1):
        x = round(x0 + (x1 - x0) * i / steps)
        y = round(y0 + (y1 - y0) * i / steps)
        for dx in range(width):
            for dy in range(width):
                m9a_put(im, x + dx, y + dy, ch)


def harvester_front():
    """작물 수확기 앞면: 어두운 창 안에 푸른 새싹과 낫 날."""
    im = panel(seed=401)
    m9a_rect(im, 3, 3, 12, 11, "a")
    m9a_rect(im, 4, 4, 11, 10, "m")
    # 새싹
    m9a_line(im, 7, 10, 7, 7, "G", 2)
    m9a_rect(im, 4, 6, 6, 7, "F")
    m9a_rect(im, 9, 5, 11, 6, "F")
    m9a_put(im, 5, 6, "G")
    m9a_put(im, 10, 5, "G")
    # 낫 날 (위쪽 둥근 모양)
    for x in range(4, 12):
        m9a_put(im, x, 4, "d")
    m9a_put(im, 4, 5, "c")
    m9a_put(im, 11, 5, "c")
    # 증기 눈금
    m9a_rect(im, 4, 13, 11, 13, "k")
    for x in (5, 7, 9):
        m9a_put(im, x, 13, "g")
    return im


def harvester_top():
    im = panel(seed=402)
    m9a_rect(im, 4, 4, 11, 11, "a")
    m9a_rect(im, 5, 5, 10, 10, "m")
    for i in range(5, 11):
        m9a_put(im, i, 7, "n")
        m9a_put(im, i, 8, "n")
    return im


def feller_front():
    """벌목기 앞면: 어두운 창 안에 톱날 원."""
    im = panel(seed=411)
    m9a_rect(im, 3, 3, 12, 12, "a")
    m9a_rect(im, 4, 4, 11, 11, "m")
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 4 <= x <= 11 and 4 <= y <= 11:
                if d < 1.6:
                    m9a_put(im, x, y, "a")
                elif d < 3.3:
                    m9a_put(im, x, y, "d")
                elif d < 4.2 and (x + y) % 2 == 0:
                    m9a_put(im, x, y, "c")
    m9a_put(im, 7, 7, "t")
    m9a_put(im, 8, 8, "t")
    m9a_rect(im, 4, 13, 11, 13, "k")
    for x in (5, 7, 9):
        m9a_put(im, x, 13, "g")
    return im


def feller_top():
    im = panel(seed=412)
    m9a_rect(im, 4, 4, 11, 11, "a")
    m9a_rect(im, 5, 5, 10, 10, "w")
    for i in range(5, 11):
        m9a_put(im, i, 7, "x")
        m9a_put(im, i, 8, "x")
    return im


def jetpack_icon():
    """증기 제트팩: 구리 띠를 두른 통 두 개와 아래 분사구."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x0 in (2, 9):
        m9a_rect(im, x0, 2, x0 + 4, 11, "b")
        m9a_rect(im, x0, 2, x0, 11, "d")
        m9a_rect(im, x0 + 4, 2, x0 + 4, 11, "a")
        m9a_rect(im, x0 + 1, 1, x0 + 3, 1, "c")
        m9a_rect(im, x0 + 1, 2, x0 + 1, 10, "c")
        m9a_put(im, x0 + 2, 1, "g")
        m9a_rect(im, x0, 4, x0 + 4, 4, "t")
        m9a_rect(im, x0, 9, x0 + 4, 9, "s")
        m9a_rect(im, x0 + 1, 12, x0 + 3, 13, "a")
        m9a_put(im, x0 + 2, 14, "h")
    m9a_rect(im, 7, 5, 8, 7, "a")
    m9a_rect(im, 5, 6, 10, 6, "a")
    return im


def drill_icon():
    """증기 드릴: 오른쪽 위를 향한 나선 드릴 날, 구리 손잡이."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    m9a_line(im, 2, 14, 7, 9, "r", 3)
    m9a_line(im, 2, 14, 6, 10, "t", 2)
    m9a_line(im, 7, 9, 10, 6, "c", 3)
    m9a_line(im, 8, 8, 10, 6, "d", 1)
    m9a_put(im, 6, 8, "g")
    m9a_put(im, 7, 7, "h")
    # 나선 날
    m9a_line(im, 10, 6, 13, 3, "b", 2)
    m9a_put(im, 14, 2, "e")
    m9a_put(im, 13, 2, "d")
    m9a_put(im, 14, 3, "d")
    for px_, py_ in ((11, 6), (12, 4), (13, 5)):
        m9a_put(im, px_, py_, "a")
    return im


def chainsaw_icon():
    """전기톱: 주황 몸통과 손잡이, 이빨이 달린 긴 날."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    m9a_rect(im, 1, 7, 7, 12, "s")
    m9a_rect(im, 1, 7, 7, 7, "u")
    m9a_rect(im, 1, 12, 7, 12, "r")
    m9a_rect(im, 1, 7, 1, 12, "t")
    m9a_rect(im, 2, 3, 5, 5, "a")
    m9a_rect(im, 3, 4, 4, 4, "m")
    m9a_rect(im, 3, 5, 3, 7, "a")
    m9a_rect(im, 5, 9, 6, 10, "g")
    # 날
    m9a_line(im, 8, 9, 14, 4, "c", 2)
    m9a_line(im, 8, 8, 14, 3, "d", 1)
    for i in range(0, 6):
        x = 8 + i
        y = 10 - round(i * 0.83)
        if i % 2 == 0:
            m9a_put(im, x, y + 1, "a")
    m9a_put(im, 15, 3, "b")
    return im


def jetpack_layer():
    """제트팩이 몸통에 입혀지는 모양: 강철 몸통 위에 등에 통 두 개, 앞에 구리 띠."""
    im = armor_layer(False)
    # 앞면(20..27, 20..31)에 구리 띠
    m9a_rect(im, 20, 26, 27, 27, "t")
    m9a_rect(im, 20, 27, 27, 27, "s")
    # 등판(32..39, 20..31)에 통 두 개
    for x0 in (33, 37):
        m9a_rect(im, x0, 21, x0 + 1, 30, "b")
        m9a_rect(im, x0, 21, x0, 30, "d")
        m9a_rect(im, x0 + 1, 21, x0 + 1, 30, "a")
        m9a_rect(im, x0, 23, x0 + 1, 23, "t")
        m9a_rect(im, x0, 27, x0 + 1, 27, "s")
        m9a_put(im, x0, 21, "g")
    return im


def main_m9a(made):
    made.append(save(harvester_front(), "block/crop_harvester_front.png"))
    made.append(save(harvester_top(), "block/crop_harvester_top.png"))
    made.append(save(feller_front(), "block/tree_feller_front.png"))
    made.append(save(feller_top(), "block/tree_feller_top.png"))
    made.append(save(jetpack_icon(), "item/steam_jetpack.png"))
    made.append(save(drill_icon(), "item/steam_drill.png"))
    made.append(save(chainsaw_icon(), "item/chainsaw.png"))
    made.append(save(jetpack_layer(), "entity/equipment/humanoid/steam_jetpack.png"))


# ---- M9 part B: 몹 분쇄기 · 경험치 탱크 · 배낭 ----
XP_GREEN = {"k": "#145a2a", "g": "#3ae05a", "h": "#c8ffa6"}


def grinder_side():
    """몹 분쇄기 옆면: 강철 판에 가운데 톱날 틈과 이빨."""
    im = panel(seed=601)
    fill_rect(im, 3, 6, 12, 9, "#14181d")
    for x in range(3, 13, 2):
        fill_rect(im, x, 5, x, 5, "#eaf0f5")
        fill_rect(im, x, 10, x, 10, "#eaf0f5")
    fill_rect(im, 3, 7, 12, 7, "#5f6873")
    return im


def grinder_top():
    """몹 분쇄기 윗면: 둥근 톱날 (이빨 8개)."""
    im = panel(seed=602)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            ang = math.atan2(y - 7.5, x - 7.5)
            tooth = 6.4 if int(round(ang / (math.pi / 4))) % 2 == 0 else 5.2
            if d < tooth:
                im.putpixel((x, y), px("e") if d > 4.4 else px("d") if d > 2.6 else px("b"))
    for y in range(6, 10):
        for x in range(6, 10):
            im.putpixel((x, y), px("a"))
    return im


def grinder_bottom():
    return panel(seed=603)


def xp_tank_side():
    """경험치 탱크 옆면: 철판 테두리에 초록 유리창."""
    im = panel(seed=611)
    for y in range(2, 14):
        for x in range(4, 12):
            edge = x in (4, 11) or y in (2, 13)
            if edge:
                im.putpixel((x, y), px("a"))
            else:
                im.putpixel((x, y), hex_px(XP_GREEN["h"]) if x == 5 and y < 9 else hex_px(XP_GREEN["g"]) if y > 6 else hex_px(XP_GREEN["k"]))
    for y in (5, 8, 11):
        im.putpixel((10, y), px("e"))
    return im


def xp_tank_top():
    """경험치 탱크 윗면: 둥근 뚜껑 가운데에 초록 구슬."""
    im = tank_top()
    for y in range(6, 10):
        for x in range(6, 10):
            if math.hypot(x - 7.5, y - 7.5) < 2.2:
                im.putpixel((x, y), hex_px(XP_GREEN["g"]))
    im.putpixel((7, 7), hex_px(XP_GREEN["h"]))
    return im


def art_hex(rows, legend):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    im = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), hex_px(legend[ch]))
    return im


BACKPACK_ART = [
    "................",
    ".....aaaaaa.....",
    "....abbbbbba....",
    "...abccccccba...",
    "..abccccccccba..",
    "..abcdddddddba..",
    "..abcdeeeeedba..",
    "..abcdeeeeedba..",
    "..abcdddddddba..",
    "..abccccccccba..",
    "..abcfffffcccba.",
    "..abcfgggfcccba.",
    "..abcfffffccba..",
    "..abccccccccba..",
    "...abbbbbbbba...",
    "....aaaaaaaa....",
]
BACKPACK_LEATHER = {"a": "#2e1a0c", "b": "#5c3a1e", "c": "#8a5a30", "d": "#6b4423", "e": "#b5844e", "f": "#2e1a0c", "g": "#d9b45a"}
BACKPACK_STEEL = {"a": "#1e2633", "b": "#3c4a5e", "c": "#5f708a", "d": "#8a9bb5", "e": "#c5d2e6", "f": "#1e2633", "g": "#f0c64a"}


def main_m9b(made):
    made.append(save(grinder_side(), "block/mob_grinder_side.png"))
    made.append(save(grinder_top(), "block/mob_grinder_top.png"))
    made.append(save(grinder_bottom(), "block/mob_grinder_bottom.png"))
    made.append(save(xp_tank_side(), "block/experience_tank_side.png"))
    made.append(save(xp_tank_top(), "block/experience_tank_top.png"))
    made.append(save(art_hex(BACKPACK_ART, BACKPACK_LEATHER), "item/backpack.png"))
    made.append(save(art_hex(BACKPACK_ART, BACKPACK_STEEL), "item/steel_backpack.png"))


# ---------- M8 묶음 A: 대형 보일러 · 증기 기관차 ----------
DARK_STEEL = {"a": "#1d2126", "b": "#3a414a", "c": "#545d68", "d": "#717b87", "e": "#9aa4b0"}


def big_casing(formed):
    """대형 보일러 외벽: 어두운 강철판에 리벳 띠. 모양이 맞으면 가운데 띠가 달아오른 구리색."""
    im = recolor(panel(seed=501), DARK_STEEL)
    for y in (5, 10):
        for x in range(16):
            im.putpixel((x, y), px("a"))
    for x in range(1, 15, 3):
        im.putpixel((x, 4), px("e"))
        im.putpixel((x, 11), px("e"))
    for y in range(6, 10):
        for x in range(16):
            if formed:
                im.putpixel((x, y), px("t" if (x + y) % 4 else "u"))
            else:
                im.putpixel((x, y), px("b" if (x + y) % 4 else "c"))
    return im


def big_controller_front(lit):
    """대형 보일러 제어기 앞면: 위에 압력계, 아래에 큰 화구."""
    im = recolor(panel(seed=502), DARK_STEEL)
    for y in range(2, 7):
        for x in range(5, 11):
            d = math.hypot(x - 7.5, y - 4.0)
            if d < 2.9:
                im.putpixel((x, y), px("a") if d > 2.1 else px("e"))
    im.putpixel((8, 3), px("R"))
    im.putpixel((7, 4), px("a"))
    for y in range(8, 15):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (8, 14)
            if edge:
                im.putpixel((x, y), px("a"))
            elif lit:
                im.putpixel((x, y), px("u" if (x * 3 + y) % 4 == 0 else "t" if (x + y) % 2 else "s"))
            else:
                im.putpixel((x, y), px("m" if (x + y) % 2 else "a"))
    for x in range(4, 12, 2):
        im.putpixel((x, 9), px("b"))
    return im


def station_side():
    """역 옆면: 강철판에 상자 무늬와 노란 띠."""
    im = panel(seed=511)
    for y in range(4, 13):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (4, 12)
            im.putpixel((x, y), px("w") if edge else px("x" if (y % 3) else "w"))
    for x in range(16):
        im.putpixel((x, 1), px("u" if (x // 2) % 2 else "a"))
    return im


def station_top(unload):
    """역 윗면: 화살표 (적재 = 바깥으로 초록, 하역 = 안으로 주황)."""
    im = panel(seed=512)
    color = "t" if unload else "g"
    arrow = [
        "................",
        "................",
        ".......XX.......",
        "......XXXX......",
        ".....XXXXXX.....",
        "....XXXXXXXX....",
        ".......XX.......",
        ".......XX.......",
        ".......XX.......",
        ".......XX.......",
        ".......XX.......",
        ".......XX.......",
        "................",
        "................",
        "................",
        "................",
    ]
    for y, row in enumerate(arrow):
        for x, ch in enumerate(row):
            if ch == "X":
                yy = 15 - y if unload else y
                im.putpixel((x, yy), px(color))
    return im


LOCOMOTIVE = [
    "................",
    "...aa...........",
    "...ab...........",
    "..aaaa..........",
    "..abba.aaaaaa...",
    "..abbaaadddcca..",
    ".aaccccccccccba.",
    ".acdddddcttucba.",
    ".accccccctssbba.",
    ".aaaaaaaaaaaaaa.",
    "..abba....abba..",
    ".abeeba..abeeba.",
    ".abeeba..abeeba.",
    "..abba....abba..",
    "................",
    "................",
]

CARGO_WAGON = [
    "................",
    "................",
    "................",
    "...wwwwwwwwww...",
    "...wxxxxxxxxw...",
    "...wxwwxxwwxw...",
    "...wxxxxxxxxw...",
    ".aaaaaaaaaaaaaa.",
    ".acddddddddddca.",
    ".abccccccccccba.",
    ".aaaaaaaaaaaaaa.",
    "..abba....abba..",
    ".abeeba..abeeba.",
    ".abeeba..abeeba.",
    "..abba....abba..",
    "................",
]

COUPLER = [
    "................",
    "................",
    "..........aaa...",
    ".........acdca..",
    ".........ad.da..",
    ".........acdca..",
    "........aaaaa...",
    ".......acda.....",
    "......acda......",
    ".....acda.......",
    "...aaaaa........",
    "..acdca.........",
    "..ad.da.........",
    "..acdca.........",
    "...aaa..........",
    "................",
]


def main_m8a(made):
    made.append(save(big_casing(False), "block/big_boiler_casing.png"))
    made.append(save(big_casing(True), "block/big_boiler_casing_formed.png"))
    made.append(save(big_controller_front(False), "block/big_boiler_controller_front.png"))
    made.append(save(big_controller_front(True), "block/big_boiler_controller_front_on.png"))
    made.append(save(station_side(), "block/cargo_station_side.png"))
    made.append(save(station_top(False), "block/cargo_station_top_load.png"))
    made.append(save(station_top(True), "block/cargo_station_top_unload.png"))
    made.append(save(from_art(LOCOMOTIVE), "item/steam_locomotive.png"))
    made.append(save(from_art(CARGO_WAGON), "item/cargo_wagon.png"))
    made.append(save(from_art(COUPLER), "item/coupler.png"))


# ---------- M5 네더 심화 2 ----------
SOUL = {"a": "#120d0b", "b": "#2e221c", "c": "#46362c", "d": "#5e4b3d", "e": "#7d6754"}
HP_STEEL = {"a": "#15171b", "b": "#2b2f36", "c": "#3d424b", "d": "#555c66", "e": "#7a838f"}
GOLDEN = {"a": "#5a3b06", "b": "#9a6a10", "c": "#d6a21e", "d": "#f2cf4a", "e": "#fff3a0"}

GILDED_QUARTZ_ICON = [
    "................",
    "................",
    "......eeee......",
    ".....eddddc.....",
    "....edduddcc....",
    "...eddudddccc...",
    "...edddddccbc...",
    "...eddddcccbc...",
    "...ddddcccbbc...",
    "...dddcccbbbc...",
    "....ddccbbbb....",
    ".....ccbbbb.....",
    "......cbbb......",
    "................",
    "................",
    "................",
]

PRECISION_PART_ICON = [
    "................",
    "................",
    "...aaaaaaaaaa...",
    "..adeeeeeeeeca..",
    "..aedgggggdbca..",
    "..aedgkkkgdbca..",
    "..aedgkhkgdbca..",
    "..aedgkkkgdbca..",
    "..aedgggggdbca..",
    "..aeddtddtdbca..",
    "..aedtutdutbca..",
    "..aeddtddtdbca..",
    "..abbbbbbbbbba..",
    "...aaaaaaaaaa...",
    "................",
    "................",
]

DEBRIS_SONAR_ICON = [
    "................",
    "........h.......",
    ".......hgh......",
    "........g.......",
    "......aaaaa.....",
    ".....abbbbba....",
    "....abkkkkkba...",
    "....abkghgkba...",
    "....abkhRhkba...",
    "....abkghgkba...",
    "....abkkkkkba...",
    ".....abbbbba....",
    "......acdca.....",
    "......acdca.....",
    "......aaaaa.....",
    "................",
]

LAVA_STEAMBOAT_ICON = [
    "................",
    "................",
    "................",
    "........mm......",
    "........mn......",
    "........mn......",
    "...a....mn....a.",
    "..ab..aaaaaa.ba.",
    "..abaaccccccaba.",
    "..abccddddddcba.",
    "...abcdddddcba..",
    "....abbbbbbbba..",
    ".....aaaaaaaa...",
    "...ttsstussts...",
    "..sttutsstutts..",
    "................",
]


def soul_face(part, lit=False):
    im = recolor(panel(seed={"front": 501, "side": 502, "top": 503}[part]), SOUL)
    flame = ("#5ef2ff", "#1fb8c9", "#0d6b78") if lit else ("#2a2420", "#1f1a17", "#171310")
    if part == "front":
        for y in range(6, 14):
            for x in range(3, 13):
                im.putpixel((x, y), px("a"))
        rnd = random.Random(504)
        for y in range(8, 13):
            for x in range(4, 12):
                k = rnd.random() + (12 - y) * 0.12
                im.putpixel((x, y), hex_px(flame[0] if k > 1.0 else flame[1] if k > 0.6 else flame[2]))
        for x in range(3, 13):
            im.putpixel((x, 5), hex_px("#7d6754"))
    elif part == "top":
        for y in range(4, 12):
            for x in range(4, 12):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 3.6:
                    im.putpixel((x, y), px("a") if d > 2.6 else hex_px("#1fb8c9"))
    else:
        for x in range(16):
            im.putpixel((x, 12), hex_px("#2e8a96"))
    return im


def hp_pipe_texture():
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, "ccd", 511)
    im = recolor(im, HP_STEEL)
    for i in range(16):
        im.putpixel((i, 0), hex_px(HP_STEEL["a"]))
        im.putpixel((i, 15), hex_px(HP_STEEL["a"]))
    for x in (3, 12):
        for y in range(16):
            im.putpixel((x, y), hex_px("#b3262a"))
    for y in (4, 11):
        for x in range(4, 12):
            im.putpixel((x, y), hex_px(HP_STEEL["e"]))
    return im


def precision_front(lit):
    im = recolor(panel(seed=521), HP_STEEL)
    for y in range(3, 13):
        for x in range(3, 13):
            im.putpixel((x, y), px("a"))
    # 가는 집게 팔 + 십자 조준선
    for x, y in ((5, 4), (5, 5), (6, 6), (10, 4), (10, 5), (9, 6)):
        im.putpixel((x, y), hex_px("#d6a21e"))
    line = hex_px("#2fb8ef" if lit else "#1b3a48")
    for i in range(5, 11):
        im.putpixel((i, 9), line)
        im.putpixel((7, i + 1), line)
    for x in range(4, 12):
        im.putpixel((x, 12), hex_px("#b3262a"))
    return im


def precision_top():
    im = recolor(panel(seed=522), HP_STEEL)
    for y in range(5, 11):
        for x in range(5, 11):
            im.putpixel((x, y), px("a" if x in (5, 10) or y in (5, 10) else "b"))
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        im.putpixel((x, y), hex_px("#d6a21e"))
    return im


def turbine_side():
    im = recolor(panel(seed=531), HP_STEEL)
    for y in range(2, 14):
        im.putpixel((7, y), hex_px("#b3262a"))
        im.putpixel((8, y), hex_px("#b3262a"))
    for x in range(2, 14):
        im.putpixel((x, 4), px("a"))
        im.putpixel((x, 11), px("a"))
    for x in (3, 4, 11, 12):
        for y in range(5, 11):
            im.putpixel((x, y), hex_px(COPPER["c"]))
    return im


def turbine_top(on):
    im = recolor(panel(seed=532), HP_STEEL)
    blade = hex_px("#d9e2ea" if on else "#7a838f")
    for y in range(2, 14):
        for x in range(2, 14):
            d = math.hypot(x - 7.5, y - 7.5)
            if 5.0 < d < 6.0:
                im.putpixel((x, y), px("a"))
    # 날개 4장 (켜지면 45도 돌린 모양)
    for t in range(1, 6):
        if on:
            pts = ((7 + t, 7 + t), (8 - t, 8 - t), (8 - t, 7 + t), (7 + t, 8 - t))
        else:
            pts = ((7 + t, 7), (8 - t, 8), (8, 7 + t), (7, 8 - t))
        for x, y in pts:
            if 0 <= x < 16 and 0 <= y < 16:
                im.putpixel((x, y), blade)
    im.putpixel((7, 7), px("a"))
    im.putpixel((8, 8), px("a"))
    return im


def gold_dispenser_face(part):
    im = recolor(panel(seed={"top": 541, "side": 542, "bottom": 543}[part]), GOLDEN if part == "top" else COPPER)
    if part == "top":
        for y in range(5, 11):
            for x in range(5, 11):
                im.putpixel((x, y), px("a") if x in (5, 10) or y in (5, 10) else hex_px("#2a1a05"))
    elif part == "side":
        for y in range(5, 11):
            for x in range(4, 12):
                im.putpixel((x, y), hex_px(GOLDEN["c" if (x + y) % 3 else "e"]))
        for x in range(4, 12):
            im.putpixel((x, 11), hex_px(GOLDEN["a"]))
    return im


def collector_face(part):
    im = recolor(panel(seed={"top": 551, "side": 552, "bottom": 553}[part]), COPPER if part == "top" else NETHER)
    if part == "top":
        for y in range(2, 14):
            for x in range(2, 14):
                if x % 3 == 0 or y % 3 == 0:
                    im.putpixel((x, y), px("a"))
        for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
            im.putpixel((x, y), hex_px("#2fd3b4"))
    elif part == "side":
        for x in range(2, 14):
            im.putpixel((x, 3), hex_px(COPPER["d"]))
        for y in range(6, 12):
            for x in range(5, 11):
                im.putpixel((x, y), px("a") if y in (6, 11) or x in (5, 10) else hex_px("#3a1f12"))
    return im


def boat_texture():
    """용암 증기선 텍스처 (128x64, 바닐라 배 모델 BoatModel의 UV 배치). 판은 짙은 강철 + 리벳, 앞뒤는 네더 벽돌색, 노는 구리."""
    im = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    rnd = random.Random(561)

    def box(u, v, w, h, d, colors, rivets=True):
        # 상자 전개도: 윗줄 높이 d (위 · 아래 면), 아랫줄 높이 h (옆면 넷), 폭 2 * (w + d)
        for y in range(v, v + d + h):
            for x in range(u, u + 2 * (w + d)):
                if y < v + d and not (u + d <= x < u + d + 2 * w):
                    continue
                im.putpixel((x, y), hex_px(rnd.choice(colors)))
        if rivets:
            for x in range(u + 1, u + 2 * (w + d) - 1, 4):
                yy = v + d + 1
                if yy < v + d + h:
                    im.putpixel((x, yy), hex_px("#c9d2da"))

    plate = ["#3d424b", "#3d424b", "#454b55", "#353a42"]
    rim = ["#5a1e22", "#6e2a2c", "#4a181b"]
    box(0, 0, 28, 16, 3, plate)          # 바닥
    box(0, 19, 18, 6, 2, rim)            # 뒤
    box(0, 27, 16, 6, 2, rim)            # 앞
    box(0, 35, 28, 6, 2, plate)          # 오른쪽
    box(0, 43, 28, 6, 2, plate)          # 왼쪽
    oar = ["#b06a35", "#7a4423", "#d99a5c"]
    # 노: 자루(2x2x18) + 날(1x6x7)을 한 자리에. 겹쳐도 같은 색이라 영역만 넉넉히 칠한다
    for v in (0, 20):
        for y in range(v, v + 20):
            for x in range(62, 102):
                im.putpixel((x, y), hex_px(rnd.choice(oar)))
    # 옆판 아래쪽에 주황 열 띠
    for v in (35, 43):
        for x in range(2, 58):
            im.putpixel((x, v + 6), hex_px("#de8442"))
    return im


def main_m5(made):
    made.append(save(soul_face("front"), "block/soul_furnace_front.png"))
    made.append(save(soul_face("front", True), "block/soul_furnace_front_on.png"))
    made.append(save(soul_face("side"), "block/soul_furnace_side.png"))
    made.append(save(soul_face("top"), "block/soul_furnace_top.png"))
    made.append(save(hp_pipe_texture(), "block/high_pressure_pipe.png"))
    made.append(save(precision_front(False), "block/precision_assembler_front.png"))
    made.append(save(precision_front(True), "block/precision_assembler_front_on.png"))
    made.append(save(precision_top(), "block/precision_assembler_top.png"))
    made.append(save(turbine_side(), "block/steam_turbine_side.png"))
    made.append(save(turbine_top(False), "block/steam_turbine_top.png"))
    made.append(save(turbine_top(True), "block/steam_turbine_top_on.png"))
    for part in ("top", "side", "bottom"):
        made.append(save(gold_dispenser_face(part), f"block/gold_dispenser_{part}.png"))
        made.append(save(collector_face(part), f"block/barter_collector_{part}.png"))
    quartz = {"b": "#cbbfb1", "c": "#e3d9cc", "d": "#f4efe8", "e": "#ffffff", "u": "#f2cf4a"}
    made.append(save(recolor(from_art(GILDED_QUARTZ_ICON), quartz), "item/gilded_quartz.png"))
    made.append(save(from_art(PRECISION_PART_ICON), "item/precision_part.png"))
    made.append(save(from_art(DEBRIS_SONAR_ICON), "item/debris_sonar.png"))
    made.append(save(from_art(LAVA_STEAMBOAT_ICON), "item/lava_steamboat.png"))
    made.append(save(boat_texture(), "entity/lava_steamboat.png"))


# ---- M11 X1: 보스 전용 모델 텍스처 (128x64, 모델의 texOffs와 같은 자리) ----
def m11a_hex(c):
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def m11a_faces(u, v, w, h, d):
    """상자 하나의 면 자리 (x, y, 너비, 높이). right = 모델 -X 쪽, front = -Z (앞)."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


class M11aBox:
    """면 단위로 칠하는 도우미. ramp는 어두운 색 → 밝은 색."""

    def __init__(self, im, rnd, u, v, w, h, d):
        self.im = im
        self.rnd = rnd
        self.faces = m11a_faces(u, v, w, h, d)

    def put(self, face, x, y, col):
        fx, fy, fw, fh = self.faces[face]
        if 0 <= x < fw and 0 <= y < fh:
            self.im.putpixel((fx + x, fy + y), m11a_hex(col) if isinstance(col, str) else col)

    def size(self, face):
        return self.faces[face][2], self.faces[face][3]

    def fill(self, ramp, rust=None, rust_amount=0.0, bevel=True):
        """면마다 기본 음영 (위 밝게 · 아래 어둡게) + 잡티 + 모서리 + 녹 얼룩."""
        shade = {"top": 1, "front": 0, "right": 0, "left": 0, "back": -1, "bottom": -1}
        for face, (fx, fy, fw, fh) in self.faces.items():
            blobs = []
            if rust:
                n = int(fw * fh * rust_amount / 14) + (1 if self.rnd.random() < rust_amount * 4 else 0)
                for _ in range(n):
                    blobs.append((self.rnd.uniform(0, fw), self.rnd.uniform(0, fh), self.rnd.uniform(1.4, 3.2)))
            for y in range(fh):
                for x in range(fw):
                    level = 2 + shade[face]
                    r = self.rnd.random()
                    if r < 0.07:
                        level -= 1
                    elif r > 0.95:
                        level += 1
                    if bevel and fw > 2 and fh > 2:
                        if y == 0:
                            level += 1
                        elif y == fh - 1:
                            level -= 1
                    level = max(0, min(len(ramp) - 1, level))
                    col = ramp[level]
                    if rust:
                        for bx, by, br in blobs:
                            dd = math.hypot(x + 0.5 - bx, (y + 0.5 - by) * 1.3)
                            if dd < br:
                                k = 1 if dd > br * 0.65 else (2 if dd > br * 0.3 else 3)
                                k = max(0, min(len(rust) - 1, k + shade[face] - (1 if self.rnd.random() < 0.12 else 0)))
                                col = rust[k]
                                break
                    self.im.putpixel((fx + x, fy + y), m11a_hex(col))

    def drips(self, face, rust, count):
        """위에서 흘러내린 녹 줄기."""
        fw, fh = self.size(face)
        for _ in range(count):
            x = self.rnd.randrange(fw)
            length = self.rnd.randint(2, max(2, fh // 2))
            for y in range(self.rnd.randint(0, 2), min(fh, length)):
                self.put(face, x, y, rust[2] if y < length - 1 else rust[1])

    def rivets(self, face, positions, light, dark):
        for x, y in positions:
            self.put(face, x, y, light)
            self.put(face, x + 1, y + 1, dark)

    def rect(self, face, x0, y0, x1, y1, col):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(face, x, y, col)

    def hline(self, face, y, col, x0=0, x1=None):
        fw, _ = self.size(face)
        self.rect(face, x0, y, fw - 1 if x1 is None else x1, y, col)

    def vline(self, face, x, col, y0=0, y1=None):
        _, fh = self.size(face)
        self.rect(face, x, y0, x, fh - 1 if y1 is None else y1, col)


def m11a_edge_rivets(box, face, light, dark, step=4, inset=1):
    fw, fh = box.size(face)
    pts = []
    for x in range(inset, fw - inset - 1, step):
        pts.append((x, inset))
        pts.append((x, fh - inset - 2))
    for y in range(inset + step, fh - inset - 2, step):
        pts.append((inset, y))
        pts.append((fw - inset - 2, y))
    box.rivets(face, pts, light, dark)


# 녹슨 수호자: 바랜 쇠 (따뜻한 회색) + 주황 녹, 푸른 눈 · 핵
RG_IRON = ["#272020", "#40362f", "#5c4d43", "#786659", "#957f70", "#b3a091"]
RG_RUST = ["#4a200e", "#7a3618", "#a84e22", "#d0702e", "#eb9446"]
RG_RIVET = ("#a89688", "#2a221e")
RG_GLOW = ["#0e3a52", "#16577a", "#2fb8ef", "#a6efff", "#e8fbff"]


def rusty_guardian_textures():
    rnd = random.Random(1101)
    im = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (128, 64), (0, 0, 0, 0))

    def box(u, v, w, h, d, rust_amount=0.12):
        b = M11aBox(im, rnd, u, v, w, h, d)
        b.fill(RG_IRON, RG_RUST, rust_amount)
        return b

    # 몸통 20x18x12: 가로 이음매 두 줄, 리벳 테두리, 녹 줄기
    torso = box(0, 0, 20, 18, 12, 0.14)
    for face in ("front", "back", "right", "left"):
        fw, fh = torso.size(face)
        for y in (5, 12):
            torso.hline(face, y, RG_IRON[1])
            torso.hline(face, y + 1, RG_IRON[4])
        m11a_edge_rivets(torso, face, *RG_RIVET, step=5)
        torso.drips(face, RG_RUST, 3 if fw > 12 else 2)
    # 등판: 보일러 뚜껑 같은 둥근 해치
    for y in range(4, 13):
        for x in range(6, 15):
            dd = math.hypot(x - 9.5, y - 8.5)
            if dd < 4.6:
                inner = RG_IRON[4] if y < 7 else (RG_IRON[3] if y < 11 else RG_IRON[2])
                torso.put("back", x, y, RG_IRON[1] if dd > 3.6 else inner)
    torso.rivets("back", [(10, 4), (6, 8), (14, 8), (10, 12)], *RG_RIVET)
    # 위 · 아래: 판 이음
    torso.vline("top", 10, RG_IRON[1])
    torso.vline("bottom", 10, RG_IRON[0])

    # 가슴판 14x8x1: 두꺼운 판 + 가운데 핵 (빛남)
    plate = M11aBox(im, rnd, 0, 30, 14, 8, 1)
    plate.fill(RG_IRON[1:], RG_RUST, 0.08)
    plate.rivets("front", [(1, 1), (11, 1), (1, 5), (11, 5), (4, 1), (8, 1)], *RG_RIVET)
    for y in range(8):
        for x in range(14):
            dd = math.hypot(x - 6.5, y - 3.5)
            if dd < 3.2:
                if dd > 2.2:
                    plate.put("front", x, y, "#8a6a2a" if y < 4 else "#5a4418")  # 놋쇠 테
                else:
                    plate.put("front", x, y, RG_GLOW[1])
                    gx, gy = plate.faces["front"][0] + x, plate.faces["front"][1] + y
                    glow.putpixel((gx, gy), m11a_hex(RG_GLOW[3] if dd < 1.0 else RG_GLOW[2]))

    # 배기통 3x7x3 (둘이 같은 자리): 끝은 그을음
    stack = box(110, 44, 3, 7, 3, 0.2)
    for face in ("front", "back", "right", "left"):
        stack.hline(face, 0, RG_IRON[0])
        stack.hline(face, 2, RG_IRON[4])
    stack.rect("top", 0, 0, 2, 2, "#120e0c")
    stack.put("top", 1, 1, "#000000")

    # 골반 12x4x8
    pelvis = box(30, 34, 12, 4, 8, 0.1)
    for face in ("front", "back"):
        pelvis.hline(face, 0, RG_IRON[1])
        pelvis.rivets(face, [(1, 1), (5, 1), (9, 1)], *RG_RIVET)

    # 머리 8x8x8: 외눈 틈 (가로 한 줄), 이마 판
    head = box(64, 0, 8, 8, 8, 0.1)
    for face in ("front", "back", "right", "left"):
        head.hline(face, 1, RG_IRON[4])
        head.hline(face, 2, RG_IRON[2])
    head.rect("front", 1, 3, 6, 4, "#0e0a08")
    head.rect("front", 1, 3, 6, 3, RG_GLOW[0])
    head.rect("front", 2, 3, 5, 3, RG_GLOW[1])
    head.rivets("front", [(0, 6), (6, 6)], *RG_RIVET)
    head.rect("front", 2, 6, 5, 6, RG_IRON[1])  # 입 쪽 환기구
    head.put("front", 3, 7, RG_IRON[0])
    head.put("front", 4, 7, RG_IRON[0])
    hx, hy = head.faces["front"][0], head.faces["front"][1]
    for x in range(1, 7):
        glow.putpixel((hx + x, hy + 3), m11a_hex(RG_GLOW[4] if x in (3, 4) else (RG_GLOW[3] if x in (2, 5) else RG_GLOW[2])))
    for x in range(2, 6):
        glow.putpixel((hx + x, hy + 4), m11a_hex(RG_GLOW[1]))
    head.rivets("top", [(1, 1), (5, 1), (1, 5), (5, 5)], *RG_RIVET)

    # 어깨 갑옷 7x7x9: 둥근 테 + 리벳
    paul = box(96, 0, 7, 7, 9, 0.16)
    for face in ("front", "back", "right", "left"):
        m11a_edge_rivets(paul, face, *RG_RIVET, step=3)
    for face in ("front", "back", "right", "left"):
        paul.hline(face, 6, RG_IRON[0])

    # 위팔 5x8x6, 피스톤 통 3x6x2 (놋쇠), 막대 1x6x1 (반짝이는 쇠)
    upper = box(96, 30, 5, 8, 6, 0.12)
    for face in ("front", "back", "right", "left"):
        upper.hline(face, 0, RG_IRON[1])
        upper.hline(face, 7, RG_IRON[1])
    cyl = M11aBox(im, rnd, 96, 44, 3, 6, 2)
    cyl.fill(["#3a2a10", "#5a4418", "#8a6a2a", "#b08a3a", "#d8b060"])
    for face in ("front", "back", "right", "left"):
        cyl.hline(face, 1, "#3a2a10")
        cyl.hline(face, 4, "#3a2a10")
    rod = M11aBox(im, rnd, 106, 44, 1, 6, 1)
    rod.fill(["#5c6066", "#7d828a", "#9ea4ab", "#c4c9cf", "#e4e8ec"], bevel=False)

    # 아래팔 7x10x7: 띠 두 줄, 주먹 8x5x8: 손가락 마디
    fore = box(64, 16, 7, 10, 7, 0.15)
    for face in ("front", "back", "right", "left"):
        fore.hline(face, 2, RG_IRON[1])
        fore.hline(face, 3, RG_IRON[4])
        fore.hline(face, 7, RG_IRON[1])
        fore.rivets(face, [(1, 4), (4, 4)], *RG_RIVET)
    fist = box(96, 16, 8, 5, 8, 0.1)
    for face in ("front", "back", "right", "left"):
        for x in (2, 4, 6):
            fist.vline(face, x - 1, RG_IRON[1], 1, 4)
        fist.hline(face, 0, RG_IRON[4])
    fist.rect("bottom", 1, 1, 6, 6, RG_IRON[1])
    for x in (1, 3, 5, 7):
        fist.put("front", x - 1, 1, RG_IRON[5])

    # 다리 6x10x6, 발 8x6x9
    leg = box(70, 34, 6, 10, 6, 0.12)
    for face in ("front", "back", "right", "left"):
        leg.hline(face, 4, RG_IRON[1])
        leg.hline(face, 5, RG_IRON[4])
        leg.rivets(face, [(1, 1), (3, 7)], *RG_RIVET)
    foot = box(0, 46, 8, 6, 9, 0.18)
    for face in ("front", "right", "left", "back"):
        foot.hline(face, 0, RG_IRON[4])
        foot.hline(face, 5, RG_IRON[0])
    for x in (1, 3, 5):
        foot.vline("front", x, RG_IRON[1], 2, 4)
    foot.rivets("top", [(1, 1), (5, 1), (1, 6), (5, 6)], *RG_RIVET)
    return im, glow


# 용광로 파수꾼: 검게 그을린 쇠 + 구리 테, 주황 불빛
FW_IRON = ["#141418", "#202026", "#2e2e36", "#3e3e48", "#52525e", "#6a6a78"]
FW_SOOT = ["#060606", "#0e0c0c", "#18140f", "#241c14"]
FW_COPPER = ["#3a1e0e", "#6a3818", "#9a5a28", "#c8803c", "#e8a860"]
FW_GLOW = ["#5a1804", "#a8380a", "#ff7a1a", "#ffb040", "#fff0a0"]
FW_RIVET = ("#6a6a76", "#0a0a0c")


def furnace_warden_textures():
    rnd = random.Random(1102)
    im = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (128, 64), (0, 0, 0, 0))

    def box(u, v, w, h, d, soot=0.05):
        b = M11aBox(im, rnd, u, v, w, h, d)
        b.fill(FW_IRON, FW_SOOT, soot)
        return b

    def glow_px(b, face, x, y, col):
        fx, fy, fw, fh = b.faces[face]
        if 0 <= x < fw and 0 <= y < fh:
            glow.putpixel((fx + x, fy + y), m11a_hex(col))

    def copper_band(b, face, y):
        b.hline(face, y, FW_COPPER[3])
        b.hline(face, y + 1, FW_COPPER[1])

    # 몸통 20x14x14: 철판 이음 + 리벳, 아래쪽 구리 띠, 옆 환기 틈
    torso = box(0, 18, 20, 14, 14)
    for face in ("front", "back", "right", "left"):
        fw, fh = torso.size(face)
        copper_band(torso, face, fh - 3)
        m11a_edge_rivets(torso, face, *FW_RIVET, step=4)
        torso.vline(face, fw // 2, FW_IRON[1], 1, fh - 4)
    for face in ("right", "left"):
        for y in (4, 6, 8):
            torso.rect(face, 4, y, 9, y, FW_SOOT[0])
            for x in range(4, 10):
                glow_px(torso, face, x, y, FW_GLOW[0] if x in (4, 9) else FW_GLOW[1])
    # 앞면: 창살(4..15, 3..9) 둘레 구리 틀, 뒤는 불 빛
    torso.rect("front", 3, 2, 16, 10, FW_COPPER[2])
    torso.rect("front", 3, 2, 16, 2, FW_COPPER[4])
    torso.rect("front", 3, 10, 16, 10, FW_COPPER[0])
    torso.rect("front", 4, 3, 15, 9, FW_GLOW[1])
    torso.rivets("front", [(3, 2), (15, 2), (3, 9), (15, 9)], FW_COPPER[4], FW_COPPER[0])
    # 등: 큰 해치 + 경첩
    torso.rect("back", 5, 2, 14, 9, FW_IRON[2])
    torso.rect("back", 5, 2, 14, 2, FW_IRON[4])
    torso.rect("back", 5, 9, 14, 9, FW_IRON[0])
    torso.rivets("back", [(6, 3), (12, 3), (6, 7), (12, 7)], *FW_RIVET)
    torso.rect("back", 3, 4, 4, 5, FW_COPPER[2])
    torso.rect("back", 3, 7, 4, 8, FW_COPPER[2])

    # 덮개판 22x2x16: 테두리는 구리, 위는 두꺼운 판
    slab = M11aBox(im, rnd, 0, 0, 22, 2, 16)
    slab.fill(FW_IRON[1:], FW_SOOT, 0.06)
    for face in ("front", "back", "right", "left"):
        slab.hline(face, 0, FW_COPPER[3])
        slab.hline(face, 1, FW_COPPER[1])
    m11a_edge_rivets(slab, "top", *FW_RIVET, step=4)
    slab.rect("top", 6, 4, 15, 11, FW_IRON[2])
    slab.rect("top", 6, 4, 15, 4, FW_IRON[4])

    # 화구 창살 12x7x1: 놋쇠 틀, 세로 창살 사이로 불
    grate = M11aBox(im, rnd, 38, 46, 12, 7, 1)
    grate.fill(FW_COPPER)
    for y in range(1, 6):
        for x in range(1, 11):
            if x % 2 == 0:
                grate.put("front", x, y, FW_IRON[2] if y > 1 else FW_IRON[3])
            else:
                hot = 4 if y >= 4 else (3 if y >= 2 else 2)
                if rnd.random() < 0.2:
                    hot -= 1
                grate.put("front", x, y, FW_GLOW[0])
                glow_px(grate, "front", x, y, FW_GLOW[hot])
    grate.rivets("front", [(0, 0), (10, 0), (0, 5), (10, 5)], FW_COPPER[4], FW_COPPER[0])

    # 머리 아래 통 8x6x8: 눈 틈 둘
    head = box(68, 18, 8, 6, 8)
    for face in ("front", "back", "right", "left"):
        copper_band(head, face, 4)
    for x0 in (1, 5):
        head.rect("front", x0, 2, x0 + 1, 2, FW_GLOW[0])
        glow_px(head, "front", x0, 2, FW_GLOW[3])
        glow_px(head, "front", x0 + 1, 2, FW_GLOW[3])
        head.put("front", x0, 3, FW_SOOT[0])
        head.put("front", x0 + 1, 3, FW_SOOT[0])
        glow_px(head, "front", x0, 3, FW_GLOW[1])
        glow_px(head, "front", x0 + 1, 3, FW_GLOW[1])
    head.rivets("front", [(0, 0), (6, 0)], *FW_RIVET)

    # 굴뚝 6x5x6: 그을음 줄, 테 7x1x7: 안에 불씨
    chim = box(100, 18, 6, 5, 6, 0.25)
    for face in ("front", "back", "right", "left"):
        chim.hline(face, 0, FW_SOOT[1])
        chim.hline(face, 3, FW_COPPER[2])
    cap = M11aBox(im, rnd, 38, 54, 7, 1, 7)
    cap.fill(FW_COPPER[:4], bevel=False)
    cap.rect("top", 1, 1, 5, 5, FW_SOOT[0])
    for y in range(1, 6):
        for x in range(1, 6):
            dd = math.hypot(x - 3, y - 3)
            if dd < 2.2:
                cap.put("top", x, y, FW_GLOW[0])
                glow_px(cap, "top", x, y, FW_GLOW[4] if dd < 0.8 else (FW_GLOW[3] if dd < 1.5 else FW_GLOW[2]))

    # 어깨 6x6x8, 팔 7x10x8, 주먹 8x5x9
    shoulder = box(68, 32, 6, 6, 8)
    for face in ("front", "back", "right", "left"):
        m11a_edge_rivets(shoulder, face, *FW_RIVET, step=3)
    arm = box(76, 0, 7, 10, 8)
    for face in ("front", "back", "right", "left"):
        copper_band(arm, face, 1)
        copper_band(arm, face, 7)
        arm.vline(face, 3, FW_IRON[1], 3, 6)
    fist = box(66, 46, 8, 5, 9, 0.15)
    for face in ("front", "back", "right", "left"):
        fist.hline(face, 0, FW_COPPER[3])
        fist.hline(face, 4, FW_IRON[0])
    for x in (1, 3, 5, 7):
        fist.put("front", x - 1, 2, FW_COPPER[4])
        fist.put("front", x - 1, 3, FW_COPPER[2])
    fist.rect("bottom", 1, 1, 6, 7, FW_IRON[1])

    # 다리 7x8x7, 발 9x3x10
    leg = box(100, 29, 7, 8, 7)
    for face in ("front", "back", "right", "left"):
        copper_band(leg, face, 2)
        leg.rivets(face, [(1, 5), (4, 5)], *FW_RIVET)
    foot = box(0, 46, 9, 3, 10, 0.2)
    for face in ("front", "back", "right", "left"):
        foot.hline(face, 0, FW_COPPER[3])
    for x in (1, 4, 7):
        foot.put("front", x, 1, FW_IRON[5])
    return im, glow


def main_m11a(made):
    for name, maker in (("rusty_guardian", rusty_guardian_textures), ("furnace_warden", furnace_warden_textures)):
        im, glow = maker()
        made.append(save(im, f"entity/{name}.png"))
        made.append(save(glow, f"entity/{name}_glow.png"))


# ---------- M8 묶음 B: 세척기 · 고압 정련기 · 정제 가루 · 용광로 탑 ----------
WASHER_STEEL = {"a": "#1c2a33", "b": "#355063", "c": "#4d6d82", "d": "#6f91a6", "e": "#a9c7d8"}
HEATPROOF_BRICK = {"a": "#1e0d0e", "b": "#3b1a1c", "c": "#55262a", "d": "#6f3236", "e": "#8c4448"}


def washer_front(lit):
    """세척기 앞면: 둥근 유리창 너머 물. 돌 때는 물살이 밝다."""
    im = recolor(panel(seed=531), WASHER_STEEL)
    water = ("#7fb4ff", "#3f76e4", "#2a4fa8") if lit else ("#3f76e4", "#2a4fa8", "#1d3570")
    rnd = random.Random(532)
    for y in range(2, 14):
        for x in range(2, 14):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 5.6:
                if d > 4.6:
                    im.putpixel((x, y), px("e"))
                else:
                    k = rnd.random() + (y - 4) * 0.08
                    im.putpixel((x, y), hex_px(water[0] if k < 0.35 else water[1] if k < 1.0 else water[2]))
    for x in range(4, 12):
        im.putpixel((x, 14), px("a"))
    return im


def washer_top():
    """세척기 윗면: 물 넣는 구멍."""
    im = recolor(panel(seed=533), WASHER_STEEL)
    for y in range(5, 11):
        for x in range(5, 11):
            edge = x in (5, 10) or y in (5, 10)
            im.putpixel((x, y), px("a") if edge else hex_px("#3f76e4"))
    return im


def refiner_front(lit):
    """고압 정련기 앞면: 눌러 짜는 두 판과 붉은 고압 띠."""
    im = recolor(panel(seed=541), HP_STEEL)
    for y in range(3, 13):
        for x in range(3, 13):
            im.putpixel((x, y), px("a"))
    glow = hex_px("#ffb347" if lit else "#4a2a12")
    for x in range(4, 12):
        im.putpixel((x, 5), px("d"))
        im.putpixel((x, 10), px("d"))
    for x in range(5, 11):
        for y in range(7, 9):
            im.putpixel((x, y), glow)
    for y in range(3, 13):
        im.putpixel((2, y), hex_px("#b3262a"))
        im.putpixel((13, y), hex_px("#b3262a"))
    return im


def refiner_top():
    im = recolor(panel(seed=542), HP_STEEL)
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 3.8:
                im.putpixel((x, y), px("a") if d > 2.8 else hex_px("#b3262a"))
    return im


def heatproof_bricks(formed):
    """내열 벽돌: 짙은 네더 벽돌 줄눈. 모양이 서면 줄눈이 달아오른 주황."""
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, "ccd", 551)
    im = recolor(im, HEATPROOF_BRICK)
    mortar = ("#c2551c", "#ff9a3c") if formed else (HEATPROOF_BRICK["a"], HEATPROOF_BRICK["b"])
    for y in (3, 7, 11, 15):
        for x in range(16):
            im.putpixel((x, y), hex_px(mortar[(x + y) % 3 == 0]))
    for row in range(4):
        y0 = row * 4
        offset = 0 if row % 2 == 0 else 4
        for x in (offset, offset + 8):
            for y in range(y0, y0 + 3):
                im.putpixel((x % 16, y), hex_px(mortar[0]))
    for x, y in ((2, 1), (10, 5), (5, 9), (13, 13)):
        im.putpixel((x, y), hex_px(HEATPROOF_BRICK["e"]))
    return im


def tower_controller_front(lit):
    """용광로 탑 제어기 앞면: 내열 벽돌에 쇠 문, 돌 때는 불빛."""
    im = heatproof_bricks(False)
    for y in range(3, 15):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (3, 14)
            if edge:
                im.putpixel((x, y), hex_px(HP_STEEL["d"]))
            elif y < 7:
                im.putpixel((x, y), hex_px(HP_STEEL["b"] if (x + y) % 2 else HP_STEEL["c"]))
            elif lit:
                im.putpixel((x, y), px("u" if (x * 3 + y) % 4 == 0 else "t" if (x + y) % 2 else "s"))
            else:
                im.putpixel((x, y), px("m" if (x + y) % 2 else "a"))
    for x in range(4, 12, 2):
        im.putpixel((x, 5), hex_px("#b3262a"))
    return im


def gui_washer():
    """세척기 화면: 기본 기계 화면 + 왼쪽(재료 칸 옆) 물 눈금 틀."""
    from PIL import ImageDraw
    im = gui_machine(False)
    d = ImageDraw.Draw(im)
    x0, y0, x1, y1 = 34, 16, 45, 69
    d.rectangle([x0, y0, x1, y1], fill="#2a2a2a")
    d.line([(x0, y0), (x1 - 1, y0)], fill=GUI_EDGE)
    d.line([(x0, y0), (x0, y1 - 1)], fill=GUI_EDGE)
    d.line([(x0 + 1, y1), (x1, y1)], fill=GUI_LIGHT)
    d.line([(x1, y0 + 1), (x1, y1)], fill=GUI_LIGHT)
    for y in range(y0 + 6, y1, 13):
        d.line([(x1 - 3, y), (x1 - 1, y)], fill="#6b6b6b")
    return im


def main_m8b(made):
    made.append(save(washer_front(False), "block/washer_front.png"))
    made.append(save(washer_front(True), "block/washer_front_on.png"))
    made.append(save(washer_top(), "block/washer_top.png"))
    made.append(save(refiner_front(False), "block/hp_refiner_front.png"))
    made.append(save(refiner_front(True), "block/hp_refiner_front_on.png"))
    made.append(save(refiner_top(), "block/hp_refiner_top.png"))
    made.append(save(heatproof_bricks(False), "block/heatproof_bricks.png"))
    made.append(save(heatproof_bricks(True), "block/heatproof_bricks_formed.png"))
    made.append(save(tower_controller_front(False), "block/furnace_tower_controller_front.png"))
    made.append(save(tower_controller_front(True), "block/furnace_tower_controller_front_on.png"))
    made.append(save(gui_washer(), "gui/washer.png"))
    dust = from_art(ITEMS["iron_dust"])
    # 정제 가루: 가루 위에 반짝이는 점 몇 개
    def refined(mapping):
        im = recolor(dust, mapping)
        for x, y in ((6, 8), (9, 9), (7, 11), (11, 11)):
            im.putpixel((x, y), hex_px("#ffffff"))
        return im
    made.append(save(refined({"a": "#3a4048", "b": "#9aa3ad", "c": "#c9d1da", "d": "#e4eaf0", "e": "#ffffff"}), "item/refined_iron_dust.png"))
    made.append(save(refined(COPPER_DUST), "item/refined_copper_dust.png"))
    made.append(save(refined(GOLD_DUST), "item/refined_gold_dust.png"))
    made.append(save(recolor(dust, {"a": "#2b1347", "b": "#5b2a8c", "c": "#8a4fd0", "d": "#b98cf0", "e": "#ecdcff"}), "item/rare_dust.png"))


# ---------- M6 엔드 최종 ----------
VOID = {"a": "#0d0716", "b": "#24133a", "c": "#43255f", "d": "#73479e", "e": "#c7a2ff"}
CHORUS = {"a": "#2a1a2e", "b": "#5b3d63", "c": "#8a6390", "d": "#b48fb8", "e": "#e6d2e8"}


def void_core_icon():
    """공허 핵: 짙은 보라 구슬 안에 밝은 점, 둘레에 은은한 테."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            hl = math.hypot(x - 6.0, y - 6.0)
            if r < 2.4:
                im.putpixel((x, y), hex_px("#f2e6ff" if hl < 1.5 else "#c7a2ff"))
            elif r < 4.6:
                im.putpixel((x, y), hex_px("#73479e" if hl < 3.4 else "#43255f"))
            elif r < 5.6:
                im.putpixel((x, y), hex_px("#24133a"))
            elif r < 6.6 and (x * 3 + y) % 4 == 0:
                im.putpixel((x, y), hex_px("#c7a2ff"))
    return im


def void_condenser_face(part, on=False):
    im = recolor(panel(seed={"top": 601, "side": 602, "bottom": 603}[part]), VOID)
    glow = "#d9b8ff" if on else "#4a3366"
    if part == "top":
        for y in range(4, 12):
            for x in range(4, 12):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 3.6:
                    im.putpixel((x, y), hex_px(VOID["a"]) if d > 2.6 else hex_px("#b3262a"))
    elif part == "side":
        for x in range(3, 13):
            im.putpixel((x, 4), hex_px(VOID["a"]))
            im.putpixel((x, 11), hex_px(VOID["a"]))
        for y in range(5, 11):
            for x in range(3, 13):
                if (x + y) % 3 == 0:
                    im.putpixel((x, y), hex_px(glow))
    else:
        # 아래로 열린 깔때기 입: 공허를 빨아들인다
        for y in range(2, 14):
            for x in range(2, 14):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 5.5:
                    im.putpixel((x, y), hex_px("#05030a") if d < 3.0 else hex_px(glow if (int(d * 2) % 2) else VOID["b"]))
    return im


def chorus_harvester_front():
    im = recolor(panel(seed=611), VOID)
    for y in range(3, 12):
        for x in range(3, 13):
            im.putpixel((x, y), hex_px(VOID["a"]))
    # 후렴 줄기와 꽃
    for y in range(6, 11):
        im.putpixel((7, y), hex_px(CHORUS["c"]))
        im.putpixel((8, y), hex_px(CHORUS["b"]))
    for x in range(5, 7):
        im.putpixel((x, 7), hex_px(CHORUS["c"]))
    for x in range(9, 11):
        im.putpixel((x, 8), hex_px(CHORUS["c"]))
    for x, y in ((7, 4), (8, 4), (7, 5), (8, 5), (5, 6), (10, 7)):
        im.putpixel((x, y), hex_px(CHORUS["e"]))
    for x in range(4, 12):
        im.putpixel((x, 13), hex_px("#6fd0f0" if x % 2 else VOID["a"]))
    return im


def chorus_harvester_top():
    im = recolor(panel(seed=612), VOID)
    for y in range(4, 12):
        for x in range(4, 12):
            im.putpixel((x, y), hex_px(VOID["a"] if x in (4, 11) or y in (4, 11) else CHORUS["b"]))
    return im


def vault_face(part):
    im = recolor(panel(seed={"top": 621, "side": 622, "bottom": 623}[part]), VOID)
    if part == "side":
        for y in range(3, 13):
            for x in range(3, 13):
                im.putpixel((x, y), hex_px(VOID["a"] if x in (3, 12) or y in (3, 12) else VOID["b"]))
        # 가운데 열쇠 구멍 같은 엔더 눈
        for x, y in ((7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
            im.putpixel((x, y), hex_px("#2fd3b4"))
        for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
            im.putpixel((x, y), hex_px("#0f6d5e"))
    elif part == "top":
        for y in range(4, 12):
            for x in range(4, 12):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 3.8:
                    im.putpixel((x, y), hex_px("#c7a2ff") if d < 1.6 else hex_px(VOID["a"]))
    return im


def breath_face(part):
    im = recolor(panel(seed={"top": 631, "side": 632, "bottom": 633}[part]), CHORUS if part != "side" else VOID)
    if part == "side":
        # 유리창 안의 분홍 숨결
        for y in range(3, 13):
            for x in range(4, 12):
                edge = x in (4, 11) or y in (3, 12)
                im.putpixel((x, y), hex_px(VOID["a"] if edge else ("#e07ad0" if (x * 7 + y * 3) % 5 < 2 else "#8a3f8f")))
    elif part == "top":
        for y in range(5, 11):
            for x in range(5, 11):
                im.putpixel((x, y), hex_px(VOID["a"] if x in (5, 10) or y in (5, 10) else "#3a2040"))
    return im


def main_m6(made):
    made.append(save(recolor(from_art(ITEMS["guardian_summoner"]), {**VOID, "R": "#7a2fbf", "Q": "#c77dff", "u": "#f2e6ff",
                                                                   "k": "#24133a", "g": "#9d5cff", "h": "#e2c8ff"}),
                     "item/colossus_summoner.png"))
    made.append(save(void_core_icon(), "item/void_core.png"))
    for part in ("helmet", "chestplate", "leggings", "boots", "sword"):
        made.append(save(recolor(from_art(ITEMS[f"steel_{part}"]), VOID), f"item/void_{part}.png"))
    made.append(save(recolor(armor_layer(False), VOID), "entity/equipment/humanoid/void.png"))
    made.append(save(recolor(armor_layer(True), VOID), "entity/equipment/humanoid_leggings/void.png"))
    made.append(save(void_condenser_face("top"), "block/void_condenser_top.png"))
    made.append(save(void_condenser_face("side"), "block/void_condenser_side.png"))
    made.append(save(void_condenser_face("side", True), "block/void_condenser_side_on.png"))
    made.append(save(void_condenser_face("bottom"), "block/void_condenser_bottom.png"))
    made.append(save(void_condenser_face("bottom", True), "block/void_condenser_bottom_on.png"))
    made.append(save(chorus_harvester_front(), "block/chorus_harvester_front.png"))
    made.append(save(chorus_harvester_top(), "block/chorus_harvester_top.png"))
    for part in ("top", "side", "bottom"):
        made.append(save(vault_face(part), f"block/dimensional_vault_{part}.png"))
        made.append(save(breath_face(part), f"block/breath_collector_{part}.png"))


def main_m8c(made):
    """M8 I4 렌치 (+ X2는 텍스처 없음)."""
    wrench = [
        "................",
        "..........cd.de.",
        ".........cdc.dc.",
        ".........bc..cb.",
        ".........bcbcbb.",
        "..........bcbb..",
        ".........bcb....",
        "........bcb.....",
        ".......bcb......",
        "......bcb.......",
        ".....bcb........",
        "....bcb.........",
        "...acb..........",
        "..aba...........",
        "..aa............",
        "................",
    ]
    made.append(save(from_art(wrench), "item/wrench.png"))


# ---------- 마지막 묶음 (fin): 공허 거상 전용 모델 텍스처 ----------
# 흑요석 같은 검보라 몸, 자홍 빛 금 · 핵 · 눈, 떠 있는 바위판은 회보라 돌
VC_OBS = ["#0b0812", "#160f22", "#22172f", "#2e2042", "#3d2b58", "#523b76"]
VC_ROCK = ["#141019", "#211b29", "#2f2739", "#3e344b", "#51455f", "#685a78"]
VC_VEIN = ["#1f0f33", "#36195a", "#572a8a", "#7a3cb8"]
VC_GLOW = ["#3d0b52", "#7a1a9e", "#b632d8", "#e872ff", "#ffd2ff"]
VC_CRYSTAL = ["#2a1240", "#4a1f70", "#6d2fa0", "#9550cc", "#c088ec"]


def void_colossus_textures():
    rnd = random.Random(1103)
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    glow = Image.new("RGBA", (128, 128), (0, 0, 0, 0))

    def box(u, v, w, h, d, ramp=VC_OBS):
        b = M11aBox(im, rnd, u, v, w, h, d)
        b.fill(ramp)
        return b

    def glow_px(b, face, x, y, col):
        fx, fy, fw, fh = b.faces[face]
        if 0 <= x < fw and 0 <= y < fh:
            glow.putpixel((fx + x, fy + y), m11a_hex(col))

    def crack(b, face, x, y, length, lit=0):
        """아래로 구불구불 내려가는 금. lit > 0이면 그 세기로 빛난다."""
        fw, fh = b.size(face)
        for i in range(length):
            if not (0 <= x < fw and 0 <= y < fh):
                break
            b.put(face, x, y, VC_VEIN[1] if i % 3 else VC_VEIN[2])
            if lit:
                glow_px(b, face, x, y, VC_GLOW[min(4, lit + (1 if i == 0 else 0))])
            y += 1
            r = rnd.random()
            if r < 0.3:
                x -= 1
            elif r > 0.7:
                x += 1

    def strata(b, face, rows, col):
        fw, _ = b.size(face)
        for y in rows:
            for x in range(fw):
                if rnd.random() < 0.8:
                    b.put(face, x, y, col)

    # 가슴 20x10x12: 앞면 한가운데 핵이 든 구멍(빛나는 테), 거기서 퍼지는 금
    chest = box(0, 0, 20, 10, 12)
    cx0, cy0, cx1, cy1 = 6, 1, 13, 8
    for y in range(cy0, cy1 + 1):
        for x in range(cx0, cx1 + 1):
            edge = x in (cx0, cx1) or y in (cy0, cy1)
            corner = x in (cx0, cx1) and y in (cy0, cy1)
            if corner:
                continue
            if edge:
                chest.put("front", x, y, VC_VEIN[2])
                glow_px(chest, "front", x, y, VC_GLOW[2])
            else:
                dd = math.hypot(x - 9.5, y - 4.5)
                chest.put("front", x, y, "#050309" if dd > 1.6 else "#12051c")
                if dd < 2.6:
                    glow_px(chest, "front", x, y, VC_GLOW[1] if dd > 1.5 else VC_GLOW[2])
    for x, y, n in ((5, 2, 7), (14, 3, 6), (4, 6, 4), (15, 7, 3), (9, 9, 1)):
        crack(chest, "front", x, y, n, lit=1)
    chest.hline("front", 0, VC_OBS[4])
    for face in ("right", "left", "back"):
        fw, fh = chest.size(face)
        for _ in range(3 if face == "back" else 2):
            crack(chest, face, rnd.randrange(1, fw - 1), rnd.randrange(0, 3), rnd.randint(4, 8), lit=1 if rnd.random() < 0.5 else 0)
        strata(chest, face, [fh - 2], VC_OBS[1])
    for _ in range(6):
        chest.put("top", rnd.randrange(20), rnd.randrange(12), VC_OBS[5])

    # 머리 8x9x8: 눈 틈 둘 (자홍), 이마의 빛나는 금
    head = box(64, 0, 8, 9, 8)
    head.hline("front", 2, VC_OBS[4])
    for x0 in (1, 5):
        for x in (x0, x0 + 1):
            head.put("front", x, 3, VC_GLOW[0])
            head.put("front", x, 4, "#050309")
            glow_px(head, "front", x, 3, VC_GLOW[4])
            glow_px(head, "front", x, 4, VC_GLOW[2])
    crack(head, "front", 4, 0, 2, lit=2)
    head.rect("front", 2, 7, 5, 7, VC_OBS[1])
    for face in ("right", "left", "back"):
        crack(head, face, rnd.randrange(1, 7), 0, rnd.randint(3, 6))

    # 배 14x8x9 · 골반 12x4x8: 층진 돌
    belly = box(0, 22, 14, 8, 9)
    for face in ("front", "back", "right", "left"):
        strata(belly, face, [2, 5], VC_OBS[1])
    crack(belly, "front", 4, 0, 5, lit=1)
    crack(belly, "front", 10, 1, 4)
    pelvis = box(46, 22, 12, 4, 8, VC_OBS[:5])
    for face in ("front", "back", "right", "left"):
        pelvis.hline(face, 0, VC_OBS[4])

    # 위팔 6x14x6: 세로 결 + 앞면에 빛나는 금 하나
    upper = box(96, 0, 6, 14, 6)
    for face in ("front", "back", "right", "left"):
        strata(upper, face, [4, 9], VC_OBS[1])
    crack(upper, "front", 2, 1, 11, lit=1)
    crack(upper, "right", 3, 2, 6)

    # 아래팔 7x14x7: 지그재그로 빛나는 금 (공허 무늬)
    fore = box(86, 20, 7, 14, 7)
    for face in ("front", "back", "right", "left"):
        fw, fh = fore.size(face)
        x = rnd.randrange(1, fw - 1)
        for y in range(1, fh - 1):
            x = max(0, min(fw - 1, x + (1 if (y // 3) % 2 == 0 else -1)))
            fore.put(face, x, y, VC_VEIN[2])
            if face in ("front", "left", "right"):
                glow_px(fore, face, x, y, VC_GLOW[2] if y % 4 else VC_GLOW[3])
        strata(fore, face, [fh - 1], VC_OBS[1])

    # 손 8x5x8: 앞쪽 주먹 마디
    hand = box(0, 39, 8, 5, 8)
    for x in (0, 2, 4, 6):
        hand.put("front", x, 1, VC_OBS[5])
        hand.put("front", x + 1, 1, VC_OBS[4])
        hand.put("front", x, 3, VC_OBS[1])
    hand.rect("bottom", 1, 1, 6, 6, VC_OBS[0])
    for x in (1, 3, 5):
        glow_px(hand, "front", x, 2, VC_GLOW[1])

    # 다리 7x12x7 · 발 9x4x9
    leg = box(32, 39, 7, 12, 7)
    for face in ("front", "back", "right", "left"):
        strata(leg, face, [4, 8], VC_OBS[1])
    crack(leg, "front", 3, 0, 7, lit=1)
    crack(leg, "right", 2, 2, 5)
    foot = box(60, 41, 9, 4, 9)
    for face in ("front", "back", "right", "left"):
        foot.hline(face, 0, VC_OBS[4])
    for x in (1, 4, 7):
        foot.put("front", x, 2, VC_OBS[5])

    # 떠 있는 바위판 12x4x12 + 덩어리 7x4x7: 회보라 돌, 위에 빛나는 금
    plate = box(0, 60, 12, 4, 12, VC_ROCK)
    for _ in range(4):
        crack(plate, "top", rnd.randrange(1, 11), rnd.randrange(0, 5), rnd.randint(4, 8), lit=1)
    for face in ("front", "back", "right", "left"):
        plate.hline(face, 3, VC_ROCK[0])
        crack(plate, face, rnd.randrange(1, 11), 0, 3, lit=1 if rnd.random() < 0.5 else 0)
    plate.rect("bottom", 2, 2, 9, 9, VC_ROCK[0])
    for (x, y) in ((5, 5), (6, 6), (5, 6), (6, 5)):
        plate.put("bottom", x, y, VC_GLOW[0])
        glow_px(plate, "bottom", x, y, VC_GLOW[2])
    chunk = box(48, 60, 7, 4, 7, VC_ROCK)
    crack(chunk, "top", 3, 0, 6, lit=1)
    for face in ("front", "right", "left"):
        crack(chunk, face, rnd.randrange(1, 6), 0, 3)

    # 핵 4x4x4: 자홍 빛 덩어리
    core = M11aBox(im, rnd, 76, 60, 4, 4, 4)
    core.fill(VC_GLOW[:4], bevel=False)
    for face, (fx, fy, fw, fh) in core.faces.items():
        for y in range(fh):
            for x in range(fw):
                dd = math.hypot(x - 1.5, y - 1.5)
                glow.putpixel((fx + x, fy + y), m11a_hex(VC_GLOW[4] if dd < 0.8 else (VC_GLOW[3] if dd < 1.6 else VC_GLOW[2])))

    # 머리 위 조각 3x5x3 · 등 가시 3x8x3: 자수정 같은 결정, 끝이 빛남
    shard = box(92, 60, 3, 5, 3, VC_CRYSTAL)
    for face in ("front", "back", "right", "left"):
        shard.vline(face, 1, VC_CRYSTAL[4], 0, 3)
        glow_px(shard, face, 1, 0, VC_GLOW[3])
        glow_px(shard, face, 1, 1, VC_GLOW[2])
    for x in range(3):
        for y in range(3):
            glow_px(shard, "top", x, y, VC_GLOW[3 if (x, y) == (1, 1) else 2])
    spike = box(104, 60, 3, 8, 3, VC_CRYSTAL)
    for face in ("front", "back", "right", "left"):
        spike.vline(face, 1, VC_CRYSTAL[4], 0, 4)
        for y in range(0, 3):
            glow_px(spike, face, 1, y, VC_GLOW[3 if y == 0 else 2])
    for x in range(3):
        for y in range(3):
            glow_px(spike, "top", x, y, VC_GLOW[3])
    return im, glow


def void_shield_texture():
    """보호막 무늬 128x128 (모델 텍스처와 같은 크기라 면마다 줄무늬가 몇 개씩 보인다): 이어 붙여도 이음매가 없는 비스듬한 물결.
    밝기만 칠하고 색은 렌더러가 곱한다 (더하기 섞기라 검정 = 안 보임)."""
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 255))
    for y in range(128):
        for x in range(128):
            a = math.sin((x + y) * 2 * math.pi / 16)
            b = math.sin((x - 2 * y) * 2 * math.pi / 32 + 1.3)
            v = max(0.0, (a + b) / 2) ** 2
            g = int(255 * v)
            im.putpixel((x, y), (g, g, g, 255))
    return im


def main_fin(made):
    im, glow = void_colossus_textures()
    made.append(save(im, "entity/void_colossus.png"))
    made.append(save(glow, "entity/void_colossus_glow.png"))
    made.append(save(void_shield_texture(), "entity/void_colossus_shield.png"))


# ---------- 확장 3 C3 상태 이상 아이콘 (18x18, textures/mob_effect) ----------
def effect_art(rows, colors):
    assert len(rows) == 18 and all(len(r) == 18 for r in rows), rows
    im = Image.new("RGBA", (18, 18))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), hex_px(colors[ch]))
    return im


def rust_effect_icon():
    """녹: 녹이 번진 강철 볼트 머리."""
    return effect_art([
        "..................",
        "......aaaaaa......",
        "....aabccccbaa....",
        "...abccdddccsba...",
        "..abcdddeddcstba..",
        "..abcdeeeedstuba..",
        ".abcddeaaedsttcba.",
        ".abcdea..aestscba.",
        ".abcsta..ascdcba..",
        ".abstsa..adddcba..",
        ".abcsta..aedcbba..",
        ".abcdea..aedcba...",
        "..abcdeaaedcsba...",
        "..abcdddddcstba...",
        "...abcccsstsba....",
        "....aabbssbaa.....",
        "......aaaaaa......",
        "..................",
    ], {"a": "#2c3138", "b": "#5f6873", "c": "#8f99a5", "d": "#bcc6d0", "e": "#eaf0f5",
        "s": "#a85628", "t": "#de8442", "u": "#ffc27a"})


def overheat_effect_icon():
    """과열: 붉게 달아오른 온도계와 불꽃."""
    return effect_art([
        "..................",
        "........aa........",
        ".......abba.......",
        ".......abea....f..",
        ".......abea...fgf.",
        ".......abea..fghf.",
        ".......arra..fghf.",
        ".......arra...ff..",
        ".......arra.......",
        ".......arra.......",
        ".......arra.......",
        "......arrrra......",
        ".....arrsssra.....",
        ".....arsshsra.....",
        ".....arssssra.....",
        "......arrrra......",
        ".......aaaa.......",
        "..................",
    ], {"a": "#2c3138", "b": "#bcc6d0", "e": "#eaf0f5", "r": "#a3262a", "s": "#e04848", "h": "#ffc27a",
        "f": "#de8442", "g": "#ff9a3c"})


def void_erosion_effect_icon():
    """공허 침식: 금 간 하트를 보라 공허가 갉아 먹는다."""
    return effect_art([
        "..................",
        "..................",
        "...aaaa....aaaa...",
        "..arrrra..arrdca..",
        ".arrsssraarrdcdca.",
        ".arsshssrrsr.cdea.",
        ".arshhssssr..dca..",
        ".arssssssr.c..ca..",
        ".arsssssr.ccd.ca..",
        "..arssssr..dc.a...",
        "...arsssr.ca.a....",
        "....arssr.c.a.....",
        ".....arsr..a......",
        "......arr.a.......",
        ".......aaa........",
        "..................",
        "..................",
        "..................",
    ], {"a": "#24133a", "r": "#7a1c22", "s": "#c0313a", "h": "#f07a80", "c": "#73479e", "d": "#c7a2ff", "e": "#f2e6ff"})


def main_ex3_c3(made):
    made.append(save(rust_effect_icon(), "mob_effect/rust.png"))
    made.append(save(overheat_effect_icon(), "mob_effect/overheat.png"))
    made.append(save(void_erosion_effect_icon(), "mob_effect/void_erosion.png"))


# ---------- 확장 3 C2 · R7: 새 몹 셋 (녹슨 자동인형 · 잿불 정령 · 공허 그림자), 정예 테두리, 생성 알 · 공허 조각 ----------
# 자동인형: 푸른 기 도는 무쇠 + 짙은 주황 녹, 호박색 외눈 (금 감) · 계기 · 굴뚝 불씨
RA_IRON = ["#1d2026", "#2f343c", "#454c56", "#5d6672", "#78828e", "#97a1ac"]
RA_RUST = ["#3e1a0a", "#6a2e12", "#94441c", "#bf5f26", "#e08440"]
RA_RIVET = ("#a0a8b0", "#16181c")
RA_GLOW = ["#5a2a04", "#a85a0a", "#ffa21f", "#ffd27a", "#fff4d0"]
# 정령: 숯덩이 핵 (용암 같은 금), 주황 → 노랑 불꽃
EW_CHAR = ["#120a08", "#1e1210", "#2c1a14", "#3c241a", "#4e3020"]
EW_FIRE = ["#7a1c06", "#c8400c", "#ff7a1a", "#ffb340", "#ffe9a0", "#fffbe6"]
# 그림자: 보라 기 도는 검정, 자홍 눈 · 균열
VS_DARK = ["#06040a", "#0d0914", "#16101f", "#20182c", "#2c213b", "#3a2c4c"]
VS_GLOW = ["#3a0f5a", "#6a1fa0", "#b04aff", "#e09cff", "#f8e6ff"]


def c2_glow_px(glow, b, face, x, y, col):
    fx, fy, fw, fh = b.faces[face]
    if 0 <= x < fw and 0 <= y < fh:
        glow.putpixel((fx + x, fy + y), m11a_hex(col) if isinstance(col, str) else col)


def rust_automaton_textures():
    rnd = random.Random(1201)
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def box(u, v, w, h, d, rust_amount=0.18):
        b = M11aBox(im, rnd, u, v, w, h, d)
        b.fill(RA_IRON, RA_RUST, rust_amount)
        return b

    # 머리 8x8x8: 이마 판, 금 간 외눈 (호박색, 가운데에서 오른쪽 위로 금), 턱 환기구, 옆 귀 볼트
    head = box(0, 0, 8, 8, 8, 0.16)
    for face in ("front", "back", "right", "left"):
        head.hline(face, 0, RA_IRON[4])
        head.hline(face, 1, RA_IRON[2])
    for y in range(1, 7):
        for x in range(1, 7):
            dd = math.hypot(x - 3.5, y - 3.5)
            if dd < 2.6:
                head.put("front", x, y, "#2a1606" if dd > 1.7 else RA_GLOW[1])
                if dd <= 1.7:
                    c2_glow_px(glow, head, "front", x, y, RA_GLOW[2] if dd > 0.8 else RA_GLOW[4])
    # 금: 눈 가운데에서 오른쪽 위로, 이마 끝까지 (금 자리는 빛이 새지 않는다)
    for x, y in ((4, 3), (5, 2), (5, 1), (6, 0)):
        head.put("front", x, y, "#0a0604")
        glow.putpixel((head.faces["front"][0] + x, head.faces["front"][1] + y), (0, 0, 0, 0))
    head.put("front", 2, 4, "#0a0604")
    glow.putpixel((head.faces["front"][0] + 2, head.faces["front"][1] + 4), (0, 0, 0, 0))
    head.rect("front", 2, 7, 5, 7, RA_IRON[1])
    for x in (2, 4):
        head.put("front", x, 7, RA_IRON[0])
    for face in ("right", "left"):
        head.rect(face, 3, 3, 4, 4, RA_IRON[4])
        head.put(face, 4, 4, RA_IRON[0])
    head.rivets("top", [(1, 1), (5, 1), (1, 5), (5, 5)], *RA_RIVET)
    head.drips("back", RA_RUST, 3)
    # 안테나 1x3x1: 끝이 빛남
    ant = M11aBox(im, rnd, 32, 0, 1, 3, 1)
    ant.fill(RA_IRON[2:], bevel=False)
    ant.put("top", 0, 0, RA_GLOW[3])
    for face in ("front", "back", "right", "left"):
        ant.put(face, 0, 0, RA_GLOW[2])
        c2_glow_px(glow, ant, face, 0, 0, RA_GLOW[3])
    c2_glow_px(glow, ant, "top", 0, 0, RA_GLOW[4])

    # 어깨 갑옷 5x4x5
    paul = box(40, 0, 5, 4, 5, 0.22)
    for face in ("front", "back", "right", "left"):
        paul.hline(face, 3, RA_IRON[0])
        paul.rivets(face, [(0, 0), (3, 0)], *RA_RIVET)
    paul.rivets("top", [(1, 1), (3, 3)], *RA_RIVET)

    # 몸통 10x12x6: 판 이음 두 줄, 리벳 테두리, 녹 줄기, 배의 번호판
    torso = box(0, 16, 10, 12, 6, 0.2)
    for face in ("front", "back", "right", "left"):
        fw, fh = torso.size(face)
        torso.hline(face, 8, RA_IRON[1])
        torso.hline(face, 9, RA_IRON[4])
        m11a_edge_rivets(torso, face, *RA_RIVET, step=3)
        torso.drips(face, RA_RUST, 2 if fw > 6 else 1)
    torso.rect("front", 3, 9, 6, 10, RA_IRON[1])
    torso.put("front", 4, 10, RA_IRON[4])
    torso.put("front", 5, 9, RA_IRON[4])
    torso.vline("top", 5, RA_IRON[1])

    # 가슴 계기판 8x5x1: 놋쇠 테 안에 둥근 계기 둘 (바늘이 빛남)
    plate = M11aBox(im, rnd, 32, 16, 8, 5, 1)
    plate.fill(["#2e2210", "#4a3818", "#6e5524", "#94733a", "#b89350"])
    for cx in (2, 5):
        for y in range(5):
            for x in range(cx - 1, cx + 2):
                dd = math.hypot(x - cx, y - 2)
                if dd < 1.6:
                    plate.put("front", x, y, "#150f06" if dd > 0.9 else RA_GLOW[0])
                    if dd < 0.9:
                        c2_glow_px(glow, plate, "front", x, y, RA_GLOW[2])
        plate.put("front", cx, 1, RA_GLOW[3])
        c2_glow_px(glow, plate, "front", cx, 1, RA_GLOW[4])
    plate.vline("front", 7, "#2e2210")
    plate.vline("front", 0, "#2e2210")

    # 굴뚝 2x4x2: 그을린 끝, 안에 불씨
    stack = box(50, 16, 2, 4, 2, 0.25)
    for face in ("front", "back", "right", "left"):
        stack.hline(face, 0, RA_IRON[0])
        stack.hline(face, 1, RA_IRON[4])
    stack.rect("top", 0, 0, 1, 1, "#3a1404")
    stack.put("top", 0, 0, RA_GLOW[2])
    for x in range(2):
        for y in range(2):
            c2_glow_px(glow, stack, "top", x, y, RA_GLOW[2] if (x + y) % 2 == 0 else RA_GLOW[1])

    # 등 보일러 6x7x3: 둥근 뚜껑 + 압력 밸브
    boiler = box(32, 22, 6, 7, 3, 0.25)
    for face in ("back", "right", "left", "front"):
        boiler.hline(face, 0, RA_IRON[4])
        boiler.hline(face, 6, RA_IRON[0])
    for y in range(1, 6):
        for x in range(0, 6):
            dd = math.hypot(x - 2.5, y - 3.0)
            if 1.7 < dd < 2.6:
                boiler.put("back", x, y, "#6e5524")
    boiler.put("back", 2, 3, "#94733a")
    boiler.put("back", 3, 3, "#4a3818")
    boiler.rivets("top", [(0, 0), (4, 1)], *RA_RIVET)

    # 다리 4x12x4: 무릎 이음 · 정강이 판
    leg = box(0, 34, 4, 12, 4, 0.18)
    for face in ("front", "back", "right", "left"):
        leg.hline(face, 5, RA_IRON[1])
        leg.hline(face, 6, RA_IRON[4])
        leg.rivets(face, [(0, 1), (2, 8)], *RA_RIVET)
    leg.rect("front", 1, 2, 2, 4, RA_IRON[4])
    # 팔 4x12x4: 팔꿈치 이음 · 피스톤 줄
    arm = box(16, 34, 4, 12, 4, 0.2)
    for face in ("front", "back", "right", "left"):
        arm.hline(face, 5, RA_IRON[1])
        arm.hline(face, 6, RA_IRON[4])
    arm.vline("right", 1, "#6e5524", 1, 9)
    arm.vline("left", 2, "#6e5524", 1, 9)
    arm.vline("back", 1, "#94733a", 2, 8)
    # 발 5x3x5: 앞코 판
    foot = box(32, 34, 5, 3, 5, 0.22)
    for face in ("front", "back", "right", "left"):
        foot.hline(face, 0, RA_IRON[4])
        foot.hline(face, 2, RA_IRON[0])
    foot.rivets("front", [(0, 1), (3, 1)], *RA_RIVET)
    foot.rivets("top", [(1, 1), (3, 3)], *RA_RIVET)
    # 주먹 5x4x5: 손가락 마디
    fist = box(32, 42, 5, 4, 5, 0.15)
    for face in ("front", "back", "right", "left"):
        for x in (1, 3):
            fist.vline(face, x, RA_IRON[1], 1, 3)
        fist.hline(face, 0, RA_IRON[4])
    fist.rect("bottom", 1, 1, 3, 3, RA_IRON[1])
    for x in (0, 2, 4):
        fist.put("front", x, 1, RA_IRON[5])
    return im, glow


def ember_wisp_textures():
    rnd = random.Random(1202)
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def lava_cracks(b, face, count):
        fw, fh = b.size(face)
        for _ in range(count):
            x, y = rnd.randrange(fw), rnd.randrange(fh)
            for i in range(rnd.randint(2, 4)):
                if not (0 <= x < fw and 0 <= y < fh):
                    break
                b.put(face, x, y, EW_FIRE[1])
                c2_glow_px(glow, b, face, x, y, EW_FIRE[2] if i % 2 else EW_FIRE[3])
                if rnd.random() < 0.5:
                    x += rnd.choice((-1, 1))
                else:
                    y += rnd.choice((-1, 1))

    # 핵 6x6x6: 숯덩이에 용암 금, 앞면은 두 눈 (밝은 노랑) + 입 틈
    core = M11aBox(im, rnd, 0, 0, 6, 6, 6)
    core.fill(EW_CHAR)
    for face in ("front", "back", "right", "left", "top", "bottom"):
        lava_cracks(core, face, 1 if face == "front" else 3)
    core.rect("front", 0, 1, 5, 4, EW_CHAR[1])
    for x in (1, 4):
        core.put("front", x, 2, EW_FIRE[4])
        core.put("front", x, 3, EW_FIRE[2])
        c2_glow_px(glow, core, "front", x, 2, EW_FIRE[5])
        c2_glow_px(glow, core, "front", x, 3, EW_FIRE[3])
    for x in (2, 3):
        core.put("front", x, 5, EW_FIRE[1])
        c2_glow_px(glow, core, "front", x, 5, EW_FIRE[2])

    # 불꽃 2x5x2: 아래 주황 → 위 노랑 · 흰빛, 끝은 듬성듬성 (전부 빛남)
    flame = M11aBox(im, rnd, 24, 0, 2, 5, 2)
    for face, (fx, fy, fw, fh) in flame.faces.items():
        for y in range(fh):
            for x in range(fw):
                if face == "top":
                    col = EW_FIRE[5]
                elif face == "bottom":
                    col = EW_FIRE[1]
                else:
                    if y == 0 and rnd.random() < 0.5:
                        continue
                    col = EW_FIRE[[5, 4, 3, 3, 2][min(4, y)]]
                im.putpixel((fx + x, fy + y), m11a_hex(col))
                glow.putpixel((fx + x, fy + y), m11a_hex(col))

    # 꼬리 3x4x3 · 끝 1x3x1: 위는 주황, 아래로 어두워지며 듬성 (빛은 위쪽만)
    for (u, v, w, h, d) in ((32, 0, 3, 4, 3), (44, 0, 1, 3, 1)):
        tail = M11aBox(im, rnd, u, v, w, h, d)
        for face, (fx, fy, fw, fh) in tail.faces.items():
            for y in range(fh):
                for x in range(fw):
                    t = y / max(1, fh - 1)
                    if face in ("top", "bottom"):
                        t = 0.0 if face == "top" else 1.0
                    if u == 44:
                        t = 0.5 + t * 0.5
                    if t > 0.75 and rnd.random() < 0.45:
                        continue
                    col = EW_FIRE[3] if t < 0.25 else (EW_FIRE[2] if t < 0.55 else (EW_FIRE[1] if t < 0.8 else EW_FIRE[0]))
                    im.putpixel((fx + x, fy + y), m11a_hex(col))
                    if t < 0.7:
                        glow.putpixel((fx + x, fy + y), m11a_hex(col))

    # 불씨 2x2x2 · 3x3x3: 달아오른 덩어리 (가운데 밝게, 모서리 어둡게)
    for (u, v, s) in ((0, 12, 2), (8, 12, 3)):
        ember = M11aBox(im, rnd, u, v, s, s, s)
        for face, (fx, fy, fw, fh) in ember.faces.items():
            for y in range(fh):
                for x in range(fw):
                    edge = (x in (0, fw - 1)) + (y in (0, fh - 1))
                    col = EW_FIRE[4] if edge == 0 else (EW_FIRE[2] if edge == 1 else EW_FIRE[1])
                    if s == 2:
                        col = EW_FIRE[3] if (x + y) % 2 == 0 else EW_FIRE[2]
                    im.putpixel((fx + x, fy + y), m11a_hex(col))
                    glow.putpixel((fx + x, fy + y), m11a_hex(col))
    return im, glow


def void_shade_textures():
    rnd = random.Random(1203)
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def box(u, v, w, h, d):
        b = M11aBox(im, rnd, u, v, w, h, d)
        b.fill(VS_DARK[:5])
        return b

    def wisps(b, face, count, col=VS_DARK[4]):
        """연기처럼 위로 올라가는 옅은 줄."""
        fw, fh = b.size(face)
        for _ in range(count):
            x = rnd.randrange(fw)
            y = rnd.randrange(fh)
            for i in range(rnd.randint(2, 4)):
                b.put(face, x, y - i, col)
                if rnd.random() < 0.4:
                    x += rnd.choice((-1, 1))

    # 머리 6x7x6: 얼굴은 더 깊은 어둠, 바깥이 치켜 올라간 가는 두 눈 (자홍 빛), 아래로 흐르는 빛 자국
    head = box(0, 0, 6, 7, 6)
    head.rect("front", 0, 1, 5, 6, VS_DARK[0])
    head.rect("front", 1, 2, 4, 5, "#030206")
    for x, y in ((0, 2), (1, 3), (4, 3), (5, 2)):
        head.put("front", x, y, VS_GLOW[3])
        c2_glow_px(glow, head, "front", x, y, VS_GLOW[4])
    for x, y in ((1, 2), (4, 2)):
        head.put("front", x, y, VS_GLOW[2])
        c2_glow_px(glow, head, "front", x, y, VS_GLOW[2])
    for x, y in ((1, 4), (4, 4), (1, 5)):
        head.put("front", x, y, VS_GLOW[0])
        c2_glow_px(glow, head, "front", x, y, VS_GLOW[1])
    for face in ("right", "left", "back", "top"):
        wisps(head, face, 3)
    # 뿔 1x3x1: 검정, 끝은 보라
    horn = M11aBox(im, rnd, 40, 14, 1, 3, 1)
    horn.fill(VS_DARK[:4], bevel=False)
    horn.put("top", 0, 0, VS_GLOW[1])
    for face in ("front", "back", "right", "left"):
        horn.put(face, 0, 0, VS_GLOW[0])

    # 몸통 6x12x3: 가슴 가운데 세로 균열 (빛남), 갈비 같은 옅은 줄
    torso = box(0, 13, 6, 12, 3)
    for y in (3, 5, 7):
        torso.hline("front", y, VS_DARK[3], 0, 1)
        torso.hline("front", y, VS_DARK[3], 4, 5)
    rift = [(3, 1), (3, 2), (2, 3), (2, 4), (3, 5), (3, 6), (2, 7), (3, 8)]
    for i, (x, y) in enumerate(rift):
        torso.put("front", x, y, VS_GLOW[1])
        c2_glow_px(glow, torso, "front", x, y, VS_GLOW[3] if i in (3, 4) else VS_GLOW[2])
    for face in ("back", "right", "left"):
        wisps(torso, face, 3)

    # 옷자락 7x10x4 · 자락 끝 5x6x3 · 띠 1x5x1: 아래로 갈수록 듬성듬성 투명 (찢어진 끝)
    for (u, v, w, h, d, holes) in ((24, 0, 7, 10, 4, 0.0), (24, 14, 5, 6, 3, 0.25), (44, 18, 1, 5, 1, 0.3)):
        robe = M11aBox(im, rnd, u, v, w, h, d)
        robe.fill(VS_DARK[1:6] if holes == 0.0 else VS_DARK[:5])
        for face in ("front", "back", "right", "left"):
            fx, fy, fw, fh = robe.faces[face]
            wisps(robe, face, 2)
            for y in range(fh):
                for x in range(fw):
                    t = y / max(1, fh - 1)
                    if holes and t > 0.5 and rnd.random() < holes * (t - 0.4) * 2.2:
                        im.putpixel((fx + x, fy + y), (0, 0, 0, 0))
            if holes == 0.0:
                robe.hline(face, 0, VS_DARK[5])
                # 옷자락 아래 가장자리의 희미한 보라 실
                for x in range(fw):
                    if rnd.random() < 0.3:
                        robe.put(face, x, fh - 1, VS_GLOW[0])
        bw, bh = robe.size("bottom")
        robe.rect("bottom", 0, 0, bw - 1, bh - 1, VS_DARK[0])

    # 팔 2x18x2: 마른 팔, 팔꿈치 마디 · 손목의 보라 띠
    arm = box(48, 0, 2, 18, 2)
    for face in ("front", "back", "right", "left"):
        arm.hline(face, 8, VS_DARK[4])
        arm.hline(face, 15, VS_GLOW[0])
        c2_glow_px(glow, arm, face, 0, 15, VS_GLOW[1])
        wisps(arm, face, 2)
    # 갈고리 손 3x4x2: 손끝이 보라로 빛남
    claw = box(48, 20, 3, 4, 2)
    for face in ("front", "back"):
        for x in (0, 2):
            claw.put(face, x, 3, VS_GLOW[2])
            c2_glow_px(glow, claw, face, x, 3, VS_GLOW[3])
        claw.put(face, 1, 3, (0, 0, 0, 0))
        claw.vline(face, 1, VS_DARK[0], 1, 2)
    for face in ("right", "left"):
        claw.put(face, 0, 3, VS_GLOW[1])
        c2_glow_px(glow, claw, face, 0, 3, VS_GLOW[2])
    claw.rect("bottom", 0, 0, 2, 1, VS_GLOW[0])
    return im, glow


def elite_aura_texture():
    """정예 테두리 무늬 (충전된 크리퍼처럼 흐른다): 회색 빗금 + 흩어진 반짝임. 색은 렌더러가 능력 색으로 곱한다."""
    rnd = random.Random(1204)
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for y in range(64):
        for x in range(64):
            band = (x + y * 2) % 16
            v = 0
            if band < 3:
                v = 200 - band * 50
            elif rnd.random() < 0.04:
                v = 255
            if v:
                im.putpixel((x, y), (v, v, v, v))
    return im


def spawn_egg_icon(base, spots, light, dark, seed):
    """생성 알: 바닐라 알 윤곽 (16x16) + 무늬 점, 밝은 쪽 하이라이트."""
    rows = [
        "......aaaa......",
        ".....abbbba.....",
        "....abbbbbba....",
        "...abbbbbbbba...",
        "...abbbbbbbba...",
        "..abbbbbbbbbba..",
        "..abbbbbbbbbba..",
        "..abbbbbbbbbba..",
        ".abbbbbbbbbbbba.",
        ".abbbbbbbbbbbba.",
        ".abbbbbbbbbbbba.",
        ".abbbbbbbbbbbba.",
        "..abbbbbbbbbba..",
        "..abbbbbbbbbba..",
        "...abbbbbbbba...",
        ".....aaaaaa.....",
    ]
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    inside = [(x, y) for y, row in enumerate(rows) for x, ch in enumerate(row) if ch == "b"]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "a":
                im.putpixel((x, y), hex_px(dark))
            elif ch == "b":
                im.putpixel((x, y), hex_px(base))
    for _ in range(7):
        x, y = rnd.choice(inside)
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            if (x + dx, y + dy) in inside and rnd.random() < 0.85:
                im.putpixel((x + dx, y + dy), hex_px(spots))
    for x, y in ((5, 3), (5, 4), (4, 5), (4, 6), (6, 2)):
        im.putpixel((x, y), hex_px(light))
    return im


def void_shard_icon():
    """공허 조각: 비스듬히 깨진 검보라 결정 조각, 가운데 금이 빛남."""
    return effect_art([
        "..................",
        "..................",
        "..........aa......",
        ".........abca.....",
        "........abcdca....",
        ".......abcdeca....",
        "......abcdeeca....",
        ".....abcdfedba....",
        "....abccfdcba.....",
        "....abcfddcba.....",
        "...abcfdccba......",
        "...abfdcbba.......",
        "..abfdcbaa........",
        "..abdcba..........",
        "..abbaa...........",
        "...aa.............",
        "..................",
        "..................",
    ], {"a": "#0d0914", "b": "#2c213b", "c": "#4a2f6e", "d": "#73479e", "e": "#a77ad6", "f": "#e09cff"}).crop((1, 1, 17, 17))


def main_ex3_c2(made):
    for name, maker in (("rust_automaton", rust_automaton_textures), ("ember_wisp", ember_wisp_textures),
                        ("void_shade", void_shade_textures)):
        im, glow = maker()
        made.append(save(im, f"entity/{name}.png"))
        made.append(save(glow, f"entity/{name}_glow.png"))
    made.append(save(elite_aura_texture(), "entity/elite_aura.png"))
    made.append(save(spawn_egg_icon("#5d6672", "#bf5f26", "#97a1ac", "#1d2026", 31), "item/rust_automaton_spawn_egg.png"))
    made.append(save(spawn_egg_icon("#2c1a14", "#ff7a1a", "#ffb340", "#120a08", 32), "item/ember_wisp_spawn_egg.png"))
    made.append(save(spawn_egg_icon("#16101f", "#b04aff", "#3a2c4c", "#06040a", 33), "item/void_shade_spawn_egg.png"))
    made.append(save(void_shard_icon(), "item/void_shard.png"))


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
    main_m3(made)
    main_gui(made)
    main_redesign(made)
    main_m4(made)
    main_bundle1(made)
    main_bundle2(made)
    main_bundle3(made)
    main_bundle4(made)
    main_m10(made)
    main_m9a(made)
    main_m9b(made)
    main_m8a(made)
    main_m7(made)
    main_m5(made)
    main_m11a(made)
    main_m8b(made)
    main_m6(made)
    main_m8c(made)
    main_fin(made)
    main_ex3_c3(made)
    main_ex3_c2(made)

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
