# -*- coding: utf-8 -*-
"""Generate the app's plain notification tones.

Why these are synthesised rather than sampled: they are meant to be neutral. Someone who cannot let
the Adhan play — at work, in a lecture, on a ward — still wants to know the time has come, and what
they need is a sound that is calm and unremarkable. A struck bell does that: a fast attack, a long
exponential decay, and a small set of harmonics that ring together the way metal does.

The Adhan itself is NOT generated here. That is a human voice, and it belongs to the muezzin who
recorded it.

Run:  python tools/gen_tones.py
"""

import math
import os
import struct
import wave

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "res", "raw")
RATE = 44100


def bell(frequency, seconds, decay, harmonics):
    """One struck note: harmonics over a fundamental, each fading at its own rate."""
    samples = []
    total = int(RATE * seconds)
    for i in range(total):
        t = i / RATE
        value = 0.0
        for multiple, weight, detune in harmonics:
            # Higher partials die away faster, which is what makes a bell sound like metal rather
            # than like an organ.
            envelope = math.exp(-t * decay * multiple)
            value += weight * envelope * math.sin(2 * math.pi * frequency * multiple * detune * t)
        # A short attack ramp removes the click a hard start would otherwise make.
        attack = min(1.0, t / 0.006)
        samples.append(value * attack)
    return samples


def mix(layers, seconds):
    total = int(RATE * seconds)
    out = [0.0] * total
    for offset_seconds, layer in layers:
        offset = int(RATE * offset_seconds)
        for i, v in enumerate(layer):
            if offset + i < total:
                out[offset + i] += v
    peak = max(1e-9, max(abs(v) for v in out))
    # Leave a little headroom so the file is not sitting on the ceiling.
    return [v / peak * 0.86 for v in out]


def write(name, samples):
    path = os.path.join(OUT, name + ".wav")
    with wave.open(path, "w") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(RATE)
        f.writeframes(b"".join(
            struct.pack("<h", int(max(-1.0, min(1.0, s)) * 32767)) for s in samples))
    print("%-16s %6.1f KB" % (name + ".wav", os.path.getsize(path) / 1024))


HARMONICS_BELL = [(1.0, 1.0, 1.0), (2.0, 0.5, 1.002), (3.0, 0.28, 0.999),
                  (4.2, 0.16, 1.001), (5.4, 0.09, 1.0)]
HARMONICS_GONG = [(1.0, 1.0, 1.0), (1.5, 0.62, 1.003), (2.4, 0.4, 0.998),
                  (3.7, 0.2, 1.002), (5.1, 0.11, 1.0)]


def main():
    if not os.path.isdir(OUT):
        os.makedirs(OUT)

    # A single struck bell, D5. Short enough for a notification, warm enough not to startle.
    write("tone_bell", mix([(0.0, bell(587.33, 2.6, 1.7, HARMONICS_BELL))], 2.6))

    # Lower and longer — the closest a synthesised tone gets to a mosque's hand bell. For people
    # who want to hear the prayer time from another room without the Adhan.
    write("tone_gong", mix([(0.0, bell(196.00, 4.2, 0.62, HARMONICS_GONG))], 4.2))

    # Two soft notes a fourth apart, the second following closely: brief, quiet, and easy to hear
    # past a conversation. Meant for announcements rather than prayer times.
    write("tone_soft", mix([
        (0.00, bell(659.25, 1.5, 2.6, HARMONICS_BELL)),
        (0.16, bell(880.00, 1.4, 2.8, HARMONICS_BELL)),
    ], 1.7))


if __name__ == "__main__":
    main()
