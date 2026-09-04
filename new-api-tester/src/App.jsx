import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import Placeholder from './pages/Placeholder.jsx'
import { ROUTES } from './screens.js'

/**
 * The router, built from the same list the navigation is built from.
 *
 * `ROUTES` is derived from `SURFACES` in screens.js, so a nav item pointing at an address that
 * has no screen is not something to remember to check — it cannot be written down.
 */
export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Navigate to={ROUTES[0].path} replace />} />
        {ROUTES.map(({ path, surface, module, submodule }) => (
          <Route
            key={path}
            path={path}
            element={
              submodule.screen ? (
                <submodule.screen />
              ) : (
                <Placeholder
                  title={submodule.label}
                  surface={surface.label}
                  module={module.label}
                  group={submodule.group}
                  endpoints={submodule.endpoints}
                />
              )
            }
          />
        ))}
        <Route path="*" element={<Placeholder title="Page not found" />} />
      </Routes>
    </Layout>
  )
}
