import { Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'

import { PageContainer } from '../shared/ui/PageContainer'

export function NotFoundPage() {
  return (
    <PageContainer>
      <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          페이지를 찾을 수 없습니다
        </Typography>
        <Typography color="text.secondary">
          주소를 확인하거나 홈으로 돌아가 주세요.
        </Typography>
        <Button component={RouterLink} to="/" variant="contained">
          홈으로 돌아가기
        </Button>
      </Stack>
    </PageContainer>
  )
}
