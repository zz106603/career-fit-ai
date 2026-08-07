import {
  Alert,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useState, type ChangeEvent } from 'react'
import { Link, useParams } from 'react-router-dom'

import {
  type CareerCandidate,
  type CareerCandidateContent,
  useCareerCandidates,
  useEditCareerCandidate,
  useRejectCareerCandidate,
} from '../features/career-candidate'
import { useCareerDocumentAnalyses } from '../features/career-document'
import { getApiErrorMessage } from '../shared/api/getApiErrorMessage'
import { PageContainer } from '../shared/ui/PageContainer'

export function CareerCandidateReviewPage() {
  const { documentId = '', analysisId = '' } = useParams()
  const analyses = useCareerDocumentAnalyses(documentId)
  const candidates = useCareerCandidates(analysisId)
  const analysis = analyses.data?.find(
    (item) => item.documentAnalysisId === analysisId,
  )

  if (analyses.isPending || candidates.isPending) {
    return (
      <Message severity="info" text="경력 후보와 근거를 불러오는 중입니다." />
    )
  }
  if (analyses.isError || candidates.isError) {
    return (
      <Message
        severity="error"
        text={getApiErrorMessage(analyses.error ?? candidates.error)}
      />
    )
  }
  if (!analysis || analysis.status !== 'SUCCEEDED') {
    return (
      <Message
        severity="info"
        text="분석 완료 후 경력 후보를 검토할 수 있습니다."
      />
    )
  }

  return (
    <PageContainer>
      <Stack spacing={3}>
        <Typography component="h1" variant="h4" sx={{ fontWeight: 700 }}>
          AI 경력 후보 검토
        </Typography>
        <Alert severity="warning">
          AI가 만든 미확정 후보입니다. 원문 Evidence와 비교해 검토해 주세요.
        </Alert>
        {candidates.data.length === 0 ? (
          <Alert severity="info">검토할 경력 후보가 없습니다.</Alert>
        ) : (
          candidates.data.map((candidate) => (
            <CandidateCard
              key={candidate.candidateId}
              analysisId={analysisId}
              candidate={candidate}
            />
          ))
        )}
        <Button
          component={Link}
          to={`/career-documents/${documentId}`}
          variant="outlined"
        >
          분석 상태로 돌아가기
        </Button>
      </Stack>
    </PageContainer>
  )
}

function CandidateCard({
  analysisId,
  candidate,
}: {
  analysisId: string
  candidate: CareerCandidate
}) {
  const [editing, setEditing] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [content, setContent] = useState(() => toContent(candidate))
  const edit = useEditCareerCandidate(analysisId)
  const reject = useRejectCareerCandidate(analysisId)
  const editable = candidate.status !== 'CONFIRMED'

  const save = () => {
    edit.mutate(
      { candidateId: candidate.candidateId, content },
      { onSuccess: () => setEditing(false) },
    )
  }

  return (
    <Card variant="outlined">
      <CardContent>
        <Stack spacing={2}>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Chip label={statusLabel(candidate.status)} />
            <Typography color="text.secondary">
              revision {candidate.revisionNo} · 미확정
            </Typography>
          </Stack>
          {(edit.isError || reject.isError) && (
            <Alert severity="error">
              {getApiErrorMessage(edit.error ?? reject.error)}
            </Alert>
          )}
          {editing ? (
            <CandidateForm content={content} onChange={setContent} />
          ) : (
            <CandidateFields candidate={candidate} />
          )}
          <EvidenceList candidate={candidate} />
          {editable && (
            <Stack direction="row" spacing={1}>
              {editing ? (
                <>
                  <Button
                    variant="contained"
                    onClick={save}
                    disabled={
                      edit.isPending ||
                      !content.candidateType.trim() ||
                      !content.description.trim()
                    }
                  >
                    {edit.isPending ? '저장 중…' : '수정 저장'}
                  </Button>
                  <Button onClick={() => setEditing(false)}>취소</Button>
                </>
              ) : (
                <Button variant="outlined" onClick={() => setEditing(true)}>
                  수정
                </Button>
              )}
              {!editing && (
                <Button color="error" onClick={() => setConfirmingDelete(true)}>
                  삭제
                </Button>
              )}
            </Stack>
          )}
        </Stack>
      </CardContent>
      <Dialog
        open={confirmingDelete}
        onClose={() => setConfirmingDelete(false)}
      >
        <DialogTitle>경력 후보를 삭제할까요?</DialogTitle>
        <DialogContent>
          검토 목록에서는 제외되지만 원문 Evidence는 추적을 위해 보존됩니다.
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmingDelete(false)}>취소</Button>
          <Button
            color="error"
            disabled={reject.isPending}
            onClick={() =>
              reject.mutate(candidate.candidateId, {
                onSuccess: () => setConfirmingDelete(false),
              })
            }
          >
            삭제
          </Button>
        </DialogActions>
      </Dialog>
    </Card>
  )
}

function CandidateForm({
  content,
  onChange,
}: {
  content: CareerCandidateContent
  onChange: (value: CareerCandidateContent) => void
}) {
  const field = (name: keyof CareerCandidateContent) => ({
    value: content[name],
    onChange: (event: ChangeEvent<HTMLInputElement>) =>
      onChange({ ...content, [name]: event.target.value }),
  })
  return (
    <Stack spacing={2}>
      <TextField label="후보 유형" required {...field('candidateType')} />
      <TextField label="회사·조직" {...field('organization')} />
      <TextField label="직무·역할" {...field('role')} />
      <TextField label="기간" {...field('period')} />
      <TextField
        label="업무·성과·기술"
        required
        multiline
        minRows={4}
        {...field('description')}
      />
    </Stack>
  )
}

function CandidateFields({ candidate }: { candidate: CareerCandidate }) {
  return (
    <Stack spacing={1}>
      <Typography>유형: {candidate.candidateType}</Typography>
      <Typography>
        회사·조직: {candidate.organization ?? '확인 불가'}
      </Typography>
      <Typography>직무·역할: {candidate.role ?? '확인 불가'}</Typography>
      <Typography>기간: {candidate.period ?? '확인 불가'}</Typography>
      <Typography sx={{ whiteSpace: 'pre-wrap' }}>
        업무·성과·기술: {candidate.description}
      </Typography>
    </Stack>
  )
}

function EvidenceList({ candidate }: { candidate: CareerCandidate }) {
  return (
    <Stack spacing={1}>
      <Divider />
      <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
        원문 Evidence
      </Typography>
      {candidate.evidences.length === 0 ? (
        <Alert severity="warning">확인할 수 있는 원문 근거가 없습니다.</Alert>
      ) : (
        candidate.evidences.map((evidence) => (
          <Stack
            key={`${evidence.documentId}-${evidence.pageNumber}-${evidence.excerpt}`}
            spacing={0.5}
          >
            <Typography variant="body2" color="text.secondary">
              {evidence.documentName} · {evidence.pageNumber}페이지
            </Typography>
            <Typography
              component="blockquote"
              sx={{ m: 0, pl: 2, borderLeft: 3, borderColor: 'divider' }}
            >
              {evidence.excerpt}
            </Typography>
          </Stack>
        ))
      )}
    </Stack>
  )
}

function Message({
  severity,
  text,
}: {
  severity: 'info' | 'error'
  text: string
}) {
  return (
    <PageContainer>
      <Alert severity={severity}>{text}</Alert>
    </PageContainer>
  )
}

function toContent(candidate: CareerCandidate): CareerCandidateContent {
  return {
    candidateType: candidate.candidateType,
    organization: candidate.organization ?? '',
    role: candidate.role ?? '',
    period: candidate.period ?? '',
    description: candidate.description,
  }
}

function statusLabel(status: CareerCandidate['status']) {
  return {
    PENDING_REVIEW: '검토 대기',
    EDITED: '수정됨·미확정',
    CONFIRMED: '확정됨',
  }[status]
}
