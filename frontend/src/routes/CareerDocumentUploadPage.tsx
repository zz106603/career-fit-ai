import { Alert, Button, Stack, Typography } from '@mui/material'
import { useState, type ChangeEvent } from 'react'
import { useNavigate } from 'react-router-dom'

import {
  useRequestExtraction,
  useUploadCareerDocument,
} from '../features/career-document'
import { getApiErrorMessage } from '../shared/api/getApiErrorMessage'
import { PageContainer } from '../shared/ui/PageContainer'

const MAX_PDF_SIZE = 10 * 1024 * 1024

export function CareerDocumentUploadPage() {
  const [file, setFile] = useState<File | null>(null)
  const [validationMessage, setValidationMessage] = useState<string | null>(
    null,
  )
  const upload = useUploadCareerDocument()
  const extraction = useRequestExtraction()
  const navigate = useNavigate()
  const isPending = upload.isPending || extraction.isPending

  const selectFile = (event: ChangeEvent<HTMLInputElement>) => {
    const selected = event.target.files?.[0] ?? null
    const message = validatePdf(selected)
    setValidationMessage(message)
    setFile(message ? null : selected)
  }

  const submit = () => {
    if (!file || isPending) return
    upload.mutate(file, {
      onSuccess: (document) => {
        extraction.mutate(document.documentId, {
          onSuccess: () => {
            void navigate(`/career-documents/${document.documentId}`)
          },
        })
      },
    })
  }

  const error = upload.error ?? extraction.error

  return (
    <PageContainer>
      <Stack spacing={3}>
        <Stack spacing={1}>
          <Typography component="h1" variant="h4" sx={{ fontWeight: 700 }}>
            경력 PDF 업로드
          </Typography>
          <Typography color="text.secondary">
            PDF 파일만 업로드할 수 있으며 최대 크기는 10 MiB, 최대 페이지는
            50쪽입니다. 페이지 수와 파일 유효성은 서버에서 최종 확인합니다.
          </Typography>
        </Stack>
        {(validationMessage || error) && (
          <Alert severity="error">
            {validationMessage ?? getApiErrorMessage(error)}
          </Alert>
        )}
        <Button component="label" variant="outlined" disabled={isPending}>
          PDF 선택
          <input
            hidden
            type="file"
            accept="application/pdf,.pdf"
            onChange={selectFile}
          />
        </Button>
        <Typography>{file ? file.name : '선택된 파일이 없습니다.'}</Typography>
        <Button
          variant="contained"
          onClick={submit}
          disabled={!file || isPending}
        >
          {isPending ? '업로드 및 분석 요청 중…' : '업로드하고 분석 시작'}
        </Button>
      </Stack>
    </PageContainer>
  )
}

function validatePdf(file: File | null) {
  if (!file) return 'PDF 파일을 선택해 주세요.'
  if (
    file.type !== 'application/pdf' &&
    !file.name.toLowerCase().endsWith('.pdf')
  ) {
    return 'PDF 형식의 파일만 업로드할 수 있습니다.'
  }
  if (file.size > MAX_PDF_SIZE) return 'PDF는 10 MiB 이하여야 합니다.'
  return null
}
