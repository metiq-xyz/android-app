#!/usr/bin/env python3
"""Audits the loop quality of Metiq's audio assets.

For every file it reports duration, sample rate, integrated LUFS, salient
transients (short-time energy far above a running median, the one-off events
listeners memorize) and the discontinuity at the wrap point.

Usage: audit-loops.py [-I] FILE_OR_DIR...   (needs numpy, scipy, soundfile, pyloudnorm)
"""
import sys
from pathlib import Path

import numpy as np
import pyloudnorm as pyln
import soundfile as sf
from scipy.signal import stft

WIN_S = 0.05           # short-time energy window
MEDIAN_S = 6.0         # running-median span (centered, circular)
EVENT_DB = 10.0        # window this far above the running median counts as an event
SEAM_S = 1.0           # span compared on each side of the wrap point


def short_time_db(mono, sr):
    n = int(WIN_S * sr)
    frames = len(mono) // n
    rms = np.sqrt(np.mean(mono[: frames * n].reshape(frames, n) ** 2, axis=1) + 1e-12)
    return 20 * np.log10(rms)


def running_median_circular(x, span):
    pad = span // 2
    ext = np.concatenate([x[-pad:], x, x[:pad]])
    win = np.lib.stride_tricks.sliding_window_view(ext, 2 * pad + 1)
    return np.median(win, axis=1)[: len(x)]


def events(mono, sr):
    db = short_time_db(mono, sr)
    span = max(3, int(MEDIAN_S / WIN_S) | 1)
    excess = db - running_median_circular(db, span)
    hot = excess > EVENT_DB
    out, i = [], 0
    while i < len(hot):
        if hot[i]:
            j = i
            while j < len(hot) and hot[j]:
                j += 1
            out.append((i * WIN_S, float(excess[i:j].max())))
            i = j
        else:
            i += 1
    return out, db


def seam(x, sr):
    """Wrap-point discontinuity: sample step, level step, spectral distance."""
    mono = x.mean(axis=1)
    k = int(SEAM_S * sr)
    step = np.abs(x[0] - x[-1]).max()
    local = np.abs(np.diff(x, axis=0)).mean()
    head, tail = mono[:k], mono[-k:]
    level = 20 * np.log10((np.sqrt(np.mean(head**2)) + 1e-12) / (np.sqrt(np.mean(tail**2)) + 1e-12))
    spec = []
    for seg in (tail, head):
        _, _, z = stft(seg, sr, nperseg=2048)
        spec.append(10 * np.log10(np.mean(np.abs(z) ** 2, axis=1) + 1e-14))
    lsd = float(np.sqrt(np.mean((spec[0] - spec[1]) ** 2)))
    return float(step / (local + 1e-12)), float(level), lsd


def audit(path):
    x, sr = sf.read(path, always_2d=True, dtype="float64")
    mono = x.mean(axis=1)
    lufs = pyln.Meter(sr).integrated_loudness(x if x.shape[1] <= 2 else mono)
    ev, db = events(mono, sr)
    step, level, lsd = seam(x, sr)
    print(f"{path.name:14s} {len(x)/sr:7.2f}s  {len(x):9d} fr  {sr/1000:g}k x{x.shape[1]}  {lufs:6.1f} LUFS")
    print(f"{'':14s} peak {20*np.log10(np.abs(x).max()+1e-12):5.1f} dBFS  "
          f"events>{EVENT_DB:g}dB: {len(ev)}"
          + (f"  worst {max(e[1] for e in ev):.1f} dB" if ev else ""))
    if ev:
        top = sorted(ev, key=lambda e: -e[1])[:5]
        print(f"{'':14s} top: " + ", ".join(f"{t:.1f}s(+{e:.0f}dB)" for t, e in top))
    print(f"{'':14s} seam: step {step:.1f}x typical, level {level:+.1f} dB, spectral dist {lsd:.1f} dB")


def main(argv):
    files = []
    for a in argv:
        p = Path(a)
        files += sorted(p.rglob("*")) if p.is_dir() else [p]
    for f in files:
        if f.suffix.lower() in (".ogg", ".mp3", ".wav", ".flac"):
            audit(f)


if __name__ == "__main__":
    main(sys.argv[1:])
