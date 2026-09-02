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
 * Button: WAI-ARIA button pattern. Script: load, Tab to focus, Enter, Space, pointer click.
 * The reference counts "Pressed N times"; every target renders the same string.
 */
const e = entry('Button')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`Button on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'Press me', record)
    await page.keyboard.press('Enter')
    await record('enter')
    await page.keyboard.press('Space')
    await record('space')
    await clickByTag(page, target, 'btn')
    await record('click')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
