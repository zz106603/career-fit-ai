import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import { App } from './app/App'
import './styles.css'

const root = document.getElementById('root')

if (!root) {
  throw new Error('React 애플리케이션을 마운트할 root 요소가 없습니다.')
}

createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
