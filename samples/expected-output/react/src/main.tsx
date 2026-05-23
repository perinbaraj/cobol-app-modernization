import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import CustomerInquiry from './components/CustomerInquiry/CustomerInquiry'
import './index.css'

// React 18 entry point — converted from CICS transaction CUSTINQ
// BMS screen: CUSTINQ.bms → CustomerInquiry.tsx
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <CustomerInquiry />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
)
