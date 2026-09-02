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
 * RadioGroup: load, Tab to the first radio, Space (selects Dog), ArrowDown (moves focus and
 * selection to Cat), Enter (must change nothing), pointer click on Dragon, then one more Tab
 * to record whether focus leaves the group (a single tab stop) or stays in it.
 */
const e = entry('RadioGroup')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`RadioGroup on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'Dog', record)
    await page.keyboard.press('Space')
    await record('space')
    await page.keyboard.press('ArrowDown')
    await record('down')
    await page.keyboard.press('Enter')
    await record('enter')
    await clickByTag(page, target, 'r-dragon')
    await record('click')
    await page.keyboard.press('Tab')
    await record('tab-out')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
