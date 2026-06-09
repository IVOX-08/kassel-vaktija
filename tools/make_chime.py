"""Generate a short, pleasant two-strike bell 'chime' for the prayer-time notification
(the 'distinct sound' option for users who don't want the Adhan). Pure stdlib, no deps.
Output: app/src/main/res/raw/chime.wav (16-bit mono, 44.1 kHz, ~2.2 s).
"""
import math
import struct
import wave
import os

SR = 44100


def bell(freq, dur, decay):
    """A bell-like partial stack with an exponential amplitude envelope."""
    n = int(SR * dur)
    out = [0.0] * n
    for i in range(n):
        t = i / SR
        env = math.exp(-decay * t)
        s = (
            1.00 * math.sin(2 * math.pi * freq * t)
            + 0.50 * math.sin(2 * math.pi * freq * 2.0 * t)
            + 0.25 * math.sin(2 * math.pi * freq * 3.01 * t)
            + 0.12 * math.sin(2 * math.pi * freq * 4.2 * t)
        )
        out[i] = env * s
    return out


def mix_at(buf, src, start_sec):
    start = int(SR * start_sec)
    for i, v in enumerate(src):
        idx = start + i
        if idx < len(buf):
            buf[idx] += v


total = int(SR * 2.3)
buf = [0.0] * total

# Two gentle strikes: high (E6) then a warmer (C6) — a calm "ding-dong".
mix_at(buf, bell(1318.51, 1.3, 4.2), 0.00)
mix_at(buf, bell(1046.50, 1.8, 3.4), 0.42)

# Short fade-in (3 ms) to avoid a click, and a fade-out tail.
fade_in = int(SR * 0.003)
for i in range(fade_in):
    buf[i] *= i / fade_in
fade_out = int(SR * 0.15)
for i in range(fade_out):
    buf[total - 1 - i] *= i / fade_out

peak = max(abs(v) for v in buf) or 1.0
gain = 0.89 / peak

out_path = os.path.join(
    os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw", "chime.wav"
)
out_path = os.path.abspath(out_path)
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
