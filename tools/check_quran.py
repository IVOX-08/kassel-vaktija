"""Verify the bundled Quran data is complete: ayah counts per surah vs the canonical
counts (total 6236), index total_verses agreement, page field present, and flag any
blank/suspiciously-short ayah text. Read-only check."""
import json, os

ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "app", "src", "main", "assets", "quran")

# Canonical ayah counts per surah 1..114 (total = 6236).
CANON = [7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,
112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,59,37,35,
38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,
40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,
4,7,3,6,3,5,4,5,6]

idx_path = os.path.join(ASSETS, "index.json")
index = {s["id"]: s for s in json.load(open(idx_path, encoding="utf-8"))}
print(f"index.json: {len(index)} surahs")

problems = []
total = 0
for sid in range(1, 115):
    p = os.path.join(ASSETS, f"{sid}.json")
    if not os.path.exists(p):
        problems.append(f"surah {sid}: FILE MISSING")
        continue
    data = json.load(open(p, encoding="utf-8"))
    ayahs = data.get("ayahs", [])
    n = len(ayahs)
    total += n
    canon = CANON[sid-1]
    if n != canon:
        problems.append(f"surah {sid}: {n} ayahs but canon is {canon}  (DELTA {n-canon})")
    # index total_verses agreement
    iv = index.get(sid, {}).get("total_verses")
    if iv != canon:
        problems.append(f"surah {sid}: index total_verses={iv} != canon {canon}")
    # numbering 1..n contiguous?
    nums = [a.get("n") for a in ayahs]
    if nums != list(range(1, n+1)):
        problems.append(f"surah {sid}: ayah numbers not 1..{n} contiguous")
    # blank / suspiciously short text, missing page
    for a in ayahs:
        t = (a.get("t") or "").strip()
        if not t:
            problems.append(f"surah {sid} ayah {a.get('n')}: BLANK text")
        elif len(t) < 3 and not (sid in (108,) ):  # very short ayahs exist but <3 chars is odd
            problems.append(f"surah {sid} ayah {a.get('n')}: very short text '{t}'")
        if not a.get("p"):
            problems.append(f"surah {sid} ayah {a.get('n')}: missing page")

print(f"TOTAL ayahs across files: {total}  (canonical 6236)")
print(f"problems found: {len(problems)}")
for pr in problems[:60]:
    print("  -", pr)
if len(problems) > 60:
    print(f"  ... and {len(problems)-60} more")
