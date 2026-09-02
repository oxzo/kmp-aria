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

function parseSnapshot(text) {
  const entries = []
  for (const raw of text.split('\n')) {
    const line = raw.trim()
    if (!line.startsWith('- ')) continue
    const m = line.match(/^- ([a-z]+)(?: "((?:[^"\\]|\\.)*)")?(?: \[([^\]]*)\])?(?::\s*(.*))?$/)
    if (!m) { entries.push({ role: 'unparsed', name: line, attrs: [], text: null }); continue }
    const [, role, name, attrs, text] = m
    entries.push({
      role,
      name: name ? name.replace(/ \(focused\)$/, '') : null,
      attrs: attrs ? attrs.split(',').map((a) => a.trim().split('=')[0]) : [],
      text: text ?? null,
    })
  }
  return entries
}

function widgets(entries) { return entries.filter((e) => WIDGET_ROLES.has(e.role)) }
function texts(entries) {
  const out = new Set()
  for (const e of entries) {
    if (e.text) out.add(e.text.trim())
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
    for (const w of widgets(re)) {
      const same = tw.find((t) => t.role === w.role && t.name === w.name)
      const byName = same ?? tw.find((t) => t.name === w.name)
      if (!byName) { note(`${w.role} "${w.name}"`, step); continue }
      if (!same) note(`role ${w.role}→${byName.role} "${w.name}"`, step)
      for (const a of w.attrs) if (!byName.attrs.includes(a)) note(a, step)
    }
    const tt = texts(te)
    for (const t of texts(re)) if (![...tt].some((x) => x.includes(t))) note(`text "${t}"`, step)
  }
  return missing
}

/** True if any recorded mirror DOM carries the attribute (as an attribute, not as id text). */
function mirrorHas(record, attr) {
  return record.steps.some((s) => s.mirrorHtml && new RegExp(`\\s(aria-)?${attr}(=|\\s|>)`).test(s.mirrorHtml))
}
function snapshotsHaveAny(record, attrs) {
  return record.steps.some((s) => parseSnapshot(s.snapshot).some((e) => e.attrs.some((a) => attrs.includes(a))))
}

function attribute(key, compose, m3) {
  const attr = key.replace(/^aria-/, '')
  if (/^(text|role|step)\b/.test(key) || key.includes('"')) return 'port (behaviour/name/role)'
  const parts = []
  if (compose && !mirrorHas(compose, attr)) parts.push(`mirror writes no aria-${attr}`)
  else if (compose) parts.push(`mirror has aria-${attr}: instrument?`)
  if (m3) {
    const family = STATE_FAMILY[attr] ?? [attr]
    parts.push(snapshotsHaveAny(m3, family) ? `M3 control exposes ${family.join('/')}` : `M3 control also lacks ${family.join('/')}`)
  }
  const framework = compose && !mirrorHas(compose, attr) && (!m3 || !snapshotsHaveAny(m3, STATE_FAMILY[attr] ?? [attr]))
  return `${framework ? 'framework' : m3 ? 'port' : 'unattributed'} (${parts.join('; ')})`
}

/** What the M3 control exposed: widget roles seen, and states seen, across all steps. */
function m3Summary(record) {
  const roles = new Set()
  const states = new Set()
  for (const s of record.steps) for (const e of widgets(parseSnapshot(s.snapshot))) {
    roles.add(e.role)
    for (const a of e.attrs) states.add(a)
  }
  return `roles: ${[...roles].join(', ') || 'none'}; states: ${[...states].map((a) => `\`${a}\``).join(', ') || 'none at any step'}`
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
  details.push(`### ${c.component}\n\nReference: ${c.reference} · route \`#${c.route}\`${c.m3Route ? ` · M3 control \`#${c.m3Route}\`` : ''} · recorded ${r.recordedAt}\n\nTabs until the widget reported focus: ${focus} (M3 controls carry no focus marker, so n/a there is unobservable, not a failure).\n`)
  for (const t of r.targets) {
    details.push(`<details><summary>${t.target} snapshots</summary>\n`)
    for (const s of t.steps) details.push(`\n**${s.step}** (focused: ${s.focused ?? 'none'})\n\n\`\`\`yaml\n${s.snapshot.trim()}\n\`\`\``)
    details.push(`\n</details>\n`)
  }
}

const out = `# Conformance — per-component table

**Generated by \`node conformance/report.js\` on ${new Date().toISOString().slice(0, 10)}. Do not hand-edit; a hand-edited row is a bug.**

Columns: behaviour = Compose UI tests passed/total on wasmJs in Chrome (flagged \`jvm-only\` when
the browser run is missing); a11y = roles, states, and text the React Aria reference exposes
that the Compose port's browser accessibility tree does not, per Playwright \`ariaSnapshot\`
diff, with the interaction steps where it was missing; M3 = the widget roles and states the
Material3 control route (the framework's own widget) exposed at any step, not a name diff;
attribution = for each missing item, whether the
Compose accessibility mirror wrote the attribute at all and whether the M3 control exposes the
same state family, hence framework ceiling vs port bug; CMP = Compose Multiplatform version.

No aggregate, no rank. "none" means nothing the reference exposed was missing at any step.

| Component | Tier | Behaviour (pass/total) | A11y missing vs reference | M3 control: roles and states exposed | Attribution | CMP |
|---|---|---|---|---|---|---|
${rows.join('\n')}

## Per-step records

${details.join('\n')}
`
process.stdout.write(out)
