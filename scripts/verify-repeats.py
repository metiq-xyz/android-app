#!/usr/bin/env python3
"""Renders a long listen of one ambient sound and looks for exact repeats.

Renders HOURS of the old single loop and of the two-bed pair (each at 1/sqrt(2), as the app plays
them), then slides a PROBE_S probe taken from the middle across +-SEARCH_S around it and reports
the strongest normalized cross-correlation away from the probe itself. 1.0 means an exact repeat.

Usage: verify-repeats.py ID [HOURS]    needs numpy, scipy, soundfile
"""
import sys
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy.signal import fftconvolve, resample_poly

AUDIO = Path(__file__).resolve().parent.parent / "app/src/main/assets/audio/ambient"
SR = 4000          # analysis rate; repeats of broadband sound are exact at any rate
PROBE_S, SEARCH_S, GUARD_S = 30, 300, 2


def load(name):
    x, sr = sf.read(AUDIO / name, always_2d=True, dtype="float32")
    m = x.mean(axis=1)
    g = np.gcd(SR, sr)
    return resample_poly(m, SR // g, sr // g).astype(np.float32)


def tile(loop, seconds):
    n = seconds * SR
    return np.tile(loop, -(-n // len(loop)))[:n]


def worst_repeat(x):
    mid = len(x) // 2
    p = PROBE_S * SR
    probe = x[mid:mid + p]
    lo, hi = mid - SEARCH_S * SR, mid + SEARCH_S * SR + p
    seg = x[lo:hi]
    num = fftconvolve(seg, probe[::-1], mode="valid")
    energy = np.concatenate([[0], np.cumsum(seg.astype(np.float64) ** 2)])
    seg_e = energy[p:] - energy[:-p]
    ncc = num / np.sqrt((probe.astype(np.float64) ** 2).sum() * seg_e + 1e-12)
    lags = (np.arange(len(ncc)) + lo - mid) / SR
    ncc[np.abs(lags) < GUARD_S] = -1
    i = int(np.argmax(ncc))
    return float(ncc[i]), float(lags[i])


if __name__ == "__main__":
    sid = sys.argv[1]
    secs = int(float(sys.argv[2] if len(sys.argv) > 2 else 2) * 3600)
    a, b = load(f"{sid}.ogg"), load(f"{sid}_b.ogg")
    old = tile(a, secs)
    new = (tile(a, secs) + tile(b, secs)) * np.float32(2 ** -0.5)
    for label, x in (("old, one loop ", old), ("new, two beds  ", new)):
        peak, lag = worst_repeat(x)
        print(f"{sid:9s} {label} {secs/3600:.1f} h  strongest repeat: ncc {peak:.2f} at lag {lag:+.1f} s")
