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
  `Tab` on a clickable node is overridden to `button`. Observed and recorded in
  CONFORMANCE.md's framework-controls section (`m3-controls.spec.ts`): Material3 `Switch` is
  emitted as a nameless `button`; Material3 `IconToggleButton` as `button`; my mutation
  setting `Role.Checkbox` on the port produced `button` (mutation log only, restored). `jb-main` guards this with
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
  "Cold" means no Compose Multiplatform, skiko or Kotlin 2.4.10 wasm toolchain in the Gradle
  cache; the machine already had a warm Gradle distribution, binaryen, a Node from the July
  spike and the Playwright Chromium. The 0.81 GB adds the conformance `node_modules` (0.135 GB)
  to the cache deltas (0.67 GB).
- The Lane C citations in README.md and `tools/bundle-size.sh` point at
  `~/coding/mp-lab/mp-lab-compose-mp/PLAN.md` (§5 item 6 for the Orca ruling, the `bundle.sh`
  bullet for the `.wasm` glob), not at the vault's lab overview, which does not carry them.

## Session 2 — 2026-09-02

### The build

Gate re-run (session-1 state, forced `--rerun`) green at 10:52; Checkbox and Switch green on
the JVM at 11:00, all four components green on wasmJs at 11:06, first browser run 11:09,
mutations seen red 11:16, final run 11:24. Four components in about 100 minutes, of which the
Compose side cost almost nothing: all 28 new UI tests passed on the first JVM run and the first
wasmJs run, including the RadioGroup single-tab-stop test (`focusProperties { canFocus }` is
read synchronously at `requestFocus` time, so writing the state and then requesting focus in
the same handler works).

What fought back was the instrument, three times, and each time the fix is in the harness:

- **The 1.12.0 mirror never removes `aria-label`.** `syncNode` sets it when
  `ContentDescription` is present and has no `removeAttribute` for it (the shipped listener's
  single `removeAttribute` is for `role`, line 417), so a focus-only marker left "(focused)" on
  every radio that had ever held focus, and `focusedName` reported the first stale one. Evidence:
  `.logs/26-radiogroup-stale-aria-label.json` (Dog, Cat and Dragon all "(focused)" at the click
  step). The marker now writes the description in both states so the attribute is overwritten.
  `jb-main` still only sets `aria-label` (line 385); its `removeAttribute` calls are for
  `aria-disabled` and `aria-modal` (lines 410, 418). This is a real bug for any app whose
  `contentDescription` comes and goes, not only for the harness.
- **The reference exposes each label twice**: `checkbox "Subscribe"` plus `text: Subscribe`
  (the `<label>` wraps the input). Compose merges the label into the widget's name, so the
  first report flagged every label as a missing text. `report.js` now counts a widget's name as
  a text the target exposes; the diff stays one-directional.
- **Playwright's `ariaSnapshot` omits contenteditable content.** Synthetic check:
  `<div role="textbox" contenteditable aria-label="Labelled">Ada</div>` renders as
  `textbox "Labelled"`, `<input aria-label="Real input" value="Cee">` as
  `textbox "Real input": Cee`. So the Compose textbox shows no value although the mirror node
  carries `Ada` as its text. Recorded here as an instrument blind spot, not as a row.

Three smaller harness fixes: `focusedName` on the reference reads `labels[0]` for inputs named by
a `<label>` (a checkbox has no text of its own); the M3 target caps `tabUntilFocused` at
two Tabs (canvas, then the first widget) since M3 routes carry no marker; and widget values
left the text diff after the final run, where the TextField row read `text "hunter2"` missing
at the tab-out step. Playwright had rendered the reference's password `<input>` value into the
snapshot while the Compose side had matched it only through the backing input while focused;
neither is what assistive technology receives, so values are recorded and not diffed. The attribution
function inherited from session 1 called every role diff a port bug; it now asks the same two
questions it asks for a state — did the mirror ever write `role="…"`, does the M3 control
expose the role — and a missing widget whose name the mirror never carried stays the port's.

### What the browser received (attribution, metric 8)

Every row is from `conformance/report.js`; this is the mechanism per missing item.

- **Role collapse, now measured on three rows.** `checkbox`, `switch` and `radio` all arrive
  as `button` on the port; the M3 `Checkbox`, `Switch` and `RadioButton` routes arrive as
  `button` too; across every recorded step the mirror DOM never contains `role="checkbox"`,
  `role="switch"` or `role="radio"`. Session 1's lines 345–348. **Framework**, fixed on
  `jb-main` (the `roleId == AriaRoleId.Unknown` guard), not shipped.
- **`radiogroup "Favorite pet"`.** `SelectableGroup` is not among the properties the listener
  reads (Text, ContentDescription, OnClick, TestTag, EditableText, Role, Heading,
  CollectionInfo); the M3 route's `selectableGroup()` produces no role either. The group's
  `contentDescription` lands as `aria-label` on a role-less `div`, which assistive technology
  ignores. **Framework.**
- **`checked` (Checkbox, Switch, RadioGroup).** `ToggleableState` and `Selected` are never
  read; same family as `pressed`. **Framework.** The Switch row reads `checked (space, enter)`:
  the reference had it at both steps, which is the empirical confirmation that Enter does not
  toggle a native checkbox input (the port consumes Enter for the same reason; see below).
- **`disabled` (Checkbox "Disabled").** As Button: version-bound, on `jb-main`. **Framework.**
- **TextField: nothing missing.** `EditableText` crosses as `contenteditable` + `role="textbox"`,
  the label's `contentDescription` as `aria-label`, so `textbox "Name"` and `textbox "Password"`
  match the reference by role and name. Typing through the canvas works: Playwright's
  `keyboard.type` reached the Compose field on the first try and the state line followed.
  Two observations the row cannot show: (1) the value blind spot above; (2) Compose's backing
  `<input class="compose-backing-field">` appears in the tree as a second, nameless `textbox`
  holding the typed text, and at the password step it holds `hunter2` in plain text while the
  mirror node shows bullets. CMP-10652 is about the mirror's `innerText`; in this run the mirror
  masked, and the plain text sat in the backing input instead.
- **M3 TextField** is a nameless `textbox`: the `label` slot is visual only. Tab did not leave
  the M3 field (the two typed strings concatenated). Recorded, not investigated.
- **Focus.** Two Tabs to the first widget on Compose, one on the reference, as before. In the
  RadioGroup the arrow keys move focus and selection identically on both (Dog → Cat, then
  Dragon by click). After the last step the reference's Tab leaves to the body; on Compose
  the same Tab wrapped inside the canvas: to Dragon, the only focusable radio, in the
  RadioGroup demo, and from the Password field back to Name in the TextField demo. Measured on
  pages with nothing after the canvas; what it does to a page with content after the canvas
  is the obvious next question, not a result.

### Mutation checks (seen red before any row counted)

1. Semantics: behaviour mutations, one per component (`onToggle = {}` in Checkbox and Switch,
   the two `onSelect`/`onChange` calls removed in RadioGroup, `onValueChange = {}` in
   TextField). JVM: Checkbox 3/8 failed, Switch 2/5, RadioGroup 5/9, TextField 1/6
   (`.logs/27-mutation-jvm-and-dist.log`).
2. Browser, same mutations: Checkbox `text "Selected"` (space, enter); Switch `text "On"`
   (space, enter); RadioGroup `text "Selected: dog"` (space), `"Selected: cat"` (down, enter),
   `"Selected: dragon"` (click, tab-out); TextField `text "Ada"`, `text "Value: Ada"`,
   `text "hunter2"`. All attributed `port (behaviour/name)`
   (`.logs/28-mutation-conformance.md`). Under the final rule (widget values not diffed) the
   TextField red is `text "Value: Ada"` alone, which was in that list. No name mutation this
   session; behaviour is the axis the browser instrument can see in 1.12.0.

Restored and re-run clean (JVM, wasmJs, browser) before CONFORMANCE.md was generated.

### Substitutions and cuts

- **Enter.** Compose's `toggleable` and `selectable` activate on Enter and Space alike; react-aria's
  `usePress` lets only Space through for checkbox and radio inputs (`isValidInputKey`). The
  port consumes Enter and NumPadEnter in `onPreviewKeyEvent` before the primitive
  (`Modifier.spaceOnlyActivation()`), and the reference confirmed the rule on the instrument.
- **No `aria-readonly`, no `aria-required`.** Compose has no semantics property for either on a
  toggleable; `readOnly` is behaviour only (focusable, inert) and `required` is not in the API.
- **RadioGroup tab stop.** Compose has no focusable-but-not-tabbable state (`canFocus = false`
  refuses programmatic focus too), so the port makes the last focused radio focusable, falling
  back to the selected one and then the first enabled one. Deviation from react-aria: after a
  programmatic value change the last focused radio, not the selected one, is the tab stop.
  In the KDoc.
- **RadioGroup label** is rendered by the component (there is no `labelledby` to point at a
  caller's text). TextField likewise renders its label.
- `stately.RadioGroupState` is the react-stately analogue with three tests; the `aria`
  composables take controlled props like ToggleButton, and the demo holds plain state, so
  nothing uses it yet.
- The M3 Switch entry left the framework-controls section for the Switch row's M3 column,
  per the rule the generated report states. M3 Button and IconToggleButton stay there as the
  record of the two-Tab control script.
- The reference Vite build for the bundle metric now carries six routes.
- Manual Orca pass: **still not done.** Same reason as session 1.

### Metrics (session 2)

- Truth run: 34 Compose UI tests on wasmJs in Chrome, plus the distribution, 44 s warm
  (`.logs/29-final-gradle.log`). Browser instrument: 20 targets, 2.5 min.
- Bundle (`.logs/metric5-bundle-s2.txt`): 4.49 MB gzipped, of which the app `.wasm` 1.06 MB
  (was 0.70), skiko 3.33 MB (unchanged), JS 0.10 MB. The +0.36 MB is Material3's Checkbox,
  RadioButton and TextField for the control routes plus the four ports; not separated.
- LOC (`.logs/metric7-loc-s2.txt`): main Kotlin 918, commonMain 895 (97 % shared); `aria`
  530 main and 530 test; conformance 880 lines of TS/JS.

## Would I pick this for real work

Not yet an opinion with enough behind it. Two components in, the developer experience is
better than the plan feared (15-second browser test loop, 7-second edit-to-visible), and the
accessibility result is exactly the vault note's ceiling plus one version-bound regression
(`disabled`) and one shipped bug (role collapse) the note did not have. The question the
ladder exists to answer starts at Checkbox.

Six components in (session 2): the Compose side keeps being the easy part. Four components,
28 UI tests, first-try green on both targets, roving focus included; every hour of this
session went into the instrument, not into Compose. The accessibility answer for stateful
widgets in 1.12.0 is total rather than partial: no role and no state crosses for Checkbox,
Switch or RadioGroup, so to a screen reader they are three unlabelled-state buttons; the M3
controls are the same three buttons. The text field is the one widget that crosses well
(name, editability, typing), with its value in a place the instrument cannot see and a
plain-text copy in a place it can. `jb-main` fixes the roles and `disabled`; nothing on it
reads `ToggleableState` or `Selected`, so the next release should flip the role rows and leave
the state rows where they are. That is the number to watch, and the Orca pass is still owed.
