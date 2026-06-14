# One-off: insert hadith_of_the_day after hadith_riyad in all locales.
import io, os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "app", "src", "main", "res")

STRINGS = {
    "values": "Hadis dana",
    "values-de": "Hadith des Tages",
    "values-en": "Hadith of the day",
    "values-ar": "حديث اليوم",
    "values-tr": "Günün hadisi",
    "values-sq": "Hadithi i ditës",
    "values-ur": "آج کی حدیث",
    "values-ru": "Хадис дня",
}

ANCHOR = 'name="hadith_riyad"'

for folder, label in STRINGS.items():
    path = os.path.join(ROOT, folder, "strings.xml")
    with io.open(path, encoding="utf-8") as f:
        lines = f.readlines()
    if any("hadith_of_the_day" in l for l in lines):
        print(f"{folder}: already present, skipped")
        continue
    out, inserted = [], False
    for line in lines:
        out.append(line)
        if ANCHOR in line and not inserted:
            out.append(f'    <string name="hadith_of_the_day">{label}</string>\n')
            inserted = True
    if not inserted:
        raise SystemExit(f"ANCHOR NOT FOUND in {path}")
    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.writelines(out)
    print(f"{folder}: ok")
