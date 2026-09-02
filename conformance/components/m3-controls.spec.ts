import { test } from '@playwright/test'
import { COMPOSE_ORIGIN, StepRecord, Target, clickByTag, recorder, writeControlResult } from '../lib/harness'

/**
 * Material3 widgets recorded on their own, before their port counterparts exist. These rows
 * exist so that a framework-ceiling claim about a widget the ladder has not reached yet
 * (for example "Switch is emitted as a nameless button") has a reproducible record behind it.
 * Script: load, Tab, Tab, Space, pointer click.
 */
const controls = [
  { control: 'M3 Switch', route: '/m3-switch', tag: 'sw' },
  { control: 'M3 IconToggleButton', route: '/m3-toggle-button', tag: 'tb' },
  { control: 'M3 Button', route: '/m3-button', tag: 'btn' },
]

for (const c of controls) {
  test(`${c.control} control`, async ({ page }) => {
    const target: Target = { name: 'm3', url: `${COMPOSE_ORIGIN}#${c.route}`, root: '#composeApp' }
    await page.goto(target.url)
    const steps: StepRecord[] = []
    const record = recorder(page, target, steps)
    await record('load')
    await page.keyboard.press('Tab')
    await record('tab1')
    await page.keyboard.press('Tab')
    await record('tab2')
    await page.keyboard.press('Space')
    await record('space')
    await clickByTag(page, target, c.tag)
    await record('click')
    writeControlResult({ control: c.control, route: c.route, recordedAt: new Date().toISOString(), steps })
  })
}
