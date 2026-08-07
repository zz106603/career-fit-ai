import { apiRequest } from '../../../shared/api'

export interface CareerCandidateEvidence {
  documentId: string
  documentName: string
  pageNumber: number
  excerpt: string
}

export interface CareerCandidate {
  candidateId: string
  candidateType: string
  organization: string | null
  role: string | null
  period: string | null
  description: string
  status: 'PENDING_REVIEW' | 'EDITED' | 'CONFIRMED'
  revisionNo: number
  evidences: CareerCandidateEvidence[]
}

export type CareerCandidateContent = Pick<
  CareerCandidate,
  'candidateType' | 'description'
> & {
  organization: string
  role: string
  period: string
}

export const careerCandidateApi = {
  findAll(analysisId: string) {
    return apiRequest<CareerCandidate[]>(
      `/api/career-candidates?${new URLSearchParams({ analysisId })}`,
    )
  },
  edit(candidateId: string, content: CareerCandidateContent) {
    return apiRequest(`/api/career-candidates/${candidateId}`, {
      method: 'PATCH',
      json: content,
    })
  },
  reject(candidateId: string) {
    return apiRequest<void>(`/api/career-candidates/${candidateId}`, {
      method: 'DELETE',
    })
  },
}
