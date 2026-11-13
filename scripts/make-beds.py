#!/usr/bin/env python3
"""Builds a second bed for each ambient sound and each colored noise.

Each ambient sound ships as two beds that play together at -3 dB each (uncorrelated beds add in
power, so the level stays what one bed was). Bed "b" has a different length from bed "a" and
different content, so the combined pattern only repeats at the least common multiple of the two.

  * Masters with at least TAIL_MIN_S of material the "a" loop did not use: bed b is that tail,
    closed into a loop with an equal-power crossfade.
  * Shorter masters: bed b is granular resynthesis of the whole master (random grains with
    equal-power crossfades), then closed the same way. Grains containing sharp one-off events
    are skipped where the sound is a steady texture.

Output is deterministic (fixed seed) and is committed, never built (see encode-audio.sh).
Usage: make-beds.py [ids...]    needs numpy, scipy, soundfile and ffmpeg (on PATH, or `pip install imageio-ffmpeg`); writes
app/src/main/assets/audio/ambient/<id>_b.ogg
"""
import math
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy.signal import butter, sosfilt

ROOT = Path(__file__).resolve().parent.parent
MASTERS = ROOT / "audio-src/masters/ambient"
OUT = ROOT / "app/src/main/assets/audio/ambient"

A_LOOP_S = 45.0        # what encode-audio.sh ships as bed a (shorter masters use all of it)
A_CROSSFADE_S = 3.0
TAIL_MIN_S = 40.0
B_LENGTH_S = {"rain": 52, "wind": 53, "seawaves": 49, "stream": 51, "crickets": 47,
              "fire": 50, "birds": 53, "cafe": 47}
CLOSE_FADE_S = 3.0     # tail-into-head crossfade that closes the loop
GRAIN_S = 1.6
HOP_S = 0.8            # grains overlap by half
MIN_JUMP_S = 4.0       # next grain must start this far from the previous one
EVENT_DB = 12.0        # steady textures reject grains with a burst this far above their median
EVENT_FREE = {"rain", "wind", "seawaves", "stream", "crickets"}
TARGET_LUFS = -16      # same as encode-audio.sh
HORIZON_S = 8 * 3600   # a long night
SEARCH_S = 3.0         # how far from the wanted length pick_length may wander
AMBIENT_TIERS = [(HORIZON_S, 0.05)]
# Over 8 h a 10 s loop has 2880 repeats to keep clear of, so the best reachable gap is ~7.5 ms
# (pigeonhole); ask for much more where there are few repeats.
NOISE_TIERS = [(1800, 0.04), (7200, 0.015), (HORIZON_S, 0.005)]
NOISE_DIR = ROOT / "app/src/main/assets/audio/noise"
NOISE_COLORS = ["pink", "brown", "white", "grey"]
NOISE_B_S = 21         # longer second loops leave more room to avoid alignment
NOISE_SMOOTH_HZ = 60   # smooth the measured spectrum so the new noise has the color, not the loop
SEED = 20261113


def find_ffmpeg():
    exe = shutil.which("ffmpeg")
    if exe:
        return exe
    import imageio_ffmpeg  # pip install imageio-ffmpeg
    return imageio_ffmpeg.get_ffmpeg_exe()


FFMPEG = find_ffmpeg()


def decoded_frames(path):
    """Frames the app's decoder produces. sf.info().frames is the Ogg granule position, which
    includes encoder padding and is wrong for files made by ffmpeg (checked on a Pixel)."""
    return len(sf.read(path, always_2d=True, dtype="float32")[0])


def equal_power(n):
    t = np.linspace(0.0, 1.0, n, endpoint=False)
    return np.cos(t * math.pi / 2), np.sin(t * math.pi / 2)


def close_loop(x, length, fade):
    """Return `length` frames whose end flows into its start (equal-power tail-into-head)."""
    f = int(fade)
    body = x[: length + f].copy()
    out_w, in_w = equal_power(f)
    body[:f] = body[length:length + f] * out_w[:, None] + body[:f] * in_w[:, None]
    return body[:length]


def burst_excess_db(x, sr):
    """Per-5ms high-band energy excess over a 2 s running median, as the audit script does."""
    from scipy.ndimage import median_filter
    hp = sosfilt(butter(4, 2000, "highpass", fs=sr, output="sos"), x.mean(axis=1))
    n = int(0.005 * sr)
    frames = len(hp) // n
    db = 10 * np.log10(np.mean(hp[: frames * n].reshape(frames, n) ** 2, axis=1) + 1e-14)
    return db - median_filter(db, size=int(2.0 / 0.005) | 1, mode="reflect"), n


def granular(x, sr, total, rng, event_free):
    grain, hop = int(GRAIN_S * sr), int(HOP_S * sr)
    n_src = len(x)
    excess, blk = burst_excess_db(x, sr) if event_free else (None, 1)
    rms = np.sqrt(np.mean(x.mean(axis=1) ** 2) + 1e-12)
    win_out, win_in = equal_power(grain - hop)
    out = np.zeros((total + grain, x.shape[1]))
    pos, prev, tries = 0, -10**9, 0
    while pos < total:
        tries += 1
        s = int(rng.integers(0, n_src - grain))
        if abs(s - prev) < MIN_JUMP_S * sr and tries < 400:
            continue
        g = x[s:s + grain]
        if tries < 400:
            if event_free and excess[s // blk:(s + grain) // blk].max() > EVENT_DB:
                continue
            level = np.sqrt(np.mean(g.mean(axis=1) ** 2) + 1e-12)
            if abs(20 * math.log10(level / rms)) > 2.0:
                continue
        tries, prev = 0, s
        g = g.copy()
        if pos > 0:
            g[: grain - hop] *= win_in[:, None]
            out[pos:pos + grain - hop] *= win_out[:, None]
        out[pos:pos + grain] += g
        pos += hop
    return out[:total]


def pick_length(sr, a_frames, seconds, tiers, max_frames=None):
    """Frames for bed b near `seconds` such that multiples of a's length stay clear of multiples
    of b's length. `tiers` is [(horizon_s, tol_s), ...]: within each horizon the nearest multiple
    of b must be at least tol_s away, so early near-alignments need the widest clearance.
    Tries the lengths closest to `seconds` first."""
    k = np.arange(1, int(tiers[-1][0] * sr / a_frames) + 1, dtype=np.int64) * a_frames
    tol = np.zeros(len(k), dtype=np.int64)
    for horizon, t in reversed(tiers):
        tol[k <= horizon * sr] = int(t * sr)
    centre = int(seconds * sr)
    for delta in range(int(SEARCH_S * sr)):
        for n in (centre + delta, centre - delta):
            if max_frames is not None and n > max_frames:
                continue
            r = k % n
            if (np.minimum(r, n - r) >= tol).all():
                return n
    raise RuntimeError(f"no bed length within {SEARCH_S}s of {seconds}s avoids alignment")


def encode(bed, sr, dst, lufs):
    """Same normalize + encode chain as encode-audio.sh, so beds a and b are treated alike.
    The decoded file must have exactly len(bed) frames: pick_length reasoned about that length."""
    frames = len(bed)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "bed.wav"
        sf.write(wav, bed, sr, subtype="FLOAT")
        subprocess.run(
            [FFMPEG, "-hide_banner", "-loglevel", "error", "-y", "-i", str(wav),
             "-af", f"loudnorm=I={lufs:.1f}:TP=-1.5:LRA=11,aresample={sr}",
             "-c:a", "libvorbis", "-q:a", "6", str(dst)],
            check=True,
        )
    got = decoded_frames(dst)
    if got != frames:
        raise RuntimeError(f"{dst.name}: encoded {got} frames, expected {frames}")


def build(sound_id):
    src = next(MASTERS.glob(f"{sound_id}.*"))
    x, sr = sf.read(src, always_2d=True, dtype="float64")
    n = len(x)
    a_len = decoded_frames(OUT / f"{sound_id}.ogg")
    used = int((A_LOOP_S + A_CROSSFADE_S) * sr)
    tail = x[used:]
    rng = np.random.default_rng(SEED + sum(map(ord, sound_id)))
    close = int(CLOSE_FADE_S * sr)
    from_tail = len(tail) >= TAIL_MIN_S * sr
    cap = len(tail) - close if from_tail else None
    want = min(B_LENGTH_S[sound_id], cap / sr - 0.1) if from_tail else B_LENGTH_S[sound_id]
    b_len = pick_length(sr, a_len, want, AMBIENT_TIERS, cap)
    if from_tail:
        how = f"tail {len(tail)/sr:.0f}s of master"
        bed = close_loop(tail, b_len, close)
    else:
        how = "granular"
        bed = close_loop(granular(x, sr, b_len + close, rng, sound_id in EVENT_FREE), b_len, close)

    dst = OUT / f"{sound_id}_b.ogg"
    encode(bed, sr, dst, TARGET_LUFS)
    print(f"{sound_id:9s} b = {b_len/sr:6.2f}s ({b_len} fr) {how}; {dst.stat().st_size/1024:.0f} KiB")


def build_noise(color):
    from scipy.ndimage import uniform_filter1d
    from scipy.signal import welch
    import pyloudnorm as pyln

    x, sr = sf.read(NOISE_DIR / f"{color}.ogg", dtype="float64")
    n = pick_length(sr, len(x), NOISE_B_S, NOISE_TIERS)
    f, psd = welch(x, sr, nperseg=8192)
    psd = uniform_filter1d(psd, max(1, int(NOISE_SMOOTH_HZ / (f[1] - f[0]))), mode="nearest")
    freqs = np.fft.rfftfreq(n, 1 / sr)
    rng = np.random.default_rng(SEED + sum(map(ord, color)))
    phase = rng.uniform(0, 2 * np.pi, len(freqs))
    phase[0] = 0
    if n % 2 == 0:
        phase[-1] = 0
    y = np.fft.irfft(np.sqrt(np.interp(freqs, f, psd)) * np.exp(1j * phase), n)
    y *= 0.5 / np.abs(y).max()
    target = pyln.Meter(sr).integrated_loudness(x[:, None])
    dst = NOISE_DIR / f"{color}_b.ogg"
    encode(y[:, None], sr, dst, target)
    print(f"{color:9s} b = {n/sr:6.2f}s ({n} fr) synthesized; target {target:.1f} LUFS; "
          f"{dst.stat().st_size/1024:.0f} KiB")


if __name__ == "__main__":
    for i in sys.argv[1:] or list(B_LENGTH_S) + NOISE_COLORS:
        build_noise(i) if i in NOISE_COLORS else build(i)
