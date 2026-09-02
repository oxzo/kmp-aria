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
| 1 | Setup: minutes from empty dir to green wasmJs demo | 90 | 12 (07:55 → 08:07, first Compose render seen by Playwright) | −78 | yes |
| 1b | Setup: GB downloaded (cold Compose Multiplatform + skiko + wasm toolchain) | 1.2 | 0.81 (Gradle caches +0.46, a second Node for wasm +0.21, conformance npm +0.14) | −0.4 | no |
| 2a | wasmJs compiles first try | yes | yes (the only compile error was a JVM signature clash) | — | no |
| 2b | Compose UI tests run on wasmJs (Karma + Chrome) first try | no | yes, 6/6 in ChromeHeadless 147 via CHROME_BIN, first run | wrong | yes |
| 3 | Tier-1 components with behaviour pass in session 1 | 2 (Button, ToggleButton) | 2, both 3/3 on wasmJs | 0 | no |
| 4 | Tier-1 components with full a11y-tree parity, whole tier | 1 (Button only; Separator/Group/Toolbar/Form have no role in the web role map) | 0 of the 2 measured so far: Button loses `disabled` in 1.12.0 (tier not finished) | already falsified for Button | yes |
| 4a | ToggleButton row: `pressed` missing on Compose, present on Material3 control too | yes, both | yes: missing on the port and on M3 IconToggleButton (which is also emitted as `button`) | 0 | no |
| 5 | Demo bundle, gzipped (.wasm + JS glue + skiko) | 2.4 MB | 4.13 MB (skiko 3.33 + app 0.70 + JS 0.10); reference React app 74 KB, apples-to-oranges | +1.7 MB | yes |
| 6 | Inner loop: seconds from text edit to visible in browser (`-t` dev server) | 20 | 7.1 | −13 | yes |
| 7 | Shared fraction (commonMain / all Kotlin) at session-1 end | 85% | 93% (302 of 325 main lines; wasmJsMain is the 23-line entry point) | +8 | no |

## What I expect to fight

1. Karma launching the Chrome-for-Testing binary on an immutable host via `CHROME_BIN`; fallback
   is a custom launcher config, second fallback the headless shell.
2. Tab traversal into and inside the canvas (CMP-4714 open, CMP-9388 fixed but ship version
   unknown); focus is unobservable from the DOM (CMP-10679), so the test-only
   `contentDescription` marker is needed from the start.
3. `ariaSnapshot()` on `#composeApp` descending into the open shadow root, and the 100 ms /
   1 s mirror debounce making snapshots flaky without an explicit wait.

## What actually fought (resolved 2026-09-02, same day)

None of the three predicted fights happened: Karma launched Chrome for Testing on the first
run, Tab reached the canvas on the first press and the first widget on the second, and the
aria snapshot descended into the shadow root with a 1.2 s wait. What fought instead is in
NOTES.md: two trivial compile errors, two harness bugs of my own, and one conceptual fight
that the plan did not foresee, the 1.12.0 role override that makes role mutations invisible
to the browser instrument.

## What would change my mind about the canvas path

If a grep of `ComposeWebSemanticsListener.kt` on `jb-main` at session time finds
`aria-checked` or `aria-pressed`, the ceiling moved and every a11y column must be re-pinned to
the CMP version that ships it.

Checked 2026-09-02 on `jb-main`: `aria-checked` 0, `aria-pressed` 0, `aria-valuenow` 0,
`aria-live` 0. The ceiling has not moved. `aria-disabled` is written on `jb-main` (2 hits) and
not in 1.12.0 (0 hits in the shipped sources jar), so the `disabled` column is version-bound.
