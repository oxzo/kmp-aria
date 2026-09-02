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
 * NumberField: a textbox named by its label between two step buttons ("Decrease Quantity",
 * "Increase Quantity") that are outside the tab order, inside an unnamed group. Script: load
 * (5), Tab to Quantity, ArrowUp (6), ArrowDown (5), Home (0: Decrease disabled), End (10:
 * Increase disabled), pointer click on Decrease (9; the press moves focus to the input),
 * select-all and type "7" (the field shows 7, the committed value is still 9), Enter (commits
 * 7). The state line carries the committed value; the field's own value is recorded, not
 * diffed. One action per recorded step, as in the other specs. The first recording bundled
 * select-all, "7" and Enter into one step and Compose committed 9: on the web target the
 * keystroke and the Enter are replayed in the same animation frame, and the port's commit read a
 * value captured at composition, a frame stale. Fixed in the port (it parses the field's text at
 * call time); NOTES "Session 5", `.logs/67-numberfield-first-recording-bundled-keys.json`,
 * `.logs/71-enter-after-typing-delay-sweep.log`, `.logs/76-enter-after-typing-after-fix.log`.
 */
const e = entry('NumberField')
const records: TargetRecord[] = []

for (const target of targetsFor(e)) {
  test(`NumberField on ${target.name}`, async ({ page }) => {
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    const tabsToFocus = await tabUntilFocused(page, target, 'Quantity', record)
    await page.keyboard.press('ArrowUp')
    await record('up')
    await page.keyboard.press('ArrowDown')
    await record('down')
    await page.keyboard.press('Home')
    await record('home')
    await page.keyboard.press('End')
    await record('end')
    await clickByTag(page, target, 'dec')
    await record('click-dec')
    await page.keyboard.press('Control+A')
    await page.keyboard.type('7')
    await record('type')
    await page.keyboard.press('Enter')
    await record('enter')
    records.push({ target: target.name, url: target.url, tabsToFocus, steps })
  })
}

test.afterAll(() => {
  writeResult({ component: e.component, recordedAt: new Date().toISOString(), targets: records })
})
