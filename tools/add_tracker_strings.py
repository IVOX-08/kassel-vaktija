# One-off: insert prayer-tracker strings after library_tasbih in all locales.
import io, os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "app", "src", "main", "res")

STR = {
    "library_tracker": {
        "values": "Praćenje namaza", "values-de": "Gebets-Tracker", "values-en": "Prayer tracker",
        "values-ar": "متابعة الصلوات", "values-tr": "Namaz takibi", "values-sq": "Ndjekja e namazit",
        "values-ur": "نماز ٹریکر", "values-ru": "Трекер намаза",
    },
    "tracker_streak": {
        "values": "dana zaredom", "values-de": "Tage in Folge", "values-en": "day streak",
        "values-ar": "أيام متتالية", "values-tr": "gün üst üste", "values-sq": "ditë rresht",
        "values-ur": "دن لگاتار", "values-ru": "дней подряд",
    },
    "tracker_today": {
        "values": "Danas", "values-de": "Heute", "values-en": "Today",
        "values-ar": "اليوم", "values-tr": "Bugün", "values-sq": "Sot",
        "values-ur": "آج", "values-ru": "Сегодня",
    },
}

ANCHOR = 'name="library_tasbih"'
locales = ["values", "values-de", "values-en", "values-ar", "values-tr", "values-sq", "values-ur", "values-ru"]

for folder in locales:
    path = os.path.join(ROOT, folder, "strings.xml")
    with io.open(path, encoding="utf-8") as f:
        lines = f.readlines()
    if any("library_tracker" in l for l in lines):
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
