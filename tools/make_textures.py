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


def upgrade_icon(void):
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
    made.append(save(upgrade_icon(False), "item/drawer_upgrade.png"))
    made.append(save(upgrade_icon(True), "item/drawer_void_upgrade.png"))
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
