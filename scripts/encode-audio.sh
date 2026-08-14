#!/usr/bin/env bash
# Regenerates the app's ambient OGG assets from the masters in
# audio-src/masters/ambient. For each master it:
#
#   1. cuts a seamless loop: takes LOOP_S seconds and crossfades the master's
#      tail into the head, so the asset's end flows into its start with no seam
#   2. loudness-normalizes to TARGET_LUFS so all sounds sit at the same level
#   3. encodes to Vorbis q6 (~192 kbps stereo) into app/src/main/assets/audio/ambient
#
# Run manually after adding or changing a master, then commit the regenerated
# .ogg files. Never wire this into the build: F-Droid reproducible builds
# require the committed assets to be the encode, since ffmpeg output is not
# bit-stable across versions.
#
# Masters must be named <ambient-id>.<ext> (e.g. cafe.mp3); unknown ids are
# skipped so a not-yet-wired sound (say rain) can live in masters without
# shipping an unused asset.
set -euo pipefail

cd "$(dirname "$0")/.."

MASTERS=audio-src/masters/ambient
OUT=app/src/main/assets/audio/ambient
LOOP_S=45          # loop length to ship (shorter masters use their full length)
CROSSFADE_S=3      # tail-into-head crossfade
TARGET_LUFS=-16

KNOWN_IDS="seawaves rain fire birds cafe wind crickets stream"

for src in "$MASTERS"/*; do
    id=$(basename "${src%.*}")
    if ! grep -qw "$id" <<<"$KNOWN_IDS"; then
        echo "SKIP $src: '$id' is not a known ambient id" >&2
        continue
    fi

    dur=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$src")
    sr=$(ffprobe -v error -select_streams a:0 -show_entries stream=sample_rate -of csv=p=0 "$src")
    # Loop body length: LOOP_S when the master is long enough, else everything
    # minus the crossfade.
    body=$(awk -v d="$dur" -v t="$LOOP_S" -v x="$CROSSFADE_S" \
        'BEGIN { l = d - x - 0.1; if (l > t) l = t; printf "%.3f", l }')
    end=$(awk -v b="$body" -v x="$CROSSFADE_S" 'BEGIN { printf "%.3f", b + x }')

    echo "== $id: master ${dur%.*}s -> loop ${body}s (${CROSSFADE_S}s crossfade)"
    ffmpeg -hide_banner -loglevel error -y -i "$src" -filter_complex "
        [0:a]atrim=start=${body}:end=${end},asetpts=PTS-STARTPTS[tail];
        [0:a]atrim=start=0:end=${CROSSFADE_S},asetpts=PTS-STARTPTS[head];
        [tail][head]acrossfade=d=${CROSSFADE_S}:c1=tri:c2=tri[loopstart];
        [0:a]atrim=start=${CROSSFADE_S}:end=${body},asetpts=PTS-STARTPTS[rest];
        [loopstart][rest]concat=n=2:v=0:a=1,
        loudnorm=I=${TARGET_LUFS}:TP=-1.5:LRA=11,aresample=${sr}[out]
    " -map "[out]" -c:a libvorbis -q:a 6 "$OUT/$id.ogg"
done

echo
echo "Results:"
for f in "$OUT"/*.ogg; do
    ffprobe -v error -show_entries format=duration,bit_rate -of csv=p=0 "$f" \
        | xargs echo "  $(basename "$f")"
done
