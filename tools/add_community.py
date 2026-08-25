"""Turn a community's name + postal address into a ready, verified catalogue entry.

The bottleneck in going nationwide is not code, it is 84 sets of details arriving by e-mail. This
does the fiddly part of each one: find the mosque's real coordinates, pick the town whose published
prayer times to use, and confirm that town actually returns times before anyone relies on it.

Why coordinates matter beyond the map: they drive the Qibla bearing (which spans ~12 degrees across
Germany) and the computed month calendar. A city-centre guess is typically 1-3 km out, which is
harmless for times but sloppy for a mosque's own entry.

Why the town can differ from the mosque's town: vaktija.eu publishes ~1300 German towns, not all of
them. Where a town is missing, the nearest published one is used — at these latitudes 20 km is well
under a minute of difference, and a real published time beats a locally computed guess.

Usage:
    python tools/add_community.py "IGBD-Gemeinde Rosenheim" "Burgfriedstraße 55, 83024 Rosenheim"
    python tools/add_community.py --file communities.tsv     # name<TAB>address per line

Writes/updates docs/multi-gemeinde/communities.json, which is the import source for Firestore.
"""

import json
import math
import os
import re
import sys
import time
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "docs", "multi-gemeinde", "communities.json")
CITIES = os.path.join(ROOT, "docs", "multi-gemeinde", "vaktija_cities.json")
UA = "KasselVaktija/1.0 (IGBD community app; kontakt via igbd.org)"


def _get(url, timeout=30):
    return urllib.request.urlopen(
        urllib.request.Request(url, headers={"User-Agent": UA}), timeout=timeout
    ).read().decode("utf-8", "replace")


def load_cities():
    """The town list vaktija.eu embeds in its /bs/cities page: name, slug, coordinates."""
    if os.path.exists(CITIES):
        return json.load(open(CITIES, encoding="utf-8"))
    html = _get("https://vaktija.eu/bs/cities", timeout=60)
    pat = re.compile(
        r'\{"id":(\d+),"name":"([^"]+)","slug":"([^"]+)",'
        r'"latitude":([-\d.]+),"longitude":([-\d.]+)\}'
    )
    rows = {c: {"name": n, "slug": c, "lat": float(la), "lon": float(lo)}
            for _, n, c, la, lo in pat.findall(html)}
    rows = list(rows.values())
    os.makedirs(os.path.dirname(CITIES), exist_ok=True)
    json.dump(rows, open(CITIES, "w", encoding="utf-8"), ensure_ascii=False)
    return rows


def geocode(address):
    """Mosque coordinates from the postal address. Nominatim asks for <=1 request/second."""
    url = ("https://nominatim.openstreetmap.org/search?format=json&limit=1&q="
           + urllib.parse.quote(address))
    hits = json.loads(_get(url))
    time.sleep(1.1)
    if not hits:
        return None
    return float(hits[0]["lat"]), float(hits[0]["lon"]), hits[0].get("display_name", "")


def km(a, b):
    r, p = 6371.0, math.pi / 180
    dla, dlo = (b[0] - a[0]) * p, (b[1] - a[1]) * p
    x = (math.sin(dla / 2) ** 2
         + math.cos(a[0] * p) * math.cos(b[0] * p) * math.sin(dlo / 2) ** 2)
    return 2 * r * math.asin(math.sqrt(x))


def town_in(address):
    """The town out of a German postal address: everything after the 5-digit postcode."""
    m = re.search(r"\b\d{5}\s+(.+)$", address.strip())
    return m.group(1).strip() if m else None


def fold(s):
    s = s.lower()
    for a, b in [("ä", "a"), ("ö", "o"), ("ü", "u"), ("ß", "ss"), ("."," "), ("-", " ")]:
        s = s.replace(a, b)
    return " ".join(s.split())


def usable(city):
    """
    Drop towns whose coordinates are 0,0.

    vaktija.eu has 13 of these, Offenburg among them, and they are not merely cosmetic: the times
    published under such an entry do not match the real town (Offenburg's sunset came out EARLIER
    than Karlsruhe's, 60 km to the north-east, which cannot happen). Matching a community to one
    would have handed it a plausible-looking but wrong prayer schedule.
    """
    return abs(city["lat"]) > 0.01 or abs(city["lon"]) > 0.01


def pick_town(cities, lat, lon, address):
    """
    Prefer the town named in the address, fall back to the nearest published one.

    Nearest-only was wrong in a way that would have shipped: Kassel's mosque is 2.2 km from
    Niestetal's centre and less than that from nothing, so a Kassel community would have been
    labelled "Niestetal" even though Kassel is published. People expect to see their own town.
    """
    cities = [c for c in cities if usable(c)]
    wanted = town_in(address)
    if wanted:
        target = fold(wanted)
        named = [c for c in cities if fold(c["name"]) == target]
        if named:
            best = min(named, key=lambda c: km((lat, lon), (c["lat"], c["lon"])))
            return best, km((lat, lon), (best["lat"], best["lon"])), "Name"
    best = min(cities, key=lambda c: km((lat, lon), (c["lat"], c["lon"])))
    return best, km((lat, lon), (best["lat"], best["lon"])), "Nähe"


def times_work(slug):
    """Confirm the town really publishes times — a slug that 200s can still carry nothing."""
    try:
        html = _get(f"https://vaktija.eu/{slug}")
    except Exception:
        return None
    for block in re.findall(
        r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', html, re.S
    ):
        try:
            data = json.loads(block)
        except Exception:
            continue
        for item in data.get("@graph", [data]):
            if item.get("@type") == "Dataset":
                events = item.get("mainEntity", {}).get("eventSchedule", [])
                if events:
                    return {e.get("name"): e.get("startTime") for e in events}
    return None


def slugify(name):
    s = name.lower()
    for a, b in [("ä", "ae"), ("ö", "oe"), ("ü", "ue"), ("ß", "ss"),
                 ("č", "c"), ("ć", "c"), ("ž", "z"), ("š", "s"), ("đ", "d")]:
        s = s.replace(a, b)
    s = re.sub(r"igbd[- ]*(gemeinde)?", "", s)
    s = re.sub(r"[^a-z0-9]+", "-", s).strip("-")
    return s or "gemeinde"


def build(name, address, cities):
    geo = geocode(address)
    if not geo:
        return {"name": name, "address": address, "error": "Adresse nicht gefunden"}
    lat, lon, resolved = geo
    town, distance, how = pick_town(cities, lat, lon, address)
    times = times_work(town["slug"])
    entry = {
        "id": slugify(name),
        "name": name,
        "address": address,
        "status": "active",
        "locations": [{
            "id": town["slug"],
            "name": town["name"],
            "vaktijaSlug": town["slug"],
            "latitude": round(lat, 6),
            "longitude": round(lon, 6),
        }],
        "_check": {
            "geocoded_as": resolved,
            "town_distance_km": round(distance, 1),
            "town_matched_by": how,
            "times_ok": bool(times),
            "sample_times": times,
        },
    }
    return entry


def main(argv):
    cities = load_cities()
    pairs = []
    if argv and argv[0] == "--file":
        for line in open(argv[1], encoding="utf-8"):
            if line.strip() and "\t" in line:
                n, a = line.rstrip("\n").split("\t", 1)
                pairs.append((n.strip(), a.strip()))
    elif len(argv) >= 2:
        pairs.append((argv[0], argv[1]))
    else:
        print(__doc__)
        return 1

    existing = json.load(open(OUT, encoding="utf-8")) if os.path.exists(OUT) else []
    by_id = {c["id"]: c for c in existing if "id" in c}

    for name, address in pairs:
        entry = build(name, address, cities)
        if "error" in entry:
            print(f"  ! {name}: {entry['error']}")
            continue
        by_id[entry["id"]] = entry
        c = entry["_check"]
        flag = "OK " if c["times_ok"] else "!! "
        print(f"  {flag}{name}")
        print(f"      {entry['locations'][0]['name']} ({entry['locations'][0]['vaktijaSlug']}), "
              f"{c['town_distance_km']} km, ueber {c['town_matched_by']}, Zeiten: "
              f"{'ja' if c['times_ok'] else 'NEIN'}")

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    json.dump(sorted(by_id.values(), key=lambda c: c["name"]),
              open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"\n{len(by_id)} Gemeinde(n) in {os.path.relpath(OUT, ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
