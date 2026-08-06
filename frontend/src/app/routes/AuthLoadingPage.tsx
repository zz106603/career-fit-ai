import { CircularProgress, Stack, Typography } from '@mui/material'

import { PageContainer } from '../../shared/ui/PageContainer'

export function AuthLoadingPage() {
  return (
    <PageContainer>
      <Stack spacing={2} sx={{ alignItems: 'center' }}>
        <CircularProgress aria-label="인증 상태 확인 중" />
        <Typography color="text.secondary">
          로그인 상태를 확인하고 있습니다.
        </Typography>
      </Stack>
    </PageContainer>
  )
}
