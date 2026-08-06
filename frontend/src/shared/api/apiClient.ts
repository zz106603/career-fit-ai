import { ApiError, type ApiErrorResponse } from './ApiError'
import { clearCsrfToken, getCsrfToken } from './csrfToken'

type ApiRequestOptions = Omit<RequestInit, 'body'> & {
  body?: BodyInit | null
  json?: unknown
}

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  if (!path.startsWith('/api/')) {
    throw new Error('API 경로는 /api/로 시작해야 합니다.')
  }
  if (options.body !== undefined && options.json !== undefined) {
    throw new Error('body와 json은 동시에 지정할 수 없습니다.')
  }

  const { json, ...requestOptions } = options

  const method = (options.method ?? 'GET').toUpperCase()
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')

  let body = options.body
  if (json !== undefined) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(json)
  }

  if (!SAFE_METHODS.has(method)) {
    try {
      const csrf = await getCsrfToken()
      headers.set(csrf.headerName, csrf.token)
    } catch (error) {
      if (error instanceof ApiError) throw error
      throw ApiError.network(error)
    }
  }

  let response: Response
  try {
    response = await fetch(path, {
      ...requestOptions,
      method,
      body,
      headers,
      credentials: 'same-origin',
    })
  } catch (error) {
    throw ApiError.network(error)
  }

  if (!response.ok) {
    if (response.status === 403) clearCsrfToken()
    throw await toApiError(response)
  }

  if (response.status === 204) return undefined as T
  const contentType = response.headers.get('content-type') ?? ''
  if (!contentType.includes('application/json')) return undefined as T
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  const fallback: ApiErrorResponse = {
    code: 'API_REQUEST_FAILED',
    message: '요청을 처리하지 못했습니다.',
  }

  try {
    const contentType = response.headers.get('content-type') ?? ''
    if (!contentType.includes('application/json')) {
      return ApiError.fromResponse(response.status, fallback)
    }
    const body = (await response.json()) as Partial<ApiErrorResponse>
    return ApiError.fromResponse(response.status, {
      code: body.code?.trim() || fallback.code,
      message: body.message?.trim() || fallback.message,
    })
  } catch {
    return ApiError.fromResponse(response.status, fallback)
  }
}
