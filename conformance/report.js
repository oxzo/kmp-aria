#!/usr/bin/env node
/**
 * Generates CONFORMANCE.md from:
 *   results/<Component>.json         Playwright aria-snapshot records (lib/harness.ts)
 *   ../aria/build/test-results/...   Compose UI test XML (behaviour instrument)
 *   ../build.gradle.kts              the Compose Multiplatform version pin
 *
 * Usage: node report.js > ../CONFORMANCE.md
 *
 * The diff is mechanical: for every widget the reference exposes at a step, the same role
 * and name must exist on the target with the same states; every text string the reference
 * exposes must exist somewhere on the target. Anything else is listed, not scored.
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const here = path.dirname(fileURLToPath(import.meta.url))
const root = path.join(here, '..')
const components = JSON.parse(fs.readFileSync(path.join(here, 'components.json'), 'utf8'))

const WIDGET_ROLES = new Set([
  'button', 'checkbox', 'switch', 'radio', 'link', 'textbox', 'searchbox', 'spinbutton', 'slider',
  'progressbar', 'meter', 'tab', 'tablist', 'tabpanel', 'menu', 'menuitem', 'menuitemcheckbox',
  'menuitemradio', 'listbox', 'option', 'combobox', 'dialog', 'tooltip', 'group', 'radiogroup',
  'toolbar', 'separator', 'heading', 'list', 'listitem', 'grid', 'gridcell', 'row', 'tree', 'treeitem',
  'img', 'region', 'form', 'alert', 'status', 'navigation',
])
/** States driven by the same dropped semantics family on Compose for Web. */
const STATE_FAMILY = {
  pressed: ['pressed', 'checked', 'selected'],
  checked: ['pressed', 'checked', 'selected'],
  selected: ['pressed', 'checked', 'selected'],
  disabled: ['disabled'],
  expanded: ['expanded'],
}

const cmpVersion = (() => {
  const m = fs.readFileSync(path.join(root, 'build.gradle.kts'), 'utf8').match(/id\("org\.jetbrains\.compose"\) version "([^"]+)"/)
  return m ? m[1] : 'unknown'
})()

/** Playwright quotes a string that contains a colon (`paragraph: "Value: 30"`); strip that. */
const unquote = (s) => (s && /^"(?:[^"\\]|\\.)*"$/.test(s) ? s.slice(1, -1).replace(/\\(.)/g, '$1') : s)

function parseSnapshot(text) {
  const entries = []
  for (const raw of text.split('\n')) {
    const line = raw.trim()
    if (!line.startsWith('- ')) continue
    // A property line under a widget (`- /url: https://…` under a link) is that widget's
    // attribute; a link's destination is diffed as `href`.
    const prop = line.match(/^- \/([a-z]+): (.*)$/)
    if (prop) {
      const last = entries[entries.length - 1]
      if (last) last.attrs.push(prop[1] === 'url' ? 'href' : prop[1])
      continue
    }
    const m = line.match(/^- ([a-z]+)(?: "((?:[^"\\]|\\.)*)")?(?: \[([^\]]*)\])?(?::\s*(.*))?$/)
    if (!m) { entries.push({ role: 'unparsed', name: line, attrs: [], text: null }); continue }
    const [, role, name, attrs, text] = m
    entries.push({
      role,
      name: name ? name.replace(/ \(focused\)$/, '') : null,
      attrs: attrs ? attrs.split(',').map((a) => a.trim().split('=')[0]) : [],
      text: text ? unquote(text.trim()) : null,
    })
  }
  return entries
}

function widgets(entries) { return entries.filter((e) => WIDGET_ROLES.has(e.role)) }
/**
 * Static text: text nodes and the names of non-widget entries. A widget's trailing text is its
 * value (`textbox "Name": Ada`) and is not diffed: Playwright renders an <input>'s value, a
 * password's included, but never a contenteditable node's content, so the Compose value is
 * invisible to it. Values stay in the per-step snapshots; behaviour is read from the demo's
 * state line instead.
 */
function texts(entries) {
  const out = new Set()
  for (const e of entries) {
    if (e.text && !WIDGET_ROLES.has(e.role)) out.add(e.text.trim())
    if (e.name && !WIDGET_ROLES.has(e.role)) out.add(e.name.trim())
  }
  return out
}

/** Align steps: 'load', 'focus' (= last tabN), then interaction steps by name. */
function stepMap(record) {
  const m = new Map()
  let lastTab = null
  for (const s of record.steps) {
    if (s.step === 'load') m.set('load', s)
    else if (/^tab\d+$/.test(s.step)) lastTab = s
    else m.set(s.step, s)
  }
  if (lastTab) m.set('focus', lastTab)
  return m
}

function behaviour(entry) {
  const cls = entry.testClass
  const tryRead = (dir) => {
    const f = path.join(root, 'aria', 'build', 'test-results', dir, `TEST-${cls}.xml`)
    if (!fs.existsSync(f)) return null
    const x = fs.readFileSync(f, 'utf8')
    const m = x.match(/tests="(\d+)" skipped="(\d+)" failures="(\d+)" errors="(\d+)"/)
    if (!m) return null
    const [, tests, skipped, failures, errors] = m.map(Number)
    return { pass: tests - skipped - failures - errors, total: tests }
  }
  const wasm = tryRead('wasmJsBrowserTest')
  if (wasm) return `${wasm.pass}/${wasm.total}`
  const jvm = tryRead('jvmTest')
  if (jvm) return `${jvm.pass}/${jvm.total} (jvm-only)`
  return '—'
}

function diffTarget(ref, target) {
  const refSteps = stepMap(ref)
  const tgtSteps = stepMap(target)
  const missing = new Map() // key -> Set(steps)
  const note = (key, step) => { if (!missing.has(key)) missing.set(key, new Set()); missing.get(key).add(step) }
  for (const [step, rs] of refSteps) {
    const ts = tgtSteps.get(step)
    if (!ts) { note(`step "${step}" not recorded`, step); continue }
    const re = parseSnapshot(rs.snapshot)
    const te = parseSnapshot(ts.snapshot)
    const tw = widgets(te)
    const rw = widgets(re)
    // Exact role+name matches are claimed first, so a same-named widget of another role (a
    // heading labelled by its button, a group labelled by its trigger) cannot pose as a role
    // change of a widget that is simply absent. Each target widget is matched at most once.
    const consumed = new Set()
    const exact = new Map()
    for (const w of rw) {
      const t = tw.find((x) => !consumed.has(x) && x.role === w.role && x.name === w.name)
      if (t) { exact.set(w, t); consumed.add(t) }
    }
    for (const w of rw) {
      const same = exact.get(w)
      // A nameless reference widget (an unnamed group, a separator) has no name to fall back
      // on, so it is missing unless the same role is there; matching it to any nameless target
      // widget would let Compose's nameless backing textbox pose as it.
      if (!same && w.name === null) { note(`${w.role} (unnamed)`, step); continue }
      const byName = same ?? tw.find((t) => !consumed.has(t) && t.name === w.name)
      if (!byName) { note(`${w.role} "${w.name}"`, step); continue }
      if (!same) { note(`role ${w.role}→${byName.role} "${w.name}"`, step); consumed.add(byName) }
      for (const a of w.attrs) if (!byName.attrs.includes(a)) note(a, step)
    }
    // The reference renders a label as both the widget's name and a text node; a target that
    // merged the label into the name still exposes the string, so widget names count as texts.
    const tt = new Set([...texts(te), ...tw.map((w) => w.name).filter(Boolean)])
    for (const t of texts(re)) if (![...tt].some((x) => x.includes(t))) note(`text "${t}"`, step)
  }
  return missing
}

/** True if any recorded mirror DOM carries the attribute (as an attribute, not as id text). */
function mirrorHas(record, attr) {
  return record.steps.some((s) => s.mirrorHtml && new RegExp(`\\s(aria-)?${attr}(=|\\s|>)`).test(s.mirrorHtml))
}
const esc = (t) => t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
function mirrorHasRole(record, role) {
  return record.steps.some((s) => s.mirrorHtml && new RegExp(`\\srole="${esc(role)}"`).test(s.mirrorHtml))
}
/** True if any mirror node carries the name, as aria-label or as its text: the port produced it. */
function mirrorHasName(record, name) {
  const re = new RegExp(`aria-label="${esc(name)}( \\(focused\\))?"|>${esc(name)}<`)
  return record.steps.some((s) => s.mirrorHtml && re.test(s.mirrorHtml))
}
function m3Roles(record) {
  const roles = new Set()
  for (const s of record.steps) for (const e of widgets(parseSnapshot(s.snapshot))) roles.add(e.role)
  return roles
}
function snapshotsHaveAny(record, attrs) {
  return record.steps.some((s) => parseSnapshot(s.snapshot).some((e) => e.attrs.some((a) => attrs.includes(a))))
}

function attribute(key, compose, m3) {
  const attr = key.replace(/^aria-/, '')
  if (/^text\b/.test(key)) return 'port (behaviour/name)'
  if (/^step\b/.test(key)) return 'harness (step not recorded)'
  // `role X→Y "name"`: the widget is there under another role. `X "name"`: no widget by that
  // name; if the mirror still carries the name (as aria-label or text) the role was dropped,
  // otherwise the port never produced the name.
  const changed = key.match(/^role ([a-z]+)→[a-z]+ "(.*)"$/)
  const missing = key.match(/^([a-z]+) "(.*)"$/)
  // `X (unnamed)`: a nameless widget with no counterpart of that role; attributed by role alone.
  const unnamed = key.match(/^([a-z]+) \(unnamed\)$/)
  if (changed || missing || unnamed) {
    const role = (changed ?? missing ?? unnamed)[1]
    const name = (changed ?? missing)?.[2]
    if (missing && compose && !mirrorHasName(compose, name)) return 'port (name)'
    const parts = []
    if (compose) parts.push(mirrorHasRole(compose, role) ? `mirror writes role=${role}: instrument?` : `mirror never writes role=${role}`)
    const m3Lacks = m3 ? !m3Roles(m3).has(role) : null
    if (m3) parts.push(m3Lacks ? `M3 control also lacks role ${role}` : `M3 control exposes role ${role}`)
    const framework = compose && !mirrorHasRole(compose, role) && (m3 ? m3Lacks : false)
    return `${framework ? 'framework' : m3 ? 'port' : 'unattributed'} (${parts.join('; ')})`
  }
  const parts = []
  if (attr === 'href') {
    // A destination is not an aria-* state: mirror nodes are <div>s, so no node can carry one.
    parts.push('mirror nodes are div elements, no anchor and no href')
    if (m3) parts.push(snapshotsHaveAny(m3, ['href']) ? 'M3 control exposes href' : 'M3 control also lacks href')
    const framework = compose && (!m3 || !snapshotsHaveAny(m3, ['href']))
    return `${framework ? 'framework' : m3 ? 'port' : 'unattributed'} (${parts.join('; ')})`
  }
  if (compose && !mirrorHas(compose, attr)) parts.push(`mirror writes no aria-${attr}`)
  else if (compose) parts.push(`mirror has aria-${attr}: instrument?`)
  if (m3) {
    const family = STATE_FAMILY[attr] ?? [attr]
    parts.push(snapshotsHaveAny(m3, family) ? `M3 control exposes ${family.join('/')}` : `M3 control also lacks ${family.join('/')}`)
  }
  const framework = compose && !mirrorHas(compose, attr) && (!m3 || !snapshotsHaveAny(m3, STATE_FAMILY[attr] ?? [attr]))
  return `${framework ? 'framework' : m3 ? 'port' : 'unattributed'} (${parts.join('; ')})`
}

/** What the M3 control exposed: widget roles (with whether any was nameless) and states, across all steps. */
function m3Summary(record) {
  const roles = new Set()
  const states = new Set()
  let nameless = false
  for (const s of record.steps) for (const e of widgets(parseSnapshot(s.snapshot))) {
    roles.add(e.role)
    if (e.name === null) nameless = true
    for (const a of e.attrs) states.add(a)
  }
  return `roles: ${[...roles].join(', ') || 'none'}${nameless ? ' (a nameless one among them)' : ''}; states: ${[...states].map((a) => `\`${a}\``).join(', ') || 'none at any step'}`
}

function fmtMissing(missing) {
  if (missing.size === 0) return 'none'
  return [...missing].map(([k, steps]) => `\`${k}\` (${[...steps].join(', ')})`).join('; ')
}

const rows = []
const details = []
for (const c of components) {
  const f = path.join(here, 'results', `${c.component}.json`)
  if (!fs.existsSync(f)) {
    rows.push(`| ${c.component} | ${c.tier} | ${behaviour(c)} | not measured | not measured | — | ${cmpVersion} |`)
    continue
  }
  const r = JSON.parse(fs.readFileSync(f, 'utf8'))
  const ref = r.targets.find((t) => t.target === 'reference')
  const compose = r.targets.find((t) => t.target === 'compose')
  const m3 = r.targets.find((t) => t.target === 'm3')
  const missCompose = ref && compose ? diffTarget(ref, compose) : null
  const attribution = missCompose && missCompose.size
    ? [...missCompose.keys()].map((k) => `\`${k}\`: ${attribute(k, compose, m3)}`).join('; ')
    : '—'
  const focus = [ref, compose, m3].filter(Boolean).map((t) => `${t.target} ${t.tabsToFocus ?? 'n/a'}`).join(', ')
  rows.push(`| ${c.component} | ${c.tier} | ${behaviour(c)} | ${missCompose ? fmtMissing(missCompose) : 'not measured'} | ${m3 ? m3Summary(m3) : 'no M3 control'} | ${attribution} | ${cmpVersion} |`)
  details.push(`### ${c.component}\n\nReference: ${c.reference} · route \`#${c.route}\`${c.m3Route ? ` · M3 control \`#${c.m3Route}\`${c.controlNote ? ` (${c.controlNote})` : ''}` : ''} · recorded ${r.recordedAt}\n\nTabs until the widget reported focus: ${focus} (M3 controls carry no focus marker, so n/a there is unobservable, not a failure).\n`)
  for (const t of r.targets) {
    details.push(`<details><summary>${t.target} snapshots</summary>\n`)
    for (const s of t.steps) details.push(`\n**${s.step}** (focused: ${s.focused ?? 'none'})\n\n\`\`\`yaml\n${s.snapshot.trim()}\n\`\`\``)
    details.push(`\n</details>\n`)
  }
}

// Framework controls recorded on their own (conformance/components/m3-controls.spec.ts).
const controlsDir = path.join(here, 'results', 'controls')
const controlRows = []
const controlDetails = []
if (fs.existsSync(controlsDir)) {
  for (const f of fs.readdirSync(controlsDir).filter((n) => n.endsWith('.json')).sort()) {
    const r = JSON.parse(fs.readFileSync(path.join(controlsDir, f), 'utf8'))
    const roles = new Set()
    const states = new Set()
    const names = new Set()
    for (const s of r.steps) for (const e of widgets(parseSnapshot(s.snapshot))) {
      roles.add(e.role)
      names.add(e.name === null ? '(no name)' : `"${e.name}"`)
      for (const a of e.attrs) states.add(a)
    }
    const textsSeen = new Set()
    for (const s of r.steps) for (const t of texts(parseSnapshot(s.snapshot))) textsSeen.add(t)
    controlRows.push(`| ${r.control} | \`#${r.route}\` | ${[...roles].join(', ') || 'none'} | ${[...names].join(', ') || '—'} | ${[...states].map((a) => `\`${a}\``).join(', ') || 'none at any step'} | ${[...textsSeen].map((t) => `"${t}"`).join(', ')} | ${cmpVersion} |`)
    controlDetails.push(`<details><summary>${r.control} snapshots (recorded ${r.recordedAt})</summary>\n`)
    for (const s of r.steps) controlDetails.push(`\n**${s.step}**\n\n\`\`\`yaml\n${s.snapshot.trim()}\n\`\`\``)
    controlDetails.push(`\n</details>\n`)
  }
}

const out = `# Conformance — per-component table

**Generated by \`node conformance/report.js\` on ${new Date().toISOString().slice(0, 10)}. Do not hand-edit; a hand-edited row is a bug.**

Columns: behaviour = Compose UI tests passed/total on wasmJs in Chrome (flagged \`jvm-only\` when
the browser run is missing); a11y = roles, states, and text the React Aria reference exposes
that the Compose port's browser accessibility tree does not, per Playwright \`ariaSnapshot\`
diff, with the interaction steps where it was missing (widget values are not diffed, see
below); M3 = the widget roles and states the
Material3 control route (the framework's own widget) exposed at any step, not a name diff;
attribution = for each missing item, whether the
Compose accessibility mirror wrote the attribute (or role) at all and whether the M3 control
exposes the same state family (or role), hence framework ceiling vs port bug; a missing widget
whose name the mirror never carried, and a missing text, are the port's; CMP = Compose
Multiplatform version.

No aggregate, no rank. "none" means nothing the reference exposed was missing at any step.
Widget values (\`textbox "Name": Ada\`) are recorded but not diffed: Playwright's snapshot
renders an \`<input>\`'s value and never a contenteditable node's content, so a Compose text
field's value is invisible to the instrument although the mirror node carries it.

| Component | Tier | Behaviour (pass/total) | A11y missing vs reference | M3 control: roles and states exposed | Attribution | CMP |
|---|---|---|---|---|---|---|
${rows.join('\n')}

## Framework controls recorded without a port counterpart

Material3 widgets on the same Compose build, recorded by \`m3-controls.spec.ts\` (load, Tab, Tab,
Space, click). Roles, names and states are what the browser accessibility tree exposed at any
step. A ladder row for the matching React Aria component replaces the entry here once ported.

| Control | Route | Roles | Names | States | Text seen | CMP |
|---|---|---|---|---|---|---|
${controlRows.join('\n') || '| (none recorded) | | | | | | |'}

${controlDetails.join('\n')}

## Per-step records

${details.join('\n')}
`
process.stdout.write(out)
