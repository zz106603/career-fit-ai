import {
  Alert,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControlLabel,
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
  useMergeCareerCandidates,
  useRejectCareerCandidate,
  useSplitCareerCandidate,
} from '../features/career-candidate'
import { useCareerDocumentAnalyses } from '../features/career-document'
import {
  type CareerConfirmationContent,
  useConfirmCareerCandidate,
} from '../features/career-experience'
import { getApiErrorMessage } from '../shared/api/getApiErrorMessage'
import { PageContainer } from '../shared/ui/PageContainer'

export function CareerCandidateReviewPage() {
  const { documentId = '', analysisId = '' } = useParams()
  const analyses = useCareerDocumentAnalyses(documentId)
  const candidates = useCareerCandidates(analysisId)
  const analysis = analyses.data?.find(
    (item) => item.documentAnalysisId === analysisId,
  )
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [merging, setMerging] = useState(false)

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
        <Button component={Link} to="/career-experiences" variant="outlined">
          확정 경력 보기
        </Button>
        {candidates.data.length === 0 ? (
          <Alert severity="info">검토할 경력 후보가 없습니다.</Alert>
        ) : (
          <>
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
              <Button
                variant="contained"
                disabled={selectedIds.length < 2}
                onClick={() => setMerging(true)}
              >
                선택 후보 병합 ({selectedIds.length})
              </Button>
              <Typography color="text.secondary">
                같은 경력을 나타내는 후보를 2개 이상 선택하세요.
              </Typography>
            </Stack>
            {candidates.data.map((candidate) => (
              <CandidateCard
                key={candidate.candidateId}
                analysisId={analysisId}
                candidate={candidate}
                selected={selectedIds.includes(candidate.candidateId)}
                onSelectedChange={(selected) =>
                  setSelectedIds((current) =>
                    selected
                      ? [...current, candidate.candidateId]
                      : current.filter((id) => id !== candidate.candidateId),
                  )
                }
              />
            ))}
            <MergeDialog
              key={selectedIds.join('-')}
              analysisId={analysisId}
              candidates={candidates.data.filter((candidate) =>
                selectedIds.includes(candidate.candidateId),
              )}
              open={merging}
              onClose={() => setMerging(false)}
              onMerged={() => {
                setMerging(false)
                setSelectedIds([])
              }}
            />
          </>
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
  selected,
  onSelectedChange,
}: {
  analysisId: string
  candidate: CareerCandidate
  selected: boolean
  onSelectedChange: (selected: boolean) => void
}) {
  const [editing, setEditing] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [splitting, setSplitting] = useState(false)
  const [confirming, setConfirming] = useState(false)
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
            {editable && (
              <FormControlLabel
                control={
                  <Checkbox
                    checked={selected}
                    onChange={(event) => onSelectedChange(event.target.checked)}
                  />
                }
                label="병합 선택"
              />
            )}
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
                <>
                  <Button onClick={() => setSplitting(true)}>분리</Button>
                  <Button
                    variant="contained"
                    onClick={() => setConfirming(true)}
                  >
                    이 후보 확정
                  </Button>
                  <Button
                    color="error"
                    onClick={() => setConfirmingDelete(true)}
                  >
                    삭제
                  </Button>
                </>
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
                onSuccess: () => {
                  setConfirmingDelete(false)
                  onSelectedChange(false)
                },
              })
            }
          >
            삭제
          </Button>
        </DialogActions>
      </Dialog>
      <SplitDialog
        analysisId={analysisId}
        candidate={candidate}
        open={splitting}
        onClose={() => setSplitting(false)}
        onSplit={() => {
          setSplitting(false)
          onSelectedChange(false)
        }}
      />
      <ConfirmationDialog
        analysisId={analysisId}
        candidate={candidate}
        open={confirming}
        onClose={() => setConfirming(false)}
        onConfirmed={() => {
          setConfirming(false)
          onSelectedChange(false)
        }}
      />
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

function MergeDialog({
  analysisId,
  candidates,
  open,
  onClose,
  onMerged,
}: {
  analysisId: string
  candidates: CareerCandidate[]
  open: boolean
  onClose: () => void
  onMerged: () => void
}) {
  const [content, setContent] = useState(() => mergeContent(candidates))
  const merge = useMergeCareerCandidates(analysisId)
  const valid = isValidContent(content) && candidates.length >= 2

  return (
    <Dialog
      open={open}
      onClose={merge.isPending ? undefined : onClose}
      fullWidth
    >
      <DialogTitle>선택한 경력 후보 병합</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Alert severity="info">
            원본 후보 {candidates.length}개는 검토 목록에서 제외되고, 모든
            Evidence를 가진 새 미확정 후보가 생성됩니다.
          </Alert>
          {merge.isError && (
            <Alert severity="error">{getApiErrorMessage(merge.error)}</Alert>
          )}
          <Typography sx={{ fontWeight: 700 }}>병합 결과 미리보기</Typography>
          <CandidateForm content={content} onChange={setContent} />
          <CombinedEvidenceList candidates={candidates} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={merge.isPending}>
          취소
        </Button>
        <Button
          variant="contained"
          disabled={!valid || merge.isPending}
          onClick={() =>
            merge.mutate(
              {
                candidateIds: candidates.map(
                  (candidate) => candidate.candidateId,
                ),
                content,
              },
              { onSuccess: onMerged },
            )
          }
        >
          {merge.isPending ? '병합 중…' : '미확정 후보로 병합'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function SplitDialog({
  analysisId,
  candidate,
  open,
  onClose,
  onSplit,
}: {
  analysisId: string
  candidate: CareerCandidate
  open: boolean
  onClose: () => void
  onSplit: () => void
}) {
  const [contents, setContents] = useState<CareerCandidateContent[]>(() => [
    toContent(candidate),
    toContent(candidate),
  ])
  const split = useSplitCareerCandidate(analysisId)
  const valid = contents.length >= 2 && contents.every(isValidContent)
  const change = (index: number, content: CareerCandidateContent) =>
    setContents((current) =>
      current.map((item, itemIndex) => (itemIndex === index ? content : item)),
    )

  return (
    <Dialog
      open={open}
      onClose={split.isPending ? undefined : onClose}
      fullWidth
    >
      <DialogTitle>경력 후보 분리</DialogTitle>
      <DialogContent>
        <Stack spacing={3} sx={{ pt: 1 }}>
          <Alert severity="info">
            원본 후보는 검토 목록에서 제외되고, 각 결과는 같은 Evidence를
            추적하는 새 미확정 후보로 생성됩니다.
          </Alert>
          {split.isError && (
            <Alert severity="error">{getApiErrorMessage(split.error)}</Alert>
          )}
          {contents.map((content, index) => (
            <Card key={index} variant="outlined">
              <CardContent>
                <Stack spacing={2}>
                  <Typography sx={{ fontWeight: 700 }}>
                    분리 결과 {index + 1}
                  </Typography>
                  <CandidateForm
                    content={content}
                    onChange={(value) => change(index, value)}
                  />
                  {contents.length > 2 && (
                    <Button
                      color="error"
                      onClick={() =>
                        setContents((current) =>
                          current.filter((_, itemIndex) => itemIndex !== index),
                        )
                      }
                    >
                      이 결과 제거
                    </Button>
                  )}
                </Stack>
              </CardContent>
            </Card>
          ))}
          <Button
            variant="outlined"
            onClick={() =>
              setContents((current) => [
                ...current,
                { ...toContent(candidate), description: '' },
              ])
            }
          >
            분리 결과 추가
          </Button>
          <CombinedEvidenceList candidates={[candidate]} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={split.isPending}>
          취소
        </Button>
        <Button
          variant="contained"
          disabled={!valid || split.isPending}
          onClick={() =>
            split.mutate(
              { candidateId: candidate.candidateId, contents },
              { onSuccess: onSplit },
            )
          }
        >
          {split.isPending ? '분리 중…' : '미확정 후보로 분리'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function CombinedEvidenceList({
  candidates,
}: {
  candidates: CareerCandidate[]
}) {
  const evidences = Array.from(
    new Map(
      candidates
        .flatMap((candidate) => candidate.evidences)
        .map((evidence) => [
          `${evidence.documentId}-${evidence.pageNumber}-${evidence.excerpt}`,
          evidence,
        ]),
    ).values(),
  )
  return (
    <Stack spacing={1}>
      <Typography sx={{ fontWeight: 700 }}>
        결과에 보존되는 원문 Evidence ({evidences.length})
      </Typography>
      {evidences.map((evidence) => (
        <Stack
          key={`${evidence.documentId}-${evidence.pageNumber}-${evidence.excerpt}`}
        >
          <Typography variant="body2" color="text.secondary">
            {evidence.documentName} · {evidence.pageNumber}페이지
          </Typography>
          <Typography component="blockquote" sx={{ m: 0, pl: 2 }}>
            {evidence.excerpt}
          </Typography>
        </Stack>
      ))}
    </Stack>
  )
}

function ConfirmationDialog({
  analysisId,
  candidate,
  open,
  onClose,
  onConfirmed,
}: {
  analysisId: string
  candidate: CareerCandidate
  open: boolean
  onClose: () => void
  onConfirmed: () => void
}) {
  const [content, setContent] = useState<CareerConfirmationContent>(() =>
    toConfirmationContent(candidate),
  )
  const confirm = useConfirmCareerCandidate(analysisId)
  const field = (name: keyof CareerConfirmationContent) => ({
    value: content[name] ?? '',
    onChange: (event: ChangeEvent<HTMLInputElement>) =>
      setContent({ ...content, [name]: event.target.value }),
  })
  const valid =
    content.title.trim() &&
    (content.role.trim() || content.responsibilities.trim()) &&
    (!content.startDate ||
      !content.endDate ||
      content.startDate <= content.endDate) &&
    candidate.evidences.length > 0

  return (
    <Dialog
      open={open}
      onClose={confirm.isPending ? undefined : onClose}
      fullWidth
    >
      <DialogTitle>경력 후보 최종 확인</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Alert severity="warning">
            아래 내용과 원문 Evidence를 확인한 뒤 직접 확정해야 분석용 경력으로
            사용됩니다.
          </Alert>
          {confirm.isError && (
            <Alert severity="error">{getApiErrorMessage(confirm.error)}</Alert>
          )}
          <TextField label="경험 유형" {...field('experienceType')} />
          <TextField label="경험명·프로젝트명" required {...field('title')} />
          <TextField label="회사·조직" {...field('organization')} />
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              label="시작일"
              type="date"
              slotProps={{ inputLabel: { shrink: true } }}
              {...field('startDate')}
            />
            <TextField
              label="종료일"
              type="date"
              slotProps={{ inputLabel: { shrink: true } }}
              {...field('endDate')}
            />
          </Stack>
          <TextField label="역할" {...field('role')} />
          <TextField
            label="수행 내용"
            multiline
            minRows={3}
            {...field('responsibilities')}
          />
          <TextField label="문제" multiline {...field('problem')} />
          <TextField label="행동" multiline {...field('action')} />
          <TextField label="성과" multiline {...field('outcome')} />
          <TextField label="기술" multiline {...field('technologies')} />
          <CombinedEvidenceList candidates={[candidate]} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={confirm.isPending}>
          취소
        </Button>
        <Button
          variant="contained"
          disabled={!valid || confirm.isPending}
          onClick={() =>
            confirm.mutate(
              { candidateId: candidate.candidateId, content },
              { onSuccess: onConfirmed },
            )
          }
        >
          {confirm.isPending ? '확정 중…' : '내용과 Evidence를 확인하고 확정'}
        </Button>
      </DialogActions>
    </Dialog>
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

function mergeContent(candidates: CareerCandidate[]): CareerCandidateContent {
  const first = candidates[0]
  if (!first) {
    return {
      candidateType: '',
      organization: '',
      role: '',
      period: '',
      description: '',
    }
  }
  return {
    ...toContent(first),
    description: Array.from(
      new Set(candidates.map((candidate) => candidate.description)),
    ).join('\n'),
  }
}

function toConfirmationContent(
  candidate: CareerCandidate,
): CareerConfirmationContent {
  return {
    experienceType: candidate.candidateType,
    title: candidate.role ?? candidate.organization ?? '',
    organization: candidate.organization ?? '',
    startDate: null,
    endDate: null,
    role: candidate.role ?? '',
    responsibilities: candidate.description,
    problem: '',
    action: '',
    outcome: '',
    technologies: '',
  }
}

function isValidContent(content: CareerCandidateContent) {
  return Boolean(content.candidateType.trim() && content.description.trim())
}

function statusLabel(status: CareerCandidate['status']) {
  return {
    PENDING_REVIEW: '검토 대기',
    EDITED: '수정됨·미확정',
    CONFIRMED: '확정됨',
  }[status]
}
