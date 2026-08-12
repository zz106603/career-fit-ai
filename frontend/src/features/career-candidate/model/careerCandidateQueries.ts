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

export function useMergeCareerCandidates(analysisId: string) {
  return useCandidateMutation(
    analysisId,
    (value: { candidateIds: string[]; content: CareerCandidateContent }) =>
      careerCandidateApi.merge(value.candidateIds, value.content),
  )
}

export function useSplitCareerCandidate(analysisId: string) {
  return useCandidateMutation(
    analysisId,
    (value: { candidateId: string; contents: CareerCandidateContent[] }) =>
      careerCandidateApi.split(value.candidateId, value.contents),
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
    // 충돌 시 서버 상태가 더 최신이므로 목록을 다시 받아 현재 후보 상태를 복구한다.
    onError: () =>
      queryClient.invalidateQueries({
        queryKey: careerCandidateKey(analysisId),
      }),
  })
}
