import { defineConfig } from '@playwright/test'

/**
 * Two servers: the React Aria reference (Vite, :5173) and the Compose production
 * distribution (python http.server, :8081). The Compose build must exist first:
 *   gradle :components:wasmJsBrowserDistribution
 */
export default defineConfig({
  testDir: './components',
  timeout: 90_000,
  retries: 0,
  workers: 1,
  fullyParallel: false,
  reporter: [['list']],
  use: {
    browserName: 'chromium',
    headless: true,
    viewport: { width: 1024, height: 768 },
  },
  webServer: [
    {
      command: 'npm run reference',
      url: 'http://localhost:5173/',
      reuseExistingServer: true,
      timeout: 60_000,
    },
    {
      command: 'npm run dist',
      url: 'http://localhost:8081/index.html',
      reuseExistingServer: true,
      timeout: 30_000,
    },
  ],
})
