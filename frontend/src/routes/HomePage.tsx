import { Button, Chip, Stack, Typography } from '@mui/material'
import { Link, useNavigate } from 'react-router-dom'

import { useCurrentUser, useLogout } from '../features/auth'
import { PageContainer } from '../shared/ui/PageContainer'

export function HomePage() {
  const currentUser = useCurrentUser()
  const logout = useLogout()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout.mutate(undefined, {
      onSuccess: () => {
        void navigate('/login', { replace: true })
      },
    })
  }

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
        <Typography color="text.secondary">
          {currentUser.data?.email} 계정으로 로그인했습니다.
        </Typography>
        <Stack direction="row" spacing={2}>
          <Button
            component={Link}
            to="/career-documents/new"
            variant="contained"
          >
            경력 분석 시작하기
          </Button>
          <Button
            variant="outlined"
            onClick={handleLogout}
            disabled={logout.isPending}
          >
            {logout.isPending ? '로그아웃 중…' : '로그아웃'}
          </Button>
        </Stack>
      </Stack>
    </PageContainer>
  )
}
