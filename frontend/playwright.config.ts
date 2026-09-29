import { defineConfig } from "@playwright/test"

export default defineConfig({
  testDir: "./tests/orders",
  timeout: 60_000,
  expect: { timeout: 15_000 },
  workers: 1,
  use: {
    baseURL: process.env.ORDER_UI_TEST_URL || "http://localhost:3100",
    channel: process.env.PLAYWRIGHT_CHANNEL || "chromium",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    { name: "desktop", use: { viewport: { width: 1920, height: 1080 } } },
    { name: "mobile", use: { viewport: { width: 320, height: 740 } } },
  ],
  webServer: {
    command: "node node_modules/next/dist/bin/next dev --port 3100",
    url: "http://localhost:3100/health",
    reuseExistingServer: true,
    timeout: 120_000,
    env: { FOC_ORDER_DEV_URL: process.env.FOC_ORDER_DEV_URL || "http://127.0.0.1:8083" },
  },
})
