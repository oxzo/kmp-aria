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
 * ProgressBar: the bar itself is static and not focusable, so the script drives it through an
 * "Advance" button: load (30 %), Tab to the button, Enter (60 %), pointer click (90 %). The value
 * must follow in the state line (behaviour) and in the progressbar's value (a11y tree, recorded
 * but not diffed); the indeterminate bar "Syncing" is recorded at every step.
 */
const e = entry('ProgressBar')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`ProgressBar on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'Advance', record)
    await page.keyboard.press('Enter')
    await record('enter')
    await clickByTag(page, target, 'adv')
    await record('click')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
