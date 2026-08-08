"""Generate the 1280x720 Android TV banner for the Play Store listing.

This is NOT the same asset as res/drawable-xhdpi/tv_banner.png (320x180), which is the launcher
tile shown on the TV's own home screen. Play requires a separate, much larger banner for the store
listing when the Android TV form factor is enabled.

Drawn at full size rather than upscaled from the 320x180 tile, so the emblem and lettering stay
crisp on a 4K TV browsing the store.

Run:  python tools/make_tv_banner.py
"""

import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EMBLEM = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi", "logo_emblem.png")
OUT = os.path.join(ROOT, "docs", "play", "tv", "tv_banner_1280x720.png")

W, H = 1280, 720
GREEN_DARK = (27, 94, 32)
GREEN = (46, 125, 50)
GOLD = (212, 175, 55)
WHITE = (255, 255, 255)


def font(size, bold=True):
    names = ["arialbd.ttf"] if bold else ["arial.ttf"]
    for name in names:
        try:
            return ImageFont.truetype(os.path.join(r"C:\Windows\Fonts", name), size)
        except OSError:
            continue
    return ImageFont.load_default()


img = Image.new("RGB", (W, H), GREEN_DARK)
draw = ImageDraw.Draw(img)

# Soft vertical brand gradient so the flat tile does not look dead next to other store banners.
for y in range(H):
    t = y / H
    draw.line(
        [(0, y), (W, y)],
        fill=tuple(int(GREEN_DARK[i] + (GREEN[i] - GREEN_DARK[i]) * t) for i in range(3)),
    )

# Emblem on a white rounded chip (the emblem's own ring text needs a light backing to stay legible).
chip = 300
cx, cy = 150, (H - chip) // 2
draw.rounded_rectangle([cx, cy, cx + chip, cy + chip], radius=36, fill=WHITE)
emblem = Image.open(EMBLEM).convert("RGBA")
pad = 26
box = chip - 2 * pad
scale = min(box / emblem.width, box / emblem.height)
emblem = emblem.resize((int(emblem.width * scale), int(emblem.height * scale)), Image.LANCZOS)
img.paste(
    emblem,
    (cx + (chip - emblem.width) // 2, cy + (chip - emblem.height) // 2),
    emblem,
)

# Wordmark
tx = cx + chip + 70
draw.text((tx, 250), "Kassel Vaktija", font=font(88), fill=WHITE)
draw.text((tx, 360), "IGBD-Gemeinde Sandžak-Kassel", font=font(40), fill=GOLD)
draw.text((tx, 425), "Namaska vremena · Gebetszeiten", font=font(34, bold=False), fill=WHITE)

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print(f"wrote {OUT}  ({W}x{H})")
