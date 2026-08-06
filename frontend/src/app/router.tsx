import { createBrowserRouter } from 'react-router-dom'

import { GuestOnlyRoute, ProtectedRoute } from './routes'
import { LoginPage } from '../routes/LoginPage'
import { HomePage } from '../routes/HomePage'
import { NotFoundPage } from '../routes/NotFoundPage'
import { SignupPage } from '../routes/SignupPage'

export function createAppRouter() {
  return createBrowserRouter([
    {
      element: <ProtectedRoute />,
      children: [{ path: '/', element: <HomePage /> }],
    },
    {
      element: <GuestOnlyRoute />,
      children: [
        { path: '/login', element: <LoginPage /> },
        { path: '/signup', element: <SignupPage /> },
      ],
    },
    {
      path: '*',
      element: <NotFoundPage />,
    },
  ])
}
