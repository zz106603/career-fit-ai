import { QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { HttpResponse, http } from 'msw'

import { createQueryClient } from '../../../app/queryClient'
import { authHandlers, testUser } from '../../../test/authHandlers'
import { server } from '../../../test/server'
import {
  authQueryKey,
  useCurrentUser,
  useLogin,
  useLogout,
} from './authQueries'

function createWrapper() {
  const queryClient = createQueryClient()
  return {
    queryClient,
    wrapper: ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    ),
  }
}

describe('인증 쿼리', () => {
  it('세션이 만료된 사용자는 비로그인 상태로 처리한다', async () => {
    server.use(authHandlers.expiredSession())
    const { wrapper } = createWrapper()
    const { result } = renderHook(useCurrentUser, { wrapper })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toBeNull()
  })

  it('로그인 성공 시 기존 캐시를 비우고 현재 사용자를 저장한다', async () => {
    server.use(
      authHandlers.csrf(),
      http.post('/api/auth/login', () => HttpResponse.json(testUser)),
    )
    const { queryClient, wrapper } = createWrapper()
    queryClient.setQueryData(['previous'], 'cached')
    const { result } = renderHook(useLogin, { wrapper })
    await act(() =>
      result.current.mutateAsync({
        email: testUser.email,
        password: 'password',
      }),
    )
    expect(queryClient.getQueryData(['previous'])).toBeUndefined()
    expect(queryClient.getQueryData(authQueryKey)).toEqual(testUser)
  })

  it('로그아웃 성공 시 기존 캐시를 비우고 비로그인 상태로 바꾼다', async () => {
    server.use(
      authHandlers.csrf(),
      http.post(
        '/api/auth/logout',
        () => new HttpResponse(null, { status: 204 }),
      ),
    )
    const { queryClient, wrapper } = createWrapper()
    queryClient.setQueryData(authQueryKey, testUser)
    queryClient.setQueryData(['private-data'], 'cached')
    const { result } = renderHook(useLogout, { wrapper })
    await act(() => result.current.mutateAsync())
    expect(queryClient.getQueryData(['private-data'])).toBeUndefined()
    expect(queryClient.getQueryData(authQueryKey)).toBeNull()
  })
})
