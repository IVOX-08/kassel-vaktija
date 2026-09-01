"""Regenerate the Android QR code used in the promo video.

The TV board no longer uses this picture -- it draws its own code from the link in Firestore, so
the App Store link can be switched on without a new build reaching every wall-mounted TV (see
ui/tv/QrCode.kt). What is left is the video: a frame is scanned off a phone screen, from several
metres away in the worst case. That drives every choice here: the lowest error-correction level that
still survives (ERC_L keeps the module count down, so each module stays physically large), a short
URL, a fat quiet zone, and no logo overlay eating into the pattern.

Run:  python tools/make_tv_qr.py
"""

import qrcode
from qrcode.constants import ERROR_CORRECT_L

PLAY_URL = "https://play.google.com/store/apps/details?id=de.igbdsandzakkassel.vaktija"
OUT = "docs/video/qr_play.png"

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
