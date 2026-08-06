import { createBrowserRouter } from 'react-router-dom'

import { HomePage } from '../routes/HomePage'
import { NotFoundPage } from '../routes/NotFoundPage'

export function createAppRouter() {
  return createBrowserRouter([
    {
      path: '/',
      element: <HomePage />,
    },
    {
      path: '*',
      element: <NotFoundPage />,
    },
  ])
}
