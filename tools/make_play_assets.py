"""Generate Google Play store graphic assets from the community emblem.

Outputs (docs/play/):
  - play_icon_512.png      512x512, the app icon as it appears on-device (white bg, emblem)
  - feature_graphic_1024x500.png  brand-green hero with emblem chip + app name
Run:  python tools/make_play_assets.py
"""
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EMBLEM = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi", "logo_emblem.png")
OUT = os.path.join(ROOT, "docs", "play")
os.makedirs(OUT, exist_ok=True)

# Brand palette
GREEN = (46, 125, 50)
GREEN_DARK = (27, 94, 32)
GOLD = (212, 175, 55)
WHITE = (255, 255, 255)


def font(size, bold=True):
    candidates = (
        ["arialbd.ttf", "Arialbd.ttf"] if bold else ["arial.ttf", "Arial.ttf"]
    )
    for name in candidates:
        try:
            return ImageFont.truetype(os.path.join(r"C:\Windows\Fonts", name), size)
        except OSError:
            continue
    return ImageFont.load_default()


def load_emblem():
    em = Image.open(EMBLEM).convert("RGBA")
    return em


def scaled(em, target_h):
    w, h = em.size
    target_w = round(w * target_h / h)
    return em.resize((target_w, target_h), Image.LANCZOS)


# ---------------------------------------------------------------- 512 icon
def make_icon():
    size = 512
    img = Image.new("RGBA", (size, size), WHITE + (255,))
    em = scaled(load_emblem(), 452)
    x = (size - em.width) // 2
    y = (size - em.height) // 2
    img.alpha_composite(em, (x, y))
    out = os.path.join(OUT, "play_icon_512.png")
    img.convert("RGB").save(out, "PNG")
    print("wrote", out, img.size)


# ------------------------------------------------------- feature graphic
def vertical_gradient(w, h, top, bottom):
    base = Image.new("RGB", (w, h), top)
    top_im = Image.new("RGB", (w, h), bottom)
    mask = Image.new("L", (w, h))
    md = mask.load()
    for yy in range(h):
        v = int(255 * yy / (h - 1))
        for xx in range(w):
            md[xx, yy] = v
    base.paste(top_im, (0, 0), mask)
    return base


def fit_font(draw, text, max_w, start, bold=True, floor=18):
    """Largest font (<= start px) whose rendered text width fits max_w."""
    size = start
    while size > floor:
        f = font(size, bold=bold)
        if draw.textlength(text, font=f) <= max_w:
            return f
        size -= 2
    return font(floor, bold=bold)


def make_feature():
    w, h = 1024, 500
    img = vertical_gradient(w, h, GREEN, GREEN_DARK).convert("RGBA")
    draw = ImageDraw.Draw(img)

    # White rounded chip holding the emblem (left third).
    chip = 360
    cx, cy = 56, (h - chip) // 2
    draw.rounded_rectangle([cx, cy, cx + chip, cy + chip], radius=44, fill=WHITE + (255,))
    em = scaled(load_emblem(), chip - 52)
    img.alpha_composite(em, (cx + (chip - em.width) // 2, cy + (chip - em.height) // 2))

    # Text block (right) — auto-fit each line to the remaining width.
    tx = cx + chip + 56
    avail = w - tx - 48
    title_f = fit_font(draw, "Kassel Vaktija", avail, 80, bold=True)
    sub_f = fit_font(draw, "Gebetszeiten · Namaz", avail, 42, bold=True)
    org_f = fit_font(draw, "IGBD-Gemeinde Sandžak · Kassel", avail, 32, bold=False)
    draw.text((tx, 168), "Kassel Vaktija", font=title_f, fill=WHITE)
    draw.text((tx, 262), "Gebetszeiten · Namaz", font=sub_f, fill=GOLD + (255,))
    draw.text((tx, 322), "IGBD-Gemeinde Sandžak · Kassel", font=org_f, fill=(225, 240, 225))

    out = os.path.join(OUT, "feature_graphic_1024x500.png")
    img.convert("RGB").save(out, "PNG")
    print("wrote", out, img.size)


if __name__ == "__main__":
    make_icon()
    make_feature()
