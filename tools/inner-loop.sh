#!/usr/bin/env bash
# Metric 6: seconds from a source edit to the change visible in the browser.
# Requires the continuous dev server already running:
#   gradle :components:wasmJsBrowserDevelopmentRun -t     (serves http://localhost:8080)
# Edits the demo index title with a stamp, then polls the page through Playwright (headless
# Chromium from conformance/node_modules) until the stamp shows in the accessibility tree.
# Restores the source afterwards.
set -euo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
src="$here/components/src/commonMain/kotlin/dev/oxzo/aria/demo/App.kt"
url="${1:-http://localhost:8080/}"
stamp="stamp-$(date +%s)"
grep -q '"kmp-aria demo"' "$src" || { echo "expected the literal \"kmp-aria demo\" in $src" >&2; exit 1; }
curl -fsS "$url" > /dev/null || { echo "dev server not answering at $url" >&2; exit 1; }
cleanup() { sed -i "s/\"kmp-aria demo $stamp\"/\"kmp-aria demo\"/" "$src"; }
trap cleanup EXIT
start=$(date +%s.%N)
sed -i "s/\"kmp-aria demo\"/\"kmp-aria demo $stamp\"/" "$src"
cd "$here/conformance"
node --input-type=module - "$url" "$stamp" "$start" <<'JS'
import { chromium } from '@playwright/test'
const [url, stamp, start] = process.argv.slice(2)
const browser = await chromium.launch()
const page = await browser.newPage()
const deadline = Date.now() + 10 * 60 * 1000
let seen = false
while (Date.now() < deadline) {
  try {
    await page.goto(url, { waitUntil: 'load' })
    await page.waitForTimeout(1500)
    const snap = await page.locator('#composeApp').ariaSnapshot()
    if (snap.includes(stamp)) { seen = true; break }
  } catch {}
  await page.waitForTimeout(1000)
}
await browser.close()
const secs = (Date.now() / 1000 - Number(start)).toFixed(1)
console.log(seen ? `inner loop: ${secs} s from edit to visible` : `stamp not seen within 10 min`)
process.exit(seen ? 0 : 1)
JS
