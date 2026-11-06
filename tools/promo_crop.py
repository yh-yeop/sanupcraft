"""소개 페이지용 스크린샷을 사이트 이미지로 바꾼다.

SANUPCRAFT_PROMO=1 ./gradlew runClientGameTest 로 찍은 promo-*.png 를
1600px webp + 400px 썸네일(-t.webp), 대표 이미지(hero.webp), 공유 미리보기(og.jpg)로 만든다.
화면(GUI) 장면 promo-gui-*.png 는 GUI 배율 2로 찍혀 있으니 가운데 800x450을 잘라 2배(계단 모양 그대로)로 키운다.

    python tools/promo_crop.py build/run/clientGameTest/screenshots ../yh-yeop.github.io/assets/sanupcraft
"""
import sys
from pathlib import Path

from PIL import Image

HERO = "factory-close"  # 첫 화면 배경 (글자가 왼쪽에 얹혀서 볼거리가 오른쪽인 장면)
OG = "factory"  # 공유 미리보기 (전체가 보이는 장면)


def save(img: Image.Image, out: Path, name: str) -> None:
    img.save(out / f"{name}.webp", quality=84, method=6)
    img.resize((400, 225), Image.LANCZOS).save(out / f"{name}-t.webp", quality=80, method=6)


def main() -> None:
    src, out = Path(sys.argv[1]), Path(sys.argv[2])
    out.mkdir(parents=True, exist_ok=True)
    for f in sorted(src.glob("promo-*.png")):
        img = Image.open(f).convert("RGB")
        name = f.stem.removeprefix("promo-")
        if name.startswith("gui-"):
            name = name.removeprefix("gui-")
            w, h = img.size
            left, top = (w - 800) // 2, (h - 450) // 2
            img = img.crop((left, top, left + 800, top + 450)).resize((1600, 900), Image.NEAREST)
        else:
            img = img.resize((1600, 900), Image.LANCZOS)
        save(img, out, name)
        if name == HERO:
            img.save(out / "hero.webp", quality=84, method=6)
        if name == OG:
            # 1200x630 (1.905:1): 위아래를 조금 잘라 낸다
            h = round(1600 / 1.905)
            top = (900 - h) // 2
            img.crop((0, top, 1600, top + h)).resize((1200, 630), Image.LANCZOS).save(out / "og.jpg", quality=86)
        print(name)


if __name__ == "__main__":
    main()
