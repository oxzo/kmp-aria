/**
 * React Aria reference app. Same hash routes and the same visible strings as the Compose demo
 * (components/src/commonMain/kotlin/dev/oxzo/aria/demo/App.kt), so the Playwright harness runs
 * one interaction script against both.
 */
import { useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { Button, ToggleButton } from 'react-aria-components'

const routes = ['/button', '/toggle-button']

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

function App() {
  const route = useHash().replace(/^#/, '')
  switch (route) {
    case '/button':
      return <ButtonDemo />
    case '/toggle-button':
      return <ToggleButtonDemo />
    default:
      return <Index />
  }
}

createRoot(document.getElementById('root')!).render(<App />)
