#!/usr/bin/env bash
# Metric 7: non-blank lines of Kotlin per source set, and the shared fraction
# (commonMain / all main Kotlin). Tests and the conformance harness listed separately.
set -euo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
count() { find "$@" -name '*.kt' 2>/dev/null -print0 | xargs -0 cat 2>/dev/null | grep -c -v '^[[:space:]]*$' || true; }
total_main=0; common=0
printf '%-14s %-12s %8s\n' module sourceSet loc
for m in stately aria components; do
  for ss in commonMain jvmMain wasmJsMain commonTest jvmTest wasmJsTest; do
    d="$here/$m/src/$ss"; [ -d "$d" ] || continue
    n=$(count "$d")
    printf '%-14s %-12s %8d\n' "$m" "$ss" "$n"
    case "$ss" in
      commonMain) common=$((common + n)); total_main=$((total_main + n));;
      jvmMain|wasmJsMain) total_main=$((total_main + n));;
    esac
  done
done
ts=$(find "$here/conformance/lib" "$here/conformance/components" "$here/conformance/reference/src" "$here/conformance/report.js" "$here/conformance/playwright.config.ts" \( -name '*.ts' -o -name '*.tsx' -o -name '*.js' \) -print0 2>/dev/null | xargs -0 cat | grep -c -v '^[[:space:]]*$' || true)
printf '%-14s %-12s %8d\n' conformance ts+js "$ts"
echo
echo "main Kotlin: $total_main · commonMain: $common · shared fraction: $(awk -v c="$common" -v t="$total_main" 'BEGIN{ if (t==0) print "n/a"; else printf "%.0f%%", 100*c/t }')"
