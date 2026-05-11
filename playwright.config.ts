import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 30_000,
  retries: 1,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:3000',
    // Backend is proxied through nginx at /api/
    extraHTTPHeaders: {
      Accept: 'application/json',
    },
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: 'cd backend && FRONTEND_URL=http://localhost:3000 ./mvnw spring-boot:run',
      url: 'http://localhost:8080/actuator/health',
      reuseExistingServer: true,
      timeout: 240_000,
    },
    {
      command: 'cd frontend && npm run build && npm run preview -- --host localhost --port 3000',
      url: 'http://localhost:3000/login',
      reuseExistingServer: true,
      timeout: 180_000,
    },
  ],
});
