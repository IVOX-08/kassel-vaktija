# -*- coding: utf-8 -*-
"""Build the launcher icon from the IGBD mark.

The only version of the mark IGBD publishes is 114 px. A launcher icon needs 432 px, so it has to
be enlarged — and a plain enlargement of a two-colour shape goes soft at exactly the edges the eye
checks. So the shape is rebuilt rather than stretched: enlarge, then snap every pixel back onto the
two brand colours and re-derive a clean alpha edge. Flat artwork survives that; a photograph would
not.

If IGBD ever sends the original artwork, drop it in as SOURCE and re-run — nothing else changes.

Run:  python tools/gen_launcher_icon.py
"""

import os

from PIL import Image, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
SOURCE = os.path.join(RES, "drawable-nodpi", "logo_igbd.png")

# Adaptive-icon foreground sizes. The inner two-thirds is the safe zone every launcher shows; the
# rest is what a round or squircle mask crops away.
FOREGROUND = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
# Legacy icons for Android 7 and older, which have no adaptive icon.
LEGACY = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

CRESCENT = (14, 52, 39)
STAR = (188, 162, 113)
WHITE = (255, 255, 255)


def crisp(im, scale):
    """Enlarge flat artwork without either a soft edge or a stair-stepped one.

    The shape lives in the alpha channel and the colour is flat, so the two are enlarged
    differently: alpha is resampled smoothly, which keeps every curve clean, while colour is
    snapped to the two brand tones so the enlargement cannot invent muddy in-between greens along
    an edge. Thresholding the alpha as well — the obvious shortcut — is what turns a curve into a
    staircase.
    """
    big = im.resize((im.width * scale, im.height * scale), Image.LANCZOS)
    px = big.load()
    for y in range(big.height):
        for x in range(big.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            dc = (r - CRESCENT[0]) ** 2 + (g - CRESCENT[1]) ** 2 + (b - CRESCENT[2]) ** 2
            ds = (r - STAR[0]) ** 2 + (g - STAR[1]) ** 2 + (b - STAR[2]) ** 2
            px[x, y] = (CRESCENT if dc <= ds else STAR) + (a,)
    return big


def fit(art, canvas_size, fraction):
    """Centre the mark on a transparent square, occupying [fraction] of the canvas."""
    target = int(canvas_size * fraction)
    ratio = min(target / art.width, target / art.height)
    art = art.resize((max(1, int(art.width * ratio)), max(1, int(art.height * ratio))), Image.LANCZOS)
    out = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    out.paste(art, ((canvas_size - art.width) // 2, (canvas_size - art.height) // 2), art)
    return out


def main():
    art = crisp(Image.open(SOURCE).convert("RGBA"), 6)

    for density, size in FOREGROUND.items():
        # 0.60 of the 108dp canvas keeps the mark inside the 72dp safe zone with a little air, so
        # no launcher mask clips the crescent.
        path = os.path.join(RES, "mipmap-" + density, "ic_launcher_foreground.png")
        fit(art, size, 0.60).save(path)

    for density, size in LEGACY.items():
        # Legacy icons are shown as-is, so they carry their own white ground.
        flat = Image.new("RGBA", (size, size), WHITE + (255,))
        flat.alpha_composite(fit(art, size, 0.72))
        flat.convert("RGB").save(os.path.join(RES, "mipmap-" + density, "ic_launcher.png"))

        # The round variant needs the ground clipped to a circle.
        round_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        mask = Image.new("L", (size * 4, size * 4), 0)
        from PIL import ImageDraw
        ImageDraw.Draw(mask).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
        mask = mask.resize((size, size), Image.LANCZOS)
        ground = Image.new("RGBA", (size, size), WHITE + (255,))
        round_icon.paste(ground, (0, 0), mask)
        round_icon.alpha_composite(fit(art, size, 0.66))
        round_icon.save(os.path.join(RES, "mipmap-" + density, "ic_launcher_round.png"))

    print("Symbol erzeugt fuer %d Dichten" % len(FOREGROUND))


if __name__ == "__main__":
    main()
