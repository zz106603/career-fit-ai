import { expect, test } from '@playwright/test'

import { careerPdf, emptyTextPdf } from './support/pdfFixture'
import {
  confirmFirstCandidate,
  signup,
  uniqueEmail,
  uploadPdf,
  waitForCandidateReview,
} from './support/userFlow'

test('회원가입부터 PDF 후보 확정과 새로고침 복원까지 완료한다', async ({
  page,
}) => {
  await signup(page, uniqueEmail('normal'))
  await uploadPdf(page, 'career.pdf', careerPdf())
  await waitForCandidateReview(page)

  await page.getByRole('button', { name: '수정' }).first().click()
  await page.getByLabel(/업무·성과·기술/).fill('Java Spring Boot API 개발')
  await page.getByRole('button', { name: '수정 저장' }).click()
  await confirmFirstCandidate(page, '백엔드 API 개발')

  await page.reload()
  await expect(
    page.getByRole('heading', { name: '백엔드 API 개발' }),
  ).toBeVisible()
  await expect(page.getByText('현재 버전')).toBeVisible()
})

test('빈 PDF 실패를 대체 텍스트로 복구하고 과거 분석 이력을 보존한다', async ({
  page,
}) => {
  await signup(page, uniqueEmail('recovery'))
  const documentUrl = await uploadPdf(page, 'empty.pdf', emptyTextPdf())

  await expect(page.getByText('실패 코드: PDF_TEXT_EMPTY')).toBeVisible({
    timeout: 60_000,
  })
  await page
    .getByLabel('대체 경력 텍스트')
    .fill('Java Spring Boot 기반 백엔드 API를 개발했습니다.')
  await page.getByRole('button', { name: '대체 텍스트로 분석' }).click()
  await waitForCandidateReview(page)
  await confirmFirstCandidate(page, '대체 텍스트 경력')

  const documentId = new URL(documentUrl).pathname.split('/').at(-1)
  const csrf = (await page.request
    .get('/api/auth/csrf')
    .then((response) => response.json())) as {
    headerName: string
    token: string
  }
  const rerun = await page.request.post(
    `/api/career-documents/${documentId}/analyses/reruns`,
    { headers: { [csrf.headerName]: csrf.token } },
  )
  expect(rerun.ok()).toBeTruthy()
  await page.goto(documentUrl)
  await expect(
    page.getByRole('link', { name: '경력 후보 검토하기' }),
  ).toBeVisible({
    timeout: 60_000,
  })
  await expect(page.getByText(/PDF_TEXT_EMPTY/)).toBeVisible()
  await expect(page.getByText(/대체 텍스트/).first()).toBeVisible()
})

test('다른 사용자 문서 접근과 인증 만료를 차단한다', async ({
  page,
  context,
}) => {
  await signup(page, uniqueEmail('owner'))
  const ownerDocumentUrl = await uploadPdf(page, 'private.pdf', careerPdf())

  await page.goto('/')
  await page.getByRole('button', { name: '로그아웃' }).click()
  await signup(page, uniqueEmail('other'))
  await page.goto(ownerDocumentUrl)
  await expect(page.getByRole('alert')).toContainText('찾을 수 없습니다')

  await context.clearCookies()
  await page.reload()
  await expect(page).toHaveURL(/\/login$/)
})
