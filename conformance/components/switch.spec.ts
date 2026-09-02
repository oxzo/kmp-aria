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
 * Switch: same script as ToggleButton (load, Tab to focus, Space, Enter, pointer click). The
 * reference is a native input, so Enter must leave the state alone; the state text after
 * the Enter step is where a port that toggles on Enter shows up.
 */
const e = entry('Switch')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`Switch on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'Wi-Fi', record)
    await page.keyboard.press('Space')
    await record('space')
    await page.keyboard.press('Enter')
    await record('enter')
    await clickByTag(page, target, 'sw')
    await record('click')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
