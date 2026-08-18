import { apiRequest } from '../../../shared/api'

export interface ConfirmedCareerEvidence {
  documentId: string
  documentName: string
  pageNumber: number
  excerpt: string
}

export interface ConfirmedCareerVersion {
  experienceId: string
  versionId: string
  versionNo: number
  sourceType: 'DOCUMENT' | 'DIRECT'
  current: boolean
  createdAt: string
  confirmedAt: string
  supersededAt: string | null
  experienceType: string | null
  title: string
  organization: string | null
  startDate: string | null
  endDate: string | null
  role: string | null
  responsibilities: string | null
  problem: string | null
  action: string | null
  outcome: string | null
  technologies: string | null
  evidences: ConfirmedCareerEvidence[]
}

export interface CareerConfirmationContent {
  experienceType: string
  title: string
  organization: string
  startDate: string | null
  endDate: string | null
  role: string
  responsibilities: string
  problem: string
  action: string
  outcome: string
  technologies: string
}

export const careerExperienceApi = {
  findCurrent() {
    return apiRequest<ConfirmedCareerVersion[]>('/api/career-experiences')
  },
  findVersions(experienceId: string) {
    return apiRequest<ConfirmedCareerVersion[]>(
      `/api/career-experiences/${experienceId}`,
    )
  },
  confirmCandidate(candidateId: string, content: CareerConfirmationContent) {
    return apiRequest(`/api/career-candidates/${candidateId}/confirmations`, {
      method: 'POST',
      json: content,
    })
  },
}
