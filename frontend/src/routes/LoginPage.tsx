import { zodResolver } from '@hookform/resolvers/zod'
import {
  Alert,
  Button,
  Link,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useForm } from 'react-hook-form'
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom'

import { useLogin } from '../features/auth'
import { getAuthErrorMessage } from '../features/auth/model/authErrorMessage'
import {
  loginSchema,
  type LoginFormValues,
} from '../features/auth/model/authFormSchema'
import { AuthPageLayout } from '../features/auth/ui/AuthPageLayout'

export function LoginPage() {
  const login = useLogin()
  const navigate = useNavigate()
  const location = useLocation()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) })

  const submit = handleSubmit((values) => {
    login.mutate(values, {
      onSuccess: () => {
        void navigate(getReturnPath(location.state), { replace: true })
      },
    })
  })

  return (
    <AuthPageLayout
      title="로그인"
      description="계속하려면 Career Fit AI 계정으로 로그인해 주세요."
      footer={
        <Typography color="text.secondary" sx={{ textAlign: 'center' }}>
          계정이 없나요?{' '}
          <Link component={RouterLink} to="/signup">
            회원가입
          </Link>
        </Typography>
      }
    >
      <Stack
        component="form"
        spacing={2}
        onSubmit={(event) => void submit(event)}
        noValidate
      >
        {login.isError && (
          <Alert severity="error">{getAuthErrorMessage(login.error)}</Alert>
        )}
        <TextField
          label="이메일"
          type="email"
          autoComplete="email"
          error={Boolean(errors.email)}
          helperText={errors.email?.message}
          {...register('email')}
        />
        <TextField
          label="비밀번호"
          type="password"
          autoComplete="current-password"
          error={Boolean(errors.password)}
          helperText={errors.password?.message}
          {...register('password')}
        />
        <Button type="submit" variant="contained" disabled={login.isPending}>
          {login.isPending ? '로그인 중…' : '로그인'}
        </Button>
      </Stack>
    </AuthPageLayout>
  )
}

function getReturnPath(state: unknown) {
  if (!state || typeof state !== 'object' || !('from' in state)) return '/'
  const from = state.from
  if (!from || typeof from !== 'object' || !('pathname' in from)) return '/'
  return typeof from.pathname === 'string' && from.pathname.startsWith('/')
    ? from.pathname
    : '/'
}
