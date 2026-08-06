import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  authApi,
  type AuthCredentials,
  type AuthenticatedUser,
} from '../api/authApi'

export const authQueryKey = ['auth', 'current-user'] as const

export function useCurrentUser() {
  return useQuery({
    queryKey: authQueryKey,
    queryFn: () => authApi.me(),
    staleTime: 30_000,
  })
}

export function useSignup() {
  return useAuthMutation((credentials) => authApi.signup(credentials))
}

export function useLogin() {
  return useAuthMutation((credentials) => authApi.login(credentials))
}

export function useLogout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => authApi.logout(),
    onSuccess: () => {
      queryClient.clear()
      queryClient.setQueryData(authQueryKey, null)
    },
  })
}

function useAuthMutation(
  mutationFn: (credentials: AuthCredentials) => Promise<AuthenticatedUser>,
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: (user) => {
      queryClient.clear()
      queryClient.setQueryData(authQueryKey, user)
    },
  })
}
