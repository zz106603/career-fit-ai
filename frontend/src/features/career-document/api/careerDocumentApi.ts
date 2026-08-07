import { apiRequest } from '../../../shared/api'

export interface CareerDocument {
  documentId: string
  originalName: string
  byteSize: number
  pageCount: number
  uploadedAt: string
}

export type AnalysisStatus = 'QUEUED' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED'
export type AnalysisNextAction =
  | 'WAIT'
  | 'REVIEW_CANDIDATES'
  | 'ENTER_ALTERNATIVE_TEXT'
  | 'RETRY_FULL_ANALYSIS'

export interface CareerDocumentAnalysis {
  documentId: string
  documentAnalysisId: string
  jobExecutionId: string | null
  inputKind: 'PDF' | 'ALTERNATIVE_TEXT'
  inputVersion: string
  workflowVersion: string
  status: AnalysisStatus
  failureCode: string | null
  nextAction: AnalysisNextAction
  createdAt: string
  startedAt: string | null
  completedAt: string | null
}

export const careerDocumentApi = {
  upload(file: File) {
    const form = new FormData()
    form.append('file', file)
    return apiRequest<CareerDocument>('/api/career-documents', {
      method: 'POST',
      body: form,
    })
  },
  find(documentId: string) {
    return apiRequest<CareerDocument>(`/api/career-documents/${documentId}`)
  },
  analyses(documentId: string) {
    return apiRequest<CareerDocumentAnalysis[]>(
      `/api/career-documents/${documentId}/analyses`,
    )
  },
  extract(documentId: string) {
    return apiRequest(`/api/career-documents/${documentId}/extractions`, {
      method: 'POST',
    })
  },
  alternativeText(documentId: string, text: string) {
    return apiRequest(`/api/career-documents/${documentId}/alternative-texts`, {
      method: 'POST',
      json: { text },
    })
  },
  rerun(documentId: string) {
    return apiRequest(`/api/career-documents/${documentId}/analyses/reruns`, {
      method: 'POST',
    })
  },
}
