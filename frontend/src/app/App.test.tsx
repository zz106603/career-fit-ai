import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'

import { authHandlers } from '../test/authHandlers'
import { server } from '../test/server'
import { App } from './App'

describe('애플리케이션 라우팅', () => {
  test('인증된 사용자에게 기본 화면을 표시한다', async () => {
    server.use(authHandlers.authenticated())
    window.history.pushState({}, '', '/')

    render(<App />)

    expect(
      await screen.findByRole('heading', {
        name: '근거로 확인하는 나의 경력 적합도',
      }),
    ).toBeInTheDocument()
  })

  test('알 수 없는 경로에서 홈으로 돌아갈 수 있다', async () => {
    server.use(authHandlers.authenticated())
    window.history.pushState({}, '', '/unknown')
    const user = userEvent.setup()

    render(<App />)

    expect(
      screen.getByRole('heading', { name: '페이지를 찾을 수 없습니다' }),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: '홈으로 돌아가기' }))

    expect(
      await screen.findByRole('heading', {
        name: '근거로 확인하는 나의 경력 적합도',
      }),
    ).toBeInTheDocument()
  })

  test('로그아웃 후 로그인 화면으로 이동한다', async () => {
    server.use(
      authHandlers.authenticated(),
      authHandlers.csrf(),
      http.post(
        '/api/auth/logout',
        () => new HttpResponse(null, { status: 204 }),
      ),
    )
    window.history.pushState({}, '', '/')
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('button', { name: '로그아웃' }))
    expect(
      await screen.findByRole('heading', { name: '로그인' }),
    ).toBeInTheDocument()
  })
})
