import { createBrowserRouter } from 'react-router-dom'

import { GuestOnlyRoute, ProtectedRoute } from './routes'
import { LoginPage } from '../routes/LoginPage'
import { CareerDocumentAnalysisPage } from '../routes/CareerDocumentAnalysisPage'
import { CareerDocumentUploadPage } from '../routes/CareerDocumentUploadPage'
import { CareerCandidateReviewPage } from '../routes/CareerCandidateReviewPage'
import { HomePage } from '../routes/HomePage'
import { NotFoundPage } from '../routes/NotFoundPage'
import { SignupPage } from '../routes/SignupPage'

export function createAppRouter() {
  return createBrowserRouter([
    {
      element: <ProtectedRoute />,
      children: [
        { path: '/', element: <HomePage /> },
        {
          path: '/career-documents/new',
          element: <CareerDocumentUploadPage />,
        },
        {
          path: '/career-documents/:documentId',
          element: <CareerDocumentAnalysisPage />,
        },
        {
          path: '/career-documents/:documentId/analyses/:analysisId/candidates',
          element: <CareerCandidateReviewPage />,
        },
      ],
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
