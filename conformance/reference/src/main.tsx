/**
 * React Aria reference app. Same hash routes and the same visible strings as the Compose demo
 * (components/src/commonMain/kotlin/dev/oxzo/aria/demo/App.kt), so the Playwright harness runs
 * one interaction script against both.
 */
import { useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import {
  Button,
  Checkbox,
  CheckboxGroup,
  Disclosure,
  DisclosurePanel,
  Group,
  Heading,
  Input,
  Label,
  Link,
  Meter,
  NumberField,
  ProgressBar,
  Radio,
  RadioGroup,
  SearchField,
  Separator,
  Switch,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
} from 'react-aria-components'
import type { Key } from 'react-aria-components'

const routes = [
  '/button',
  '/toggle-button',
  '/toggle-button-group',
  '/checkbox',
  '/checkbox-group',
  '/switch',
  '/radio-group',
  '/text-field',
  '/search-field',
  '/number-field',
  '/link',
  '/progress-bar',
  '/meter',
  '/separator',
  '/disclosure',
]

function useHash(): string {
  const [hash, setHash] = useState(window.location.hash)
  useEffect(() => {
    const onChange = () => setHash(window.location.hash)
    window.addEventListener('hashchange', onChange)
    return () => window.removeEventListener('hashchange', onChange)
  }, [])
  return hash
}

function Index() {
  return (
    <>
      <p>kmp-aria reference</p>
      {routes.map((r) => (
        <p key={r}>
          <a href={`#${r}`}>#{r}</a>
        </p>
      ))}
    </>
  )
}

function ButtonDemo() {
  const [count, setCount] = useState(0)
  return (
    <>
      <div style={{ display: 'flex', gap: 12 }}>
        <Button data-testid="btn" onPress={() => setCount((c) => c + 1)}>
          Press me
        </Button>
        <Button data-testid="btn-disabled" isDisabled onPress={() => setCount((c) => c + 1)}>
          Disabled
        </Button>
      </div>
      <p data-testid="count">Pressed {count} times</p>
    </>
  )
}

function ToggleButtonDemo() {
  const [selected, setSelected] = useState(false)
  return (
    <>
      <ToggleButton data-testid="tb" isSelected={selected} onChange={setSelected}>
        Bold
      </ToggleButton>
      <p data-testid="state">{selected ? 'Selected' : 'Not selected'}</p>
    </>
  )
}

function ToggleButtonGroupDemo() {
  const [alignment, setAlignment] = useState<Set<Key>>(new Set())
  const [styles, setStyles] = useState<Set<Key>>(new Set())
  return (
    <>
      <ToggleButtonGroup data-testid="align" aria-label="Text alignment" selectedKeys={alignment} onSelectionChange={setAlignment}>
        <ToggleButton id="left" data-testid="tb-left">
          Left
        </ToggleButton>
        <ToggleButton id="center" data-testid="tb-center">
          Center
        </ToggleButton>
        <ToggleButton id="right" data-testid="tb-right">
          Right
        </ToggleButton>
      </ToggleButtonGroup>
      <ToggleButtonGroup
        data-testid="style"
        aria-label="Text style"
        selectionMode="multiple"
        selectedKeys={styles}
        onSelectionChange={setStyles}
      >
        <ToggleButton id="bold" data-testid="tb-bold">
          Bold
        </ToggleButton>
        <ToggleButton id="italic" data-testid="tb-italic">
          Italic
        </ToggleButton>
        <ToggleButton id="underline" data-testid="tb-underline" isDisabled>
          Underline
        </ToggleButton>
      </ToggleButtonGroup>
      <p data-testid="state">
        Alignment: {[...alignment].join(', ') || 'none'}; Style: {[...styles].join(', ') || 'none'}
      </p>
    </>
  )
}

function CheckboxDemo() {
  const [selected, setSelected] = useState(false)
  return (
    <>
      <Checkbox data-testid="cb" isSelected={selected} onChange={setSelected}>
        Subscribe
      </Checkbox>
      <Checkbox data-testid="cb-mixed" isIndeterminate>
        Select all
      </Checkbox>
      <Checkbox data-testid="cb-disabled" isDisabled>
        Disabled
      </Checkbox>
      <p data-testid="state">{selected ? 'Selected' : 'Not selected'}</p>
    </>
  )
}

function CheckboxGroupDemo() {
  const [interests, setInterests] = useState<string[]>([])
  return (
    <>
      <CheckboxGroup data-testid="group" value={interests} onChange={setInterests}>
        <Label>Interests</Label>
        <Checkbox value="sports" data-testid="cb-sports">
          Sports
        </Checkbox>
        <Checkbox value="music" data-testid="cb-music">
          Music
        </Checkbox>
        <Checkbox value="reading" data-testid="cb-reading" isDisabled>
          Reading
        </Checkbox>
      </CheckboxGroup>
      <p data-testid="state">Selected: {interests.join(', ') || 'none'}</p>
    </>
  )
}

function SwitchDemo() {
  const [on, setOn] = useState(false)
  return (
    <>
      <Switch data-testid="sw" isSelected={on} onChange={setOn}>
        Wi-Fi
      </Switch>
      <p data-testid="state">{on ? 'On' : 'Off'}</p>
    </>
  )
}

function RadioGroupDemo() {
  const [pet, setPet] = useState<string | null>(null)
  return (
    <>
      <RadioGroup data-testid="group" value={pet} onChange={setPet}>
        <Label>Favorite pet</Label>
        <Radio data-testid="r-dog" value="dog">
          Dog
        </Radio>
        <Radio data-testid="r-cat" value="cat">
          Cat
        </Radio>
        <Radio data-testid="r-dragon" value="dragon">
          Dragon
        </Radio>
      </RadioGroup>
      <p data-testid="state">Selected: {pet ?? 'none'}</p>
    </>
  )
}

function TextFieldDemo() {
  const [name, setName] = useState('')
  const [secret, setSecret] = useState('')
  return (
    <>
      <TextField value={name} onChange={setName}>
        <Label>Name</Label>
        <Input data-testid="tf" />
      </TextField>
      <TextField value={secret} onChange={setSecret} type="password">
        <Label>Password</Label>
        <Input data-testid="pw" />
      </TextField>
      <p data-testid="state">Value: {name}</p>
    </>
  )
}

function SearchFieldDemo() {
  const [value, setValue] = useState('')
  const [submitted, setSubmitted] = useState('')
  return (
    <>
      <SearchField value={value} onChange={setValue} onSubmit={setSubmitted}>
        <Label>Search</Label>
        <Input data-testid="sf" />
        {value !== '' && <Button data-testid="clear">✕</Button>}
      </SearchField>
      <p data-testid="state">Value: {value}</p>
      <p data-testid="submitted">Submitted: {submitted}</p>
    </>
  )
}

/** Quantity 0–10, step 1, starting at 5; onChange reports NaN for an empty field. */
function NumberFieldDemo() {
  const [value, setValue] = useState(5)
  return (
    <>
      <NumberField value={value} onChange={setValue} minValue={0} maxValue={10}>
        <Label>Quantity</Label>
        <Group data-testid="group">
          <Button slot="decrement" data-testid="dec">
            -
          </Button>
          <Input data-testid="nf" />
          <Button slot="increment" data-testid="inc">
            +
          </Button>
        </Group>
      </NumberField>
      <p data-testid="state">Value: {Number.isNaN(value) ? 'none' : value}</p>
    </>
  )
}

function LinkDemo() {
  const [count, setCount] = useState(0)
  return (
    <>
      <div style={{ display: 'flex', gap: 12 }}>
        <Link data-testid="lnk" onPress={() => setCount((c) => c + 1)}>
          Follow me
        </Link>
        <Link data-testid="lnk-href" href="https://react-aria.adobe.com/Link" target="_blank">
          Docs
        </Link>
        <Link data-testid="lnk-disabled" isDisabled onPress={() => setCount((c) => c + 1)}>
          Disabled
        </Link>
      </div>
      <p data-testid="count">Followed {count} times</p>
    </>
  )
}

const track = { width: 200, height: 12, border: '1px solid black' }

function ProgressBarDemo() {
  const [value, setValue] = useState(30)
  return (
    <>
      <ProgressBar data-testid="pb" value={value}>
        {({ percentage, valueText }) => (
          <>
            <Label>Loading</Label> <span className="value">{valueText}</span>
            <div style={track}>
              <div style={{ width: `${percentage}%`, height: '100%', background: 'black' }} />
            </div>
          </>
        )}
      </ProgressBar>
      <Button data-testid="adv" onPress={() => setValue((v) => Math.min(100, v + 30))}>
        Advance
      </Button>
      <ProgressBar data-testid="pb-ind" isIndeterminate>
        <Label>Syncing</Label>
        <div style={track}>
          <div style={{ width: '40%', height: '100%', background: 'black' }} />
        </div>
      </ProgressBar>
      <p data-testid="state">Value: {value}</p>
    </>
  )
}

function MeterDemo() {
  const [value, setValue] = useState(25)
  return (
    <>
      <Meter data-testid="meter" value={value}>
        {({ percentage, valueText }) => (
          <>
            <Label>Storage space</Label> <span className="value">{valueText}</span>
            <div style={track}>
              <div style={{ width: `${percentage}%`, height: '100%', background: 'black' }} />
            </div>
          </>
        )}
      </Meter>
      <Button data-testid="fill" onPress={() => setValue((v) => Math.min(100, v + 25))}>
        Fill
      </Button>
      <p data-testid="state">Value: {value}</p>
    </>
  )
}

/** A horizontal separator between two paragraphs and a vertical one between two words. */
function SeparatorDemo() {
  return (
    <>
      <p>Above</p>
      <Separator data-testid="sep" />
      <p>Below</p>
      <div style={{ display: 'flex', gap: 8, height: 24, alignItems: 'center' }}>
        <p>Left</p>
        <Separator data-testid="sep-v" orientation="vertical" style={{ width: 1, alignSelf: 'stretch', background: 'black' }} />
        <p>Right</p>
      </div>
    </>
  )
}

function DisclosureDemo() {
  const [open, setOpen] = useState(false)
  return (
    <>
      <Disclosure data-testid="disc" isExpanded={open} onExpandedChange={setOpen}>
        <Heading>
          <Button slot="trigger" data-testid="trig">
            System Requirements
          </Button>
        </Heading>
        <DisclosurePanel data-testid="panel">
          <p>Details about system requirements here.</p>
        </DisclosurePanel>
      </Disclosure>
      <p data-testid="state">{open ? 'Expanded' : 'Collapsed'}</p>
    </>
  )
}

function App() {
  const route = useHash().replace(/^#/, '')
  switch (route) {
    case '/button':
      return <ButtonDemo />
    case '/toggle-button':
      return <ToggleButtonDemo />
    case '/toggle-button-group':
      return <ToggleButtonGroupDemo />
    case '/checkbox':
      return <CheckboxDemo />
    case '/checkbox-group':
      return <CheckboxGroupDemo />
    case '/switch':
      return <SwitchDemo />
    case '/radio-group':
      return <RadioGroupDemo />
    case '/text-field':
      return <TextFieldDemo />
    case '/search-field':
      return <SearchFieldDemo />
    case '/number-field':
      return <NumberFieldDemo />
    case '/link':
      return <LinkDemo />
    case '/progress-bar':
      return <ProgressBarDemo />
    case '/meter':
      return <MeterDemo />
    case '/separator':
      return <SeparatorDemo />
    case '/disclosure':
      return <DisclosureDemo />
    default:
      return <Index />
  }
}

createRoot(document.getElementById('root')!).render(<App />)
