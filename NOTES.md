# What fought back

**Written the same day the work was done. Not reconstructed later.**

This file is the quarantine for judgment. Scorecard metrics are scripted precisely so that
everything subjective ends up here, where it can be read as opinion rather than mistaken for
measurement.

## Session 1 — 2026-09-02

### The build

Clock started 07:55, first Compose render seen by Playwright 08:07, session-1 gate met 08:22.
The plan budgeted a session for step 3 (Karma + Chrome) and it took fifteen seconds: KGP's
`useChromeHeadlessNoSandbox()` plus `CHROME_BIN` pointed at the Playwright-cached Chrome for
Testing ran the six Compose UI tests in the browser on the first invocation. Nothing about
the immutable host mattered.

What did cost time, in order:

- A JVM platform-declaration clash: `ToggleState.setSelected()` next to an `isSelected`
  property, then the private backing property generating the same `setSelected`. Two rounds
  of a one-line rename. Not a wasm problem.
- `Icons.Filled.*` needs `material-icons-core`, which the material3 artifact does not pull.
  Replaced with a text glyph for the M3 toggle control.
- My mirror-DOM walker did not descend into a shadow root attached to an element with no
  light-DOM children, so the first attribution column was computed on an empty string. The
  regex for "mirror carries `aria-disabled`" then matched the id text `btn-disabled`. Both
  mine, both caught because the report looked too good.
- `pkill -f <pattern>` killed the shell that contained the pattern in its own command line.
  Twice. Kill by PID or port.
- The conceptual one: the plan's mutation check ("remove `toggleableState`, the diff must
  report `pressed` missing") cannot go red on the browser instrument, because `pressed` is
  missing with or without it. That is the ceiling itself. Two mutations replaced it, below.

The `v2` test API (`androidx.compose.ui.test.v2.runComposeUiTest`) exists in 1.12.0 as the
plan said, and the `v1` import compiles with a deprecation warning pointing at it.

### Resolved versions and downloads

Kotlin 2.4.10, compose-compiler plugin 2.4.10, Compose Multiplatform 1.12.0, material3
1.12.0-alpha03, kotlinx-browser 0.5.0 (declared; the same version Compose pulls), skiko
as resolved by Compose 1.12.0 (see Gradle cache; not re-pinned here), compose ui-test 1.12.0.
Gradle 8.14.5, JDK Temurin 21.0.11. KGP 2.4.10 downloaded Node v25.0.0 for the wasm toolchain
(the cached node-v24.10.0 from the July spike was not reused; +209 MB). Karma ran `ChromeHeadless147.0.0.0`,
which is Chrome for Testing 147.0.7727.15 from `~/.cache/ms-playwright/chromium-1217`.
Conformance: `@playwright/test` 1.59.1 whose nested `playwright-core` 1.59.1 maps to Chromium
revision 1217, so the cached browser was reused with no download; the top-level
`playwright-core` 1.62.1 in `node_modules` belongs to `@axe-core/playwright` and is unused.
`react-aria-components` 1.21.0, React 19.2.8, Vite 7.3.6.

Downloads inside the setup metric: Gradle caches +462 MB (Compose, skiko, Kotlin 2.4.10
toolchain), `~/.gradle/nodejs` +209 MB, conformance `node_modules` 135 MB. Total 0.81 GB.

### What the browser received (attribution, metric 8)

Every row in CONFORMANCE.md was produced by `conformance/report.js`; this section is the
mechanism behind each missing item, with the source line.

- **`pressed` (ToggleButton).** The port sets `toggleableState` through
  `Modifier.toggleable`; the semantics instrument asserts it (`assertIsOn` passes on wasmJs
  in Chrome). The 1.12.0 `ComposeWebSemanticsListener.kt` never reads `ToggleableState` (0
  hits in the shipped sources jar) and writes no `aria-pressed`. Material3's IconToggleButton
  loses it the same way. **Framework ceiling**, unchanged on `jb-main` 2026-09-02.
- **`disabled` (Button).** The port sets `Disabled` (`assertIsNotEnabled` passes on wasmJs).
  The 1.12.0 listener's attribute writes are `aria-label`, `contenteditable`, `role`, `id`
  and bounds; there is no `aria-disabled` (0 hits in the shipped sources jar). `jb-main` has
  `htmlNode.setAttribute("aria-disabled", "true")` at lines 407–410 (PR #3308, merged
  2026-08-14, after the 1.12.0 cut). Material3's `Button(enabled = false)` on the M3 control
  route also lacks it. **Framework ceiling, version-bound**: expected to close in the next
  release. The vault note `kmp-web-accessibility` lists `aria-disabled` as written; that is
  true of `jb-main`, not of the shipped 1.12.0.
- **Role collapse (not yet a row; it will be every Tier-1 stateful row).** 1.12.0
  `getRoleId()` lines 345–348: `if (this.contains(SemanticsActions.OnClick)) roleId =
  Role.Button` with no guard, so any explicit `Role.Checkbox` / `Switch` / `RadioButton` /
  `Tab` on a clickable node is overridden to `button`. Observed: Material3 `Switch` is
  emitted as a nameless `button`; Material3 `IconToggleButton` as `button`; my mutation
  setting `Role.Checkbox` on the port produced `button`. `jb-main` guards this with
  `&& roleId == AriaRoleId.Unknown`. This answers the vault note's open question on
  CMP-8619: in 1.12.0 the Switch does have a role, and it is the wrong one.
- **Focus.** `document.activeElement` is the canvas throughout (CMP-10679), as expected. The
  test-only marker (`contentDescription` → `aria-label="Bold (focused)"`) works. The first
  Tab lands on the canvas (`tabindex="0"`), the second on the first widget; the reference
  needs one. No `moveFocusOnTab` workaround was needed for this page.
- **Observed:** the mirror root `#cmp_a11y_root` carries `aria-live="polite"` in 1.12.0,
  set in `ComposeWindowInternal.web.kt` line 279 of the shipped sources jar, not in the
  accessibility listener (which is why the vault note's grep of the two accessibility files
  finds no `aria-live`). Consequence: every text change inside the canvas may be announced
  as a polite live update. Worth a runtime check with Orca before Toast is ported.
- **Instrument blind spot, recorded.** In 1.12.0 the browser instrument cannot see
  `toggleableState` or an explicit role, so a port that sets neither looks identical to one
  that sets both. The semantics instrument is the only guard for those two, which is the
  reason it exists.

### Mutation checks (seen red before any row counted)

1. Semantics: `ariaToggleable` swapped for `clickable` (no toggle state, role Checkbox).
   `:aria:wasmJsBrowserTest`: ToggleButtonTest 0/3, ButtonTest 3/3. Browser diff: unchanged,
   for the reasons above.
2. Behaviour: `onValueChange = { }` (never toggles). Browser diff: `text "Selected"` missing
   at the space and click steps, attributed `port`. Compose UI tests: also red.

Restored and re-run clean before CONFORMANCE.md was generated.

### Substitutions and cuts

- `stately` wasmJs tests run under Node (`nodejs()`), not Karma; the browser run is `:aria`'s.
- `stately` depends on `compose-runtime` for `mutableStateOf` (no compiler plugin). The plan
  said "no compose"; snapshot state is the honest analogue of react-stately's React dependency.
- The M3 control for ToggleButton is `IconToggleButton`; Material3 has no plain toggle button
  in 1.12.0-alpha03 that I checked for. In 1.12.0 the distinction is moot (see role collapse).
- Manual Orca pass: **not done this session.** It needs a desktop session with the screen
  reader running; recorded as pending, not as a pass.
- Metric 1 counts to the first Compose render seen in a browser (08:07), not to the gate.

## Would I pick this for real work

Not yet an opinion with enough behind it. Two components in, the developer experience is
better than the plan feared (15-second browser test loop, 7-second edit-to-visible), and the
accessibility result is exactly the vault note's ceiling plus one version-bound regression
(`disabled`) and one shipped bug (role collapse) the note did not have. The question the
ladder exists to answer starts at Checkbox.
