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
 * CheckboxGroup: a group named by its label ("Interests") with three checkboxes, Reading
 * disabled. Script: load, Tab to Sports, Space (Sports on), Tab (every checkbox is its own tab
 * stop: Music), Space (Music on; the state line keeps press order), Enter (a native checkbox
 * ignores Enter), pointer click on Sports (off). The M3 control is Material3's Checkboxes in a
 * Column: Material3 has no group widget, which is the point of the control column here.
 */
const e = entry('CheckboxGroup')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`CheckboxGroup on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'Sports', record)
    await page.keyboard.press('Space')
    await record('space')
    await page.keyboard.press('Tab')
    await record('tab-music')
    await page.keyboard.press('Space')
    await record('space-music')
    await page.keyboard.press('Enter')
    await record('enter')
    await clickByTag(page, target, 'cb-sports')
    await record('click')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
