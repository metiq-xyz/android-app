#!/usr/bin/env python3
"""Renders a long listen of one ambient sound or colored noise and looks for exact repeats.

Renders HOURS of the old single loop and of the two-bed pair (each at 1/sqrt(2), as the app plays
them), then slides a PROBE_S probe taken from the middle across +-SEARCH_S around it and reports
the strongest normalized cross-correlation away from the probe itself. 1.0 means an exact repeat.

Usage: verify-repeats.py ID [HOURS] [ambient|noise]    needs numpy, scipy, soundfile
"""
import sys
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy.signal import fftconvolve, resample_poly

AUDIO = Path(__file__).resolve().parent.parent / "app/src/main/assets/audio"
SR = 4000          # analysis rate; repeats of broadband sound are exact at any rate
PROBE_S, SEARCH_S, GUARD_S = 30, 900, 2
HORIZON_S = 8 * 3600
TIERS = {"ambient": [(HORIZON_S, 0.05)],
         "noise": [(1800, 0.04), (7200, 0.015), (HORIZON_S, 0.005)]}   # (horizon_s, tol_s)


def load(name, kind):
    x, sr = sf.read(AUDIO / kind / name, always_2d=True, dtype="float32")
    m = x.mean(axis=1)
    g = np.gcd(SR, sr)
    return resample_poly(m, SR // g, sr // g).astype(np.float32)


def first_alignment(a_frames, b_frames, sr, tiers):
    """Seconds until a multiple of a's length lands within the tier's tolerance of a multiple of
    b's, or None. Same rule make-beds.py enforces."""
    k = np.arange(1, int(tiers[-1][0] * sr / a_frames) + 1, dtype=np.int64) * a_frames
    tol = np.zeros(len(k), dtype=np.int64)
    for horizon, t in reversed(tiers):
        tol[k <= horizon * sr] = int(t * sr)
    r = k % b_frames
    hit = np.flatnonzero(np.minimum(r, b_frames - r) < tol)
    return None if len(hit) == 0 else float(k[hit[0]] / sr)


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
    kind = sys.argv[3] if len(sys.argv) > 3 else "ambient"
    a, b = load(f"{sid}.ogg", kind), load(f"{sid}_b.ogg", kind)
    old = tile(a, secs)
    new = (tile(a, secs) + tile(b, secs)) * np.float32(2 ** -0.5)
    ra, rb = (sf.read(AUDIO / kind / f, always_2d=True)[0].shape[0] for f in (f"{sid}.ogg", f"{sid}_b.ogg"))
    rate = sf.info(AUDIO / kind / f"{sid}.ogg").samplerate
    t = first_alignment(ra, rb, rate, TIERS[kind])
    print(f"{sid:9s} beds {ra/rate:.1f} s + {rb/rate:.1f} s: first near-alignment: "
          + (f"{t/60:.1f} min" if t else f"none in {HORIZON_S//3600} h"))
    for label, x in (("old, one loop ", old), ("new, two beds  ", new)):
        peak, lag = worst_repeat(x)
        print(f"{sid:9s} {label} {secs/3600:.1f} h  strongest repeat: ncc {peak:.2f} at lag {lag:+.1f} s")
