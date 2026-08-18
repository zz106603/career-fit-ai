import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { careerCandidateKey } from '../../career-candidate/model/careerCandidateQueries'
import {
  careerExperienceApi,
  type CareerConfirmationContent,
} from '../api/careerExperienceApi'

export const confirmedCareersKey = ['confirmed-careers'] as const

export function useConfirmedCareers() {
  return useQuery({
    queryKey: confirmedCareersKey,
    queryFn: () => careerExperienceApi.findCurrent(),
  })
}

export function useConfirmedCareerVersions(experienceId: string) {
  return useQuery({
    queryKey: [...confirmedCareersKey, experienceId],
    queryFn: () => careerExperienceApi.findVersions(experienceId),
    enabled: Boolean(experienceId),
  })
}

export function useConfirmCareerCandidate(analysisId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (value: {
      candidateId: string
      content: CareerConfirmationContent
    }) =>
      careerExperienceApi.confirmCandidate(value.candidateId, value.content),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: careerCandidateKey(analysisId),
        }),
        queryClient.invalidateQueries({ queryKey: confirmedCareersKey }),
      ])
    },
    onError: () =>
      queryClient.invalidateQueries({
        queryKey: careerCandidateKey(analysisId),
      }),
  })
}
