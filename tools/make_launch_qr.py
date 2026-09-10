# -*- coding: utf-8 -*-
"""Grosse QR-Codes fuer die Vorstellung nach dem Freitagsgebet.

Diese kommen auf die Webseite der Gemeinde, nicht in die App: die TV-Tafeln haben das Update noch
nicht, also gibt es dort noch keinen iPhone-Code, und der Vorstand will die App trotzdem morgen
vorstellen. Eine Seite mit zwei grossen Codes ueberbrueckt das.

Zwei Entscheidungen, beide aus demselben Grund -- die Codes werden von einer Leinwand oder einem
Handybildschirm abgescannt, quer durch den Gebetsraum:

  - NIEDRIGSTE Fehlerkorrektur. Klingt falsch, ist hier richtig: eine hoehere Stufe presst mehr
    Module in dieselbe Flaeche, jedes Modul wird also kleiner, und die Modulgroesse entscheidet,
    ob eine Kamera aus der Entfernung noch etwas erkennt. Es gibt hier nichts zu reparieren.
  - KURZE Adressen. apps.apple.com/app/id... statt der langen Fassung mit dem App-Namen darin:
    29 Module statt 33, bei gleicher Flaeche also groessere Kaestchen.

Run:  python tools/make_launch_qr.py
"""
import os

import qrcode
from qrcode.constants import ERROR_CORRECT_L
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT = os.path.join(ROOT, "docs", "start")
FONT = os.path.join(ROOT, "app", "src", "main", "res", "font", "inter_variable.ttf")

GREEN_DARK = (0x00, 0x61, 0x2F)
BLACK = (0x11, 0x11, 0x11)
WHITE = (0xFF, 0xFF, 0xFF)

CODES = [
    ("android", u"Android",
     "https://play.google.com/store/apps/details?id=de.igbdsandzakkassel.vaktija"),
    ("iphone", u"iPhone",
     "https://apps.apple.com/app/id6803973938"),
]

# Modulgroesse in Pixeln. 40 gibt bei 29-33 Modulen plus Rand rund 1500 px -- gross genug fuer
# jede Leinwand und jeden Ausdruck, und die Kanten bleiben hart, weil nicht skaliert wird.
BOX = 40
BORDER = 4          # Ruhezone in Modulen; 4 ist die Norm und sitzt auf Weiss sicher


def build(slug, label, url):
    q = qrcode.QRCode(error_correction=ERROR_CORRECT_L, box_size=BOX, border=BORDER)
    q.add_data(url)
    q.make(fit=True)
    code = q.make_image(fill_color=BLACK, back_color=WHITE).convert("RGB")

    # Beschriftung unter den Code, auf demselben Weiss -- keine Kachel, kein Rahmen, nichts, was
    # von der Flaeche abgeht, die der Scanner braucht.
    pad = int(BOX * 3.2)
    canvas = Image.new("RGB", (code.width, code.height + pad), WHITE)
    canvas.paste(code, (0, 0))

    d = ImageDraw.Draw(canvas)
    f = ImageFont.truetype(FONT, int(BOX * 2.0))
    w = d.textbbox((0, 0), label, font=f)[2]
    d.text(((canvas.width - w) // 2, code.height - int(BOX * 0.4)), label, font=f, fill=GREEN_DARK)

    path = os.path.join(OUT, "qr_%s.png" % slug)
    canvas.save(path)
    print("%-16s %4d Module   %s" % (os.path.basename(path), q.modules_count, canvas.size))
    return path


if not os.path.isdir(OUT):
    os.makedirs(OUT)
for slug, label, url in CODES:
    build(slug, label, url)
