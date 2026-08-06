import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, HttpResponse, http } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import { createQueryClient } from '../app/queryClient'
import { authHandlers, testUser } from '../test/authHandlers'
import { server } from '../test/server'
import { LoginPage } from './LoginPage'
import { SignupPage } from './SignupPage'

function renderAuthPage(path: string, state?: unknown) {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <MemoryRouter initialEntries={[{ pathname: path, state }]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/" element={<div>대시보드</div>} />
          <Route path="/resume" element={<div>이력서 화면</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('로그인 화면', () => {
  it('잘못된 입력은 API 요청 전에 안내한다', async () => {
    const user = userEvent.setup()
    let requestCount = 0
    server.use(
      http.post('/api/auth/login', () => {
        requestCount += 1
        return HttpResponse.json(testUser)
      }),
    )
    renderAuthPage('/login')
    await user.type(screen.getByLabelText('이메일'), 'not-an-email')
    await user.click(screen.getByRole('button', { name: '로그인' }))
    expect(
      await screen.findByText('올바른 이메일 주소를 입력해 주세요.'),
    ).toBeInTheDocument()
    expect(requestCount).toBe(0)
  })

  it('로그인 성공 후 원래 접근하려던 보호 화면으로 이동한다', async () => {
    const user = userEvent.setup()
    server.use(
      authHandlers.csrf(),
      http.post('/api/auth/login', () => HttpResponse.json(testUser)),
    )
    renderAuthPage('/login', { from: { pathname: '/resume' } })
    await user.type(screen.getByLabelText('이메일'), testUser.email)
    await user.type(screen.getByLabelText('비밀번호'), 'password')
    await user.click(screen.getByRole('button', { name: '로그인' }))
    expect(await screen.findByText('이력서 화면')).toBeInTheDocument()
  })

  it('잘못된 자격 증명은 이메일 존재 여부를 구분하지 않고 안내한다', async () => {
    const user = userEvent.setup()
    server.use(
      authHandlers.csrf(),
      http.post('/api/auth/login', () =>
        HttpResponse.json(
          { code: 'INVALID_CREDENTIALS', message: '인증 실패' },
          { status: 401 },
        ),
      ),
    )
    renderAuthPage('/login')
    await user.type(screen.getByLabelText('이메일'), testUser.email)
    await user.type(screen.getByLabelText('비밀번호'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: '로그인' }))
    expect(
      await screen.findByText('이메일 또는 비밀번호를 확인해 주세요.'),
    ).toBeInTheDocument()
    expect(screen.queryByText('인증 실패')).not.toBeInTheDocument()
  })

  it('제출 중에는 버튼을 비활성화해 중복 요청을 막는다', async () => {
    const user = userEvent.setup()
    let requestCount = 0
    server.use(
      authHandlers.csrf(),
      http.post('/api/auth/login', async () => {
        requestCount += 1
        await delay(100)
        return HttpResponse.json(testUser)
      }),
    )
    renderAuthPage('/login')
    await user.type(screen.getByLabelText('이메일'), testUser.email)
    await user.type(screen.getByLabelText('비밀번호'), 'password')
    const button = screen.getByRole('button', { name: '로그인' })
    await user.dblClick(button)
    expect(
      await screen.findByRole('button', { name: '로그인 중…' }),
    ).toBeDisabled()
    await waitFor(() => expect(requestCount).toBe(1))
  })
})

describe('회원가입 화면', () => {
  it('회원가입 성공 후 인증 상태로 대시보드에 진입한다', async () => {
    const user = userEvent.setup()
    server.use(
      authHandlers.csrf(),
      http.post('/api/auth/signup', () => HttpResponse.json(testUser)),
    )
    renderAuthPage('/signup')
    await user.type(screen.getByLabelText('이메일'), testUser.email)
    await user.type(
      screen.getByLabelText('비밀번호', { exact: true }),
      'password',
    )
    await user.type(screen.getByLabelText('비밀번호 확인'), 'password')
    await user.click(screen.getByRole('button', { name: '회원가입' }))
    expect(await screen.findByText('대시보드')).toBeInTheDocument()
  })

  it('비밀번호 확인이 다르면 가입 요청을 보내지 않는다', async () => {
    const user = userEvent.setup()
    renderAuthPage('/signup')
    await user.type(screen.getByLabelText('이메일'), testUser.email)
    await user.type(
      screen.getByLabelText('비밀번호', { exact: true }),
      'password',
    )
    await user.type(screen.getByLabelText('비밀번호 확인'), 'different')
    await user.click(screen.getByRole('button', { name: '회원가입' }))
    expect(
      await screen.findByText('비밀번호가 일치하지 않습니다.'),
    ).toBeInTheDocument()
  })

  it('중복 이메일 오류를 가입 화면 메시지로 안내한다', async () => {
    const user = userEvent.setup()
    server.use(
      authHandlers.csrf(),
      http.post('/api/auth/signup', () =>
        HttpResponse.json(
          { code: 'DUPLICATE_EMAIL', message: '이미 가입된 이메일입니다.' },
          { status: 409 },
        ),
      ),
    )
    renderAuthPage('/signup')
    await user.type(screen.getByLabelText('이메일'), testUser.email)
    await user.type(
      screen.getByLabelText('비밀번호', { exact: true }),
      'password',
    )
    await user.type(screen.getByLabelText('비밀번호 확인'), 'password')
    await user.click(screen.getByRole('button', { name: '회원가입' }))
    expect(
      await screen.findByText('이미 사용할 수 없는 이메일입니다.'),
    ).toBeInTheDocument()
  })
})
