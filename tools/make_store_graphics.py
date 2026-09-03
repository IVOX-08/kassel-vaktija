# -*- coding: utf-8 -*-
"""Build the two Play-Console graphics that carry the app's name: feature graphic and TV banner.

Both had the OLD name on them ("Kassel Vaktija"), so both have to be remade for the rename. They
are generated rather than drawn by hand for the same reason the launcher icon is: the mark is
governed by a standards book, and a hand-placed mark drifts.

Rules from the standards book that this script obeys (docs/marke/README.md):

  - The mark is never recoloured. On a green ground the book's own NEGATIVE version is used --
    the whole mark in white -- not a lightened green one.
  - Clear space of x4 around the mark, where x is the height of a star arm. Nothing may enter it.
  - The mark is never distorted; it scales as a square.

And one rule from Google: the feature graphic can be cropped and, on some layouts, has a play
button laid over its middle. Nothing that must be read goes in the outer tenth, and the text sits
off-centre rather than dead centre.

Run:  python tools/make_store_graphics.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RES = os.path.join(ROOT, "app", "src", "main", "res")
OUT = os.path.join(ROOT, "docs", "play")

MARK_NEGATIVE = os.path.join(RES, "drawable-nodpi", "logo_igbd_negative.png")
FONT = os.path.join(RES, "font", "inter_variable.ttf")

# IZ zelena and the app's gold. The green is the protected brand colour; the gold is the app's own
# lettering colour and deliberately NOT the book's pale IZ Zlatna -- see docs/marke/README.md.
GREEN = (0x00, 0x83, 0x48)
GREEN_DEEP = (0x00, 0x4d, 0x2a)
GOLD = (0xD4, 0xAF, 0x37)
WHITE = (0xFF, 0xFF, 0xFF)


def vertical_gradient(size, top, bottom):
    """A quiet gradient, so the panel does not read as a flat block of colour on a white page."""
    w, h = size
    base = Image.new("RGB", (1, h))
    px = base.load()
    for y in range(h):
        t = y / float(h - 1)
        px[0, y] = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
    return base.resize(size, Image.BICUBIC)


def font_at(size):
    return ImageFont.truetype(FONT, size)


TITLE = u"IGBD Vaktija"
SUBTITLE = u"Gebetszeiten · Vaktovi"


def draw_panel(size, mark_box):
    """Green panel with the white mark on it. The wording is added by each caller."""
    img = vertical_gradient(size, GREEN, GREEN_DEEP)
    mark = Image.open(MARK_NEGATIVE).convert("RGBA")
    side = mark_box[2]
    mark = mark.resize((side, side), Image.LANCZOS)
    img.paste(mark, (mark_box[0], mark_box[1]), mark)
    return img


def write(img, text, size, colour, x=None, y=0):
    """Draw text at [x], or centred on the image when x is None."""
    d = ImageDraw.Draw(img)
    f = font_at(size)
    if x is None:
        x = (img.width - d.textbbox((0, 0), text, font=f)[2]) // 2
    d.text((x, y), text, font=f, fill=colour)


def feature_graphic():
    # 1024x500. The mark sits left of centre and the wording to its right, so the middle of the
    # image -- where a play button may land -- carries no text.
    w, h = 1024, 500
    side = 260                      # mark
    mark_x, mark_y = 96, (h - side) // 2
    img = draw_panel((w, h), (mark_x, mark_y, side))
    write(img, TITLE, 76, WHITE, x=mark_x + side + 72, y=186)
    write(img, SUBTITLE, 34, GOLD, x=mark_x + side + 76, y=280)
    path = os.path.join(OUT, "feature_graphic_1024x500.png")
    img.save(path)
    return path, img.size


def tv_banner():
    # 1280x720 for the Android-TV listing. Centred: a TV shows it as a whole tile, and there is no
    # play button to dodge.
    w, h = 1280, 720
    side = 300
    mark_x, mark_y = (w - side) // 2, 168
    img = draw_panel((w, h), (mark_x, mark_y, side))
    write(img, TITLE, 84, WHITE, y=500)
    write(img, SUBTITLE, 38, GOLD, y=608)
    path = os.path.join(OUT, "tv_banner_1280x720.png")
    img.save(path)
    return path, img.size


for make in (feature_graphic, tv_banner):
    p, size = make()
    print("%-52s %s" % (os.path.basename(p), size))
