import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { careerDocumentApi } from '../api/careerDocumentApi'

export const careerDocumentKeys = {
  document: (documentId: string) => ['career-document', documentId] as const,
  analyses: (documentId: string) =>
    ['career-document', documentId, 'analyses'] as const,
}

export function useCareerDocument(documentId: string) {
  return useQuery({
    queryKey: careerDocumentKeys.document(documentId),
    queryFn: () => careerDocumentApi.find(documentId),
  })
}

export function useCareerDocumentAnalyses(documentId: string) {
  return useQuery({
    queryKey: careerDocumentKeys.analyses(documentId),
    queryFn: () => careerDocumentApi.analyses(documentId),
    refetchInterval: (query) => {
      const latest = query.state.data?.[0]
      return latest?.status === 'QUEUED' || latest?.status === 'PROCESSING'
        ? 2_000
        : false
    },
  })
}

export function useUploadCareerDocument() {
  return useMutation({
    mutationFn: (file: File) => careerDocumentApi.upload(file),
  })
}

export function useRequestExtraction() {
  return useMutation({
    mutationFn: (documentId: string) => careerDocumentApi.extract(documentId),
  })
}

export function useAlternativeText(documentId: string) {
  return useAnalysisMutation(documentId, (text: string) =>
    careerDocumentApi.alternativeText(documentId, text),
  )
}

export function useRerunAnalysis(documentId: string) {
  return useAnalysisMutation(documentId, () =>
    careerDocumentApi.rerun(documentId),
  )
}

function useAnalysisMutation<T>(
  documentId: string,
  mutationFn: (value: T) => Promise<unknown>,
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: careerDocumentKeys.analyses(documentId),
      }),
  })
}
