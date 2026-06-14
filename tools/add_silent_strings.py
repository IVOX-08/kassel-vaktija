# One-off: insert the two play-when-silent strings after settings_notification_sound in all locales.
import io, os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "app", "src", "main", "res")

STRINGS = {
    "values": (
        "Pusti ezan i kad je telefon utišan",
        "Isključeno: ako je telefon na nečujno ili vibraciju, stiže samo tiha obavijest.",
    ),
    "values-de": (
        "Adhan auch im Lautlos-Modus abspielen",
        "Aus: Ist das Handy stumm oder auf Vibration, kommt nur eine stille Benachrichtigung.",
    ),
    "values-en": (
        "Play Adhan in silent mode",
        "Off: when the phone is muted or on vibrate, only a quiet notification is shown.",
    ),
    "values-ar": (
        "تشغيل الأذان في الوضع الصامت",
        "عند الإيقاف: إذا كان الهاتف صامتًا أو على الاهتزاز، يظهر إشعار صامت فقط.",
    ),
    "values-tr": (
        "Sessiz modda ezanı çal",
        "Kapalı: Telefon sessizde veya titreşimdeyken yalnızca sessiz bir bildirim gösterilir.",
    ),
    "values-sq": (
        "Luaje ezanin edhe në modalitetin pa zë",
        "Çaktivizuar: kur telefoni është pa zë ose me dridhje, shfaqet vetëm një njoftim i heshtur.",
    ),
    "values-ur": (
        "خاموش موڈ میں اذان چلائیں",
        "بند: اگر فون خاموش یا وائبریشن پر ہو تو صرف خاموش اطلاع دکھائی جاتی ہے۔",
    ),
    "values-ru": (
        "Воспроизводить азан в беззвучном режиме",
        "Выкл.: если телефон в беззвучном режиме или на вибрации, приходит только тихое уведомление.",
    ),
}

ANCHOR = 'name="settings_notification_sound"'

for folder, (label, hint) in STRINGS.items():
    path = os.path.join(ROOT, folder, "strings.xml")
    with io.open(path, encoding="utf-8") as f:
        lines = f.readlines()
    if any("settings_play_when_silent" in l for l in lines):
        print(f"{folder}: already present, skipped")
        continue
    out = []
    inserted = False
    for line in lines:
        out.append(line)
        if ANCHOR in line and not inserted:
            out.append(f'    <string name="settings_play_when_silent">{label}</string>\n')
            out.append(f'    <string name="settings_play_when_silent_hint">{hint}</string>\n')
            inserted = True
    if not inserted:
        raise SystemExit(f"ANCHOR NOT FOUND in {path}")
    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.writelines(out)
    print(f"{folder}: ok")
