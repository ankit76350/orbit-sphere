import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import Placeholder from './pages/Placeholder.jsx'
import { SCREENS } from './screens.js'

/**
 * Routes, one per screen the sidebar offers.
 *
 * They all render Placeholder for now. The list lives in Sidebar.jsx so the nav and the routes
 * cannot drift apart — a nav item pointing at a route that does not exist is the usual way that
 * happens, and here it is impossible.
 */
export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Navigate to={SCREENS[0].to} replace />} />
        {SCREENS.map(({ to, label, endpoints }) => (
          <Route key={to} path={to} element={<Placeholder title={label} endpoints={endpoints} />} />
        ))}
        <Route path="*" element={<Placeholder title="Page not found" />} />
      </Routes>
    </Layout>
  )
}
