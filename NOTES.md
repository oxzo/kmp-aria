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
