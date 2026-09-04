import { Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import Loans from './pages/Loans.jsx'
import Placeholder from './pages/Placeholder.jsx'

const STUBS = [
  ['/markets', 'Markets'], ['/trading', 'Trading'], ['/wallet', 'Wallet'],
  ['/vaults', 'Vaults'], ['/portfolio', 'Portfolio'], ['/liquidity-pools', 'Liquidity pools'],
  ['/swap', 'Swap'], ['/menu-styles', 'Menu Styles'], ['/tables', 'Tables'],
  ['/charts', 'Charts'], ['/forms', 'Forms'], ['/pricing', 'Pricing'],
  ['/settings', 'Settings'], ['/modals', 'Modals/Pop-Ups'],
  ['/documentation', 'Documentation'], ['/support', 'Support'],
]

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Loans />} />
        {STUBS.map(([path, title]) => (
          <Route key={path} path={path} element={<Placeholder title={title} />} />
        ))}
        <Route path="*" element={<Placeholder title="Page not found" />} />
      </Routes>
    </Layout>
  )
}
