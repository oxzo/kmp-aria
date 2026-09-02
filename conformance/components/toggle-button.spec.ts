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
 * ToggleButton: button with aria-pressed. Script: load, Tab to focus, Space (on), Enter (off),
 * pointer click (on). The M3 control is IconToggleButton, which Material3 marks Role.Checkbox,
 * so its role differs from the reference by design; the state attribute is what the control
 * column is for.
 */
const e = entry('ToggleButton')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`ToggleButton on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'Bold', record)
    await page.keyboard.press('Space')
    await record('space')
    await page.keyboard.press('Enter')
    await record('enter')
    await clickByTag(page, target, 'tb')
    await record('click')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
