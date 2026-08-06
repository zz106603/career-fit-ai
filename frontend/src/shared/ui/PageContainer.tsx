import { Container, Paper } from '@mui/material'
import type { PropsWithChildren } from 'react'

export function PageContainer({ children }: PropsWithChildren) {
  return (
    <Container component="main" maxWidth="md" sx={{ py: { xs: 5, md: 10 } }}>
      <Paper elevation={0} sx={{ p: { xs: 3, md: 6 } }}>
        {children}
      </Paper>
    </Container>
  )
}
