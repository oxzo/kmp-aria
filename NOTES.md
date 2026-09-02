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

## Session 3 — 2026-09-02

### The build

The gate re-run on the session-2 state at 11:46 executed nothing: `gradle A B C D --rerun`
applies `--rerun` to D only (a Gradle task option binds to the task named before it), and D was
the distribution. The three test tasks came back UP-TO-DATE. With `--rerun` after each test task
(11:48) the 34 tests executed; the Playwright gate (20 targets, 11:49) produced six rows
byte-identical to the committed file apart from the recorded timestamps. Link, ProgressBar and
Disclosure: 19 UI tests green on the first JVM run (11:54) and the first wasmJs run (11:56),
first browser run 11:57, mutations red on JVM, wasmJs and browser at 12:02, final run after.
Three components in about twenty minutes of wall clock from the honest gate to the commit
(11:48 to 12:07), none of it on the Compose side: no test needed a second attempt.

What fought back was the report, three times, and the harness once more after the rows:

- **Playwright quotes a string that contains a colon** (`paragraph: "Value: 30"`). The parser
  kept the quote characters, so the demo's state line came out as `text ""Value: 30""` missing on
  ProgressBar although the Compose side carried it. `parseSnapshot` now unquotes.
- **The name-only fallback matched the wrong widget.** The fallback exists for the role
  collapse (`checkbox "Subscribe"` → `button "Subscribe"`); on Disclosure it matched the
  reference's `group "System Requirements"` (labelled by its trigger) to the Compose
  `heading "System Requirements"`, which had already matched exactly, and reported a role
  change where the group is simply absent. Matching now claims exact role+name pairs first and
  never reuses a target widget. The six earlier rows did not change.
- **Playwright folds adjacent mirror text nodes into one line** (`"Syncing Value: 30"`,
  `Details about system requirements here. Expanded`). The `includes` text rule absorbs it; noted
  so the per-step snapshots read right.

One addition: a link's destination arrives in the snapshot as a property line (`- /url: …`)
under the widget. It is parsed as an `href` attribute of that widget and attributed with its own
wording, since "mirror writes no aria-href" would have been nonsense. It is the only reason
`href` appears in the Link row at all: the instrument was extended to produce it.

And one harness bug found after the rows were recorded, by the bias scan of the vault note:
**the mirror-DOM serializer dropped text nodes beside element children.** It recursed over
`children` when an element had any, so a node whose `innerText` held a newline (rendered as
text, `<br>`, text) was captured as `<br></br>` with no text; the ProgressBar `pb` node shows
it. Fixed to walk `childNodes`; ProgressBar re-recorded; no row changed, since attribution
keys on attributes and `aria-label`, but the mirror-name check (`>name<`) would have missed a
name carried as text on such a node.

### What the browser received (attribution, metric 8)

Every row is from `conformance/report.js`; this is the mechanism per missing item.

- **Link: `link` → `button`, three times; `href`; `disabled`.** The mirror never writes
  `role="link"`: 1.12.0's `AriaRoleId` has no link id at all (it ends at `Grid = 10`), so a
  clickable node is a `button` whatever it is. `jb-main` adds `Link = 12`, but only for a node
  carrying `LinkTestMarker`, which `TextLinkScope` sets on the child box of a `LinkAnnotation`
  inside text (`foundation/text/TextLinkScope.kt`, the `LinksComposables` box), never on a
  standalone clickable. So the release that ships today's `jb-main` does not flip this row; it
  would need the port to become a text annotation. `href`: the reference exposes the URL on "Docs"; mirror
  nodes are `div`s. `disabled` as Button, version-bound. **Framework**, all five.
  The framework's own link path (`#/fw-link`, `LinkAnnotation.Clickable` in `BasicText`, the
  control column since Material3 has no link): 1.12.0 emits the link's child box as a
  **nameless `button`** beside the text node (`text: Follow me`, then `button` with no name:
  the child has `OnClick` and the marker but no text of its own, and 1.12.0 has none of
  `jb-main`'s interleaving), and it activated on Enter, Space and click (1, 2, 3), so the
  framework link also breaks the Enter-only contract. The port consumed Space (1, 1, 2, the
  reference's sequence). Focus reached "Docs" on the next Tab on both targets; the disabled
  link was not tabbed to on either (the script stops at Docs).
- **ProgressBar: `progressbar "Loading"` and `"Syncing"` never arrive.** `ProgressBarRangeInfo`
  is not among the properties the listener reads, in 1.12.0 or on `jb-main` (0 hits in both),
  so no role and no `aria-valuenow` / `valuemin` / `valuemax` / `valuetext`; the mirror DOM
  contains no `aria-value*` at any step. The label lands as `aria-label` on a role-less `div`
  whose text is "Loading\n30%" (the merged descendants of `progressSemantics`); Material3's
  `LinearProgressIndicator` is an empty `div` with no label (it takes none). **Framework**, and
  not version-bound. **The browser instrument is blind to this port.** The reference's value
  text sits inside the widget, which Playwright renders as the widget's trailing content
  (`progressbar "Loading": Loading 30%`, excluded by the values rule), and the port produces no
  widget, so nothing the port does reaches the diff: the mutation that froze the value
  (semantics red 4/6 on JVM and wasmJs; the Compose snapshot read `Loading 0%` at every step)
  left the row byte-identical. This is session 1's `pressed` case in full: the semantics
  instrument is the only guard, and the row counts on that basis with the blindness stated.
  A second layer: Playwright's `ariaSnapshot` renders no `aria-valuenow` for any role (the
  injected script reads it only to compute a value string), so a mirror that did write it would
  show up in the mirror-DOM check, not in the snapshot.
- **Disclosure: `level`, `expanded`, `group`.** The heading crosses (`Heading` is read off a
  property, as `EditableText` was for the textbox row; the first structural role to arrive)
  and so does the button; `level` is
  missing because Compose semantics has no heading level at all, a vocabulary gap ahead of the
  mirror; `expanded` is missing because the `Expand` / `Collapse` actions are not read (no
  `aria-expanded` in 1.12.0 or on `jb-main`); the panel's `group "System Requirements"` is
  missing because Compose has no group role. The table says `unattributed` for the group,
  correctly: there is no Material3 disclosure to witness it, and the report attributes only by
  evidence it holds. The source grep settles it as framework here, not in the table.
- **Focus.** Two Tabs on Compose, one on the reference, as before, on all three.
- **CMP-10652, read this session** (the handoff's open item). The issue says what session 2
  assumed: a `password()`-marked field's live text written into the mirror node's `innerText`.
  The 1.12.0 `syncNode` writes `EditableText` into `innerText` with no `Password` check
  (lines 262–264 of the shipped listener). The bullets session 2 saw are explained by that:
  `EditableText` carries the visually transformed string, so `PasswordVisualTransformation`
  masked the mirror node by accident of the transformation while the backing `<input>` held the
  plain text. A field marked `password()` without a transformation leaks as the issue says;
  that case was not exercised here. Neither NOTES nor the vault note needed correcting.

### Mutation checks (seen red before any row counted)

1. Semantics, one build with all three applied, JVM and wasmJs both
   (`.logs/37-mutation-build.log`): Link with `enterOnlyActivation` removed, 1/6 failed
   (`spaceDoesNotActivate`); ProgressBar with the value ignored (`clamped = minValue`), 4/6;
   Disclosure with the press handler emptied and `heading()` removed, 4/7.
2. Browser, same build (`.logs/38-mutation-conformance.md`): Link `text "Followed 1 times"`
   (space), `text "Followed 2 times"` (click, tab-next); Disclosure `heading "System
   Requirements"` (every step), `text "Details about system requirements here."` and
   `text "Expanded"` (enter, click). The heading is the first *role* mutation seen red on the
   browser instrument, because `Heading` is read off a property rather than off `Role`.
   ProgressBar: unchanged, for the reason above.

Restored and re-run clean (JVM, wasmJs, browser) before CONFORMANCE.md was generated.

### Substitutions and cuts

- **Link** carries no `Role` (Compose has none for a link) rather than a wrong one; `href`
  opens through `LocalUriHandler` after `onPress`; Space is consumed by
  `Modifier.enterOnlyActivation()`, the mirror of session 2's `spaceOnlyActivation`. The control
  column is foundation's `LinkAnnotation`, not a Material3 widget; `components.json` says so in
  a `controlNote` the report prints in the details line. Not measured: `aria-current`, router
  integration, the disabled link's tab skip.
- **ProgressBar** renders its own label and value text (as RadioGroup and TextField do); the
  value text is a rounded integer percent, not `Intl.NumberFormat` in the page locale;
  `valueLabel` overrides it. The content slot draws the track from the percentage.
- **Disclosure**'s panel is not composed while collapsed, where the reference keeps it in the DOM
  as `hidden="until-found"` for find-in-page; no group role and no `aria-controls` (Compose
  semantics has no id association). Material3 has no disclosure, so the row has no control
  route. DisclosureGroup is not ported.
- **Gate:** `--rerun` after every test task, or the gate is a no-op that prints green.
- Manual Orca pass: **still not done.** It now has a heading and three links to listen to.

### Metrics (session 3)

- Truth run: 53 UI tests on wasmJs in Chrome plus the distribution, 28 s warm
  (`.logs/39-final-gradle.log`). Browser instrument: 28 targets, 3.4 min
  (`.logs/40-final-playwright.log`); the nine rows came out identical to the first recording
  of the day apart from timestamps.
- Bundle (`.logs/metric5-bundle-s3.txt`): 4.51 MB gzipped, of which the app `.wasm` 1.08 MB
  (was 1.06), skiko 3.33 MB (unchanged), JS 0.10 MB. The +0.02 MB is three ports,
  `LinearProgressIndicator` and the annotation-link route.
- LOC (`.logs/metric7-loc-s3.txt`): main Kotlin 1211, commonMain 1188 (98 % shared); `aria`
  714 main and 804 test; conformance 1129 lines of TS/JS.

## Session 4 — 2026-09-02

### The build

The gate with `--rerun` after every test task executed the 53 UI tests and the six stately tests
(`.logs/42-gate-s4.log`, BUILD SUCCESSFUL in 30 s ending 15:38:41; the XML timestamps read that
morning have since been overwritten by later runs); the Playwright gate (28 targets, 15:41:34 → 15:45:01) reproduced the nine
committed rows apart from the recorded timestamps (zero non-timestamp diff lines). Three
components, thirty-four new UI tests and five state tests: ToggleButtonGroup (15 + 5) and
CheckboxGroup (9) went first-try green on the JVM and on wasmJs; SearchField (10) needed two
compile fixes before its first run (a lambda whose last expression was `onClear?.invoke()` had
type `() -> Unit?`; `assertExists` and `assertDoesNotExist` are members, not imports), then went
green on both. Neither the harness nor the report changed this session; no spec helper was
touched. From the gate's end (15:38:41) to the final gate (16:10:43) is thirty-two minutes of wall
clock; the two compile fixes sit between the CheckboxGroup recording's end (16:03:36) and the
passing JVM run (16:05:17), under two minutes. The commit (c06f269, 16:17:18) closes the session.

What fought back was Gradle, once more, and the build cache once:

- **`--rerun` on an aggregate does not reach its leaf.** `:stately:wasmJsTest --rerun` left
  `:stately:wasmJsNodeTest` UP-TO-DATE in the final gate (`.logs/60-final-gate-gradle.log`); the
  leaf was forced separately (`.logs/60b-stately-wasm-node-rerun.log`, XML 13:11:16Z). The honest
  gate names the leaf test tasks: `:stately:jvmTest`, `:stately:wasmJsNodeTest`, `:aria:jvmTest`,
  `:aria:wasmJsBrowserTest`, each followed by `--rerun`.
- **A clean rebuild of byte-identical sources compiles FROM-CACHE and still runs the tests.**
  After the ToggleButtonGroup mutation was reverted from the scratchpad copies, `compileKotlin*`
  came from the build cache while the four test tasks executed (`.logs/49-clean-rebuild.log`;
  XML timestamps 12:55:03Z–12:55:05Z, 29 s total). The tell that tests ran is the XML
  `timestamp` attribute, not the build duration.

### What the browser received (attribution, metric 8)

- **ToggleButtonGroup: `radiogroup`, `toolbar`, `radio→button` three times, `checked`,
  `pressed`, `disabled`.** The demo carries both selection modes (a single-selection "Text
  alignment" group, which react-aria renders as `radiogroup` with `radio` items and
  `aria-checked`, and a multiple-selection "Text style" group, rendered as `toolbar` with
  `button` items and `aria-pressed`; `useToggleButtonGroup` over `useToolbar`, confirmed in
  `react-aria/dist/private/button/useToggleButtonGroup.mjs`). Compose has no vocabulary for any
  of the three group roles: `SelectableGroup` is not read by the listener (session 2), and
  neither 1.12.0's `AriaRoleId` nor today's `jb-main` copy has an id for `radiogroup`,
  `toolbar` or `group` (`.logs/A11YImplementationUtils.kt` lines 48–65: Button … DropdownList,
  Heading, TextBox, List, Grid, Dialog, Link). The role collapse makes every item a `button`
  and the state family is dropped as before. The control column is Material3's
  `SingleChoiceSegmentedButtonRow` and `MultiChoiceSegmentedButtonRow` (`SegmentedButton.kt` in
  the material3 1.12.0-alpha03 sources: `selectableGroup()` on the single-choice row,
  `Role.RadioButton` on its items, `toggleable` on the multi-choice items), which arrive as six
  stateless `button`s; so every item is **framework**. Behaviour matched the reference at every
  step: Space selects, ArrowRight moves focus without selecting, Space on the new item replaces
  the selection, Enter deselects, click selects, one Tab leaves the first group for Bold, the
  style group accumulates, ArrowRight from Italic stays on Italic (Underline disabled, no wrap).
  The one observable difference the diff does not score is focus at `tab-out`: the reference's
  Tab left the page (`focused: none`), the port's landed on Right, the alignment group's
  remembered item, which is the Tab finding below.
  A footnote from the M3 snapshots: the segmented buttons' mirror order changes with focus and
  selection (`Center, Right, Left` after the first Tab), because `SegmentedButton` raises the
  selected or interacted item's z-index (`interactionZIndex`) and the semantics tree is z-sorted.
- **CheckboxGroup: `group "Interests"`, `checkbox→button` three times, `checked`, `disabled`.**
  `useCheckboxGroup` gives the group `role="group"` labelled by its `Label`; Compose has no group
  role (above) and the label lands as `aria-label` on a role-less `div`, exactly as the
  RadioGroup's did. The control is Material3's `Checkbox`es in a `Column` (there is no Material3
  group widget; `components.json` says so in `controlNote`), which is what makes the group role
  attributable as **framework** rather than `unattributed`, the gap the session-3 Disclosure row
  left. The M3 checkboxes are nameless and toggled on Enter at the `enter` step
  (`Selected: sports` after `sports, music`), the session-2 finding again; the port kept its
  state on Enter, as the reference did. Values keep press order on both.
- **SearchField: `searchbox→textbox "Search"`, nothing else.** `useSearchField` renders
  `<input type="search">`; Compose has no search role, and Material3's own `SearchBar` input
  arrives as `textbox "Search"` too (its `InputField` sets a `contentDescription` of its own,
  `SearchBar.kt` line 2170), so **framework**. Everything else crossed: the field named by its
  label, the clear button as `button "Clear search"` (its `contentDescription`; the reference's
  `aria-label` from `clearButtonProps`), present only while the value is non-empty on both
  sides; Escape cleared, Enter submitted without clearing, the pointer click on the clear
  button cleared and left the value line empty while the submitted line kept "compose". As on
  the TextField row, Compose's backing `<input>` shows as a second nameless `textbox` holding
  the typed text. The M3 `SearchBar` control differs in behaviour by design: Escape does not
  clear (the value stayed "kotlin" and the second typing produced "kotlincompose"), the IME
  Search action submits, and its clear `IconButton` is a nameless `button "✕"` nested inside
  the textbox node.
- **Focus.** Two Tabs on Compose, one on the reference, on all three rows.

### Tab cannot leave the canvas (the session-2 question, answered on 1.12.0)

Measured with a plain `<button id="after-canvas">` appended to the end of `<body>` at runtime
(`.logs/51-tabwrap.log`; re-run with the sibling logged in `.logs/51b-tabwrap-sibling-check.log`,
script `.logs/tabwrap.mjs`; `index.html` untouched). The re-run records the button as present
with `tabIndex=0`, a programmatic `focus()` on it makes it `document.activeElement`, and one
Shift+Tab from it puts focus back into the canvas; so the sibling is reachable and focusable,
and the only direction that fails is out. On
`#/toggle-button-group` the first Tab focused the container `div` with no widget marked, the
second Left, then Bold, Left, Bold, Left; three Shift+Tabs went Bold, Left, Bold.
`document.activeElement` was the container `div` on every press and never the injected button.
On `#/button` (one tab stop) every Tab and Shift+Tab kept "Press me". Mechanism in the 1.12.0
sources: `ComposeWindowInternal.web.kt` `processKeyboardEvent` calls `preventDefault()` on any
key the scene reports as consumed (line 385); that the scene consumes Tab is inferred from the
observed wrap, not from a cited line; the backing text input prevents Tab's default explicitly
(`DomInputStrategy.kt` lines 82–91, "Compose logic will handle the focus movement"). Once
keyboard focus is inside the canvas, Tab and Shift+Tab do not take it out. Measured in
Chromium through Playwright key presses on two demo pages. The hedged "Tab wraps" bullet this
answers is the vault project note's own session-2 bullet, not anything in the ceiling note
(`kmp-web-accessibility` has no such bullet; its WCAG 2.1.1 Keyboard row, "at risk", gains a
measured instance). Whether the tracker's resolved tab-focus issues (CMP-9388, CMP-10554, listed
in the ceiling note) describe this case was not read this session. A footnote from the re-run:
while the DOM button held focus, the Compose focus marker still read "Left (focused)", so the
scene's focus state did not clear when DOM focus left the canvas programmatically.

### Two unverified specifics from session 3, now checked

- **Material3 1.12.0-alpha03 has no Link and no Disclosure widget.** The
  `material3-wasm-js-1.12.0-alpha03-sources.jar` was fetched from Maven Central into `.logs/`
  and unpacked to `.logs/src-material3-1.12.0-alpha03/` (355 Kotlin files). `grep -rln -E
  'Disclosure|fun [A-Za-z]*Link[A-Za-z]*\('` over `commonMain` matches only `Text.kt`, which
  styles `LinkAnnotation`s inside text. What it does have, and this session used:
  `SegmentedButton.kt`, `ButtonGroup.kt`, `SearchBar.kt`.
- **Compose release watch.** 1.12.0 is still the latest (`maven-metadata.xml` for `ui`:
  `<latest>1.12.0</latest>`, `lastUpdated 20260825095827`; fetched 16:05, saved as
  `.logs/ui-maven-metadata-2026-09-02T1605.xml`). The `jb-main`
  `ComposeWebSemanticsListener.kt` has changed since the copy in `.logs/` (23461 → 25985 bytes;
  new copy saved as `ComposeWebSemanticsListener.jb-main-2026-09-02T1605.kt`): an
  `A11YScrollController` for scroll synchronization and positioning, and `aria-live="off"` on
  `list` and `grid` nodes. None of the measured properties moved: still no `aria-checked`,
  `aria-pressed`, `aria-expanded` or `aria-value*`; `aria-disabled` still written; `aria-label`
  still never removed. `A11YImplementationUtils.kt` is byte-identical to the earlier copy (the
  16:05 fetch saved as `.logs/A11YImplementationUtils.jb-main-2026-09-02T1605.kt`).

### Mutation checks (seen red before any row counted)

Records: `.logs/47-mutation-tbg.md` and `.logs/58-mutation-cbg-sf.md`; the reports generated
under mutation are `.logs/48-mutation-conformance-tbg.md` and
`.logs/59-mutation-conformance-cbg-sf.md` (the same artifact class as sessions 2 and 3).

1. ToggleButtonGroup, one build with the arrow dispatch removed and single-mode `toggleKey`
   never deselecting: `ToggleButtonGroupTest` 9/15 on JVM and wasmJs (six named failures, the
   same on both), `ToggleGroupStateTest` 4/5 on JVM and wasmJs-node; browser: behaviour `9/15`,
   three state-line texts missing (`space-center`, `enter`, and `space-italic` onward), the
   Compose snapshots showing focus stuck on Left and the style group toggling Bold off.
2. CheckboxGroup (deselect never removes) and SearchField (Enter consumed without submit), one
   build: `CheckboxGroupTest` 8/9 and `SearchFieldTest` 9/10 on JVM and wasmJs; browser:
   `text "Selected: music"` missing at `click`, `text "Submitted: compose"` missing at `enter`
   and `click-clear`.

Both builds restored from scratchpad copies (`grep MUTATION` clean) and re-run green on JVM,
wasmJs and browser before the rows were generated.

### Substitutions and cuts

- **ToggleButtonGroup** builds its single tab stop from the `AriaRadioGroup` plumbing (only the
  last focused item is focusable) rather than react-aria's tabbable-all-plus-edge-jump; the
  observable contract is the same except that Shift+Tab into a never-focused group lands on the
  first item here and the last there. The grouped item is an overload,
  `AriaToggleButtonGroupScope.AriaToggleButton(id)`, mirroring how the reference's `ToggleButton`
  joins a group through context. The selection rule is a pure `toggleKey` in `stately`, shared
  by the composable and by `ToggleGroupState`; the state class notifies only when the set changes,
  where react-stately rebuilds a `Set` on every toggle. Not measured in the browser: RTL, vertical
  orientation, `disallowEmptySelection` (semantics instrument only).
- **CheckboxGroup** takes an insertion-ordered `Set<String>` for the reference's `string[]`;
  an invalid group marks each checkbox with `Error` and the group with the message (semantics
  only); no `aria-describedby` (Compose has no id association); description and error are not in
  the demo.
- **SearchField**'s clear button is unfocusable, not merely excluded from the tab order (the
  session-2 "no focusable-but-not-tabbable state" cut again), which also keeps a pointer press
  from moving focus off the field; it is not composed while the value is empty, where the
  reference's starter hides it with CSS. `type="search"` has no Compose equivalent beyond
  `ImeAction.Search`. The Material3 control uses the `@ExperimentalMaterial3Api` String-based
  `SearchBarDefaults.InputField` overload, collapsed.
- **Gate:** `--rerun` after every leaf test task (`:stately:wasmJsNodeTest`, not the
  `wasmJsTest` aggregate).
- Manual Orca pass: **still not done.** It now has two groups, a checkbox group and a search
  field to listen to.

### Metrics (session 4)

- Truth run: 87 UI tests on wasmJs in Chrome plus the distribution, 27 s with the compile
  cached (`.logs/60-final-gate-gradle.log`, 16:10:16 → 16:10:43); 11 stately tests on JVM and
  wasmJs-node.
- Browser instrument: 37 targets, 5.2 min (`.logs/61-final-playwright.log`, 16:11:20 →
  16:16:33); all twelve rows came out identical to their first recordings apart from the
  timestamps, and the nine session-1-to-3 rows identical to the committed file.
- Bundle (`.logs/metric5-bundle-s4.txt`): 4.58 MB gzipped, of which the app `.wasm` 1.15 MB
  (was 1.08), skiko 3.33 MB (unchanged), JS 0.10 MB. The +0.07 MB is three ports plus the
  Material3 segmented button rows, `SearchBar` and the checkbox column, not separated. The
  reference Vite bundle is 0.09 MB gzipped, apples to oranges.
- LOC (`.logs/metric7-loc-s4.txt`): main Kotlin 1872 (was 1211), commonMain 1849 (99 % shared);
  `aria` 1119 main and 1292 test; `stately` 112 and 116; conformance 1370 lines of TS/JS.

## Session 5 — 2026-09-02

### Blind spots declared before the rows (the session-3 exception, applied twice)

Written before `meter.spec.ts` and `separator.spec.ts` were run for the first time, as the row
rule requires (NOTES "Session 3": a row whose port the browser instrument cannot see counts on the
semantics instrument, with the blindness written here first).

- **Meter.** The reference's `Meter` renders its label ("Storage space") and value text ("25%")
  inside the `role="meter progressbar"` element, so both are the widget's own texts, not free
  text; the port has `progressSemantics`, which the 1.12.0 listener does not read (session 3,
  ProgressBar), so it has no widget role and the browser sees a paragraph. The report diffs
  widgets by role and name and free texts by string: the meter widget is missing at every step
  whatever the port does, and no port mutation to the value, the clamping or the formatting can
  change the browser diff. The same blindness as ProgressBar, for the same reason. The behaviour
  mutation for this row (clamping removed) is therefore expected red on the semantics instrument
  only, and the "Fill" state line, which is the demo's and not the meter's, is expected to stay
  green under it.
- **Separator.** The reference exposes two `separator` widgets with no name and no text; the
  surrounding "Above", "Below", "Left", "Right" are outside them and the port renders those as
  plain text, unchanged by anything the separator does. Compose has no separator role, so the
  port's line has no widget role; the browser diff will show `separator (unnamed)` missing at
  `load` and nothing else, and no port mutation (orientation ignored, thickness ignored) can
  reach it. The behaviour mutation for this row (orientation ignored) is expected red on the
  semantics instrument only.
- **NumberField is not blind.** Its state line is outside the group and the reference's step
  buttons are named widgets outside the input, so a behaviour mutation (ArrowUp stepping the
  wrong way) is expected red on the browser instrument at the `up` step and on the semantics
  instrument.

### The build

The gate with `--rerun` after every leaf test task executed the 87 UI tests and the eleven stately
tests in 4 s (`.logs/63-gate-s5.log`, 16:55:45 → 16:55:50; XML timestamps 13:55:46Z–13:55:48Z), and
the Playwright gate (37 targets, 16:58:40 → 17:03:53, `.logs/64-gate-playwright-s5.log`) reproduced
the twelve committed rows with zero non-timestamp diff lines. Three components, twenty-seven new
UI tests and eight state tests: NumberField (17 + 8), Meter (6) and Separator (4) went first-try
green on the JVM (`.logs/65-s5-jvm.log`, 17:08:57 → 17:09:04; XML 14:08:58Z and 14:09:01Z–14:09:03Z)
and, after the cache incident below, on wasmJs (`.logs/66b-s5-wasm-and-dist-after-ic-clear.log`,
17:10:49 → 17:11:24; XML 14:10:53Z, stately 14:10:56Z). One harness change: `report.js` keys a
nameless reference widget as `role (unnamed)` and attributes it by role alone, instead of matching
it by its null name to any nameless target widget (which would have let Compose's nameless backing
textbox stand in for the reference's unnamed `group`); the regenerated report showed zero change to
the twelve rows before the new specs ran. From the gate's end (16:55:50) to the end of the final
Playwright run (17:36:56) is forty-one minutes of wall clock, of which the dropped-keystroke
investigation (17:13 → 17:28, logs 68–76) is about fifteen.

What fought back:

- **The Kotlin/Wasm incremental cache.** The first wasm run after the new `stately` source file
  failed in the link of the stately test executable: `:stately:compileTestDevelopmentExecutableKotlinWasmJs`
  threw `NoSuchElementException: Key ic#69:kotlin.text/split|split@kotlin.CharSequence(kotlin.CharArray...;kotlin.Boolean;kotlin.Int){}[0]-0 is missing in the map`
  from `WasmIrToBinary` (`.logs/66-s5-wasm-and-dist.log`, 17:09:34 → 17:09:52, BUILD FAILED in
  17 s). Deleting `stately/build/klib/cache` (the `wasm-js` incremental cache) and rerunning the
  same command went green in 34 s. No source was touched between the two runs; beyond "an
  incremental-cache entry the linker expected was missing after a new file", the cause was not
  established.
- **The dropped keystroke** (its own section below): fifteen minutes of measuring before the
  port bug under it was found.

### What the browser received (attribution, metric 8)

- **NumberField: `group (unnamed)` at every step, `disabled` at `home` and `end`; nothing else.**
  The reference exposes a plain `textbox "Quantity"`, not a spinbutton: `useNumberField` takes the
  spinbutton role and the four value attributes from `useSpinButton` (`useSpinButton.mjs` lines
  183–184) and then nulls them on the input (`useNumberField.mjs` lines 218–223, "override the
  spinbutton role, we can't focus a spin button with VO"), describing the role as "Number field"
  through `aria-roledescription` (line 220), which Playwright's snapshot does not render. The
  session-4 handoff predicted `role spinbutton→textbox` and invisible value attributes; the grep
  before writing corrected that, and the row shows what the source says. The two step buttons are
  `button "Decrease Quantity"` / `button "Increase Quantity"` on both sides (the reference's
  `aria-label` from its `increase` / `decrease` strings, the port's `contentDescription`), both
  outside the tab order (reference `excludeFromTabOrder`, lines 262 and 276; port
  `canFocus = false`): one Tab on the reference and two on Compose reached the input. The wrapper
  is `role="group"` with no name (line 285); Compose has no group role (session 4), and the
  Material3 control (a `TextField` with `KeyboardType.Number` between two `IconButton`s in a
  `Row`) arrives as a nameless `textbox` between `button "-"` and `button "+"` with no group, so
  **framework**. `disabled` on a step button at its bound (Decrease at 0 after Home, Increase at
  10 after End) is the Button row's `disabled` again: not written by the mirror, absent on the M3
  `IconButton`s too, **framework**. Behaviour matched at every step: ArrowUp 6, ArrowDown 5, Home
  0, End 10, the pointer press on Decrease 9 with focus staying on the input, select-all and "7"
  showing 7 in the field with 9 still committed, Enter committing 7. The backing `<input>` shows
  as the nameless second textbox holding the value, as on the TextField and SearchField rows.
- **`inputmode="number"`.** The port asks for `KeyboardType.Number`, the nearest Compose
  expression of the reference's `inputMode="numeric"`. On the web target that becomes
  `inputmode="number"` on the backing `<input>` (`DomInputStrategy.kt` lines 184–195 map the
  keyboard type and line 209 sets the attribute; the recorded mirror DOM shows
  `<input autocorrect="on" autocomplete="off" autocapitalize="off" spellcheck="false" inputmode="number" enterkeyhint="enter" class="compose-backing-field">`).
  `number` is not one of the eight `inputmode` keywords (`none`, `text`, `decimal`, `numeric`,
  `tel`, `search`, `email`, `url`; MDN's `inputmode` page, fetched 2026-09-02), so on a device
  with a soft keyboard the numeric hint is lost; the same table maps `KeyboardType.Password` to
  `"password"`, also not a keyword (not measured here). This is not the `type="number"` element
  the session-5 draft first assumed: the backing input has no `type` attribute, it is a textbox,
  and no spinbutton appears on the Compose side.
- **Meter: `meter "Storage space"` at every step.** Playwright resolves the reference's
  `role="meter progressbar"` (`useMeter.mjs` line 24; the second token is the fallback for
  browsers without the meter role) to `meter`; Compose has no vocabulary for either (session 3,
  ProgressBar), so the port is `text: Storage space 25%` and the Material3
  `LinearProgressIndicator` an empty element, **framework**. The Fill button moved the value
  25 → 50 → 75 on all three targets. Blind spot as declared above; the mutation was seen red on
  the semantics instrument only.
- **Separator: `separator (unnamed)` at `load`.** The reference exposes two separators, the
  `<hr>` and the vertical `div role="separator"` (`useSeparator.mjs` lines 23–26;
  `Separator.mjs` lines 44–45), collapsed into one row key because the report keys by role and
  name; `aria-orientation` is not in the snapshot on either side. Compose renders
  `text: Above Below Left Right`: the lines produce no node at all, and Material3's
  `HorizontalDivider` / `VerticalDivider` (a `Canvas` with no semantics, `Divider.kt`) produce
  none either, so **framework**. Blind spot as declared; mutation red on the semantics instrument
  only.
- **Focus.** Two Tabs on Compose, one on the reference, on the two rows with a tab stop.

### The dropped keystroke (a port bug the frame batching exposed)

The first recording bundled select-all, "7" and Enter into one step, and the Compose state line
stayed at 9 with the backing input at "9" (`.logs/67-numberfield-first-recording-bundled-keys.json`);
the rerun reproduced it (`.logs/69-s5-number-field-rerun.log`). A standalone script that redid the
sequence with a probe between the keys did not reproduce it
(`.logs/68b-typeafterclick-fresh-loads.log`; the first version of that script,
`.logs/68-typeafterclick.log`, reported `document.activeElement` as the shadow host and navigated
by hash only, so it did not reload between scenarios; both flaws are fixed in 68b), and neither
did the harness with a probe between the keys (`.logs/70-debug-number-field-harness.log`). The
delay sweep settled it: a fresh load, Tab, Tab, Ctrl+A, "7", a wait, Enter. With no wait the
commit read 5, twice out of twice; with 16 ms or more it read 7 every time
(`.logs/71-enter-after-typing-delay-sweep.log`, 17:20:20 → 17:20:52).

Mechanism, cited: the backing input's `keydown`, `beforeinput` and `compositionend` events are
collected (`DomInputStrategy.kt` lines 86–116; `NativeInputEventsProcessor.registerEvent`, line
242) and replayed in timestamp order at one checkpoint scheduled for the next animation frame
(`DomInputStrategy.kt` lines 57–62, `window.requestAnimationFrame`; `runCheckpoint`, line 99),
where a typed character becomes an edit command and a non-typed key such as Enter becomes a key
event to the scene. So a digit and an Enter pressed within one frame reach the text field's
`onValueChange` and then the port's `onPreviewKeyEvent` in one checkpoint, before any
recomposition. The port's `commit()` read a `parsed` value computed at composition, one frame
stale, and wrote the old text back over the digit. The fix parses the field's current text at
call time (`NumberField.kt`, `parsed()`); after it, five zero-gap runs in a row committed 7
(`.logs/76-enter-after-typing-after-fix.log`, 17:28:20 → 17:28:44; script
`.logs/enter-after-typing.mjs`). The semantics instrument cannot see this class of bug:
`runComposeUiTest` awaits idle between `performTextInput` and `performKeyInput`, so every test
sees a fresh composition; the JVM and wasmJs runs were green before and after the fix. The spec
was then split into `type` and `enter` steps, one action per recorded step as in the other specs,
and re-recorded (`.logs/72-s5-number-field-split-steps.log`). What of this is the framework's: the
batching makes a composition-time capture in a key handler unsafe on the web target where it is
safe on the JVM, which is a real difference between the targets, not a bug in either.

### Mutation checks (seen red before any row counted)

Record: `.logs/73-mutation-nf-meter-sep.md`; the report generated under mutation is
`.logs/74-mutation-conformance-nf-meter-sep.md`. One build with three mutations
(`.logs/73-mutation-build-nf-meter-sep.log`, 17:23:04 → 17:23:50): ArrowUp and PageUp step the
wrong way, Meter's clamping removed, Separator's orientation ignored. Semantics instrument, JVM
and wasmJs alike: `NumberFieldTest` 12/17 (five named failures, the same on both),
`MeterTest` 5/6 (the range info is still clamped, by foundation's `progressSemantics` itself; the
value text "150%" is what failed), `SeparatorTest` 3/4. Browser instrument
(`.logs/74-mutation-playwright-nf-meter-sep.log`, 9 targets, 59.3 s): NumberField behaviour
`17/17` → `12/17` with `text "Value: 6"` (up) and `text "Value: 5"` (down) missing, the Compose
state line reading 4 and 3; Meter `6/6` → `5/6` and Separator `4/4` → `3/4` with their a11y
columns unchanged, as declared. Restored from the scratchpad copies (`grep MUTATION` clean), the
parse-at-call-time fix applied, rebuilt and re-run green on JVM and wasmJs
(`.logs/75-s5-rebuild-after-parse-fix.log`, 17:27:17 → 17:28:05) before the final gate.

### Substitutions and cuts

- **NumberField** formats and parses `en-US` only (`formatNumber`, `parseNumber` in `stately`:
  grouping comma, up to three decimals, halves away from zero) where the reference takes
  `Intl.NumberFormatOptions` and a locale; partial-input validation handles the latin numbering
  system and the plain minus sign only. `decimalPlaces` counts a step's decimals exactly where
  react-stately's `roundToStepPrecision` counts from the string with the point as a digit (one
  digit looser). No press-and-hold spinning, no scroll wheel, no assertive live announcement of
  the value, no `aria-roledescription`, no `aria-controls`, no hidden form input. The step
  buttons are unfocusable, not merely untabbable (the session-2 cut again). Home and End are
  consumed even without a bound, as the hook's shortcuts are. A value that rounds to zero formats
  without a sign where `Intl` prints `-0`.
- **Meter** as ProgressBar: percent by default, no `Intl`; no indeterminate state (the reference
  has none either).
- **Separator** has no semantics of its own; the tests hold it to geometry and inertness.
- **Gate:** unchanged from session 4.
- Manual Orca pass: **still not done.** Five sessions.

### Metrics (session 5)

- Truth run: 114 UI tests on wasmJs in Chrome plus the distribution in the final gate, 4 s with
  the compile cached and the distribution up to date (`.logs/77-final-gate-gradle-s5.log`,
  17:29:09 → 17:29:14; XML 14:29:10Z–14:29:14Z); 19 stately tests on JVM and wasmJs-node. The
  rebuild that produced the shipped distribution took 48 s (`.logs/75-s5-rebuild-after-parse-fix.log`).
- Browser instrument: 46 targets, 6.2 min (`.logs/78-final-playwright-s5.log`, 17:30:46 →
  17:36:56); all fifteen rows came out identical to their recordings apart from the timestamps, the twelve session-1-to-4 rows to the committed file and the three new rows to their first (Meter, Separator) or re-recorded (NumberField) runs.
- Bundle (`.logs/metric5-bundle-s5.txt`): 4.61 MB gzipped, of which the app `.wasm` 1.18 MB
  (was 1.15), skiko 3.33 MB (unchanged), JS 0.10 MB. The +0.03 MB is three ports, the number-field
  state and the Material3 number-field row, dividers and progress indicator, not separated.
- LOC (`.logs/metric7-loc-s5.txt`): main Kotlin 2508 (was 1872), commonMain 2485 (99 % shared);
  `aria` 1425 main and 1635 test; `stately` 332 and 216; conformance 1558 lines of TS/JS.

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

Nine components in (session 3): the pattern for Tier 1 is now legible. What crosses is what the
1.12.0 listener reads off a property rather than off `Role`: text, a name, editable text, and a
heading. Everything that is a `Role` or a state is a button without state, a link included, and
`jb-main` changes that only for the roles and for text links, not for any state, not for a
progress value, not for expanded. ProgressBar is the first row the browser instrument cannot see
the port on at all, which makes it the clearest statement of the ceiling: a progress bar built
in Compose for Web is, to Chromium's accessibility tree, a paragraph. Disclosure is the first
row where a role mutation went red, because heading is derived from a property, not from
`Role` (as textbox, list and grid are). Three components cost twenty minutes of wall clock from
the honest gate to the commit, and again almost none of it in Kotlin. The Orca pass is still
owed.

Twelve components in (session 4): nothing new crossed, and that is the finding. Three group
components brought three group roles (`radiogroup`, `toolbar`, `group`) and one input role
(`searchbox`), and none of them has a Compose `Role` or a `jb-main` role id (the one group property,
`SelectableGroup`, is not read), so nothing on today's `jb-main` flips those rows; the per-item roles and states fall as before. The Compose side
kept being the easy part: thirty-four UI tests, two compile slips, no behaviour bug, roving focus
and keyboard shortcuts included. The new information this session is on the keyboard side.
Focus that enters the canvas does not leave it by Tab or Shift+Tab, measured with a sibling
button to leave to; on 1.12.0 that is the shape of the web target, not a demo-page artefact.
The Orca pass is still owed, and it now matters more: a screen reader user who tabs into this
canvas hears buttons without state and then cannot tab out.

Fifteen components in (session 5): the ceiling held its shape, and the new information is in the
text pipeline. Meter and Separator added nothing to the browser tree at all, the second and third
rows the browser instrument cannot see the port on, and the reference's own NumberField turns out
to be a textbox by design, so its row is the cleanest since TextField: an unnamed group and a
disabled bound button, both framework. What is new: the Compose web target replays a frame's
keystrokes and keys together at the next animation frame, so a handler that captures state at
composition time is one frame stale there and correct on the JVM. That cost a port bug the
semantics instrument could not see and the browser instrument only saw because a step bundled
two keys. And the numeric keyboard hint asks for `inputmode="number"`, which no browser
recognises. Five components remain in Tier 1; the Orca pass is still owed.
