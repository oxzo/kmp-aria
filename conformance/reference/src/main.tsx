/**
 * React Aria reference app. Same hash routes and the same visible strings as the Compose demo
 * (components/src/commonMain/kotlin/dev/oxzo/aria/demo/App.kt), so the Playwright harness runs
 * one interaction script against both.
 */
import { useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { Button, Checkbox, Input, Label, Radio, RadioGroup, Switch, TextField, ToggleButton } from 'react-aria-components'

const routes = ['/button', '/toggle-button', '/checkbox', '/switch', '/radio-group', '/text-field']

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

function App() {
  const route = useHash().replace(/^#/, '')
  switch (route) {
    case '/button':
      return <ButtonDemo />
    case '/toggle-button':
      return <ToggleButtonDemo />
    case '/checkbox':
      return <CheckboxDemo />
    case '/switch':
      return <SwitchDemo />
    case '/radio-group':
      return <RadioGroupDemo />
    case '/text-field':
      return <TextFieldDemo />
    default:
      return <Index />
  }
}

createRoot(document.getElementById('root')!).render(<App />)
