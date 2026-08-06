import { HttpResponse, http } from 'msw'

import type { AuthenticatedUser } from '../features/auth'

export const testUser: AuthenticatedUser = {
  userId: '7fb6c7e2-c70e-4ac5-a4eb-d89fe4b553e2',
  email: 'user@example.com',
}

export const authHandlers = {
  authenticated(user = testUser) {
    return http.get('/api/auth/me', () => HttpResponse.json(user))
  },
  expiredSession() {
    return http.get('/api/auth/me', () =>
      HttpResponse.json(
        { code: 'UNAUTHORIZED', message: '로그인이 필요합니다.' },
        { status: 401 },
      ),
    )
  },
  csrf(token = 'test-csrf-token') {
    return http.get('/api/auth/csrf', () =>
      HttpResponse.json({
        headerName: 'X-XSRF-TOKEN',
        parameterName: '_csrf',
        token,
      }),
    )
  },
  csrfFailure() {
    return http.get('/api/auth/csrf', () =>
      HttpResponse.json(
        { code: 'CSRF_UNAVAILABLE', message: 'CSRF 토큰 조회 실패' },
        { status: 500 },
      ),
    )
  },
}
