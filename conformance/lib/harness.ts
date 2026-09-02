/**
 * Shared harness for the browser-a11y-tree instrument.
 *
 * One interaction script runs against up to three targets per component:
 *   reference  React Aria (Vite, :5173, root #root)
 *   compose    the kmp-aria port (Compose for Web distribution, :8081, root #composeApp)
 *   m3         Material3's own widget on the same Compose build (framework ceiling control)
 * After each step the harness records `locator.ariaSnapshot()` of the root. The diff against
 * the reference is computed by report.js, not here: this file records, it does not judge.
 */
import { Page } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

export const REFERENCE_ORIGIN = 'http://localhost:5173/'
export const COMPOSE_ORIGIN = 'http://localhost:8081/index.html'

/** Mirror sync is debounced 100 ms and force-synced at 1 s (ComposeWebSemanticsListener). */
export const SYNC_MS = 1200

export type TargetName = 'reference' | 'compose' | 'm3'

export interface Target {
  name: TargetName
  url: string
  root: string
}

export interface ComponentEntry {
  component: string
  tier: number
  route: string
  m3Route?: string
  testClass: string
  reference: string
}

const here = path.dirname(fileURLToPath(import.meta.url))
export const componentsFile = path.join(here, '..', 'components.json')
export const resultsDir = path.join(here, '..', 'results')

export function componentEntries(): ComponentEntry[] {
  return JSON.parse(fs.readFileSync(componentsFile, 'utf8')) as ComponentEntry[]
}

export function entry(component: string): ComponentEntry {
  const e = componentEntries().find((c) => c.component === component)
  if (!e) throw new Error(`components.json has no entry for ${component}`)
  return e
}

export function targetsFor(e: ComponentEntry): Target[] {
  const t: Target[] = [
    { name: 'reference', url: `${REFERENCE_ORIGIN}#${e.route}`, root: '#root' },
    { name: 'compose', url: `${COMPOSE_ORIGIN}#${e.route}`, root: '#composeApp' },
  ]
  if (e.m3Route) t.push({ name: 'm3', url: `${COMPOSE_ORIGIN}#${e.m3Route}`, root: '#composeApp' })
  return t
}

export interface StepRecord {
  step: string
  snapshot: string
  /** Accessible name of the focused widget, or null. See focusedName(). */
  focused: string | null
  /** Compose targets only: the accessibility mirror's DOM, for attribution. */
  mirrorHtml?: string
}

export interface TargetRecord {
  target: TargetName
  url: string
  /** Tabs pressed before the target widget reported focus; null if it never did. */
  tabsToFocus: number | null
  steps: StepRecord[]
}

export interface ComponentResult {
  component: string
  recordedAt: string
  targets: TargetRecord[]
}

export async function snapshot(page: Page, target: Target): Promise<string> {
  await page.waitForTimeout(SYNC_MS)
  return await page.locator(target.root).ariaSnapshot()
}

/**
 * The Compose a11y mirror lives inside an open shadow root under the viewport container.
 * Its raw DOM is what a screen reader receives, so it is stored beside the snapshot: when a
 * state is missing, the mirror shows whether the framework wrote nothing (ceiling) or wrote
 * something Playwright does not surface.
 */
export async function mirrorHtml(page: Page, target: Target): Promise<string | undefined> {
  if (target.name === 'reference') return undefined
  return await page.evaluate((root) => {
    const container = document.querySelector(root)
    if (!container) return '<!-- no root -->'
    const serialize = (node: ParentNode): string => {
      let html = ''
      for (const child of Array.from(node.children)) {
        if (child.tagName === 'CANVAS' || child.tagName === 'STYLE') continue
        const attrs = Array.from(child.attributes).map((a) => ` ${a.name}="${a.value}"`).join('')
        html += `<${child.tagName.toLowerCase()}${attrs}>`
        if (child.shadowRoot) html += '<!--shadow-->' + serialize(child.shadowRoot)
        html += child.children.length ? serialize(child) : (child.textContent ?? '')
        html += `</${child.tagName.toLowerCase()}>\n`
      }
      return html
    }
    return serialize(container)
  }, target.root)
}

/** Record one step: snapshot, focus, and (Compose) the mirror DOM. */
export function recorder(page: Page, target: Target, steps: StepRecord[]) {
  return async (step: string): Promise<StepRecord> => {
    const snap = await snapshot(page, target)
    const r: StepRecord = {
      step,
      snapshot: snap,
      focused: await focusedName(page, target, snap),
      mirrorHtml: await mirrorHtml(page, target),
    }
    steps.push(r)
    return r
  }
}

/**
 * Click the widget by its test id. The reference renders data-testid; the Compose demo sets
 * Modifier.testTag, which the mirror writes as the element id (inside the shadow root, which
 * Playwright locators pierce). The click lands at the element's centre, which is over the
 * canvas pixels the mirror node overlays, so the press reaches Compose whether or not the
 * mirror forwards it.
 */
export async function clickByTag(page: Page, target: Target, tag: string): Promise<void> {
  const locator = target.name === 'reference' ? page.locator(`[data-testid="${tag}"]`) : page.locator(`#${tag}`)
  const box = await locator.first().boundingBox()
  if (!box) throw new Error(`no bounding box for ${tag} on ${target.name}`)
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2)
}

/**
 * Focus is observed two different ways on purpose. On the reference, document.activeElement
 * is real. On Compose, activeElement is always the canvas (CMP-10679); the demo turns on
 * AriaDebug.focusMarker, which appends "(focused)" to the focused node's contentDescription,
 * and that crosses the mirror as aria-label. The reference never carries the marker.
 */
export async function focusedName(page: Page, target: Target, snap: string): Promise<string | null> {
  if (target.name === 'reference') {
    return await page.evaluate(() => {
      const el = document.activeElement
      if (!el || el === document.body) return null
      // An input named by its <label> (checkbox, radio, text field) has no text of its own.
      const label = (el as HTMLInputElement).labels?.[0]?.textContent?.trim()
      return el.getAttribute('aria-label') ?? (label || el.textContent?.trim() || null)
    })
  }
  const m = snap.match(/"([^"]*) \(focused\)"/)
  return m ? m[1] : null
}

/**
 * Press Tab until `name` reports focus, at most `max` times. Returns the count or null.
 * M3 routes carry no focus marker, so on the m3 target the cap is two Tabs (the canvas, then
 * the first widget), which is where the rest of the script assumes focus to be.
 */
export async function tabUntilFocused(
  page: Page,
  target: Target,
  name: string,
  record: (step: string) => Promise<StepRecord>,
  max = 3,
): Promise<number | null> {
  if (target.name === 'm3') max = Math.min(max, 2)
  for (let i = 1; i <= max; i++) {
    await page.keyboard.press('Tab')
    const r = await record(`tab${i}`)
    if (r.focused === name) return i
  }
  return null
}

export function writeResult(result: ComponentResult): void {
  fs.mkdirSync(resultsDir, { recursive: true })
  fs.writeFileSync(path.join(resultsDir, `${result.component}.json`), JSON.stringify(result, null, 2))
}

/** A framework widget recorded on its own, with no port counterpart yet (Material3 controls). */
export interface ControlResult {
  control: string
  route: string
  recordedAt: string
  steps: StepRecord[]
}

export function writeControlResult(result: ControlResult): void {
  const dir = path.join(resultsDir, 'controls')
  fs.mkdirSync(dir, { recursive: true })
  fs.writeFileSync(path.join(dir, `${result.control.replace(/\W+/g, '-')}.json`), JSON.stringify(result, null, 2))
}
