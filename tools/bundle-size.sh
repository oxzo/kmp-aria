#!/usr/bin/env bash
# Metric 5: demo distribution size, raw and gzip -9, per file and total. Globs .wasm AND .js
# (Lane C's warning in ~/coding/mp-lab/mp-lab-compose-mp/PLAN.md: a .js-only glob misses the payload). Source maps excluded.
# The reference app's Vite build is printed beside it, labelled apples-to-oranges: it is a
# React bundle with two components, not a canvas renderer.
set -euo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
dist="$here/components/build/dist/wasmJs/productionExecutable"
[ -d "$dist" ] || { echo "no distribution at $dist — run: gradle :components:wasmJsBrowserDistribution" >&2; exit 1; }

report() {
  local dir="$1" label="$2" total_raw=0 total_gz=0
  echo "## $label ($dir)"
  printf '%-40s %12s %12s\n' file raw gzip
  while IFS= read -r f; do
    raw=$(stat -c %s "$f"); gz=$(gzip -9 -c "$f" | wc -c)
    printf '%-40s %12d %12d\n' "$(basename "$f")" "$raw" "$gz"
    total_raw=$((total_raw + raw)); total_gz=$((total_gz + gz))
  done < <(find "$dir" -maxdepth 1 -type f \( -name '*.wasm' -o -name '*.js' \) ! -name '*.map' | sort)
  printf '%-40s %12d %12d\n' TOTAL "$total_raw" "$total_gz"
  echo
}

report "$dist" "Compose demo distribution (.wasm + .js, no maps)"

refdist="$here/conformance/reference/dist"
if [ -d "$refdist/assets" ]; then
  report "$refdist/assets" "React Aria reference (Vite build) — apples-to-oranges"
else
  echo "(reference build absent; run: cd conformance && npx vite build reference)"
fi
