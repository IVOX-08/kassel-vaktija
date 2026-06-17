# One-off: insert quran_continue + quran_bookmarks after quran_verses in all locales.
import io, os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "app", "src", "main", "res")

STR = {
    "quran_continue": {
        "values": "Nastavi čitanje", "values-de": "Weiterlesen", "values-en": "Continue reading",
        "values-ar": "متابعة القراءة", "values-tr": "Okumaya devam et", "values-sq": "Vazhdo leximin",
        "values-ur": "پڑھنا جاری رکھیں", "values-ru": "Продолжить чтение",
    },
    "quran_bookmarks": {
        "values": "Oznake", "values-de": "Lesezeichen", "values-en": "Bookmarks",
        "values-ar": "العلامات المرجعية", "values-tr": "Yer imleri", "values-sq": "Faqeshënuesit",
        "values-ur": "بک مارکس", "values-ru": "Закладки",
    },
}

ANCHOR = 'name="quran_verses"'
locales = ["values", "values-de", "values-en", "values-ar", "values-tr", "values-sq", "values-ur", "values-ru"]

for folder in locales:
    path = os.path.join(ROOT, folder, "strings.xml")
    with io.open(path, encoding="utf-8") as f:
        lines = f.readlines()
    if any("quran_continue" in l for l in lines):
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
