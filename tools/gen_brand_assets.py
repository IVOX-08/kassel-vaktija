# -*- coding: utf-8 -*-
"""Generate every image of the IGBD mark from the official artwork.

Source: `docs/marke/Znak 1_0 pozitiv i negativ.pdf`, the vector mark from the graphic standards of
the Islamska zajednica u Bosni i Hercegovini (version 1.0). It is rendered here rather than traced,
so nothing about the shape is guessed.

Two forms are produced, and both are variations the standards explicitly permit (chapter 2.2):
  * positive — IZ Zelena crescent with the IZ Zlatna star, for light surfaces
  * negative — the mark in white, for the black surfaces of dark mode ("Negativ CB", chapter 2.2.1)

The standards forbid recolouring the mark into anything else, which is why the earlier hand-lightened
green version had to go.

Run:  python tools/gen_brand_assets.py
"""

import os

import pymupdf
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
SOURCE = os.path.join(ROOT, "docs", "marke", "Znak 1_0 pozitiv i negativ.pdf")

# Rendered large, then reduced to each target — a big render reduced is sharper than a small one.
RENDER_SCALE = 14

WHITE = (255, 255, 255)

# The protected colours from chapter 2.1 of the standards, exactly as printed there.
IZ_GREEN = (0, 131, 72)     # IZ zelena — Pantone 356 C — #008348
IZ_GOLD = (165, 149, 115)   # IZ zlatna — Pantone 871 C — #A59573

# Adaptive-icon foreground sizes; the inner two-thirds is the safe zone every launcher shows.
FOREGROUND = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
# Legacy icons for Android 7 and older, which have no adaptive icon.
LEGACY = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

# In-app logo. nodpi so it is used at its own pixel size whatever the screen density.
IN_APP = 512
# What the Play Console asks for.
STORE = 512


def render_mark():
    """The positive mark on transparent, cropped to the artwork."""
    page = pymupdf.open(SOURCE)[0]
    pix = page.get_pixmap(matrix=pymupdf.Matrix(RENDER_SCALE, RENDER_SCALE))
    im = Image.frombytes("RGB", (pix.width, pix.height), pix.samples).convert("RGBA")

    # The PDF paints the mark on white, and the white has to come away without touching the two
    # inks. Measuring "how dark is this pixel" would do it for the green and ruin the gold: gold is
    # a light colour, so it would come out half transparent and wash away over a light page — which
    # is exactly what a first attempt did.
    #
    # Instead every pixel is read as a mix of white and ONE of the two inks. Which ink is decided
    # by whichever line white→ink the pixel sits closest to; how much of it is the position along
    # that line, and that is the alpha. An anti-aliased edge halfway between white and green comes
    # out as half-transparent green, which is what it actually is.
    px = im.load()
    inks = (IZ_GREEN, IZ_GOLD)
    lines = [tuple(c - 255 for c in ink) for ink in inks]
    lengths = [sum(v * v for v in line) for line in lines]

    for y in range(im.height):
        for x in range(im.width):
            r, g, b, _ = px[x, y]
            d = (r - 255, g - 255, b - 255)
            best, best_residual, best_t = 0, None, 0.0
            for i, line in enumerate(lines):
                t = sum(dv * lv for dv, lv in zip(d, line)) / lengths[i]
                t = max(0.0, min(1.0, t))
                residual = sum((dv - t * lv) ** 2 for dv, lv in zip(d, line))
                if best_residual is None or residual < best_residual:
                    best, best_residual, best_t = i, residual, t
            alpha = int(round(best_t * 255))
            px[x, y] = inks[best] + (alpha,) if alpha else (0, 0, 0, 0)

    return im.crop(im.getbbox())


def to_white(mark):
    """The same shape in white — the standards' Negativ CB, for black surfaces."""
    out = mark.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            _, _, _, a = px[x, y]
            px[x, y] = WHITE + (a,)
    return out


def fit(art, canvas, fraction):
    """Centre the mark on a transparent square, occupying [fraction] of it."""
    target = int(canvas * fraction)
    ratio = min(target / art.width, target / art.height)
    art = art.resize((max(1, round(art.width * ratio)), max(1, round(art.height * ratio))),
                     Image.LANCZOS)
    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    out.paste(art, ((canvas - art.width) // 2, (canvas - art.height) // 2), art)
    return out


def circle_mask(size):
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    return mask.resize((size, size), Image.LANCZOS)


def main():
    mark = render_mark()
    white = to_white(mark)
    print("Vorlage: %dx%d" % (mark.width, mark.height))

    # --- in-app logo, following the theme ---
    fit(mark, IN_APP, 0.98).save(os.path.join(RES, "drawable-nodpi", "logo_igbd.png"))
    fit(white, IN_APP, 0.98).save(os.path.join(RES, "drawable-night-nodpi", "logo_igbd.png"))

    # --- the same mark with NO night variant ---
    #
    # The TV board and its picker always paint a light page, whatever the television's own
    # day/night setting is. Asking for logo_igbd there hands back the white negative on a TV in
    # night mode — white on a white card, invisible. This one is always the positive.
    fit(mark, IN_APP, 0.98).save(os.path.join(RES, "drawable-nodpi", "logo_igbd_positive.png"))
    # And the white negative, likewise fixed: it is needed on the picker's green focused row even
    # when the television is in day mode.
    fit(white, IN_APP, 0.98).save(os.path.join(RES, "drawable-nodpi", "logo_igbd_negative.png"))

    # --- launcher icon ---
    for density, size in FOREGROUND.items():
        # 0.60 of the 108dp canvas keeps the mark inside the 72dp safe zone with air to spare, so
        # no launcher's mask can clip the crescent.
        fit(mark, size, 0.60).save(
            os.path.join(RES, "mipmap-" + density, "ic_launcher_foreground.png"))

    for density, size in LEGACY.items():
        flat = Image.new("RGBA", (size, size), WHITE + (255,))
        flat.alpha_composite(fit(mark, size, 0.72))
        flat.convert("RGB").save(os.path.join(RES, "mipmap-" + density, "ic_launcher.png"))

        round_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        ground = Image.new("RGBA", (size, size), WHITE + (255,))
        round_icon.paste(ground, (0, 0), circle_mask(size))
        round_icon.alpha_composite(fit(mark, size, 0.66))
        round_icon.save(os.path.join(RES, "mipmap-" + density, "ic_launcher_round.png"))

    # --- Play Store icon: 512x512, no transparency allowed there ---
    store = Image.new("RGBA", (STORE, STORE), WHITE + (255,))
    store.alpha_composite(fit(mark, STORE, 0.66))
    store_dir = os.path.join(ROOT, "docs", "marke")
    store.convert("RGB").save(os.path.join(store_dir, "play_store_icon_512.png"))

    print("Alle Bilder erzeugt.")


if __name__ == "__main__":
    main()
