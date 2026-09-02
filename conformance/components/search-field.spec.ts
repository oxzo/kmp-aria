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
 * SearchField: a searchbox named by its label with a clear button that exists only while the
 * field has a value. Script: load, Tab to Search, type "kotlin", Escape (clears the value; the
 * clear button goes away), type "compose", Enter (submits: the second state line changes, the
 * value stays), pointer click on the clear button (value empty again). The clear button is not
 * in the tab order on the reference; Compose has no focusable-but-untabbable state, so the port
 * makes it unfocusable.
 */
const e = entry('SearchField')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`SearchField on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'Search', record)
    await page.keyboard.type('kotlin')
    await record('type')
    await page.keyboard.press('Escape')
    await record('escape')
    await page.keyboard.type('compose')
    await record('type-again')
    await page.keyboard.press('Enter')
    await record('enter')
    await clickByTag(page, target, 'clear')
    await record('click-clear')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
