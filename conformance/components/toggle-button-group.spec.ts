import { test } from '@playwright/test'
import {
  TargetRecord,
  StepRecord,
  clickByTag,
  entry,
  recorder,
  tabUntilFocused,
  targetsFor,
  writeResult,
} from '../lib/harness'

/**
 * ToggleButtonGroup: two groups on one route. "Text alignment" is single selection (radiogroup,
 * radio items with aria-checked on the reference); "Text style" is multiple selection (toolbar,
 * buttons with aria-pressed), its Underline item disabled. Script: load, Tab to Left, Space (Left
 * on), ArrowRight (focus Center, selection unchanged), Space (Center on, Left off), Enter (Center
 * off, nothing selected), click Right, Tab (one tab stop: leaves the group for Bold), Space (Bold
 * on), ArrowRight (Italic), Space (Italic on), ArrowRight (Underline is disabled and there is no
 * wrap: focus stays), Tab (leaves the second group). The M3 control is a single-choice and a
 * multi-choice SegmentedButtonRow; Material3 has no arrow-key focus movement, so its steps after
 * the first Space diverge by design; the control column is about roles and states.
 */
const e = entry('ToggleButtonGroup')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`ToggleButtonGroup on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'Left', record)
    await page.keyboard.press('Space')
    await record('space')
    await page.keyboard.press('ArrowRight')
    await record('right')
    await page.keyboard.press('Space')
    await record('space-center')
    await page.keyboard.press('Enter')
    await record('enter')
    await clickByTag(page, target, 'tb-right')
    await record('click')
    await page.keyboard.press('Tab')
    await record('tab-next')
    await page.keyboard.press('Space')
    await record('space-bold')
    await page.keyboard.press('ArrowRight')
    await record('right-italic')
    await page.keyboard.press('Space')
    await record('space-italic')
    await page.keyboard.press('ArrowRight')
    await record('right-edge')
    await page.keyboard.press('Tab')
    await record('tab-out')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
