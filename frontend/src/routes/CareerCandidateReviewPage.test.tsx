import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import { createQueryClient } from '../app/queryClient'
import { authHandlers } from '../test/authHandlers'
import { server } from '../test/server'
import { CareerCandidateReviewPage } from './CareerCandidateReviewPage'

const candidate = {
  candidateId: 'candidate-1',
  candidateType: 'WORK',
  organization: '테스트 회사',
  role: '백엔드 개발자',
  period: '2024-01 ~ 2025-01',
  description: 'API를 개발했습니다.',
  status: 'PENDING_REVIEW',
  revisionNo: 1,
  evidences: [
    {
      documentId: 'document-1',
      documentName: 'resume.pdf',
      pageNumber: 2,
      excerpt: 'API 개발 및 운영',
    },
  ],
}

function renderPage() {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <MemoryRouter
        initialEntries={[
          '/career-documents/document-1/analyses/analysis-1/candidates',
        ]}
      >
        <Routes>
          <Route
            path="/career-documents/:documentId/analyses/:analysisId/candidates"
            element={<CareerCandidateReviewPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function useSuccessHandlers() {
  server.use(
    http.get('/api/career-documents/document-1/analyses', () =>
      HttpResponse.json([
        {
          documentAnalysisId: 'analysis-1',
          status: 'SUCCEEDED',
          nextAction: 'REVIEW_CANDIDATES',
        },
      ]),
    ),
    http.get('/api/career-candidates', () => HttpResponse.json([candidate])),
  )
}

describe('경력 후보 검토 화면', () => {
  it('미확정 후보와 문서·페이지·발췌 Evidence를 함께 표시한다', async () => {
    useSuccessHandlers()
    renderPage()

    expect(
      await screen.findByText('테스트 회사', { exact: false }),
    ).toBeInTheDocument()
    expect(screen.getByText('resume.pdf · 2페이지')).toBeInTheDocument()
    expect(screen.getByText('API 개발 및 운영')).toBeInTheDocument()
    expect(screen.getByText('revision 1 · 미확정')).toBeInTheDocument()
  })

  it('후보 수정 결과를 저장하고 다시 미확정 목록을 조회한다', async () => {
    const user = userEvent.setup()
    let savedDescription = ''
    useSuccessHandlers()
    server.use(
      authHandlers.csrf(),
      http.patch('/api/career-candidates/candidate-1', async ({ request }) => {
        const body = (await request.json()) as { description: string }
        savedDescription = body.description
        return HttpResponse.json({ ...candidate, ...body, status: 'EDITED' })
      }),
    )
    renderPage()

    await user.click(await screen.findByRole('button', { name: '수정' }))
    const description = screen.getByLabelText(/업무·성과·기술/)
    await user.clear(description)
    await user.type(description, '사용자가 확인한 API 개발')
    await user.click(screen.getByRole('button', { name: '수정 저장' }))

    expect(savedDescription).toBe('사용자가 확인한 API 개발')
    expect(
      await screen.findByRole('button', { name: '수정' }),
    ).toBeInTheDocument()
  })
})
