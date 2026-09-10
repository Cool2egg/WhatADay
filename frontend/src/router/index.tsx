import { createBrowserRouter } from 'react-router-dom'
import App from '@/App'
import Dashboard from '@/pages/Dashboard'
import Timeline from '@/pages/Timeline'
import Reports from '@/pages/Reports'
import Notes from '@/pages/Notes'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'timeline', element: <Timeline /> },
      { path: 'reports', element: <Reports /> },
      { path: 'notes', element: <Notes /> },
    ],
  },
])
