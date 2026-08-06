import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import { createQueryClient } from '../queryClient'
import { authHandlers } from '../../test/authHandlers'
import { server } from '../../test/server'
import { GuestOnlyRoute } from './GuestOnlyRoute'
import { ProtectedRoute } from './ProtectedRoute'

function renderRoutes(initialPath: string) {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/" element={<div>홈</div>} />
          <Route element={<ProtectedRoute />}>
            <Route path="/private" element={<div>보호 화면</div>} />
          </Route>
          <Route element={<GuestOnlyRoute />}>
            <Route path="/login" element={<div>로그인 화면</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('인증 상태 라우트', () => {
  it('세션이 만료되면 보호 화면에서 로그인 화면으로 이동한다', async () => {
    server.use(authHandlers.expiredSession())
    renderRoutes('/private')
    expect(await screen.findByText('로그인 화면')).toBeInTheDocument()
  })

  it('로그인 사용자는 보호 화면에 접근한다', async () => {
    server.use(authHandlers.authenticated())
    renderRoutes('/private')
    expect(await screen.findByText('보호 화면')).toBeInTheDocument()
  })

  it('로그인 사용자가 비회원 화면에 접근하면 홈으로 이동한다', async () => {
    server.use(authHandlers.authenticated())
    renderRoutes('/login')
    expect(await screen.findByText('홈')).toBeInTheDocument()
  })
})
