import { Navigate, Outlet } from 'react-router-dom'

import { useCurrentUser } from '../../features/auth'
import { AuthLoadingPage } from './AuthLoadingPage'

export function GuestOnlyRoute() {
  const currentUser = useCurrentUser()

  if (currentUser.isPending) return <AuthLoadingPage />
  if (currentUser.isError) throw currentUser.error
  if (currentUser.data) return <Navigate to="/" replace />
  return <Outlet />
}
