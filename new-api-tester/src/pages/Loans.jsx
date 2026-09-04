import PromoCard from '../components/PromoCard.jsx'
import WalletBalanceCard from '../components/WalletBalanceCard.jsx'
import AllocationCard from '../components/AllocationCard.jsx'
import WalletsTable from '../components/WalletsTable.jsx'

export default function Loans() {
  return (
    <div className="page">
      <div className="page-head">
        <h1 className="page-title">Loans</h1>
      </div>

      <div className="grid-promos">
        <PromoCard
          art="swap"
          title="Swap %s Market Pairs with Zero Fees"
          highlight="500+"
          highlightTone="blue"
          cta="Exchange Now"
        />
        <PromoCard
          art="earn"
          title="Earn up to %s Market Pairs with Zero Fees"
          highlight="15%"
          highlightTone="brand"
          cta="Start Earning"
        />
      </div>

      <div className="grid-balance">
        <WalletBalanceCard />
        <AllocationCard />
      </div>

      <WalletsTable />
    </div>
  )
}
