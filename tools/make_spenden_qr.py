# -*- coding: utf-8 -*-
"""GiroCode (EPC-QR) für eine Gemeinde, die keinen Spendenlink hat, sondern eine IBAN.

WARUM NICHT EINFACH DIE IBAN HINSCHREIBEN
-----------------------------------------
Weil niemand 22 Zeichen abtippt, während er im Moscheeeingang steht. Ein GiroCode wird von jeder
deutschen Banking-App erkannt: scannen, und Empfänger, IBAN und Verwendungszweck stehen schon im
Überweisungsformular. Der Betrag bleibt leer — den bestimmt der Spender.

Das Format ist EPC069-12, eine schlichte Textliste mit einer Zeile je Feld. Die Reihenfolge ist
fest; eine Zeile zu viel oder zu wenig, und die Banking-App erkennt nichts.

DIE IBAN WIRD GEPRÜFT, NICHT GEGLAUBT. Ein Zahlendreher in einer Spendenadresse fällt erst auf,
wenn Geld fehlt — also rechnet das Skript die Prüfsumme nach (ISO 7064, Mod 97-10) und bricht ab,
wenn sie nicht stimmt.

Fehlerkorrektur M, nicht L: so steht es in der EPC-Empfehlung, und der Code wird aus der Hand
gescannt, nicht von einer Wand — Modulgröße ist hier also kein Thema.

Run:  python tools/make_spenden_qr.py
"""
import os

import qrcode
from qrcode.constants import ERROR_CORRECT_M

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT = os.path.join(ROOT, "docs", "spenden")

# Gemeinden ohne Spendenlink. name -> (Kontoinhaber, IBAN, BIC)
# Der Kontoinhaber muss so geschrieben sein wie bei der Bank, höchstens 70 Zeichen.
COMMUNITIES = {
    "ikre-berlin": (
        u"Bosniakische Gemeinde IKRE Berlin e.V.",
        u"DE90100500000190218150",
        u"BELADEBEXXX",
    ),
}


def iban_ok(iban):
    """ISO 7064, Mod 97-10: die ersten vier Zeichen ans Ende, Buchstaben zu Zahlen, mod 97 == 1."""
    s = iban.replace(" ", "").upper()
    rotated = s[4:] + s[:4]
    digits = "".join(str(ord(c) - 55) if c.isalpha() else c for c in rotated)
    return digits.isdigit() and int(digits) % 97 == 1


def epc_payload(holder, iban, bic, remittance=u"Spende"):
    """EPC069-12. Die Reihenfolge der Zeilen ist vorgeschrieben und darf sich nicht verschieben."""
    return u"\n".join([
        u"BCD",          # Dienstkennung
        u"002",          # Version
        u"1",            # Zeichensatz: 1 = UTF-8
        u"SCT",          # SEPA Credit Transfer
        bic,
        holder,
        iban,
        u"",             # Betrag leer -> der Spender entscheidet
        u"CHAR",         # Zweckschlüssel: Spende
        u"",             # strukturierte Referenz
        remittance,      # Verwendungszweck
    ])


def main():
    if not os.path.isdir(OUT):
        os.makedirs(OUT)
    for slug, (holder, iban, bic) in COMMUNITIES.items():
        if not iban_ok(iban):
            raise SystemExit(u"IBAN-Pruefsumme falsch fuer %s: %s" % (slug, iban))

        payload = epc_payload(holder, iban, bic)
        q = qrcode.QRCode(error_correction=ERROR_CORRECT_M, box_size=14, border=4)
        q.add_data(payload)
        q.make(fit=True)
        img = q.make_image(fill_color="#111111", back_color="white").convert("RGB")

        path = os.path.join(OUT, "girocode-%s.png" % slug)
        img.save(path, optimize=True)
        print(u"%-22s %3d Module  %s  (IBAN geprueft)" % (os.path.basename(path),
                                                          q.modules_count, img.size))


main()
