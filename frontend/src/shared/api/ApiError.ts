export type ApiErrorKind =
  | 'AUTHENTICATION'
  | 'ACCESS_DENIED'
  | 'VALIDATION'
  | 'SERVER'
  | 'NETWORK'
  | 'UNKNOWN'

export interface ApiErrorResponse {
  code: string
  message: string
}

export class ApiError extends Error {
  readonly status: number | null
  readonly code: string
  readonly kind: ApiErrorKind

  constructor(
    message: string,
    options: {
      status: number | null
      code: string
      kind: ApiErrorKind
      cause?: unknown
    },
  ) {
    super(message, { cause: options.cause })
    this.name = 'ApiError'
    this.status = options.status
    this.code = options.code
    this.kind = options.kind
  }

  static fromResponse(status: number, response: ApiErrorResponse) {
    return new ApiError(response.message, {
      status,
      code: response.code,
      kind: kindOf(status),
    })
  }

  static network(cause: unknown) {
    return new ApiError('서버에 연결할 수 없습니다.', {
      status: null,
      code: 'NETWORK_ERROR',
      kind: 'NETWORK',
      cause,
    })
  }
}

function kindOf(status: number): ApiErrorKind {
  if (status === 401) return 'AUTHENTICATION'
  if (status === 403) return 'ACCESS_DENIED'
  if (status >= 400 && status < 500) return 'VALIDATION'
  if (status >= 500) return 'SERVER'
  return 'UNKNOWN'
}
