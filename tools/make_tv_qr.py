"""Regenerate the Android QR code shown on the TV wall board.

The board hangs high on a wall in the mosque, so the code is scanned from several metres away with
a phone held at arm's length. That drives every choice here: the lowest error-correction level that
still survives (ERC_L keeps the module count down, so each module stays physically large), a short
URL, a fat quiet zone, and no logo overlay eating into the pattern.

Run:  python tools/make_tv_qr.py
"""

import qrcode
from qrcode.constants import ERROR_CORRECT_L

PLAY_URL = "https://play.google.com/store/apps/details?id=de.igbdsandzakkassel.vaktija"
OUT = "app/src/main/res/drawable-nodpi/tv_qr_play.png"

# box_size is px per module. 24 gives a ~1000px image for this URL's version — far more than the TV
# needs, but it means the drawable never softens when Compose scales it up on a 4K panel.
img = qrcode.QRCode(
    error_correction=ERROR_CORRECT_L,
    box_size=24,
    border=3,  # quiet zone in modules; 3 is scanner-safe against the white card behind it
)
img.add_data(PLAY_URL)
img.make(fit=True)
img.make_image(fill_color="black", back_color="white").save(OUT)

print(f"wrote {OUT}")
