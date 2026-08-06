import { Button, Chip, Stack, Typography } from '@mui/material'

import { PageContainer } from '../shared/ui/PageContainer'

export function HomePage() {
  return (
    <PageContainer>
      <Stack spacing={3} sx={{ alignItems: 'flex-start' }}>
        <Chip label="Frontend 준비 완료" color="primary" variant="outlined" />
        <Typography variant="h3" sx={{ fontWeight: 700 }}>
          근거로 확인하는 나의 경력 적합도
        </Typography>
        <Typography color="text.secondary" sx={{ maxWidth: 640 }}>
          이 화면은 Career Fit AI Web Client의 기반 구성을 확인하기 위한
          시작점입니다. 로그인과 경력 문서 화면은 다음 작업에서 연결합니다.
        </Typography>
        <Button variant="contained" disabled>
          경력 분석 시작하기
        </Button>
      </Stack>
    </PageContainer>
  )
}
