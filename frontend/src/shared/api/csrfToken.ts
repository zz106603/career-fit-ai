import { ApiError, type ApiErrorResponse } from './ApiError'

interface CsrfTokenResponse {
  headerName: string
  parameterName: string
  token: string
}

export interface CsrfToken {
  headerName: string
  token: string
}

let cachedToken: CsrfToken | null = null

export async function getCsrfToken(): Promise<CsrfToken> {
  if (cachedToken) return cachedToken

  const response = await fetch('/api/auth/csrf', {
    method: 'GET',
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  })

  if (!response.ok) {
    const fallback: ApiErrorResponse = {
      code: 'CSRF_TOKEN_REQUEST_FAILED',
      message: '보안 토큰을 불러오지 못했습니다.',
    }
    let body = fallback
    try {
      body = { ...fallback, ...((await response.json()) as ApiErrorResponse) }
    } catch {
      // JSON 오류 응답이 아니면 사용자에게 안전한 기본 메시지를 사용한다.
    }
    throw ApiError.fromResponse(response.status, body)
  }

  const body = (await response.json()) as CsrfTokenResponse
  if (!body.headerName || !body.token) {
    throw new Error('CSRF 토큰 응답 형식이 올바르지 않습니다.')
  }

  cachedToken = { headerName: body.headerName, token: body.token }
  return cachedToken
}

export function clearCsrfToken() {
  cachedToken = null
}
