import { ApiError } from './ApiError'

export function getApiErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) return '요청을 처리하지 못했습니다.'
  if (error.code === 'CAREER_DOCUMENT_NOT_FOUND') {
    return '경력 문서를 찾을 수 없습니다.'
  }
  if (error.kind === 'NETWORK') return '서버에 연결할 수 없습니다.'
  if (error.kind === 'SERVER')
    return '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
  return error.message
}
