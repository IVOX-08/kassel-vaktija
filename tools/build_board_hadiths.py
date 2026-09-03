# -*- coding: utf-8 -*-
"""Build the TV board's hadith pool: assets/hadith/board.json.

WHY THIS IS A SCRIPT AND NOT A HAND-WRITTEN FILE
------------------------------------------------
The board used to carry twelve hadiths, so it came round again every twelve days -- the whole
mosque had them memorised. It needs enough that nobody recognises a repeat, which means hundreds,
and hundreds cannot be typed by hand.

Nothing here is written, shortened or paraphrased by us. Inventing or reshaping a hadith is not a
formatting decision; it is putting words in the Prophet's mouth. Every sentence comes verbatim from
a published translation, and this script only *selects* and *pairs*.

THE SOURCE
----------
hadeethenc.com -- the Encyclopedia of Translated Prophetic Hadiths, which the board already
credited before this change. Two properties make it the right source here:

  1. Its Bosnian and German editions are written in deliberately plain language. The congregation
     does not all read German well, and a wall board is read in passing.
  2. Every hadith carries the SAME numeric id in every language. Bosnian and German are therefore
     paired by id, never by matching text -- so a German sentence can never end up beside a
     different Bosnian one.

Each record gives `hadeeth` (chain of transmission + saying) and `hadeeth_intro` (the chain alone).
Subtracting one from the other leaves the saying, which is what a board should show.

THE CEILING, AND WHY IT IS NOT NEGOTIABLE
-----------------------------------------
The Bosnian edition holds ~2770 hadiths, the German one ~757, and 618 exist in both. 618 is
therefore the hard maximum for a bilingual board, whatever number we would like. Machine-translating
the Bosnian remainder into German would close the gap on paper, and is exactly what must not
happen: a machine-translated hadith presented as the Prophet's words is a fabrication with a
citation attached to it.

Run:
    python tools/harvest_hadith.py        # fetches the pairs (slow, hits the network)
    python tools/build_board_hadiths.py   # filters and writes the asset
"""
import io
import json
import os
import random
import re
import unicodedata

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
PAIRS = os.path.join(HERE, "hadith_pairs.json")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "hadith", "board.json")
HELD_OUT = os.path.join(HERE, "board_zurueckgehalten.json")

# THREE LINES IS THE LIMIT, AND IT WAS MEASURED, NOT GUESSED.
#
# The band allows four lines at 16sp, so 400 characters was the first cap tried. It fits -- the text
# is not cut off -- but a screenshot of the board on that day showed the *Dzuma* row below had lost
# its time: the four-line band takes the height out of a weighted row, and the one that gives way is
# the last one. A hadith that costs the congregation the Friday prayer time is a bad trade.
#
# At 300 the German text stays within three lines (the Bosnian is consistently shorter), which the
# board renders with the Dzuma row intact. It also matches what was already there: the longest
# hadith the board has ever shown is 308 characters. Verified on the TV emulator both ways round.
MAX_CHARS = 300
MIN_CHARS = 40

# A hadith is only taken when it stands on its own as a sentence. The subtraction above leaves a
# clean saying most of the time, but not always: where the chain of transmission ran into the
# narration grammatically ("Prenosi se DA JE Poslanik rekao..."), what remains starts mid-clause --
# "da je neki covjek upitao", "dass eine Frau tot aufgefunden wurde". On a wall that reads as a
# fragment, and the honest fix is to drop it, not to patch the words.
#
# Two tests catch every case seen in the data, in both languages, without touching the text:
#   - it must begin the way a sentence begins (a capital letter, or an opening quote)
#   - it must end the way a sentence ends
# German and Bosnian both capitalise sentence openings, so a lower-case first letter IS the tell.
SENTENCE_END = u".!?…"
OPENERS = u"„“‘‚«‹\"'"

# Typographic noise around a quoted saying: the editions wrap sayings in quotes inconsistently, and
# a lone opening mark with no partner looks like a mistake on the board. Only whole wrapping pairs
# come off, and the closing full stop is NEVER stripped -- a hadith ending without punctuation looks
# cut off, which is the very impression to avoid.
QUOTES = u"„“”‟«»‘’‚‹›\"'`"

# Latin letters plus what Bosnian and German add to them.
LETTERS = u"A-Za-zČčĆćĐđŠšŽžÄäÖöÜüß"


# -------------------------------------------------------------------------------------------------
# WHAT DOES NOT GO ON AN UNATTENDED PUBLIC WALL
# -------------------------------------------------------------------------------------------------
# This board hangs in a mosque entrance in Germany, in German, where anyone may read it: visitors,
# neighbours, a journalist, a photograph on the internet by the evening. It has no room for context,
# nobody stands beside it to explain, and the community carries whatever it says.
#
# So a few hadiths are held back. Not because they are doubted -- they are in Bukhari and Muslim --
# but because a wall board is the wrong medium for them. "Die Stunde wird nicht eintreffen, bis ihr
# gegen die Juden kaempfen werdet", read cold by someone with no background, is not a lesson; it is
# a headline. In a lesson, with a teacher and the room to place it, it is another matter, and that
# is where it belongs.
#
# Two mistakes were made getting these patterns right, and both are worth remembering:
#
#   1. RAW STRINGS. Written as "\b(Juden)\b" in a normal string, the \b is a backspace character,
#      not a word boundary, and the rule silently matches nothing at all. Every pattern here is a
#      raw string, and the count printed at the end is what proves they bite.
#   2. WORD BOUNDARIES. Without them "Jude" matches inside the Bosnian word "ljude" (people), which
#      threw out "Der Starke ist nicht derjenige, der jemanden im Ringen besiegt" and a dozen other
#      perfectly good hadiths. Over-pruning that quietly removes good content is its own error --
#      the whole point of this change was to have MORE to show.
HELD_BACK = [
    (u"Juden und Christen als Gruppe",
     r"\b(Juden|Jude|Jüdin|Christen)\b|\b(Jevrej\w*|Židov\w*|kršćan\w*|hrišćan\w*)\b"),
    # Only the calls to fight or kill, not every sentence containing the word. A prohibition --
    # "Wer einen Schutzbefohlenen toetet, wird den Duft des Paradieses nicht riechen" -- is the
    # opposite of what needs holding back, and is exactly what a wall in Germany should be saying.
    (u"Aufruf zum Kampf oder Toeten",
     r"töte ihn|tötet ihn|so töte|kämpfen werdet|Kampf gegen die|Wenn ihr also tötet"
     r"|tötet auf beste|Schwertern aufeinander|ubij ga|borit\w* se protiv"),
    (u"Koerperstrafe an Kindern oder Frauen",
     r"schlagt sie|schlägt seine Frau|udarite ih|udari ih"),
    (u"Koerperstrafen (Hudud)",
     r"steinig\w*|kamenova\w*|Hand abschneiden|odsjeći ruku|auspeitsch\w*|Peitschenhieb\w*"
     r"|bičev\w*|verheirateten Ehebrecher"),
]


def clean(text):
    text = u" ".join((text or u"").replace(u"\r", u" ").replace(u"\n", u" ").split())
    changed = True
    while changed and len(text) > 2:
        changed = False
        if text[0] in QUOTES and text[-1] in QUOTES:
            text, changed = text[1:-1].strip(), True
    # An opening mark whose partner was lost when the chain was subtracted.
    while text and text[0] in QUOTES:
        text = text[1:].strip()
    while text and text[-1] in QUOTES:
        text = text[:-1].strip()
    return text.strip()


def starts_like_a_sentence(text):
    if not text:
        return False
    return text[0] in OPENERS or text[0].isupper() or text[0].isdigit()


def ends_like_a_sentence(text):
    return bool(text) and text[-1] in SENTENCE_END


def key(text):
    """Loose identity for de-duplication: accents, case and punctuation removed."""
    t = unicodedata.normalize("NFKD", text.lower())
    t = u"".join(c for c in t if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9 ]+", "", t)[:60].strip()


# The sentence-shape test above is necessary but NOT sufficient, and it took an audit of all 349
# entries to see why: a leftover chain of transmission can begin with a capital letter and end with
# a full stop, and then it sails straight through. "Von Anas - moege Allah mit ihm zufrieden sein -
# wird vom Propheten ueberliefert: Wer hinauszieht, um Wissen zu erlangen..." is a well-formed
# sentence and still the wrong thing on a board.
#
# Each pattern here was written against a case actually found in the data, never on suspicion --
# which is why the Bosnian rule insists on "prenosi" nearby instead of just an opening "Od":
# "Od najgorih ljudi su oni..." ("Among the worst people are...") is a perfectly good hadith that a
# bare "^Od" would have thrown away.
NOT_A_CLEAN_SAYING = [
    r"^Von .{0,60}(überliefert|berichtete)",   # de: chain survived the subtraction
    r"^Od .{0,50}(prenosi|pripovijeda|kaže se)",  # bs: same
    r"^Dass\b",                                 # de: an answer fragment, not a sentence
    r"[<>]",                                    # broken quote markup in the source
]

# An elision inside the text -- not at the very end -- is the source's own "...", and on a board it
# is indistinguishable from our own truncation.
INNER_ELLIPSIS = re.compile(r"(\.\.\.|…).")


def usable(entry):
    for text in (entry["de"], entry["bs"]):
        if not (MIN_CHARS <= len(text) <= MAX_CHARS):
            return False
        if not starts_like_a_sentence(text) or not ends_like_a_sentence(text):
            return False
        if any(re.search(p, text) for p in NOT_A_CLEAN_SAYING):
            return False
        if INNER_ELLIPSIS.search(text):
            return False
        # An unclosed bracket means the sentence was cut somewhere it should not have been.
        if text.count("(") != text.count(")"):
            return False
        # Still partly in Arabic script, or the text was lost entirely.
        if not re.search(u"[" + LETTERS + u"]", text):
            return False
    return True


def held_back_reason(entry):
    """The label under which an entry is kept off the board, or None when it may show."""
    text = entry["de"] + u" " + entry["bs"]
    for label, pattern in HELD_BACK:
        if re.search(pattern, text, re.IGNORECASE):
            return label
    return None


def main():
    raw = json.load(io.open(PAIRS, encoding="utf-8"))
    print("geerntet:        %4d" % len(raw))

    cleaned = [{"id": r["id"], "de": clean(r["de"]), "bs": clean(r["bs"])} for r in raw]

    kept, seen, dropped, held = [], set(), 0, []
    for row in cleaned:
        if not usable(row):
            dropped += 1
            continue
        k = key(row["de"])
        if k in seen:
            dropped += 1
            continue
        seen.add(k)
        reason = held_back_reason(row)
        if reason:
            held.append(dict(row, reason=reason))
            continue
        kept.append(row)

    print("verworfen:       %4d  (unvollstaendig, zu lang oder doppelt)" % dropped)
    print("zurueckgehalten: %4d  (nicht fuer eine oeffentliche Wand)" % len(held))
    for label, _ in HELD_BACK:
        n = sum(1 for h in held if h["reason"] == label)
        print("                       %-38s %2d" % (label, n))
    print("uebernommen:     %4d" % len(kept))

    # Written out so the decision stays reviewable: the owner can read exactly what was held back
    # and say "that one is fine, put it up" -- rather than discovering an absence months later.
    io.open(HELD_OUT, "w", encoding="utf-8").write(
        json.dumps(held, ensure_ascii=False, indent=1) + "\n")

    # Shuffled once, with a fixed seed, then stored in that order. The board picks by epoch-day
    # modulo the list length, so without this the mosque would get a week of hadiths on the same
    # subject in a row -- the ids run in subject order. Fixed seed, so a rebuild does not reshuffle
    # what people have already seen.
    random.Random(20260903).shuffle(kept)

    out = {
        "metadata": {
            "name": u"Hadith des Tages — Auswahl für die Wandanzeige / "
                    u"Hadis dana — odabir za zidni ekran",
            "note": u"Kurze, in sich vollständige Hadithe für die TV-Tafel im Moschee-Eingang. "
                    u"Jeder ist OHNE Überliefererkette und Quellenangabe gespeichert (nur der "
                    u"eigentliche Spruch), wortgetreu aus der veröffentlichten Übersetzung — "
                    u"nichts ist nacherzählt, gekürzt oder maschinell übersetzt. Bosnisch und "
                    u"Deutsch gehören über die Kennung zusammen, nicht über ähnlichen Text. "
                    u"Erzeugt mit tools/build_board_hadiths.py — nicht von Hand ändern.",
            "sources": u"Enciklopedija prevedenih vjerovjesničkih hadisa / Enzyklopädie der "
                       u"übersetzten prophetischen Hadithe (hadeethenc.com), bosnische und "
                       u"deutsche Ausgabe. Die Kennung je Hadith ist die dortige.",
            "count": len(kept),
        },
        "hadiths": [{"ref": u"hadeethenc %s" % r["id"], "bs": r["bs"], "de": r["de"]}
                    for r in kept],
    }
    io.open(OUT, "w", encoding="utf-8").write(
        json.dumps(out, ensure_ascii=False, indent=1) + "\n")
    print("geschrieben:     %4d  ->  %s" % (len(kept), OUT))
    print("laengster de: %d Zeichen, bs: %d Zeichen"
          % (max(len(r["de"]) for r in kept), max(len(r["bs"]) for r in kept)))


main()
