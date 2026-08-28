# -*- coding: utf-8 -*-
"""Add tajweed-marked text to the bundled Quran.

Source: alquran.cloud's `quran-tajweed` edition, which returns the Uthmani text with the recitation
rules marked inline, e.g. `[h:1[ٱ]` for a hamzat wasl and `[g:1[نّ]` for a ghunnah. The app colours
those spans; it does NOT try to work the rules out for itself. Tajweed is a discipline with exact
rules, and a colour applied by guesswork in a Quran is worse than no colour at all.

The marked text is merged into the existing per-surah files as a `tj` field, so a surah is still one
file read whichever script the reader has chosen.

Run:  python tools/fetch_tajweed.py
"""

import io
import json
import os
import sys
import time
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
QURAN = os.path.join(ROOT, "app", "src", "main", "assets", "quran")
API = "https://api.alquran.cloud/v1/surah/%d/quran-tajweed"


def fetch(surah):
    request = urllib.request.Request(API % surah, headers={"User-Agent": "KasselVaktija/1.0"})
    with urllib.request.urlopen(request, timeout=45) as response:
        return json.loads(response.read().decode("utf-8"))


def main():
    missing, done = [], 0
    for surah in range(1, 115):
        path = os.path.join(QURAN, "%d.json" % surah)
        data = json.load(io.open(path, encoding="utf-8"))
        if all("tj" in a for a in data["ayahs"]):
            done += 1
            continue

        for attempt in range(3):
            try:
                payload = fetch(surah)
                break
            except Exception as error:                       # noqa: BLE001 - report and retry
                if attempt == 2:
                    missing.append((surah, str(error)))
                    payload = None
                time.sleep(2 * (attempt + 1))
        if payload is None:
            continue

        marked = {a["numberInSurah"]: a["text"] for a in payload["data"]["ayahs"]}
        # Only write when every ayah is covered — a half-marked surah would colour some verses and
        # silently leave others plain, which reads as a bug in the text itself.
        if any(a["n"] not in marked for a in data["ayahs"]):
            missing.append((surah, "unvollstaendig"))
            continue
        for ayah in data["ayahs"]:
            ayah["tj"] = marked[ayah["n"]]
        io.open(path, "w", encoding="utf-8").write(
            json.dumps(data, ensure_ascii=False, separators=(", ", ": ")))
        done += 1
        sys.stdout.write("\r%d/114" % done)
        sys.stdout.flush()
        time.sleep(0.25)

    print("\nfertig: %d von 114" % done)
    if missing:
        print("fehlgeschlagen:", missing)


if __name__ == "__main__":
    main()
