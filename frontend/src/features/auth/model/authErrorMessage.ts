import { ApiError } from '../../../shared/api'

const AUTH_ERROR_MESSAGES: Record<string, string> = {
  DUPLICATE_EMAIL: '이미 사용할 수 없는 이메일입니다.',
  INVALID_CREDENTIALS: '이메일 또는 비밀번호를 확인해 주세요.',
  INVALID_REQUEST: '입력한 내용을 다시 확인해 주세요.',
  AUTHENTICATION_REQUIRED: '로그인이 필요합니다.',
}

export function getAuthErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) {
    return '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  }
  return (
    AUTH_ERROR_MESSAGES[error.code] ??
    (error.kind === 'NETWORK'
      ? '서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.'
      : '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.')
  )
}
