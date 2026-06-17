# One-off: insert Ramadan strings after library_tracker in all locales.
import io, os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "app", "src", "main", "res")

STR = {
    "library_ramadan": {
        "values": "Ramazan", "values-de": "Ramadan", "values-en": "Ramadan",
        "values-ar": "رمضان", "values-tr": "Ramazan", "values-sq": "Ramazan",
        "values-ur": "رمضان", "values-ru": "Рамадан",
    },
    "ramadan_until_iftar": {
        "values": "Do iftara", "values-de": "Bis zum Iftar", "values-en": "Until Iftar",
        "values-ar": "حتى الإفطار", "values-tr": "İftara kalan", "values-sq": "Deri në iftar",
        "values-ur": "افطار میں باقی", "values-ru": "До ифтара",
    },
    "ramadan_until_sehur": {
        "values": "Do kraja sehura", "values-de": "Bis Sehur-Ende", "values-en": "Until end of Suhoor",
        "values-ar": "حتى نهاية السحور", "values-tr": "Sahurun sonuna", "values-sq": "Deri në fund të syfyrit",
        "values-ur": "سحری کے اختتام تک", "values-ru": "До конца сухура",
    },
    "ramadan_iftar": {
        "values": "Iftar", "values-de": "Iftar", "values-en": "Iftar",
        "values-ar": "الإفطار", "values-tr": "İftar", "values-sq": "Iftar",
        "values-ur": "افطار", "values-ru": "Ифтар",
    },
    "ramadan_sehur": {
        "values": "Sehur", "values-de": "Sehur", "values-en": "Suhoor",
        "values-ar": "السحور", "values-tr": "Sahur", "values-sq": "Syfyr",
        "values-ur": "سحری", "values-ru": "Сухур",
    },
    "ramadan_teravija": {
        "values": "Teravija", "values-de": "Tarawih", "values-en": "Tarawih",
        "values-ar": "التراويح", "values-tr": "Teravih", "values-sq": "Teravia",
        "values-ur": "تراویح", "values-ru": "Таравих",
    },
    "ramadan_fasted_today": {
        "values": "Danas postih", "values-de": "Heute gefastet", "values-en": "Fasted today",
        "values-ar": "صمت اليوم", "values-tr": "Bugün oruç tuttum", "values-sq": "Sot agjërova",
        "values-ur": "آج روزہ رکھا", "values-ru": "Постился сегодня",
    },
}

ANCHOR = 'name="library_tracker"'
locales = ["values", "values-de", "values-en", "values-ar", "values-tr", "values-sq", "values-ur", "values-ru"]

for folder in locales:
    path = os.path.join(ROOT, folder, "strings.xml")
    with io.open(path, encoding="utf-8") as f:
        lines = f.readlines()
    if any("library_ramadan" in l for l in lines):
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
