import { Paper, Stack, Typography } from '@mui/material'
import type { PropsWithChildren, ReactNode } from 'react'

import { PageContainer } from '../../../shared/ui/PageContainer'

interface AuthPageLayoutProps extends PropsWithChildren {
  title: string
  description: string
  footer: ReactNode
}

export function AuthPageLayout({
  title,
  description,
  footer,
  children,
}: AuthPageLayoutProps) {
  return (
    <PageContainer>
      <Paper elevation={0} sx={{ maxWidth: 480, mx: 'auto' }}>
        <Stack spacing={3}>
          <Stack spacing={1}>
            <Typography component="h1" variant="h4" sx={{ fontWeight: 700 }}>
              {title}
            </Typography>
            <Typography color="text.secondary">{description}</Typography>
          </Stack>
          {children}
          {footer}
        </Stack>
      </Paper>
    </PageContainer>
  )
}
