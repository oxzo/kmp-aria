import { test } from '@playwright/test'
import {
  TargetRecord,
  StepRecord,
  entry,
  recorder,
  tabUntilFocused,
  targetsFor,
  writeResult,
} from '../lib/harness'

/**
 * TextField: load, Tab to the Name field, type "Ada", Tab to the Password field, type
 * "hunter2", then one more Tab past the last field to record whether focus can leave the
 * canvas. The typed text must show up in the state line (behaviour) and in the textbox (a11y
 * tree); the password field is where a plain-text value (CMP-10652) would show, in the
 * recorded mirror DOM rather than the snapshot.
 */
const e = entry('TextField')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`TextField on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'Name', record)
    await page.keyboard.type('Ada')
    await record('type')
    await page.keyboard.press('Tab')
    await record('to-password')
    await page.keyboard.type('hunter2')
    await record('type-secret')
    await page.keyboard.press('Tab')
    await record('tab-out')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
