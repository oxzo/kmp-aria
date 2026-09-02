# Pre-registration — kmp-aria

**Committed BEFORE the first source commit.** Comparing a prediction to a result is the only
way a scorecard number becomes calibration data instead of a thing that felt obvious in
hindsight.

**Provenance: AI-drafted (Claude, 2026-09-02), quarantined.** These are not oxzo's numbers.
They must never be recorded to `journal/calibration-log.md`. They exist so the session has a
pre-registered expectation to compare against; the comparison is informative, the confidence
value is not oxzo's calibration.

Recorded by: Claude (AI-drafted) · Date: 2026-09-02 · Confidence in these as a set: 40%

| # | Metric | Predicted | Actual | Delta | Surprised? |
|---|---|---|---|---|---|
| 1 | Setup: minutes from empty dir to green wasmJs demo | 90 | | | |
| 1b | Setup: GB downloaded (cold Compose Multiplatform + skiko + wasm toolchain) | 1.2 | | | |
| 2a | wasmJs compiles first try | yes | | | |
| 2b | Compose UI tests run on wasmJs (Karma + Chrome) first try | no | | | |
| 3 | Tier-1 components with behaviour pass in session 1 | 2 (Button, ToggleButton) | | | |
| 4 | Tier-1 components with full a11y-tree parity, whole tier | 1 (Button only; Separator/Group/Toolbar/Form have no role in the web role map) | | | |
| 4a | ToggleButton row: `pressed` missing on Compose, present on Material3 control too | yes, both | | | |
| 5 | Demo bundle, gzipped (.wasm + JS glue + skiko) | 2.4 MB | | | |
| 6 | Inner loop: seconds from text edit to visible in browser (`-t` dev server) | 20 | | | |
| 7 | Shared fraction (commonMain / all Kotlin) at session-1 end | 85% | | | |

## What I expect to fight

1. Karma launching the Chrome-for-Testing binary on an immutable host via `CHROME_BIN`; fallback
   is a custom launcher config, second fallback the headless shell.
2. Tab traversal into and inside the canvas (CMP-4714 open, CMP-9388 fixed but ship version
   unknown); focus is unobservable from the DOM (CMP-10679), so the test-only
   `contentDescription` marker is needed from the start.
3. `ariaSnapshot()` on `#composeApp` descending into the open shadow root, and the 100 ms /
   1 s mirror debounce making snapshots flaky without an explicit wait.

## What would change my mind about the canvas path

If a grep of `ComposeWebSemanticsListener.kt` on `jb-main` at session time finds
`aria-checked` or `aria-pressed`, the ceiling moved and every a11y column must be re-pinned to
the CMP version that ships it.
