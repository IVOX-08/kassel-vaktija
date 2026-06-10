"""Frame raw phone screenshots into polished, Play-compliant store screenshots.

Raw 1080x2388 shots are ~2.21:1 (exceeds Play's 2:1 limit). We place each on a
1200x2380 brand-green canvas (ratio 1.98:1, safely under 2:1) with a German caption,
rounded corners and a soft shadow.

In:  docs/screenshots/store_*.png   Out: docs/play/screenshots/*.png
Run: python tools/make_store_screenshots.py
"""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "docs", "screenshots")
OUT = os.path.join(ROOT, "docs", "play", "screenshots")
os.makedirs(OUT, exist_ok=True)

GREEN = (46, 125, 50)
GREEN_DARK = (27, 94, 32)
WHITE = (255, 255, 255)
GOLD = (212, 175, 55)

CW, CH = 1200, 2380           # canvas (ratio 1.983 : 1, under the 2:1 cap)
CAP_TOP = 70                  # caption baseline area
SHOT_TOP = 300                # where the phone shot starts

SHOTS = [
    ("store_1_home.png",     "1_home.png",     "Gebetszeiten für Kassel"),
    ("store_2_calendar.png", "2_calendar.png", "Monatskalender – auch offline"),
    ("store_3_qibla.png",    "3_qibla.png",    "Qibla-Kompass nach Mekka"),
    ("store_4_more.png",     "4_library.png",  "Koran · Hadith · Dhikr"),
    ("store_5_quran.png",    "5_quran.png",    "Der vollständige Koran"),
    ("store_6_hadith.png",   "6_hadith.png",   "Hadith-Sammlungen"),
    ("store_7_settings.png", "7_settings.png", "Adhan ganz nach Wunsch"),
]


def font(size, bold=True):
    name = "arialbd.ttf" if bold else "arial.ttf"
    try:
        return ImageFont.truetype(os.path.join(r"C:\Windows\Fonts", name), size)
    except OSError:
        return ImageFont.load_default()


def fit_font(draw, text, max_w, start=64, floor=30):
    s = start
    while s > floor:
        f = font(s)
        if draw.textlength(text, font=f) <= max_w:
            return f
        s -= 2
    return font(floor)


def gradient(w, h, top, bottom):
    base = Image.new("RGB", (w, h), top)
    grad = Image.new("RGB", (w, h), bottom)
    mask = Image.new("L", (w, h))
    md = mask.load()
    for y in range(h):
        v = int(255 * y / (h - 1))
        for x in range(w):
            md[x, y] = v
    base.paste(grad, (0, 0), mask)
    return base


def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0], img.size[1]], radius=radius, fill=255)
    out = img.convert("RGBA")
    out.putalpha(mask)
    return out


def frame(src_name, out_name, caption):
    src_path = os.path.join(SRC, src_name)
    if not os.path.exists(src_path):
        print("MISSING", src_path)
        return
    canvas = gradient(CW, CH, GREEN, GREEN_DARK).convert("RGBA")
    draw = ImageDraw.Draw(canvas)

    # Caption (auto-fit), centred, with a small gold underline accent.
    cf = fit_font(draw, caption, CW - 160, start=66)
    tw = draw.textlength(caption, font=cf)
    draw.text(((CW - tw) / 2, CAP_TOP + 40), caption, font=cf, fill=WHITE)
    uw = min(tw, 360)
    draw.rounded_rectangle([(CW - uw) / 2, CAP_TOP + 150, (CW + uw) / 2, CAP_TOP + 158],
                           radius=4, fill=GOLD)

    # Phone screenshot scaled to fit, rounded, with a soft drop shadow.
    shot = Image.open(src_path).convert("RGBA")
    avail_h = CH - SHOT_TOP - 50
    scale = avail_h / shot.height
    new_w = int(shot.width * scale)
    new_h = int(shot.height * scale)
    if new_w > CW - 150:
        scale = (CW - 150) / shot.width
        new_w, new_h = int(shot.width * scale), int(shot.height * scale)
    shot = shot.resize((new_w, new_h), Image.LANCZOS)
    shot = rounded(shot, 46)

    px = (CW - new_w) // 2
    py = SHOT_TOP + (avail_h - new_h) // 2

    # shadow
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle([px, py + 14, px + new_w, py + new_h + 14], radius=46, fill=(0, 0, 0, 150))
    shadow = shadow.filter(ImageFilter.GaussianBlur(22))
    canvas = Image.alpha_composite(canvas, shadow)
    canvas.alpha_composite(shot, (px, py))

    out_path = os.path.join(OUT, out_name)
    canvas.convert("RGB").save(out_path, "PNG")
    print("wrote", out_name, canvas.size)


if __name__ == "__main__":
    for s in SHOTS:
        frame(*s)
