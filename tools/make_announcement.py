"""Generate a short, bright 'announcement' tone for community-news notifications.
Deliberately DIFFERENT from the prayer sounds: a rising 3-note major arpeggio
(C6-E6-G6) with a soft marimba-like timbre, vs. the chime's two descending bells.
Pure stdlib. Output: app/src/main/res/raw/announcement.wav (16-bit mono, 44.1 kHz, ~1.4 s).
"""
import math
import struct
import wave
import os

SR = 44100


def note(freq, dur, decay):
    """A soft mallet/marimba-ish tone: fundamental + a little 2nd/4th harmonic, fast decay."""
    n = int(SR * dur)
    out = [0.0] * n
    for i in range(n):
        t = i / SR
        env = math.exp(-decay * t)
        s = (
            1.00 * math.sin(2 * math.pi * freq * t)
            + 0.35 * math.sin(2 * math.pi * freq * 2.0 * t)
            + 0.12 * math.sin(2 * math.pi * freq * 4.0 * t)
        )
        out[i] = env * s
    return out


def mix_at(buf, src, start_sec):
    start = int(SR * start_sec)
    for i, v in enumerate(src):
        idx = start + i
        if idx < len(buf):
            buf[idx] += v


total = int(SR * 1.45)
buf = [0.0] * total

# Rising major arpeggio: C6, E6, G6, then a brighter C7 accent.
mix_at(buf, note(1046.50, 0.45, 7.0), 0.00)   # C6
mix_at(buf, note(1318.51, 0.45, 7.0), 0.13)   # E6
mix_at(buf, note(1567.98, 0.55, 6.0), 0.26)   # G6
mix_at(buf, note(2093.00, 0.70, 5.0), 0.40)   # C7 (final, longer ring)

# 3 ms fade-in (declick) + fade-out tail.
fade_in = int(SR * 0.003)
for i in range(fade_in):
    buf[i] *= i / fade_in
fade_out = int(SR * 0.12)
for i in range(fade_out):
    buf[total - 1 - i] *= i / fade_out

peak = max(abs(v) for v in buf) or 1.0
gain = 0.88 / peak

out_path = os.path.abspath(
    os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw", "announcement.wav")
)
with wave.open(out_path, "w") as w:
    w.setnchannels(1)
    w.setsampwidth(2)
    w.setframerate(SR)
    frames = bytearray()
    for v in buf:
        s = int(max(-1.0, min(1.0, v * gain)) * 32767)
        frames += struct.pack("<h", s)
    w.writeframes(bytes(frames))

print("wrote", out_path, os.path.getsize(out_path), "bytes")
