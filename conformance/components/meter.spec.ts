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
 * Meter: static and not focusable, so the script drives it through a "Fill" button as the
 * ProgressBar script does: load (25 %), Tab to the button, Enter (50 %), pointer click (75 %).
 * The value must follow in the state line (behaviour) and in the meter's value (a11y tree,
 * recorded but not diffed).
 */
const e = entry('Meter')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`Meter on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'Fill', record)
    await page.keyboard.press('Enter')
    await record('enter')
    await clickByTag(page, target, 'fill')
    await record('click')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
