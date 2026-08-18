import {
  Alert,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  Stack,
  Typography,
} from '@mui/material'
import { Link, useParams } from 'react-router-dom'

import {
  type ConfirmedCareerVersion,
  useConfirmedCareers,
  useConfirmedCareerVersions,
} from '../features/career-experience'
import { getApiErrorMessage } from '../shared/api/getApiErrorMessage'
import { PageContainer } from '../shared/ui/PageContainer'

export function ConfirmedCareerPage() {
  const { experienceId = '' } = useParams()
  const current = useConfirmedCareers()
  const versions = useConfirmedCareerVersions(experienceId)
  const query = experienceId ? versions : current

  if (query.isPending) return <Message text="확정 경력을 불러오는 중입니다." />
  if (query.isError) {
    return <Message severity="error" text={getApiErrorMessage(query.error)} />
  }

  const data = query.data ?? []
  return (
    <PageContainer>
      <Stack spacing={3}>
        <Typography component="h1" variant="h4" sx={{ fontWeight: 700 }}>
          {experienceId ? '확정 경력 버전' : '확정 경력'}
        </Typography>
        <Alert severity="success">
          이 목록만 이후 공고 분석의 경력 기준으로 사용됩니다. AI 미확정
          후보와는 별도입니다.
        </Alert>
        {data.length === 0 ? (
          <Alert severity="info">
            확정된 경력이 없습니다. 경력 문서를 분석하고 후보의 내용과
            Evidence를 확인한 뒤 직접 확정해 주세요.
          </Alert>
        ) : (
          data.map((version) => (
            <VersionCard
              key={version.versionId}
              version={version}
              linked={!experienceId}
            />
          ))
        )}
        <Stack direction="row" spacing={1}>
          {experienceId && (
            <Button
              component={Link}
              to="/career-experiences"
              variant="outlined"
            >
              확정 경력 목록
            </Button>
          )}
          <Button
            component={Link}
            to="/career-documents/new"
            variant="outlined"
          >
            경력 문서 등록
          </Button>
        </Stack>
      </Stack>
    </PageContainer>
  )
}

function VersionCard({
  version,
  linked,
}: {
  version: ConfirmedCareerVersion
  linked: boolean
}) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Stack spacing={2}>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Chip
              color={version.current ? 'success' : 'default'}
              label={version.current ? '현재 버전' : '과거 버전'}
            />
            <Chip
              variant="outlined"
              label={
                version.sourceType === 'DOCUMENT' ? '문서 기반' : '직접 입력'
              }
            />
            <Typography color="text.secondary">
              version {version.versionNo} · {formatDate(version.createdAt)} 생성
            </Typography>
          </Stack>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            {version.title}
          </Typography>
          <Typography>
            회사·조직: {version.organization ?? '확인 불가'}
          </Typography>
          <Typography>역할: {version.role ?? '확인 불가'}</Typography>
          <Typography>
            기간: {formatPeriod(version.startDate, version.endDate)}
          </Typography>
          <Typography sx={{ whiteSpace: 'pre-wrap' }}>
            수행 내용: {version.responsibilities ?? '확인 불가'}
          </Typography>
          {version.technologies && (
            <Typography>기술: {version.technologies}</Typography>
          )}
          {version.sourceType === 'DOCUMENT' && (
            <EvidenceList version={version} />
          )}
          {linked && (
            <Button
              component={Link}
              to={`/career-experiences/${version.experienceId}`}
              variant="outlined"
            >
              버전 이력 보기
            </Button>
          )}
        </Stack>
      </CardContent>
    </Card>
  )
}

function EvidenceList({ version }: { version: ConfirmedCareerVersion }) {
  return (
    <Stack spacing={1}>
      <Divider />
      <Typography sx={{ fontWeight: 700 }}>확정 시점 원문 Evidence</Typography>
      {version.evidences.length === 0 ? (
        <Alert severity="warning">확인할 수 있는 문서 근거가 없습니다.</Alert>
      ) : (
        version.evidences.map((evidence) => (
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
        ))
      )}
    </Stack>
  )
}

function Message({
  text,
  severity = 'info',
}: {
  text: string
  severity?: 'info' | 'error'
}) {
  return (
    <PageContainer>
      <Alert severity={severity}>{text}</Alert>
    </PageContainer>
  )
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium' }).format(
    new Date(value),
  )
}

function formatPeriod(startDate: string | null, endDate: string | null) {
  if (!startDate && !endDate) return '확인 불가'
  return `${startDate ?? '시작일 미확인'} ~ ${endDate ?? '현재 또는 종료일 미확인'}`
}
