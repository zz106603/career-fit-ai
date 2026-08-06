import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useCurrentUser } from '../../features/auth'
import { AuthLoadingPage } from './AuthLoadingPage'

export function ProtectedRoute() {
  const currentUser = useCurrentUser()
  const location = useLocation()

  if (currentUser.isPending) return <AuthLoadingPage />
  if (currentUser.isError) throw currentUser.error
  if (!currentUser.data) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return <Outlet />
}
