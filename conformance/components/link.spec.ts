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
 * Link: load, Tab to "Follow me", Enter (must activate), Space (must not: usePress lets only
 * Enter through for links), pointer click, then one more Tab to record whether focus moves to
 * the next link ("Docs", the one with an href; the disabled link must be skipped after it).
 * The reference counts "Followed N times"; every target renders the same string.
 */
const e = entry('Link')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`Link on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'Follow me', record)
    await page.keyboard.press('Enter')
    await record('enter')
    await page.keyboard.press('Space')
    await record('space')
    await clickByTag(page, target, 'lnk')
    await record('click')
    await page.keyboard.press('Tab')
    await record('tab-next')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
