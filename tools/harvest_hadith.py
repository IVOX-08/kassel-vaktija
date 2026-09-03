# -*- coding: utf-8 -*-
"""Harvest short Bosnian+German hadiths from hadeethenc.com for the TV board.

hadeethenc.com is the source the board already credits: the Encyclopedia of Translated Prophetic
Hadiths, whose Bosnian and German editions are deliberately written in plain language. Every hadith
carries the same numeric id in every language, so bs and de pair up exactly -- no matching by text,
no risk of putting a German sentence next to a different Bosnian one.

Nothing here is written by hand or paraphrased. `hadeeth` minus `hadeeth_intro` is precisely the
narration with its chain removed, which is what the board wants: the saying itself.

Writes the raw pairs to hadith_pairs.json for a separate filtering/build step.
"""
import io
import json
import sys
import os
import threading

if sys.version_info[0] >= 3:
    from urllib.request import urlopen, Request
    from urllib.error import URLError
else:
    raise SystemExit("Python 3 required")

BASE = "https://hadeethenc.com/api/v1"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "hadith_pairs.json")

lock = threading.Lock()


def get(url, tries=4):
    for attempt in range(tries):
        try:
            req = Request(url, headers={"User-Agent": "IGBD-Vaktija/1.0 (TV board content)"})
            return json.loads(urlopen(req, timeout=45).read().decode("utf-8"))
        except Exception as exc:  # noqa: BLE001 - retry on anything the network throws
            if attempt == tries - 1:
                with lock:
                    print("  ! %s -> %s" % (url[-60:], exc))
                return None
    return None


def collect_ids():
    """Every hadith id that exists in BOTH German and Bosnian."""
    cats = get(BASE + "/categories/list/?language=de") or []
    print("Kategorien (de): %d" % len(cats))
    ids = {}
    for cat in cats:
        cid = cat["id"]
        page = 1
        while True:
            data = get(BASE + "/hadeeths/list/?language=de&category_id=%s&page=%d&per_page=100"
                       % (cid, page))
            if not data or not data.get("data"):
                break
            for row in data["data"]:
                tr = row.get("translations") or []
                if "bs" in tr and "de" in tr:
                    ids[row["id"]] = True
            last = int(data.get("meta", {}).get("last_page", 1))
            if page >= last:
                break
            page += 1
    return sorted(ids.keys(), key=int)


def saying(record):
    """The narration with its chain of transmission removed."""
    if not record:
        return None
    full = (record.get("hadeeth") or "").replace("\r", " ").replace("\n", " ")
    intro = (record.get("hadeeth_intro") or "").replace("\r", " ").replace("\n", " ")
    full = " ".join(full.split())
    intro = " ".join(intro.split())
    if intro and full.startswith(intro):
        full = full[len(intro):]
    return full.strip()


def main():
    ids = collect_ids()
    print("gemeinsame Hadithe (de+bs): %d" % len(ids))

    pairs = []
    queue = list(ids)
    qlock = threading.Lock()
    done = [0]

    def worker():
        while True:
            with qlock:
                if not queue:
                    return
                hid = queue.pop()
            de = get(BASE + "/hadeeths/one/?language=de&id=%s" % hid)
            bs = get(BASE + "/hadeeths/one/?language=bs&id=%s" % hid)
            sd, sb = saying(de), saying(bs)
            with lock:
                done[0] += 1
                if done[0] % 100 == 0:
                    print("  %d/%d" % (done[0], len(ids)))
                if sd and sb:
                    pairs.append({
                        "id": hid,
                        "de": sd,
                        "bs": sb,
                        "grade_de": (de.get("grade") or "").strip(),
                        "grade_bs": (bs.get("grade") or "").strip(),
                    })

    threads = [threading.Thread(target=worker) for _ in range(8)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    pairs.sort(key=lambda p: int(p["id"]))
    io.open(OUT, "w", encoding="utf-8").write(
        json.dumps(pairs, ensure_ascii=False, indent=1))
    print("geschrieben: %d Paare -> %s" % (len(pairs), OUT))


main()
