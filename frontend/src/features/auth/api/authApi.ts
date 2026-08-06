import { ApiError, apiRequest, clearCsrfToken } from '../../../shared/api'

export interface AuthenticatedUser {
  userId: string
  email: string
}

export interface AuthCredentials {
  email: string
  password: string
}

export const authApi = {
  async me(): Promise<AuthenticatedUser | null> {
    try {
      return await apiRequest<AuthenticatedUser>('/api/auth/me')
    } catch (error) {
      if (error instanceof ApiError && error.kind === 'AUTHENTICATION') {
        return null
      }
      throw error
    }
  },

  async signup(credentials: AuthCredentials) {
    const user = await apiRequest<AuthenticatedUser>('/api/auth/signup', {
      method: 'POST',
      json: credentials,
    })
    clearCsrfToken()
    return user
  },

  async login(credentials: AuthCredentials) {
    const user = await apiRequest<AuthenticatedUser>('/api/auth/login', {
      method: 'POST',
      json: credentials,
    })
    clearCsrfToken()
    return user
  },

  async logout() {
    await apiRequest<void>('/api/auth/logout', { method: 'POST' })
    clearCsrfToken()
  },
}
