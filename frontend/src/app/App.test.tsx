import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { App } from './App'

describe('애플리케이션 라우팅', () => {
  test('기본 화면을 표시한다', () => {
    window.history.pushState({}, '', '/')

    render(<App />)

    expect(
      screen.getByRole('heading', {
        name: '근거로 확인하는 나의 경력 적합도',
      }),
    ).toBeInTheDocument()
  })

  test('알 수 없는 경로에서 홈으로 돌아갈 수 있다', async () => {
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
})
