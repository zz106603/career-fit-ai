import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  careerCandidateApi,
  type CareerCandidateContent,
} from '../api/careerCandidateApi'

export const careerCandidateKey = (analysisId: string) =>
  ['career-candidates', analysisId] as const

export function useCareerCandidates(analysisId: string) {
  return useQuery({
    queryKey: careerCandidateKey(analysisId),
    queryFn: () => careerCandidateApi.findAll(analysisId),
  })
}

export function useEditCareerCandidate(analysisId: string) {
  return useCandidateMutation(
    analysisId,
    (value: { candidateId: string; content: CareerCandidateContent }) =>
      careerCandidateApi.edit(value.candidateId, value.content),
  )
}

export function useRejectCareerCandidate(analysisId: string) {
  return useCandidateMutation(analysisId, (candidateId: string) =>
    careerCandidateApi.reject(candidateId),
  )
}

function useCandidateMutation<T>(
  analysisId: string,
  mutationFn: (value: T) => Promise<unknown>,
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: careerCandidateKey(analysisId),
      }),
  })
}
