"""효과음 합성기 (확장 M15 R8).

바닐라 소리를 다시 쓰던 효과음을 numpy · scipy로 직접 합성한 소리로 바꾼다.
더하기 · 빼기 합성, 거른 잡음, 엔벨로프, 높이 미끄러짐, 간단한 FM. 난수는 소리마다 고정 시드라 몇 번을 돌려도 같은 소리가 나온다.
결과는 Ogg Vorbis 모노 44.1 kHz: src/main/resources/assets/sanupcraft/sounds/<분류>/<이름>[_n].ogg
(sounds.json의 "sanupcraft:<분류>/<이름>_n"). 파일 이름을 바꾸지 않으면 사람이 만든 소리로 덮어써도 된다.

    python -m pip install --user soundfile   # libsndfile(Ogg Vorbis 포함)이 같이 들어온다
    python tools/make_sounds.py              # 전부 다시 만든다
    python tools/make_sounds.py boss/        # 이름에 이 글자가 든 것만
    python tools/make_sounds.py --wav        # 귀로 비교할 .wav도 build/sounds_wav 에 쓴다

새 소리는 SOUNDS 표에 (경로, 변형 수, 함수) 한 줄과 함수 하나를 더하고 sounds.json에 이어 준다.
"""
import os
import sys
import zlib

import numpy as np
from scipy import signal

try:
    import soundfile as sf
except ImportError:  # pragma: no cover
    sys.exit("soundfile이 없다: python -m pip install --user soundfile")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "sanupcraft", "sounds")
WAV_OUT = os.path.join(ROOT, "build", "sounds_wav")
SR = 44100
NYQ = SR / 2


# ---------------------------------------------------------------- 기본 재료

def n_of(dur):
    return int(round(dur * SR))


def tt(dur):
    return np.arange(n_of(dur)) / SR


def noise(rng, dur):
    return rng.standard_normal(n_of(dur))


def _sos(kind, f, order=2):
    if kind == "bandpass":
        lo, hi = f
        f = (max(lo, 10.0) / NYQ, min(hi, NYQ * 0.95) / NYQ)
    else:
        f = min(max(f, 10.0), NYQ * 0.95) / NYQ
    return signal.butter(order, f, btype=kind, output="sos")


def lp(x, f, order=2):
    return signal.sosfilt(_sos("lowpass", f, order), x)


def hp(x, f, order=2):
    return signal.sosfilt(_sos("highpass", f, order), x)


def bp(x, lo, hi, order=2):
    return signal.sosfilt(_sos("bandpass", (lo, hi), order), x)


def reson(x, f, q):
    """좁은 공진 (peak 필터). 금속 울림 · 모음 같은 봉우리."""
    b, a = signal.iirpeak(min(f, NYQ * 0.9) / NYQ, q)
    return signal.lfilter(b, a, x)


def svf(x, fc, q=0.7, mode="lp"):
    """시간에 따라 차단 주파수가 바뀌는 상태 변수 필터. fc는 표본마다 값(배열)."""
    fc = np.broadcast_to(np.asarray(fc, dtype=float), x.shape)
    f = 2.0 * np.sin(np.pi * np.clip(fc, 20.0, SR / 6.5) / SR)
    damp = 1.0 / q
    low = band = 0.0
    out = np.empty_like(x)
    pick = {"lp": 0, "bp": 1, "hp": 2}[mode]
    for i in range(len(x)):
        fi = f[i]
        low += fi * band
        high = x[i] - low - damp * band
        band += fi * high
        out[i] = low if pick == 0 else band if pick == 1 else high
    return out


def phase_of(freq):
    """주파수(상수 또는 표본마다 배열)를 위상으로."""
    return 2.0 * np.pi * np.cumsum(freq) / SR


def sine(freq, dur=None):
    if np.isscalar(freq):
        return np.sin(2.0 * np.pi * freq * tt(dur))
    return np.sin(phase_of(freq))


def saw(freq, dur=None, top=9000.0, max_k=80):
    """대역 제한 톱니 (더하기 합성). 위쪽 배음은 top 근처에서 부드럽게 줄인다."""
    freq = np.full(n_of(dur), float(freq)) if np.isscalar(freq) else np.asarray(freq, dtype=float)
    ph = phase_of(freq)
    out = np.zeros_like(ph)
    for k in range(1, max_k + 1):
        fk = freq * k
        w = np.clip((top - fk) / (top * 0.25), 0.0, 1.0)
        if not w.any():
            break
        out += w * np.sin(k * ph) / k
    return out * 0.6


def pulse_train(freq, dur, jitter, rng):
    """흔들리는 펄스열 (마찰 · 으르렁의 거친 맛). jitter는 주기의 비율."""
    out = np.zeros(n_of(dur))
    t = 0.0
    while t < dur:
        i = int(t * SR)
        if i < len(out):
            out[i] = 1.0 + rng.uniform(-0.3, 0.3)
        f = freq[min(i, len(freq) - 1)] if not np.isscalar(freq) else freq
        t += (1.0 / f) * (1.0 + rng.uniform(-jitter, jitter))
    return out


def fm(fc, fm_, index, dur=None):
    """간단한 FM: 반송파 fc, 변조파 fm_, 변조 지수 index (셋 다 상수 또는 배열)."""
    n = n_of(dur) if dur is not None else max(np.size(a) for a in (fc, fm_, index))
    fc = np.broadcast_to(np.asarray(fc, dtype=float), (n,))
    fm_ = np.broadcast_to(np.asarray(fm_, dtype=float), (n,))
    index = np.broadcast_to(np.asarray(index, dtype=float), (n,))
    return np.sin(phase_of(fc) + index * np.sin(phase_of(fm_)))


def line(dur, *points):
    """(시각, 값) 점을 이은 선. 시각은 0~1 비율."""
    t = np.linspace(0.0, 1.0, n_of(dur))
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    return np.interp(t, xs, ys)


def expline(dur, a, b):
    """a에서 b로 지수 곡선 (높이 미끄러짐)."""
    return a * (b / a) ** np.linspace(0.0, 1.0, n_of(dur))


def decay(dur, tau):
    return np.exp(-tt(dur) / tau)


def adsr(dur, a, d, s, r):
    n = n_of(dur)
    env = np.zeros(n)
    na, nd, nr = n_of(a), n_of(d), n_of(r)
    ns = max(n - na - nd - nr, 0)
    parts = [np.linspace(0, 1, na, endpoint=False), np.linspace(1, s, nd, endpoint=False), np.full(ns, s), np.linspace(s, 0, nr)]
    env[:min(n, sum(map(len, parts)))] = np.concatenate(parts)[:n]
    return env


def norm(x, peak=1.0):
    m = np.max(np.abs(x))
    return x * (peak / m) if m > 0 else x


def place(buf, x, at, gain=1.0):
    i = n_of(at)
    if i >= len(buf):
        return buf
    m = min(len(x), len(buf) - i)
    buf[i:i + m] += gain * x[:m]
    return buf


def metal_hit(rng, dur, base, partials=(1.0, 2.32, 3.86, 5.41, 7.1), tau=0.25, bright=1.0):
    """금속을 친 소리: 어긋난 배음 + 짧은 딸깍."""
    t = tt(dur)
    out = np.zeros_like(t)
    for k, r in enumerate(partials):
        f = base * r * (1.0 + rng.uniform(-0.01, 0.01))
        if f >= NYQ * 0.9:
            continue
        out += np.sin(2 * np.pi * f * t + rng.uniform(0, 6.28)) * np.exp(-t / (tau / (1 + 0.6 * k * bright))) / (1 + k * 0.5)
    click = hp(noise(rng, dur), 2000) * np.exp(-t / 0.004)
    return out + 0.5 * click


def thump(dur, f0, f1, tau):
    """낮은 쿵: 높이가 내려가는 사인."""
    return np.sin(phase_of(expline(dur, f0, f1))) * decay(dur, tau)


def room(x, rng, length=0.5, wet=0.22, damp=3000.0):
    """간단한 잔향: 지수로 줄어드는 거른 잡음과 합성곱."""
    ir = lp(noise(rng, length), damp) * np.exp(-tt(length) / (length / 5.0))
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    w = signal.fftconvolve(x, ir)[:len(x)]
    return x * (1.0 - wet) + w * wet * 1.5


def finish(x, peak=0.9, fade_in=0.004, fade_out=0.03):
    """직류 빼기 · 앞뒤 페이드 (딸깍 방지) · 봉우리 맞춤."""
    x = hp(np.asarray(x, dtype=float), 25.0)
    n = len(x)
    fi, fo = max(n_of(fade_in), 1), max(n_of(fade_out), 1)
    x[:fi] *= np.linspace(0.0, 1.0, fi) ** 2
    x[-fo:] *= np.linspace(1.0, 0.0, fo) ** 2
    m = np.max(np.abs(x))
    if m > 0:
        x = x / m * peak
    return x.astype(np.float32)


# ---------------------------------------------------------------- 기계

def crusher(rng, v):
    """분쇄기: 돌을 가는 낮은 웅웅 + 롤러가 도는 덜컹 + 자갈 부서지는 바삭."""
    dur = 1.25
    t = tt(dur)
    roll = 5.5 + v * 1.3
    bump = 0.55 + 0.45 * np.abs(np.sin(np.pi * roll * t + rng.uniform(0, 3))) ** 3
    grind = bp(noise(rng, dur), 90, 700, 2) * bump
    grind += 0.35 * bp(noise(rng, dur), 900, 2600, 2) * bump ** 2
    hum_f = 52.0 + v * 6 + 1.5 * np.sin(2 * np.pi * 0.9 * t)
    hum = lp(saw(hum_f, dur, top=1200), 500) * 0.5
    out = grind * 1.0 + hum
    for _ in range(14 + v * 3):
        at = rng.uniform(0.02, dur - 0.08)
        d = rng.uniform(0.015, 0.05)
        c = bp(noise(rng, d), rng.uniform(900, 1800), rng.uniform(2800, 5200)) * decay(d, d / 4)
        place(out, c, at, rng.uniform(0.6, 1.6))
    return finish(out * adsr(dur, 0.05, 0.1, 0.9, 0.25), 0.85)


def press(rng, v):
    """압착기: 유압 판이 내려와 쿵 + 쇠 울림 + 압력 빠지는 치익."""
    dur = 0.8
    out = np.zeros(n_of(dur))
    whine = sine(expline(0.12, 260 + v * 30, 160), 0.12) * line(0.12, (0, 0), (0.7, 1), (1, 0.4)) * 0.18
    place(out, whine, 0.0)
    hit = 0.10
    place(out, thump(0.5, 110 - v * 8, 38, 0.13), hit, 1.0)
    place(out, lp(noise(rng, 0.06), 1800) * decay(0.06, 0.012), hit, 0.9)
    place(out, metal_hit(rng, 0.5, 310 + v * 45, tau=0.22), hit, 0.22)
    hiss = bp(noise(rng, 0.45), 2500, 9000) * adsr(0.45, 0.04, 0.1, 0.6, 0.3)
    place(out, hiss, hit + 0.12, 0.12 + 0.04 * v)
    return finish(out, 0.9)


def assembler(rng, v):
    """조립대: 집게 딸깍 몇 번 + 사이사이 작은 모터 위잉."""
    dur = 0.75
    out = np.zeros(n_of(dur))
    patterns = [(0.02, 0.17, 0.30, 0.55), (0.02, 0.12, 0.40, 0.50), (0.02, 0.25, 0.33, 0.58)]
    for i, at in enumerate(patterns[v % 3]):
        d = 0.05
        click = bp(noise(rng, d), 1800, 6000) * decay(d, 0.0035)
        ping = sine(rng.uniform(1700, 2600), d) * decay(d, 0.012) * 0.6
        thunk = sine(expline(d, 420, 180), d) * decay(d, 0.01) * 0.5
        place(out, click + ping + thunk, at, 1.0 if i % 2 == 0 else 0.75)
    w0, w1 = 0.07, 0.24 + 0.04 * v
    sweep = expline(w1 - w0, 320 + 20 * v, 560 + 40 * v)
    servo = lp(saw(sweep, w1 - w0, top=3000), 1400) * adsr(w1 - w0, 0.02, 0.05, 0.8, 0.05) * 0.35
    place(out, servo, w0)
    servo2 = lp(saw(sweep[::-1], w1 - w0, top=3000), 1400) * adsr(w1 - w0, 0.02, 0.05, 0.8, 0.05) * 0.25
    place(out, servo2, 0.62 - (w1 - w0) if v != 2 else 0.38)
    return finish(out, 0.85)


def precision(rng, v):
    """정밀 조립기: 전기 지직 (불꽃 튀는 잡음 + 웅 하는 60 Hz 배음 + 아래로 떨어지는 FM 찌릿)."""
    dur = 0.65
    t = tt(dur)
    hum = (np.sign(np.sin(2 * np.pi * 120 * t)) * 0.5 + np.sin(2 * np.pi * 240 * t) * 0.5)
    hum = bp(hum, 150, 2500) * adsr(dur, 0.03, 0.1, 0.7, 0.15) * 0.4
    out = hum.copy()
    nz = n_of(dur)
    sparks = np.zeros(nz)
    k = 0
    at = rng.uniform(0.0, 0.03)
    while at < dur - 0.06 and k < 40:
        d = rng.uniform(0.004, 0.02)
        burst = bp(noise(rng, d), rng.uniform(1800, 3200), 12000) * decay(d, d / 3)
        place(sparks, burst, at, rng.uniform(0.4, 1.0))
        at += rng.exponential(0.018 + 0.006 * v)
        k += 1
    out += sparks * 0.9
    zd = 0.18
    zap = fm(expline(zd, 2400 + 300 * v, 600), 90 + 30 * v, line(zd, (0, 6), (1, 1)), zd) * decay(zd, 0.06)
    place(out, zap, 0.05 + 0.12 * v, 0.55)
    return finish(out, 0.85)


def washer(rng, v):
    """세척기: 통이 돌며 물이 출렁 + 거품 보글 + 낮은 모터."""
    dur = 1.3
    t = tt(dur)
    slosh_rate = 2.2 + 0.4 * v
    slosh = 0.45 + 0.55 * (0.5 + 0.5 * np.sin(2 * np.pi * slosh_rate * t + rng.uniform(0, 6))) ** 2
    water = svf(noise(rng, dur), 500 + 900 * slosh, 1.2, "bp") * slosh
    water += 0.25 * bp(noise(rng, dur), 2000, 6000) * slosh ** 3
    motor = lp(saw(70 + 5 * v, dur, top=800), 300) * 0.25
    out = water * 0.6 + motor
    for _ in range(18 + 4 * v):
        d = rng.uniform(0.015, 0.045)
        f0 = rng.uniform(350, 1300)
        bub = sine(expline(d, f0, f0 * rng.uniform(1.6, 2.4)), d) * decay(d, d / 3)
        place(out, bub, rng.uniform(0.02, dur - 0.06), rng.uniform(0.15, 0.4))
    return finish(out * adsr(dur, 0.08, 0.1, 0.9, 0.3), 0.85)


def steam(rng, v):
    """증기 (보일러 · 터빈 · 정련기): 치익 새는 소리 + 밑의 쉬익 바람. 셋째는 짧게 픽 + 밸브 휘파람."""
    if v == 2:
        dur = 0.6
        env = adsr(dur, 0.01, 0.08, 0.5, 0.4)
        hiss = bp(noise(rng, dur), 3000, 11000) * env
        whistle = sine(2150 + 30 * np.sin(2 * np.pi * 7 * tt(dur)), dur) * adsr(dur, 0.02, 0.05, 0.4, 0.2) * 0.12
        return finish(hiss + whistle, 0.8)
    dur = 1.1 - 0.2 * v
    t = tt(dur)
    env = adsr(dur, 0.05 + 0.04 * v, 0.1, 0.8, 0.45)
    flutter = 1.0 + 0.15 * np.sin(2 * np.pi * (11 + 4 * v) * t) * rng.uniform(0.5, 1.0)
    hiss = bp(noise(rng, dur), 2200 + 600 * v, 10000) * env * flutter
    whoosh = svf(noise(rng, dur), line(dur, (0, 400), (0.3, 1100), (1, 500)), 0.9, "bp") * env * 0.6
    return finish(hiss + whoosh, 0.8)


def quarry(rng, v):
    """굴착기: 드릴 모터 위이잉 + 바위를 두드리는 드르륵 + 낮은 울림."""
    dur = 0.95
    t = tt(dur)
    f = 150 + 20 * v + 6 * np.sin(2 * np.pi * 3.0 * t) + line(dur, (0, -15), (0.15, 0), (1, 0))
    motor = bp(saw(f, dur, top=5000), 200, 3500) * 0.45
    rate = 24 + 3 * v
    chatter = bp(noise(rng, dur), 450, 2600) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * rate * t))) * rng.uniform(0.8, 1.0)
    chatter *= 0.6 + 0.4 * lp(np.abs(rng.standard_normal(len(t))), 30) * 2
    rumble = lp(noise(rng, dur), 140) * 2.0
    out = motor + chatter * 0.7 + rumble
    for _ in range(5):
        d = 0.08
        rock = lp(noise(rng, d), rng.uniform(1500, 3000)) * decay(d, 0.015)
        place(out, rock, rng.uniform(0.05, dur - 0.1), 1.2)
    return finish(out * adsr(dur, 0.04, 0.1, 0.9, 0.2), 0.85)


# ---------------------------------------------------------------- 보스

def guardian_charge(rng, v):
    """녹슨 수호자 돌진 예고: 녹슨 관절이 끼익 갈리다가 압력이 차오르는 소리, 끝에 철컥."""
    dur = 1.15
    t = tt(dur)
    creak_f = line(dur, (0, 38 + 6 * v), (0.6, 70 + 8 * v), (0.85, 55), (1, 50))
    creak = pulse_train(creak_f, dur, 0.25, rng)
    body = sum(reson(creak, f, q) * g for f, q, g in ((620 + 40 * v, 12, 1.0), (1450, 15, 0.7), (2550 - 100 * v, 18, 0.5)))
    body = body * adsr(dur, 0.05, 0.2, 0.8, 0.2)
    rise = sine(expline(0.8, 180, 720 + 80 * v), 0.8) * line(0.8, (0, 0), (1, 1)) ** 2 * (1 + 0.3 * np.sin(2 * np.pi * 12 * tt(0.8)))
    out = norm(lp(body, 4000), 0.8)
    place(out, rise, 0.2, 0.3)
    place(out, hp(noise(rng, 0.8), 3000) * line(0.8, (0, 0), (1, 1)) ** 3, 0.25, 0.15)
    place(out, metal_hit(rng, 0.25, 420 + 60 * v, tau=0.12), dur - 0.24, 0.45)
    return finish(room(out, rng, 0.4, 0.18), 0.9)


def warden_burst(rng, v):
    """용광로 파수꾼 분출 예고: 화로가 숨을 들이켜는 소리 (높아지는 쉬익 + 깔리는 불 소리)."""
    dur = 1.3
    t = tt(dur)
    center = expline(dur, 260 + 40 * v, 3200 + 400 * v)
    swell = line(dur, (0, 0.2), (0.85, 1.0), (1, 0.6)) ** 1.3
    inhale = svf(noise(rng, dur), center, 1.6, "bp") * swell
    roar = lp(noise(rng, dur), 180) * 3.0 * line(dur, (0, 0.3), (0.9, 1.0), (1, 0.7))
    rumble = sine(42 + 4 * v + 3 * np.sin(2 * np.pi * 5 * t), dur) * 0.35 * swell
    crackle = np.zeros(len(t))
    for _ in range(25):
        d = 0.012
        place(crackle, hp(noise(rng, d), 2500) * decay(d, 0.003), rng.uniform(0, dur - 0.02), rng.uniform(0.3, 1.0))
    out = inhale + roar * 0.5 + rumble + crackle * 0.4
    return finish(room(out, rng, 0.5, 0.2), 0.9, fade_out=0.06)


def boss_phase(rng, v):
    """보스 공용 화남: 낮은 기계 으르렁 + 경보처럼 오르내리는 울음."""
    dur = 1.5
    t = tt(dur)
    f = line(dur, (0, 70), (0.25, 95), (0.7, 88), (1, 60)) + 4 * np.sin(2 * np.pi * 6 * t)
    growl = saw(f, dur, top=4000) + saw(f * 1.01, dur, top=4000)
    growl = reson(growl, 650, 3) + reson(growl, 1100, 4) * 0.6 + lp(growl, 400)
    siren = sine(line(dur, (0, 400), (0.4, 620), (1, 380)), dur) * 0.15
    out = growl * adsr(dur, 0.08, 0.3, 0.7, 0.5) + siren * adsr(dur, 0.2, 0.2, 0.8, 0.4)
    return finish(room(out, rng, 0.6, 0.25), 0.9)


def guardian_rage(rng, v):
    """녹슨 수호자 화남: 쇠가 갈리는 거친 포효 (톱니 화음 + 고리 변조 + 모음 봉우리)."""
    dur = 1.8
    t = tt(dur)
    f = line(dur, (0, 80), (0.2, 118), (0.6, 105), (1, 62)) * (1 + 0.025 * np.sin(2 * np.pi * 7.5 * t))
    tone = saw(f, dur, top=5000) + 0.7 * saw(f * 1.5, dur, top=5000) + 0.5 * saw(f * 0.5, dur, top=3000)
    tone *= 0.6 + 0.4 * np.sin(2 * np.pi * 37 * t)  # 녹슨 떨림
    voice = reson(tone, 720, 4) + 0.8 * reson(tone, 1240, 5) + 0.4 * reson(tone, 2600, 8) + 0.6 * lp(tone, 300)
    grit = bp(pulse_train(f * 0.5, dur, 0.4, rng), 800, 4000) * 0.8
    out = (voice + grit) * adsr(dur, 0.06, 0.4, 0.75, 0.7)
    place(out, metal_hit(rng, 0.6, 230, tau=0.3), 0.0, 0.8)
    return finish(room(out, rng, 0.7, 0.28), 0.92)


def warden_rage(rng, v):
    """용광로 파수꾼 화남: 불길이 울부짖는 포효 + 기적 소리 같은 증기 비명."""
    dur = 1.8
    t = tt(dur)
    f = line(dur, (0, 55), (0.25, 72), (1, 48))
    growl = pulse_train(f, dur, 0.15, rng)
    growl = lp(growl, 1500) * 3 + reson(growl, 420, 4) * 2 + reson(growl, 950, 6)
    fire = svf(noise(rng, dur), line(dur, (0, 300), (0.3, 1400), (1, 400)), 0.8, "bp") * 1.2
    wf = line(dur, (0, 820), (0.2, 1040), (1, 880)) * (1 + 0.012 * np.sin(2 * np.pi * 6 * t))
    whistle = (sine(wf) + 0.4 * sine(wf * 1.5) + 0.3 * sine(wf * 2.02)) * adsr(dur, 0.15, 0.3, 0.7, 0.6) * 0.18
    hiss = bp(noise(rng, dur), 3500, 11000) * adsr(dur, 0.1, 0.3, 0.6, 0.8) * 0.2
    out = (growl * 0.5 + fire) * adsr(dur, 0.05, 0.4, 0.8, 0.7) + whistle + hiss
    return finish(room(out, rng, 0.8, 0.3), 0.92)


def colossus_rage(rng, v):
    """공허 거상 화남: 깊은 FM 포효가 부풀다 내려앉고 위로 공허 반짝임."""
    dur = 2.0
    t = tt(dur)
    f = line(dur, (0, 45), (0.3, 62), (1, 34))
    sub = sine(f) * 0.8
    body = fm(f * 2, f * 1.41, line(dur, (0, 1), (0.3, 7), (1, 2)))
    detuned = saw(f * 2, dur, top=2500) + saw(f * 2.013, dur, top=2500)
    detuned = svf(detuned, line(dur, (0, 200), (0.3, 2200), (1, 300)), 1.5, "lp")
    shimmer = np.zeros(len(t))
    for r in (7.3, 9.7, 12.1, 15.9):
        shimmer += sine(f * r * (1 + 0.01 * np.sin(2 * np.pi * 0.7 * r * t))) / r
    shimmer *= adsr(dur, 0.4, 0.3, 0.6, 0.8) * 0.9
    out = (sub + body * 0.5 + detuned * 0.6) * adsr(dur, 0.15, 0.4, 0.8, 0.8) + shimmer
    return finish(room(out, rng, 1.0, 0.35, 2500), 0.92)


def guardian_death(rng, v):
    """녹슨 수호자 쓰러짐: 모터가 잦아들며 서고, 부품이 떨어져 철컹, 마지막에 무거운 와르르."""
    dur = 2.3
    t = tt(dur)
    f = expline(1.5, 190, 28)
    motor = lp(saw(f, 1.5, top=3000), 1200) * line(1.5, (0, 1), (1, 0)) ** 1.2
    out = np.zeros(len(t))
    place(out, motor, 0.0, 0.7)
    at = 0.25
    gap = 0.32
    for i in range(6):
        place(out, metal_hit(rng, 0.6, rng.uniform(260, 620), tau=0.25), at, 0.7 - 0.05 * i)
        at += gap
        gap *= 0.72
    crash = 1.55
    place(out, thump(0.7, 90, 30, 0.22), crash, 1.2)
    place(out, lp(noise(rng, 0.7), 2500) * decay(0.7, 0.12), crash, 0.8)
    for _ in range(5):
        place(out, metal_hit(rng, 0.5, rng.uniform(300, 900), tau=0.18), crash + rng.uniform(0.0, 0.25), 0.35)
    return finish(room(out, rng, 0.8, 0.3), 0.92, fade_out=0.15)


def warden_death(rng, v):
    """용광로 파수꾼 꺼짐: 불이 꺼지며 길게 빠지는 증기 + 내려앉는 울부짖음 + 지글지글, 마지막 쿵."""
    dur = 2.4
    t = tt(dur)
    howl_f = expline(1.6, 80, 30)
    howl = pulse_train(howl_f, 1.6, 0.2, rng)
    howl = (lp(howl, 900) * 3 + reson(howl, 380, 4) * 2) * line(1.6, (0, 1), (1, 0))
    hiss = bp(noise(rng, dur), 2500, 11000) * line(dur, (0, 0), (0.05, 1), (1, 0)) ** 1.5
    whoosh = svf(noise(rng, dur), expline(dur, 1800, 250), 0.9, "bp") * line(dur, (0, 0), (0.05, 1), (1, 0)) * 0.8
    sizzle = np.zeros(len(t))
    for _ in range(40):
        d = 0.01
        place(sizzle, hp(noise(rng, d), 3000) * decay(d, 0.002), rng.uniform(0.2, dur - 0.1), rng.uniform(0.2, 0.8))
    out = hiss * 0.4 + whoosh + sizzle * 0.25
    place(out, howl, 0.0, 0.6)
    place(out, thump(0.6, 70, 28, 0.2), 1.4, 1.0)
    return finish(room(out, rng, 0.9, 0.3), 0.92, fade_out=0.2)


def colossus_death(rng, v):
    """공허 거상 무너짐: 아래로 가라앉는 FM 울림 + 내려가는 반짝임, 빨려 들어갔다가 쿵."""
    dur = 2.5
    t = tt(dur)
    f = expline(dur, 120, 24)
    drone = fm(f, f * 0.5, line(dur, (0, 5), (1, 1))) * line(dur, (0, 0.2), (0.15, 1), (0.75, 0.8), (1, 0))
    sub = sine(f * 0.5) * line(dur, (0, 0), (0.2, 1), (0.8, 0.6), (1, 0)) * 0.8
    gl = np.zeros(len(t))
    for r in (11.0, 13.7, 17.2):
        gl += sine(f * r) / r * 4
    gl *= line(dur, (0, 0), (0.1, 1), (0.7, 0.3), (1, 0)) * 0.5
    implode = 1.75
    suck = lp(noise(rng, 0.45), 1500) * line(0.45, (0, 0), (1, 1)) ** 3
    out = drone + sub + gl
    place(out, suck, implode - 0.45, 0.9)
    place(out, thump(0.75, 75, 22, 0.25), implode, 1.4)
    place(out, lp(noise(rng, 0.5), 900) * decay(0.5, 0.1), implode, 0.6)
    return finish(room(out, rng, 1.0, 0.35, 2000), 0.92, fade_out=0.2)


def colossus_pull(rng, v):
    """공허 거상 끌어당기기 예고: 공허가 차오르는 소리 (높아지는 FM 울림 + 부풀어 오르는 바람)."""
    dur = 1.6
    t = tt(dur)
    f = expline(dur, 55 + 8 * v, 170 + 30 * v)
    idx = line(dur, (0, 0.5), (1, 6))
    drone = fm(f, f * (1.5 if v == 0 else 1.25), idx)
    beat = sine(f * 2) + sine(f * 2 * 1.008)
    wind = svf(noise(rng, dur), expline(dur, 300, 2600), 2.0, "bp")
    swell = line(dur, (0, 0.12), (1, 1)) ** 1.3
    out = (drone * 0.7 + beat * 0.3 + wind * 0.8) * swell
    out *= 1 + 0.25 * np.sin(phase_of(line(dur, (0, 3), (1, 14))))  # 점점 빨라지는 떨림
    return finish(room(out, rng, 0.8, 0.3, 2500), 0.9, fade_out=0.05)


def colossus_crystals(rng, v):
    """공허 거상 수정 소환: 차갑게 울리는 종소리 아르페지오 + 밑에 깔리는 부풂."""
    dur = 1.6
    out = np.zeros(n_of(dur))
    notes = (440.0, 554.4, 659.3, 880.0, 1108.7)
    for i, f in enumerate(notes):
        d = dur - 0.11 * i
        bell = metal_hit(rng, d, f, partials=(1.0, 2.0, 2.76, 5.4, 8.93), tau=0.7, bright=0.5) * 0.6
        place(out, bell, 0.11 * i, 0.8 - 0.08 * i)
    pad = (sine(110, dur) + 0.5 * sine(165.3, dur)) * line(dur, (0, 0), (0.5, 1), (1, 0)) * 0.3
    shine = hp(noise(rng, dur), 6000) * line(dur, (0, 0), (0.4, 1), (1, 0)) * 0.08
    return finish(room(out + pad + shine, rng, 1.0, 0.35, 5000), 0.88, fade_out=0.15)


# ---------------------------------------------------------------- 목록

# (sounds/ 아래 경로, 변형 수, 함수). 변형이 둘 이상이면 이름 끝에 _1, _2 ...
SOUNDS = [
    ("machine/crusher", 3, crusher),
    ("machine/press", 3, press),
    ("machine/assembler", 3, assembler),
    ("machine/precision", 3, precision),
    ("machine/washer", 3, washer),
    ("machine/steam", 3, steam),
    ("machine/quarry", 3, quarry),
    ("boss/guardian_charge", 2, guardian_charge),
    ("boss/warden_burst", 2, warden_burst),
    ("boss/phase", 1, boss_phase),
    ("boss/guardian_rage", 1, guardian_rage),
    ("boss/warden_rage", 1, warden_rage),
    ("boss/colossus_rage", 1, colossus_rage),
    ("boss/guardian_death", 1, guardian_death),
    ("boss/warden_death", 1, warden_death),
    ("boss/colossus_death", 1, colossus_death),
    ("boss/colossus_pull", 2, colossus_pull),
    ("boss/colossus_crystals", 1, colossus_crystals),
]


def _ogg_crc_table():
    table = []
    for i in range(256):
        r = i << 24
        for _ in range(8):
            r = ((r << 1) ^ 0x04C11DB7) if r & 0x80000000 else (r << 1)
        table.append(r & 0xFFFFFFFF)
    return table


OGG_CRC = _ogg_crc_table()


def fix_ogg_serial(path, serial):
    """libsndfile은 Ogg 흐름 번호를 매번 무작위로 정한다. 이름에서 나온 번호로 바꿔 파일이 몇 번을 만들어도 똑같게 한다."""
    data = bytearray(open(path, "rb").read())
    i = 0
    while i < len(data):
        assert data[i:i + 4] == b"OggS", path
        nseg = data[i + 26]
        size = 27 + nseg + sum(data[i + 27:i + 27 + nseg])
        data[i + 14:i + 18] = serial.to_bytes(4, "little")
        data[i + 22:i + 26] = bytes(4)
        crc = 0
        for b in data[i:i + size]:
            crc = ((crc << 8) & 0xFFFFFFFF) ^ OGG_CRC[(crc >> 24) ^ b]
        data[i + 22:i + 26] = crc.to_bytes(4, "little")
        i += size
    open(path, "wb").write(bytes(data))


def names(path, count):
    return [path] if count == 1 else [f"{path}_{i + 1}" for i in range(count)]


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    wav = "--wav" in sys.argv
    total = 0
    for path, count, fn in SOUNDS:
        for v, name in enumerate(names(path, count)):
            if args and not any(a in name for a in args):
                continue
            rng = np.random.default_rng(zlib.crc32(name.encode()))
            data = fn(rng, v)
            assert data.ndim == 1 and np.all(np.isfinite(data)), name
            out = os.path.join(OUT, *name.split("/")) + ".ogg"
            os.makedirs(os.path.dirname(out), exist_ok=True)
            sf.write(out, data, SR, format="OGG", subtype="VORBIS")
            fix_ogg_serial(out, zlib.crc32(name.encode()))
            size = os.path.getsize(out)
            total += size
            if wav:
                w = os.path.join(WAV_OUT, *name.split("/")) + ".wav"
                os.makedirs(os.path.dirname(w), exist_ok=True)
                sf.write(w, data, SR, subtype="PCM_16")
            print(f"{name}.ogg  {len(data) / SR:.2f}s  {size / 1024:.1f} KB")
    print(f"합계 {total / 1024:.0f} KB")


if __name__ == "__main__":
    main()
