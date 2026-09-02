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
 * Disclosure: load (collapsed), Tab to the trigger, Enter (expands), Space (collapses: the
 * trigger is a button, both keys toggle), pointer click (expands). The panel text must appear
 * and disappear with the state line; `expanded` on the trigger is where the state is measured.
 */
const e = entry('Disclosure')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`Disclosure on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'System Requirements', record)
    await page.keyboard.press('Enter')
    await record('enter')
    await page.keyboard.press('Space')
    await record('space')
    await clickByTag(page, target, 'trig')
    await record('click')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
