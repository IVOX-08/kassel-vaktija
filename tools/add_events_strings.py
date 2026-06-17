# One-off: insert community-events strings after library_ramadan in all locales.
import io, os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "app", "src", "main", "res")

STR = {
    "library_events": {
        "values": "Događaji", "values-de": "Veranstaltungen", "values-en": "Events",
        "values-ar": "الفعاليات", "values-tr": "Etkinlikler", "values-sq": "Ngjarjet",
        "values-ur": "تقریبات", "values-ru": "События",
    },
    "events_add": {
        "values": "Novi događaj", "values-de": "Neue Veranstaltung", "values-en": "New event",
        "values-ar": "فعالية جديدة", "values-tr": "Yeni etkinlik", "values-sq": "Ngjarje e re",
        "values-ur": "نئی تقریب", "values-ru": "Новое событие",
    },
    "events_empty": {
        "values": "Nema nadolazećih događaja.", "values-de": "Keine anstehenden Veranstaltungen.",
        "values-en": "No upcoming events.", "values-ar": "لا توجد فعاليات قادمة.",
        "values-tr": "Yaklaşan etkinlik yok.", "values-sq": "Asnjë ngjarje e ardhshme.",
        "values-ur": "کوئی آنے والی تقریب نہیں۔", "values-ru": "Нет предстоящих событий.",
    },
    "events_date": {
        "values": "Datum", "values-de": "Datum", "values-en": "Date",
        "values-ar": "التاريخ", "values-tr": "Tarih", "values-sq": "Data",
        "values-ur": "تاریخ", "values-ru": "Дата",
    },
    "events_time": {
        "values": "Vrijeme", "values-de": "Uhrzeit", "values-en": "Time",
        "values-ar": "الوقت", "values-tr": "Saat", "values-sq": "Ora",
        "values-ur": "وقت", "values-ru": "Время",
    },
}

ANCHOR = 'name="library_ramadan"'
locales = ["values", "values-de", "values-en", "values-ar", "values-tr", "values-sq", "values-ur", "values-ru"]

for folder in locales:
    path = os.path.join(ROOT, folder, "strings.xml")
    with io.open(path, encoding="utf-8") as f:
        lines = f.readlines()
    if any("library_events" in l for l in lines):
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
