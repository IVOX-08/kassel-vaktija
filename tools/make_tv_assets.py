# -*- coding: utf-8 -*-
"""TV-Banner und TV-Symbol, die die Flaeche wirklich ausfuellen.

WARUM ES DAS GIBT
-----------------
Google Play hat am 10. September 2026 abgelehnt, diesmal nicht wegen der Marke, sondern wegen der
Qualitaetsrichtlinien fuer Android-TV-Apps:

    "Ihr Symbol fuellt nicht den gesamten Symbolbereich aus."
    "Ihr Banner fuellt nicht die gesamte Bannerflaeche aus."

Zwei Ursachen, beide in Googles eigenem Text nachlesbar:

  1. DAS SYMBOL WAR ZU KLEIN. Google verlangt 512 x 512. Das groesste vorhandene war
     mipmap-xxxhdpi mit 192 x 192 -- die uebliche Groesse fuer Telefone (48dp x 4), aber der
     Fernseher zeichnet das Symbol viel groesser und bekam nur hochskalierten Brei.
  2. ZU VIEL LUFT. Das Wappen sass mit breitem weissem Rand in seinem Quadrat, und im Banner
     nahm es die linke Ecke ein, waehrend rechts Platz blieb. Der automatische Pruefer misst,
     wie viel der Flaeche das Motiv belegt.

Fuer Telefone aendert sich nichts: dort greift ab Android 8 das adaptive Symbol aus
mipmap-anydpi-v26 (Vordergrund + Hintergrundfarbe), und minSdk ist 26. Die hier erzeugten PNGs
sind die Alt-Fassung, die genau zwei Dinge bedient -- den Fernseher und den Play-Store-Eintrag.

Run:  python tools/make_tv_assets.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RES = os.path.join(ROOT, "app", "src", "main", "res")
CREST = os.path.join(RES, "drawable-nodpi", "logo_emblem.png")
FONT = os.path.join(RES, "font", "inter_variable.ttf")

GREEN = (0x00, 0x83, 0x48)
GREEN_DEEP = (0x00, 0x4d, 0x2a)
GOLD = (0xD4, 0xAF, 0x37)
WHITE = (0xFF, 0xFF, 0xFF)

# Launcher-Dichten. Der Fernseher nimmt die groesste, die er findet -- deshalb steht ganz oben
# eine 512er Fassung, die es vorher nicht gab.
ICON_SIZES = [
    ("mipmap-mdpi", 48), ("mipmap-hdpi", 72), ("mipmap-xhdpi", 96),
    ("mipmap-xxhdpi", 144), ("mipmap-xxxhdpi", 512),
]


def crest(size):
    art = Image.open(CREST).convert("RGBA")
    s = min(size / float(art.width), size / float(art.height))
    return art.resize((max(1, int(art.width * s)), max(1, int(art.height * s))), Image.LANCZOS)


def gradient(size, top, bottom):
    w, h = size
    base = Image.new("RGB", (1, h))
    px = base.load()
    for y in range(h):
        t = y / float(h - 1)
        px[0, y] = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
    return base.resize(size, Image.BICUBIC)


def make_icons():
    """Das Wappen fuellt das Quadrat -- 96 % statt der bisherigen ~70 %."""
    for folder, side in ICON_SIZES:
        canvas = Image.new("RGB", (side, side), WHITE)
        art = crest(int(side * 0.96))
        canvas.paste(art, ((side - art.width) // 2, (side - art.height) // 2), art)
        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            canvas.save(os.path.join(RES, folder, name))
        print("%-16s %4d x %-4d  Symbol" % (folder, side, side))


def make_banner():
    """320 x 180, randlos. Das Wappen links gross, die Wortmarke fuellt den Rest bis zum Rand."""
    w, h = 320, 180
    img = gradient((w, h), GREEN, GREEN_DEEP)

    # Weisse Scheibe hinter dem Wappen: sein aeusserer Ring ist selbst gruen, auf gruenem Grund
    # verschwindet die Umschrift. Die Scheibe kostet keine Flaeche -- sie IST Flaeche.
    disc = int(h * 0.92)
    plate = Image.new("RGBA", (disc, disc), (0, 0, 0, 0))
    ImageDraw.Draw(plate).ellipse([0, 0, disc - 1, disc - 1], fill=WHITE + (255,))
    img.paste(plate, (8, (h - disc) // 2), plate)

    art = crest(int(disc * 0.94))
    img.paste(art, (8 + (disc - art.width) // 2, (h - art.height) // 2), art)

    d = ImageDraw.Draw(img)
    left = 8 + disc + 12
    space = w - left - 8

    # Groesste Schrift, die noch passt -- der Pruefer misst belegte Flaeche, also wird sie genutzt.
    size = 46
    while size > 16:
        f = ImageFont.truetype(FONT, size)
        if d.textbbox((0, 0), u"IGBD Vaktija", font=f)[2] <= space:
            break
        size -= 2
    f = ImageFont.truetype(FONT, size)
    fs = ImageFont.truetype(FONT, max(12, int(size * 0.46)))
    d.text((left, h // 2 - size), u"IGBD Vaktija", font=f, fill=WHITE)
    d.text((left, h // 2 + int(size * 0.15)), u"Sandžak-Kassel", font=fs, fill=GOLD)

    out = os.path.join(RES, "drawable-xhdpi", "tv_banner.png")
    img.save(out)
    print("%-16s %4d x %-4d  Banner (deckend, randlos)" % ("drawable-xhdpi", w, h))


make_icons()
make_banner()
