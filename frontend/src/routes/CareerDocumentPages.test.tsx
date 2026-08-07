import { QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { HttpResponse, http } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import { createQueryClient } from '../app/queryClient'
import { server } from '../test/server'
import { CareerDocumentAnalysisPage } from './CareerDocumentAnalysisPage'
import { CareerDocumentUploadPage } from './CareerDocumentUploadPage'

function renderPage(path: string) {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route
            path="/career-documents/new"
            element={<CareerDocumentUploadPage />}
          />
          <Route
            path="/career-documents/:documentId"
            element={<CareerDocumentAnalysisPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('경력 문서 화면', () => {
  it('PDF가 아닌 파일은 업로드 전에 안내한다', () => {
    renderPage('/career-documents/new')

    fireEvent.change(screen.getByLabelText('PDF 선택'), {
      target: {
        files: [new File(['text'], 'resume.txt', { type: 'text/plain' })],
      },
    })

    expect(
      screen.getByText('PDF 형식의 파일만 업로드할 수 있습니다.'),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: '업로드하고 분석 시작' }),
    ).toBeDisabled()
  })

  it('PDF 추출 실패 시 대체 텍스트 행동만 표시하고 진행률은 표시하지 않는다', async () => {
    server.use(
      http.get('/api/career-documents/document-1', () =>
        HttpResponse.json({
          documentId: 'document-1',
          originalName: 'resume.pdf',
          byteSize: 1024,
          pageCount: 2,
          uploadedAt: '2026-08-07T01:00:00Z',
        }),
      ),
      http.get('/api/career-documents/document-1/analyses', () =>
        HttpResponse.json([
          {
            documentId: 'document-1',
            documentAnalysisId: 'analysis-1',
            jobExecutionId: 'job-1',
            inputKind: 'PDF',
            inputVersion: 'v1',
            workflowVersion: 'v1',
            status: 'FAILED',
            failureCode: 'PDF_TEXT_EXTRACTION_FAILED',
            nextAction: 'ENTER_ALTERNATIVE_TEXT',
            createdAt: '2026-08-07T01:00:00Z',
            startedAt: '2026-08-07T01:00:01Z',
            completedAt: '2026-08-07T01:00:02Z',
          },
        ]),
      ),
    )

    renderPage('/career-documents/document-1')

    expect(await screen.findByLabelText('대체 경력 텍스트')).toBeInTheDocument()
    expect(
      screen.getByText('실패 코드: PDF_TEXT_EXTRACTION_FAILED'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/%/)).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: '전체 분석 재실행' }),
    ).not.toBeInTheDocument()
  })
})
