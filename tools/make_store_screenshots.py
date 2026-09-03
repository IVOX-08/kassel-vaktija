# -*- coding: utf-8 -*-
"""Frame the raw phone screenshots for the Play listing.

TWO REASONS THIS EXISTS RATHER THAN UPLOADING THE RAW CAPTURES
--------------------------------------------------------------
1. ASPECT RATIO. A raw capture from a modern phone is 1080x2400 -- 2.22:1. Play allows at most
   2:1 for phone screenshots and rejects anything taller. The frame widens the canvas instead of
   cropping the picture, so nothing is lost from the screen itself.

2. A CAPTION. Most people never read the description; they swipe the pictures. The old listing had
   captions and they did their job -- but one of them said "Gebetszeiten für Kassel", which is now
   wrong: the app serves all 81 communities. A picture that contradicts the text costs more than
   no caption at all.

The TV screenshots are NOT run through here. They are 1920x1080, exactly the 16:9 that Play
requires for the TV form factor, and a caption band would break that ratio.

Run:  python tools/make_store_screenshots.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RAW = os.path.join(ROOT, "docs", "play", "screenshots")
OUT = os.path.join(RAW, "play")
FONT = os.path.join(ROOT, "app", "src", "main", "res", "font", "inter_variable.ttf")

# A LIGHT frame, at the owner's instruction. The app itself is a light app -- white cards on a
# near-white page -- and a dark green surround made the store card read as a dark-mode app, which
# is not what someone installs. The frame now continues the page colour the app actually uses
# (PageBackgroundLight) so the picture looks like one thing instead of a screenshot on a poster.
PAGE = (0xF4, 0xF4, 0xF4)
PAGE_SOFT = (0xE6, 0xEE, 0xE8)   # a breath of green, so the frame is not flat grey
GREEN_DARK = (0x00, 0x61, 0x2F)  # BrandGreenDark -- the caption, readable on the light ground

# Canvas 1300x2400 -> 1.85:1, comfortably inside Play's 2:1 limit.
W, H = 1300, 2400
BAND = 230          # room for the caption
SHOT_H = H - BAND - 90

# The order is the order they appear in the store, and it is not arbitrary: the first picture is
# the one almost everyone sees, so it shows what the app is FOR.
CAPTIONS = [
    ("1_startseite.png", u"Die Zeiten deiner Gemeinde"),
    ("3_gebetstracker.png", u"Hast du gebetet? Deine Serie wächst"),
    ("4_koran.png", u"Der ganze Kur'an, mit Tedschwid-Farben"),
    ("2_kalender.png", u"Monatskalender — auch ohne Internet"),
    ("5_qibla.png", u"Die Kibla, genau nach Mekka"),
]


def gradient(size, top, bottom):
    w, h = size
    base = Image.new("RGB", (1, h))
    px = base.load()
    for y in range(h):
        t = y / float(h - 1)
        px[0, y] = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
    return base.resize(size, Image.BICUBIC)


def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1],
                                           radius=radius, fill=255)
    out = img.convert("RGBA")
    out.putalpha(mask)
    return out


def fit_caption(draw, text, max_width, start_size):
    """Shrink until it fits on one line -- a wrapped caption on a store card reads as a mistake."""
    size = start_size
    while size > 24:
        f = ImageFont.truetype(FONT, size)
        if draw.textbbox((0, 0), text, font=f)[2] <= max_width:
            return f
        size -= 2
    return ImageFont.truetype(FONT, 24)


def main():
    if not os.path.isdir(OUT):
        os.makedirs(OUT)
    for index, (name, caption) in enumerate(CAPTIONS, start=1):
        src = os.path.join(RAW, name)
        shot = Image.open(src).convert("RGB")
        scale = SHOT_H / float(shot.height)
        shot = shot.resize((int(shot.width * scale), SHOT_H), Image.LANCZOS)
        shot = rounded(shot, 34)

        canvas = gradient((W, H), PAGE, PAGE_SOFT)
        d = ImageDraw.Draw(canvas)
        f = fit_caption(d, caption, W - 140, 62)
        tw = d.textbbox((0, 0), caption, font=f)[2]
        d.text(((W - tw) // 2, (BAND - f.size) // 2), caption, font=f, fill=GREEN_DARK)
        # On a light frame the phone needs an edge, or its white cards melt into the page.
        x = (W - shot.width) // 2
        ImageDraw.Draw(canvas).rounded_rectangle(
            [x - 3, BAND - 3, x + shot.width + 2, BAND + shot.height + 2],
            radius=37, outline=(0xD2, 0xDC, 0xD5), width=3)
        canvas.paste(shot, (x, BAND), shot)

        path = os.path.join(OUT, "%d_%s" % (index, name.split("_", 1)[1]))
        canvas.save(path)
        print("%-34s %sx%s  (%.2f:1)" % (os.path.basename(path), W, H, H / float(W)))


main()
