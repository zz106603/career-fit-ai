import {
  Alert,
  Button,
  Chip,
  CircularProgress,
  Divider,
  List,
  ListItem,
  ListItemText,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import {
  type CareerDocumentAnalysis,
  useAlternativeText,
  useCareerDocument,
  useCareerDocumentAnalyses,
  useRerunAnalysis,
} from '../features/career-document'
import { getApiErrorMessage } from '../shared/api/getApiErrorMessage'
import { PageContainer } from '../shared/ui/PageContainer'

export function CareerDocumentAnalysisPage() {
  const { documentId = '' } = useParams()
  const document = useCareerDocument(documentId)
  const analyses = useCareerDocumentAnalyses(documentId)

  if (document.isPending || analyses.isPending) {
    return (
      <StatusMessage message="서버에 저장된 분석 상태를 불러오는 중입니다." />
    )
  }
  if (document.isError || analyses.isError) {
    return (
      <StatusMessage
        severity="error"
        message={getApiErrorMessage(document.error ?? analyses.error)}
      />
    )
  }

  const latest = analyses.data[0]
  return (
    <PageContainer>
      <Stack spacing={3}>
        <Stack spacing={1}>
          <Typography component="h1" variant="h4" sx={{ fontWeight: 700 }}>
            경력 분석 상태
          </Typography>
          <Typography>{document.data.originalName}</Typography>
          <Typography color="text.secondary">
            {formatBytes(document.data.byteSize)} · {document.data.pageCount}쪽
            · {formatDate(document.data.uploadedAt)} 업로드
          </Typography>
        </Stack>
        {latest ? (
          <CurrentAnalysis documentId={documentId} analysis={latest} />
        ) : (
          <Alert severity="info">아직 요청된 분석이 없습니다.</Alert>
        )}
        <Divider />
        <AnalysisHistory analyses={analyses.data} />
        <Button component={Link} to="/career-documents/new" variant="outlined">
          다른 PDF 업로드
        </Button>
      </Stack>
    </PageContainer>
  )
}

function CurrentAnalysis({
  documentId,
  analysis,
}: {
  documentId: string
  analysis: CareerDocumentAnalysis
}) {
  const [alternativeText, setAlternativeText] = useState('')
  const alternative = useAlternativeText(documentId)
  const rerun = useRerunAnalysis(documentId)
  const pending =
    analysis.status === 'QUEUED' || analysis.status === 'PROCESSING'
  const mutationError = alternative.error ?? rerun.error

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
        {pending && <CircularProgress size={20} />}
        <Chip
          label={statusLabel(analysis.status)}
          color={statusColor(analysis.status)}
        />
        <Typography>{nextActionMessage(analysis.nextAction)}</Typography>
      </Stack>
      {analysis.failureCode && (
        <Alert severity="warning">실패 코드: {analysis.failureCode}</Alert>
      )}
      {mutationError && (
        <Alert severity="error">{getApiErrorMessage(mutationError)}</Alert>
      )}
      {analysis.nextAction === 'ENTER_ALTERNATIVE_TEXT' && (
        <Stack spacing={2}>
          <TextField
            label="대체 경력 텍스트"
            value={alternativeText}
            onChange={(event) => setAlternativeText(event.target.value)}
            multiline
            minRows={6}
            helperText="PDF에서 추출하지 못한 경력 내용을 직접 입력해 주세요."
          />
          <Button
            variant="contained"
            disabled={!alternativeText.trim() || alternative.isPending}
            onClick={() => alternative.mutate(alternativeText.trim())}
          >
            {alternative.isPending ? '제출 중…' : '대체 텍스트로 분석'}
          </Button>
        </Stack>
      )}
      {analysis.nextAction === 'RETRY_FULL_ANALYSIS' && (
        <Button
          variant="contained"
          disabled={rerun.isPending}
          onClick={() => {
            if (window.confirm('전체 분석을 다시 실행할까요?'))
              rerun.mutate(undefined)
          }}
        >
          {rerun.isPending ? '재실행 요청 중…' : '전체 분석 재실행'}
        </Button>
      )}
      {analysis.nextAction === 'REVIEW_CANDIDATES' && (
        <Alert severity="success">
          경력 후보 검토 단계로 이동할 준비가 됐습니다.
        </Alert>
      )}
    </Stack>
  )
}

function AnalysisHistory({ analyses }: { analyses: CareerDocumentAnalysis[] }) {
  return (
    <Stack spacing={1}>
      <Typography component="h2" variant="h6">
        분석 이력
      </Typography>
      {analyses.length === 0 ? (
        <Typography color="text.secondary">분석 이력이 없습니다.</Typography>
      ) : (
        <List disablePadding>
          {analyses.map((analysis) => (
            <ListItem key={analysis.documentAnalysisId} divider>
              <ListItemText
                primary={`${statusLabel(analysis.status)} · ${inputLabel(analysis.inputKind)}`}
                secondary={`${formatDate(analysis.createdAt)}${analysis.failureCode ? ` · ${analysis.failureCode}` : ''}`}
              />
            </ListItem>
          ))}
        </List>
      )}
    </Stack>
  )
}

function StatusMessage({
  message,
  severity = 'info',
}: {
  message: string
  severity?: 'info' | 'error'
}) {
  return (
    <PageContainer>
      <Alert severity={severity}>{message}</Alert>
    </PageContainer>
  )
}

function statusLabel(status: CareerDocumentAnalysis['status']) {
  return {
    QUEUED: '대기 중',
    PROCESSING: '분석 중',
    SUCCEEDED: '분석 완료',
    FAILED: '분석 실패',
  }[status]
}

function statusColor(
  status: CareerDocumentAnalysis['status'],
): 'default' | 'info' | 'success' | 'error' {
  switch (status) {
    case 'QUEUED':
      return 'default'
    case 'PROCESSING':
      return 'info'
    case 'SUCCEEDED':
      return 'success'
    case 'FAILED':
      return 'error'
  }
}

function nextActionMessage(action: CareerDocumentAnalysis['nextAction']) {
  return {
    WAIT: '분석이 끝날 때까지 기다려 주세요.',
    REVIEW_CANDIDATES: '추출된 경력 후보를 검토해 주세요.',
    ENTER_ALTERNATIVE_TEXT: 'PDF 대신 사용할 경력 텍스트를 입력해 주세요.',
    RETRY_FULL_ANALYSIS: '전체 분석을 다시 실행할 수 있습니다.',
  }[action]
}

function inputLabel(inputKind: CareerDocumentAnalysis['inputKind']) {
  return inputKind === 'PDF' ? 'PDF' : '대체 텍스트'
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatBytes(bytes: number) {
  return `${(bytes / 1024 / 1024).toFixed(1)} MiB`
}
