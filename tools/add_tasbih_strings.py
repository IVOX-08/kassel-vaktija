# One-off: insert Tasbih strings after library_dhikr in all locales.
import io, os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "app", "src", "main", "res")

# name -> {locale: value}
STR = {
    "library_tasbih": {
        "values": "Tespih", "values-de": "Tasbih", "values-en": "Tasbih",
        "values-ar": "المسبحة", "values-tr": "Tesbih", "values-sq": "Tesbih",
        "values-ur": "تسبیح", "values-ru": "Тасбих",
    },
    "tasbih_reset": {
        "values": "Poništi", "values-de": "Zurücksetzen", "values-en": "Reset",
        "values-ar": "إعادة تعيين", "values-tr": "Sıfırla", "values-sq": "Rivendos",
        "values-ur": "ری سیٹ", "values-ru": "Сбросить",
    },
    "tasbih_rounds": {
        "values": "Krugovi: %1$d", "values-de": "Runden: %1$d", "values-en": "Rounds: %1$d",
        "values-ar": "الجولات: %1$d", "values-tr": "Turlar: %1$d", "values-sq": "Raunde: %1$d",
        "values-ur": "راؤنڈز: %1$d", "values-ru": "Круги: %1$d",
    },
    "tasbih_hint": {
        "values": "Dodirnite za brojanje", "values-de": "Tippen zum Zählen", "values-en": "Tap to count",
        "values-ar": "انقر للعدّ", "values-tr": "Saymak için dokunun", "values-sq": "Prekni për të numëruar",
        "values-ur": "گننے کے لیے ٹیپ کریں", "values-ru": "Нажмите, чтобы считать",
    },
}

ANCHOR = 'name="library_dhikr"'
locales = ["values", "values-de", "values-en", "values-ar", "values-tr", "values-sq", "values-ur", "values-ru"]

for folder in locales:
    path = os.path.join(ROOT, folder, "strings.xml")
    with io.open(path, encoding="utf-8") as f:
        lines = f.readlines()
    if any("library_tasbih" in l for l in lines):
        print(f"{folder}: already present, skipped")
        continue
    out, inserted = [], False
    for line in lines:
        out.append(line)
        if ANCHOR in line and not inserted:
            for name, vals in STR.items():
                out.append(f'    <string name="{name}">{vals[folder]}</string>\n')
            inserted = True
    if not inserted:
        raise SystemExit(f"ANCHOR NOT FOUND in {path}")
    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.writelines(out)
    print(f"{folder}: ok")
