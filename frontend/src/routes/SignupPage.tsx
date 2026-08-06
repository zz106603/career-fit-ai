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
import { Link as RouterLink, useNavigate } from 'react-router-dom'

import { useSignup } from '../features/auth'
import { getAuthErrorMessage } from '../features/auth/model/authErrorMessage'
import {
  signupSchema,
  type SignupFormValues,
} from '../features/auth/model/authFormSchema'
import { AuthPageLayout } from '../features/auth/ui/AuthPageLayout'

export function SignupPage() {
  const signup = useSignup()
  const navigate = useNavigate()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SignupFormValues>({ resolver: zodResolver(signupSchema) })

  const submit = handleSubmit(({ email, password }) => {
    signup.mutate(
      { email, password },
      {
        onSuccess: () => {
          void navigate('/', { replace: true })
        },
      },
    )
  })

  return (
    <AuthPageLayout
      title="회원가입"
      description="경력 분석을 시작할 계정을 만들어 주세요."
      footer={
        <Typography color="text.secondary" sx={{ textAlign: 'center' }}>
          이미 계정이 있나요?{' '}
          <Link component={RouterLink} to="/login">
            로그인
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
        {signup.isError && (
          <Alert severity="error">{getAuthErrorMessage(signup.error)}</Alert>
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
          autoComplete="new-password"
          error={Boolean(errors.password)}
          helperText={errors.password?.message}
          {...register('password')}
        />
        <TextField
          label="비밀번호 확인"
          type="password"
          autoComplete="new-password"
          error={Boolean(errors.passwordConfirm)}
          helperText={errors.passwordConfirm?.message}
          {...register('passwordConfirm')}
        />
        <Button type="submit" variant="contained" disabled={signup.isPending}>
          {signup.isPending ? '가입 중…' : '회원가입'}
        </Button>
      </Stack>
    </AuthPageLayout>
  )
}
