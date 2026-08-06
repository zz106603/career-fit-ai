import { HttpResponse, http } from 'msw'

import { server } from '../../test/server'
import { authHandlers } from '../../test/authHandlers'
import { ApiError } from './ApiError'
import { apiRequest } from './apiClient'
import { clearCsrfToken } from './csrfToken'

describe('apiRequest', () => {
  beforeEach(clearCsrfToken)

  it('변경 요청에 쿠키 자격 증명과 메모리에 보관한 CSRF 토큰을 사용한다', async () => {
    let csrfCalls = 0
    server.use(
      http.get('/api/auth/csrf', () => {
        csrfCalls += 1
        return HttpResponse.json({
          headerName: 'X-XSRF-TOKEN',
          parameterName: '_csrf',
          token: 'csrf-value',
        })
      }),
      http.post('/api/example', ({ request }) => {
        expect(request.credentials).toBe('same-origin')
        expect(request.headers.get('X-XSRF-TOKEN')).toBe('csrf-value')
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await apiRequest<void>('/api/example', { method: 'POST', json: { id: 1 } })
    await apiRequest<void>('/api/example', { method: 'POST', json: { id: 2 } })
    expect(csrfCalls).toBe(1)
  })

  it('FormData의 multipart 경계값은 브라우저가 설정하도록 둔다', async () => {
    server.use(
      authHandlers.csrf(),
      http.post('/api/upload', ({ request }) => {
        expect(request.headers.get('content-type')).toMatch(
          /^multipart\/form-data; boundary=/,
        )
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const form = new FormData()
    form.append('resume', new Blob(['resume']), 'resume.txt')
    await apiRequest<void>('/api/upload', { method: 'POST', body: form })
  })

  it.each([
    [401, 'AUTHENTICATION'],
    [403, 'ACCESS_DENIED'],
    [400, 'VALIDATION'],
    [500, 'SERVER'],
  ] as const)('%i 응답을 %s 오류로 구분한다', async (status, kind) => {
    server.use(
      http.get('/api/failure', () =>
        HttpResponse.json({ code: 'FAILED', message: '요청 실패' }, { status }),
      ),
    )
    await expect(apiRequest('/api/failure')).rejects.toMatchObject({
      name: 'ApiError',
      status,
      kind,
      code: 'FAILED',
    })
  })

  it('CSRF 조회 실패를 서버 오류로 전달하고 본 요청은 보내지 않는다', async () => {
    let requestSent = false
    server.use(
      authHandlers.csrfFailure(),
      http.post('/api/example', () => {
        requestSent = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    await expect(
      apiRequest('/api/example', { method: 'POST' }),
    ).rejects.toEqual(
      expect.objectContaining<Partial<ApiError>>({
        kind: 'SERVER',
        code: 'CSRF_UNAVAILABLE',
      }),
    )
    expect(requestSent).toBe(false)
  })

  it('CSRF 토큰이 만료된 변경 요청은 새 토큰으로 한 번 재시도한다', async () => {
    let csrfCalls = 0
    let requestCalls = 0
    server.use(
      http.get('/api/auth/csrf', () => {
        csrfCalls += 1
        return HttpResponse.json({
          headerName: 'X-XSRF-TOKEN',
          parameterName: '_csrf',
          token: `csrf-${csrfCalls}`,
        })
      }),
      http.post('/api/example', ({ request }) => {
        requestCalls += 1
        if (request.headers.get('X-XSRF-TOKEN') === 'csrf-1') {
          return HttpResponse.json(
            { code: 'ACCESS_DENIED', message: '토큰 만료' },
            { status: 403 },
          )
        }
        expect(request.headers.get('X-XSRF-TOKEN')).toBe('csrf-2')
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await apiRequest<void>('/api/example', { method: 'POST' })

    expect(csrfCalls).toBe(2)
    expect(requestCalls).toBe(2)
  })
})
