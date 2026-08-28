# -*- coding: utf-8 -*-
"""Build docs/multi-gemeinde/communities.json from IGBD's own member map.

Source: https://igbd.org/bs/dzemati — the map embeds every member community as a JS array with
name, coordinates, street, city, postcode, phone, e-mail and website. That is the authoritative
list: it is IGBD's own register, so the names are the ones the federation itself uses.

Each community is matched to the vaktija.eu town that publishes its prayer times. That is rarely
the same place — vaktija.eu covers roughly 1300 German towns, not all 81 of these — so the nearest
one that actually publishes is used, and the distance is recorded so a bad match is visible.

Communities already in the catalogue keep their hand-checked details (extra towns, donation link,
logo). Nothing curated is thrown away by a re-run.

Run:  python tools/build_catalog.py <igbd_raw.json>
"""

import io
import json
import math
import os
import re
import sys
from html import unescape

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CITIES = os.path.join(ROOT, "docs", "multi-gemeinde", "vaktija_cities.json")
OUT = os.path.join(ROOT, "docs", "multi-gemeinde", "communities.json")

KASSEL_ID = "igbd-gemeinde-sandzak-kassel"

# Kassel's own entry is curated by hand — five towns, its own emblem, its own donation link — and
# must survive every rebuild untouched.
KEEP_AS_IS = {KASSEL_ID}

# Kassel's Iqamah, used as every community's starting point until it sends its own.
DEFAULT_RULES = {
    "fajrIqamah": "05:15",
    "jumua": "15:00",
    "dhuhrOffsetMin": 10,
    "asrOffsetMin": 10,
    "maghribOffsetMin": 5,
    "ishaOffsetMin": 0,
}


def km(a, b):
    R = 6371.0
    p1, p2 = math.radians(a[0]), math.radians(b[0])
    dp = math.radians(b[0] - a[0])
    dl = math.radians(b[1] - a[1])
    h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * R * math.asin(math.sqrt(h))


def slugify(s):
    s = unescape(s).lower()
    for a, b in [("ä", "ae"), ("ö", "oe"), ("ü", "ue"), ("ß", "ss"), ("č", "c"),
                 ("ć", "c"), ("ž", "z"), ("š", "s"), ("đ", "d"), ("&", " und ")]:
        s = s.replace(a, b)
    s = re.sub(r"[^a-z0-9]+", "-", s).strip("-")
    return s or "dzemat"


def fold(s):
    s = (s or "").lower()
    # Drop a parenthetical district: IGBD writes "Frankfurt (Höchst)".
    s = re.sub(r"\(.*?\)", " ", s)
    for a, b in [("ä", "a"), ("ö", "o"), ("ü", "u"), ("ß", "ss"), ("č", "c"),
                 ("ć", "c"), ("ž", "z"), ("š", "s"), ("đ", "d")]:
        s = s.replace(a, b)
    return re.sub(r"[^a-z0-9]+", "", s)


def pick_town(lat, lon, city, cities):
    """The town whose times this community should show.

    The NAME on the address beats the pin on the map. IGBD's map has at least one pin dropped in
    the wrong place — Džemat Heilbronn sits 34 km south of Heilbronn, next to Waiblingen — and a
    community that is told its prayer times come from another town would never know why they are
    a minute out. Where the address names a town vaktija.eu publishes, that town wins, and its
    coordinates replace the pin so the Qibla is computed from the right place too.

    Towns sitting at 0,0 are skipped: vaktija.eu has a handful whose coordinates were never filled
    in, and their published times are wrong, not merely unplaced (Offenburg's sunset arrives before
    Karlsruhe's, 60 km to its north-east).
    """
    usable = [c for c in cities if c.get("lat") and c.get("lon")]
    named = [c for c in usable if fold(c["name"]) == fold(city)]
    if named:
        best = min(named, key=lambda c: km((lat, lon), (c["lat"], c["lon"])))
        return best, km((lat, lon), (best["lat"], best["lon"])), "Name"
    best = min(usable, key=lambda c: km((lat, lon), (c["lat"], c["lon"])))
    return best, km((lat, lon), (best["lat"], best["lon"])), "Nähe"


def main(raw_path):
    rows = json.load(io.open(raw_path, encoding="utf-8"))
    cities = json.load(io.open(CITIES, encoding="utf-8"))

    existing = {}
    if os.path.exists(OUT):
        for c in json.load(io.open(OUT, encoding="utf-8")):
            existing[c["id"]] = c

    out, seen, report = [], {}, []

    for name, lat, lon, _idx, street, city, plz, phone, mail, web in rows:
        name = unescape(name).strip()
        city = (city or "").replace("\xa0", " ").strip()
        plz = (plz or "").replace("\xa0", " ").strip()
        street = (street or "").strip()

        base = slugify(name)
        # Several cities run more than one community ("Džemat Stuttgart" twice); the postcode is
        # what tells them apart in IGBD's own register.
        cid = base if base not in seen else "%s-%s" % (base, plz or len(out))
        seen[cid] = True

        # Kassel is matched by its address, because IGBD lists it under a different name than the
        # one the app has been shipping.
        if street.startswith("Schwanenweg") and city == "Kassel":
            cid = KASSEL_ID
        if cid in KEEP_AS_IS and cid in existing:
            out.append(existing[cid])
            continue

        town, dist, how = pick_town(lat, lon, city, cities)
        # A pin far from the town its own address names is the map's error, not the town's.
        # Trust the address and place the community on the town it says it is in.
        if how == "Name" and dist > 25:
            lat, lon = town["lat"], town["lon"]
            dist = 0.0
        entry = {
            "id": cid,
            "name": name,
            "address": " ".join(x for x in [street, ",".join([""]).join([]), plz, city] if x).strip(),
            # New communities start switched off: they are in IGBD's register, but none of them has
            # agreed to be in the app, and a listed community publishes prayer times in its name.
            "status": existing.get(cid, {}).get("status", "blocked"),
            "locations": [{
                "id": slugify(city or name),
                "name": city or name,
                "vaktijaSlug": town["slug"],
                "latitude": round(lat, 6),
                "longitude": round(lon, 6),
            }],
        }
        entry["address"] = " ".join(x for x in [street, plz, city] if x)
        if mail:
            entry["email"] = mail.strip()
        if phone:
            entry["phone"] = phone.strip()
        if web:
            entry["website"] = web.strip()
        entry.update(DEFAULT_RULES)

        # Carry over anything a human curated for this community on an earlier pass.
        old = existing.get(cid)
        if old:
            for field in ("donationUrl", "logoUrl", "imamName", "imamPhone"):
                if old.get(field):
                    entry[field] = old[field]
            for field in DEFAULT_RULES:
                if field in old:
                    entry[field] = old[field]

        out.append(entry)
        report.append((dist, name, city, town["name"], how))

    out.sort(key=lambda c: c["name"].lower())
    io.open(OUT, "w", encoding="utf-8").write(
        json.dumps(out, ensure_ascii=False, indent=2) + "\n")

    report.sort(reverse=True)
    print("%d Gemeinden geschrieben -> %s" % (len(out), os.path.relpath(OUT, ROOT)))
    print("\nWeiteste Gebetszeiten-Orte (zur Kontrolle):")
    for d, name, city, town, how in report[:12]:
        print("  %5.1f km  %-32s %-19s -> %-18s (%s)" % (d, name[:31], city[:18], town, how))
    far = [r for r in report if r[0] > 25]
    print("\nueber 25 km entfernt: %d von %d" % (len(far), len(report)))


if __name__ == "__main__":
    main(sys.argv[1])
