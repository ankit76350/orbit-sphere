import { useMemo, useState } from 'react'
import { Table2, TrendingUp } from 'lucide-react'
import BalanceAreaChart from './charts/BalanceAreaChart.jsx'
import Segmented from './ui/Segmented.jsx'
import { RANGES, seriesForRange } from '../data/balance.js'
import { longDate, money, plain } from '../lib/format.js'

export default function WalletBalanceCard() {
  const [range, setRange] = useState('1M')
  const [asTable, setAsTable] = useState(false)
  const data = useMemo(() => seriesForRange(range), [range])
  const latest = data[data.length - 1].value

  return (
    <section className="card balance-card">
      <div className="card-head">
        <div>
          <h2 className="card-title">Wallet Balance</h2>
          <p className="balance-hero">
            {plain(latest)} <span className="balance-hero-unit">EUR</span>
          </p>
        </div>
        <div className="card-head-tools">
          <Segmented options={RANGES} value={range} onChange={setRange} label="Time range" />
          <button
            type="button"
            className="ghost-btn"
            onClick={() => setAsTable((v) => !v)}
            aria-pressed={asTable}
          >
            {asTable ? <TrendingUp size={15} /> : <Table2 size={15} />}
            {asTable ? 'Chart' : 'Table'}
          </button>
        </div>
      </div>

      {asTable ? (
        <div className="table-scroll balance-table">
          <table className="data-table">
            <caption className="sr-only">Wallet balance by date for the {range} range</caption>
            <thead>
              <tr><th scope="col">Date</th><th scope="col" className="num">Balance</th></tr>
            </thead>
            <tbody>
              {data.map((d) => (
                <tr key={d.date.toISOString()}>
                  <td>{longDate(d.date)}</td>
                  <td className="num">{money(d.value)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <BalanceAreaChart data={data} height={318} />
      )}
    </section>
  )
}
