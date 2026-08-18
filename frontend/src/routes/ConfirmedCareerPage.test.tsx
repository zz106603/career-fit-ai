import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { HttpResponse, http } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import { createQueryClient } from '../app/queryClient'
import { server } from '../test/server'
import { ConfirmedCareerPage } from './ConfirmedCareerPage'

const current = {
  experienceId: 'experience-1',
  versionId: 'version-2',
  versionNo: 2,
  sourceType: 'DOCUMENT',
  current: true,
  createdAt: '2026-08-18T00:00:00Z',
  confirmedAt: '2026-08-18T00:00:00Z',
  supersededAt: null,
  experienceType: 'PROJECT',
  title: '결제 API 개선',
  organization: '테스트 회사',
  startDate: '2025-01-01',
  endDate: '2025-12-31',
  role: '백엔드 개발자',
  responsibilities: 'API를 개선했습니다.',
  problem: null,
  action: null,
  outcome: null,
  technologies: 'Java, Spring Boot',
  evidences: [
    {
      documentId: 'document-1',
      documentName: 'resume.pdf',
      pageNumber: 2,
      excerpt: '결제 API 개선',
    },
  ],
}

function renderPage(path = '/career-experiences') {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/career-experiences" element={<ConfirmedCareerPage />} />
          <Route
            path="/career-experiences/:experienceId"
            element={<ConfirmedCareerPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('확정 경력 화면', () => {
  it('현재 확정 버전과 문서 Evidence를 표시한다', async () => {
    server.use(
      http.get('/api/career-experiences', () => HttpResponse.json([current])),
    )
    renderPage()

    expect(
      await screen.findByRole('heading', { name: '결제 API 개선' }),
    ).toBeInTheDocument()
    expect(screen.getByText('현재 버전')).toBeInTheDocument()
    expect(screen.getByText('문서 기반')).toBeInTheDocument()
    expect(screen.getByText('resume.pdf · 2페이지')).toBeInTheDocument()
  })

  it('상세 화면에서 현재 버전과 과거 버전을 구분한다', async () => {
    server.use(
      http.get('/api/career-experiences/experience-1', () =>
        HttpResponse.json([
          current,
          {
            ...current,
            versionId: 'version-1',
            versionNo: 1,
            current: false,
            supersededAt: '2026-08-18T00:00:00Z',
          },
        ]),
      ),
    )
    renderPage('/career-experiences/experience-1')

    expect(await screen.findByText('현재 버전')).toBeInTheDocument()
    expect(screen.getByText('과거 버전')).toBeInTheDocument()
    expect(screen.getByText(/version 2/)).toBeInTheDocument()
    expect(screen.getByText(/version 1/)).toBeInTheDocument()
  })
})
