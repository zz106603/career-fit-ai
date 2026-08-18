import { expect, type Page } from '@playwright/test'

export function uniqueEmail(label: string) {
  return `e2e-${label}-${Date.now()}-${Math.random().toString(16).slice(2)}@example.com`
}

export async function signup(page: Page, email: string) {
  await page.goto('/signup')
  await page.getByLabel('이메일').fill(email)
  await page.getByLabel('비밀번호', { exact: true }).fill('E2e-password-123!')
  await page.getByLabel('비밀번호 확인').fill('E2e-password-123!')
  await page.getByRole('button', { name: '회원가입' }).click()
  await expect(
    page.getByRole('heading', { name: /근거로 확인하는/ }),
  ).toBeVisible()
}

export async function uploadPdf(page: Page, name: string, buffer: Buffer) {
  await page.goto('/career-documents/new')
  await page.locator('input[type="file"]').setInputFiles({
    name,
    mimeType: 'application/pdf',
    buffer,
  })
  await page.getByRole('button', { name: '업로드하고 분석 시작' }).click()
  await expect(page).toHaveURL(/\/career-documents\/[0-9a-f-]+$/)
  return page.url()
}

export async function waitForCandidateReview(page: Page) {
  const review = page.getByRole('link', { name: '경력 후보 검토하기' })
  await expect(review).toBeVisible({ timeout: 60_000 })
  await review.click()
  await expect(
    page.getByRole('heading', { name: 'AI 경력 후보 검토' }),
  ).toBeVisible()
}

export async function confirmFirstCandidate(page: Page, title: string) {
  await page.getByRole('button', { name: '이 후보 확정' }).first().click()
  await page.getByLabel(/경험명·프로젝트명/).fill(title)
  await page
    .getByRole('button', { name: '내용과 Evidence를 확인하고 확정' })
    .click()
  await expect(page.getByText('확정됨').first()).toBeVisible({
    timeout: 30_000,
  })
  await page.getByRole('link', { name: '확정 경력 보기' }).click()
  await expect(page.getByRole('heading', { name: title })).toBeVisible()
}
