import { test } from '@playwright/test'
import { TargetRecord, StepRecord, entry, recorder, targetsFor, writeResult } from '../lib/harness'

/**
 * Separator: nothing to interact with, so the script records the loaded page only. The reference
 * exposes two separators (an <hr> and a vertical div with role="separator"); whether the port's
 * lines exist at all in the browser tree is the whole measurement.
 */
const e = entry('Separator')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`Separator on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    records.push({ target: target.name, url: target.url, tabsToFocus: null, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
