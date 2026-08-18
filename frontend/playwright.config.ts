import { defineConfig, devices } from '@playwright/test'

const isCi = Boolean(process.env.CI)
const backendCommand =
  process.platform === 'win32'
    ? '..\\backend\\gradlew.bat -p ../backend bootRun --args="--spring.profiles.active=local"'
    : '../backend/gradlew -p ../backend bootRun --args="--spring.profiles.active=local"'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: isCi,
  retries: isCi ? 1 : 0,
  timeout: 90_000,
  reporter: [['html', { open: 'never' }]],
  use: {
    baseURL: 'http://127.0.0.1:5173',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: backendCommand,
      url: 'http://127.0.0.1:8080/api/auth/csrf',
      reuseExistingServer: !isCi,
      timeout: 120_000,
      env: {
        DB_URL:
          process.env.E2E_DB_URL ??
          'jdbc:postgresql://127.0.0.1:5432/career_fit_e2e',
        DB_USERNAME: process.env.E2E_DB_USERNAME ?? 'career_fit_e2e',
        DB_PASSWORD: process.env.E2E_DB_PASSWORD ?? 'career-fit-e2e',
        FILE_STORAGE_ROOT:
          process.env.E2E_FILE_STORAGE_ROOT ?? '../build/e2e-files',
        AI_PROVIDER: 'fake',
        ASYNC_DISPATCHER_ENABLED: 'true',
        ASYNC_DISPATCHER_FIXED_DELAY: '200ms',
      },
    },
    {
      command: 'pnpm dev --host 0.0.0.0',
      url: 'http://127.0.0.1:5173',
      reuseExistingServer: !isCi,
      timeout: 60_000,
    },
  ],
})
