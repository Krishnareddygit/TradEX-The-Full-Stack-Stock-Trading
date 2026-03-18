import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getStocks, placeOrder, getPriceHistory, getBuyOrders, getSellOrders } from '../services/api'
import { useToast } from '../context/ToastContext'
import { useAuth } from '../context/AuthContext'
import { TrendingUp, TrendingDown, Search } from 'lucide-react'
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import './Trade.css'

const CustomTooltip = ({ active, payload }) => {
  if (active && payload?.length) {
    return (
      <div style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 8, padding: '8px 12px', fontSize: 12 }}>
        <div className="mono" style={{ color: 'var(--accent)' }}>₹{Number(payload[0].value).toFixed(2)}</div>
        <div style={{ color: 'var(--text-muted)', fontSize: 11 }}>{payload[0].payload.time}</div>
      </div>
    )
  }
  return null
}

export default function Trade() {
  const { symbol: paramSymbol } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const { user, refreshUser } = useAuth()

  const [stocks, setStocks] = useState([])
  const [selected, setSelected] = useState(null)
  const [search, setSearch] = useState('')
  const [priceHistory, setPriceHistory] = useState([])
  const [buyOrders, setBuyOrders] = useState([])
  const [sellOrders, setSellOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [placing, setPlacing] = useState(false)
  const [form, setForm] = useState({
    side: 'BUY',
    type: 'MARKET',
    quantity: '',
    price: '',
    useMargin: false 
  })

  useEffect(() => {
    getStocks().then(r => {
      setStocks(r.data)
      const sym = paramSymbol || r.data[0]?.symbol
      if (sym) {
        const found = r.data.find(s => s.symbol === sym)
        if (found) selectStock(found)
      }
      setLoading(false)
    }).catch(() => setLoading(false))

    const interval = setInterval(() => {
      getStocks().then(r => {
        setStocks(r.data)
        setSelected(prev => prev ? r.data.find(s => s.symbol === prev.symbol) || prev : prev)
      }).catch(() => {})
    }, 5000)
    return () => clearInterval(interval)
  }, [paramSymbol])

  const selectStock = async (stock) => {
    setSelected(stock)
    navigate(`/trade/${stock.symbol}`, { replace: true })
    try {
      const [ph, bo, so] = await Promise.all([
        getPriceHistory(stock.symbol),
        getBuyOrders(stock.symbol),
        getSellOrders(stock.symbol)
      ])
      const history = ph.data.map(h => ({
        price: Number(h.price),
        time: new Date(h.timestamp).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })
      }))
      setPriceHistory(history)
      setBuyOrders(bo.data)
      setSellOrders(so.data)
    } catch { }
  }

  const handlePlaceOrder = async () => {
    if (!selected) return toast.error('Select a stock first')
    if (!form.quantity || Number(form.quantity) <= 0) return toast.error('Enter valid quantity')
    if (form.type === 'LIMIT' && (!form.price || Number(form.price) <= 0)) return toast.error('Enter limit price')

    setPlacing(true)
    try {
      await placeOrder({
        symbol: selected.symbol,
        side: form.side,
        type: form.type,
        quantity: Number(form.quantity),
        price: form.type === 'LIMIT' ? Number(form.price) : null,
        useMargin: form.side === "BUY" ? form.useMargin : false
      })
      toast.success(`${form.side} order placed — ${form.quantity} × ${selected.symbol}`)
      setForm(p => ({ ...p, quantity: '', price: '' }))

      // Refresh user balance immediately
      await refreshUser()

      // Refresh order book
      const [bo, so] = await Promise.all([getBuyOrders(selected.symbol), getSellOrders(selected.symbol)])
      setBuyOrders(bo.data)
      setSellOrders(so.data)
    } catch (err) {
      toast.error(err.response?.data?.error || 'Order failed')
    } finally {
      setPlacing(false)
    }
  }

  const filteredStocks = stocks.filter(s =>
    s.symbol.toLowerCase().includes(search.toLowerCase()) ||
    s.companyName?.toLowerCase().includes(search.toLowerCase())
  )

  const priceChange = priceHistory.length >= 2
    ? ((priceHistory[priceHistory.length - 1].price - priceHistory[0].price) / priceHistory[0].price) * 100
    : 0

  if (loading) return <div className="page"><div className="spinner" /></div>

  return (
    <div className="page trade-page">
      <div className="trade-layout">
        {/* Stock List */}
        <div className="stock-list-panel card">
          <div className="search-wrap">
            <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input type="text" placeholder="Search stocks..." value={search}
              onChange={e => setSearch(e.target.value)} style={{ paddingLeft: 30 }} />
          </div>
          <div className="stock-list">
            {filteredStocks.map(stock => (
              <div key={stock.id}
                className={`stock-item ${selected?.symbol === stock.symbol ? 'active' : ''} ${!stock.tradable ? 'halted' : ''}`}
                onClick={() => selectStock(stock)}>
                <div>
                  <div className="stock-symbol">{stock.symbol}</div>
                  <div className="stock-company">{stock.companyName}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div className="mono" style={{ fontSize: 13, fontWeight: 700 }}>₹{Number(stock.price).toFixed(2)}</div>
                  {!stock.tradable && <span className="tag tag-red" style={{ fontSize: 9, padding: '1px 6px' }}>Halted</span>}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Chart & Order Book */}
        <div className="trade-center">
          {selected ? (
            <>
              <div className="card stock-header">
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <h2 style={{ fontSize: 24, fontWeight: 800 }}>{selected.symbol}</h2>
                    <span className={`tag ${selected.tradable ? 'tag-green' : 'tag-red'}`}>
                      {selected.tradable ? 'Active' : 'Halted'}
                    </span>
                  </div>
                  <div className="muted" style={{ fontSize: 13 }}>{selected.companyName}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div className="mono" style={{ fontSize: 28, fontWeight: 700, color: priceChange >= 0 ? 'var(--green)' : 'var(--red)' }}>
                    ₹{Number(selected.price).toFixed(2)}
                  </div>
                  <div className={`mono ${priceChange >= 0 ? 'positive' : 'negative'}`}
                    style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 4, justifyContent: 'flex-end' }}>
                    {priceChange >= 0 ? <TrendingUp size={13} /> : <TrendingDown size={13} />}
                    {priceChange >= 0 ? '+' : ''}{priceChange.toFixed(2)}%
                  </div>
                </div>
              </div>

              <div className="card">
                <div className="card-header"><span className="card-title">Price History</span></div>
                {priceHistory.length > 1 ? (
                  <ResponsiveContainer width="100%" height={200}>
                    <LineChart data={priceHistory}>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                      <XAxis dataKey="time" tick={{ fontSize: 10, fill: 'var(--text-muted)' }} />
                      <YAxis tick={{ fontSize: 10, fill: 'var(--text-muted)' }} domain={['auto', 'auto']} />
                      <Tooltip content={<CustomTooltip />} />
                      <Line type="monotone" dataKey="price" stroke={priceChange >= 0 ? 'var(--green)' : 'var(--red)'} strokeWidth={2} dot={false} />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <div style={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', fontSize: 13 }}>
                    Price history will appear shortly (updates every 5s)
                  </div>
                )}
              </div>

              <div className="grid-2">
                <div className="card">
                  <div className="card-header"><span className="card-title" style={{ color: 'var(--green)' }}>Buy Orders</span></div>
                  <table><thead><tr><th>Price</th><th>Qty</th></tr></thead>
                    <tbody>
                      {buyOrders.length === 0
                        ? <tr><td colSpan={2} style={{ color: 'var(--text-muted)', textAlign: 'center', padding: 16 }}>No pending buy orders</td></tr>
                        : buyOrders.slice(0, 5).map((o, i) => (
                          <tr key={i}><td className="mono positive">₹{Number(o.price).toFixed(2)}</td><td className="mono">{o.quantity}</td></tr>
                        ))}
                    </tbody>
                  </table>
                </div>
                <div className="card">
                  <div className="card-header"><span className="card-title" style={{ color: 'var(--red)' }}>Sell Orders</span></div>
                  <table><thead><tr><th>Price</th><th>Qty</th></tr></thead>
                    <tbody>
                      {sellOrders.length === 0
                        ? <tr><td colSpan={2} style={{ color: 'var(--text-muted)', textAlign: 'center', padding: 16 }}>No pending sell orders</td></tr>
                        : sellOrders.slice(0, 5).map((o, i) => (
                          <tr key={i}><td className="mono negative">₹{Number(o.price).toFixed(2)}</td><td className="mono">{o.quantity}</td></tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          ) : (
            <div className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 300 }}>
              <p className="muted">Select a stock to start trading</p>
            </div>
          )}
        </div>

        {/* Order Panel */}
        <div className="order-panel card">
  <div className="card-header">
    <span className="card-title">Place Order</span>
  </div>

  {/* BALANCE */}
  <div className="balance-display">
    <span className="muted" style={{ fontSize: 12 }}>Available Balance</span>
    <span className="mono" style={{ color: 'var(--accent)', fontWeight: 700 }}>
      ₹{Number(user?.balance || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
    </span>

    {/* 🔥 Buying Power (only if margin ON) */}
    {form.side === "BUY" && form.useMargin && (
      <div style={{ marginTop: 6 }}>
        <span className="muted" style={{ fontSize: 12 }}>Buying Power</span>
        <div className="mono" style={{ fontWeight: 700 }}>
          ₹{Number((user?.balance || 0) * 20).toLocaleString('en-IN')} (20x)
        </div>
      </div>
    )}
  </div>

  {/* BUY / SELL */}
  <div className="side-toggle">
    <button className={`side-btn ${form.side === 'BUY' ? 'buy-active' : ''}`}
      onClick={() => setForm(p => ({ ...p, side: 'BUY' }))}>
      BUY
    </button>
    <button className={`side-btn ${form.side === 'SELL' ? 'sell-active' : ''}`}
      onClick={() => setForm(p => ({ ...p, side: 'SELL', useMargin: false }))}>
      SELL
    </button>
  </div>

  {/* ORDER TYPE */}
  <div className="form-group">
    <label>Order Type</label>
    <select value={form.type} onChange={e => setForm(p => ({ ...p, type: e.target.value }))}>
      <option value="MARKET">Market Order</option>
      <option value="LIMIT">Limit Order</option>
    </select>
  </div>

  {/* 🔥 MARGIN TOGGLE (NEW CLEAN UI) */}
  {form.side === "BUY" && (
  <div className="form-group">
    <label style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <span>Margin Trading</span>

      <div
        onClick={() => setForm(p => ({ ...p, useMargin: !p.useMargin }))}
        style={{
          width: 40,
          height: 20,
          background: form.useMargin ? 'var(--green)' : 'var(--border)',
          borderRadius: 20,
          position: 'relative',
          cursor: 'pointer'
        }}
      >
        <div
          style={{
            width: 16,
            height: 16,
            background: '#fff',
            borderRadius: '50%',
            position: 'absolute',
            top: 2,
            left: form.useMargin ? 22 : 2
          }}
        />
      </div>
    </label>

    <span className="muted" style={{ fontSize: 11 }}>
      {form.useMargin ? "Using 5% margin (20x)" : "Normal trade"}
    </span>
  </div>
)}

  {/* QUANTITY */}
  <div className="form-group">
    <label>Quantity</label>
    <input type="number" min="1" placeholder="Number of shares"
      value={form.quantity}
      onChange={e => setForm(p => ({ ...p, quantity: e.target.value }))} />
  </div>

  {/* LIMIT PRICE */}
  {form.type === 'LIMIT' && (
    <div className="form-group">
      <label>Limit Price (₹)</label>
      <input type="number" min="0.01" step="0.01"
        placeholder="Price per share"
        value={form.price}
        onChange={e => setForm(p => ({ ...p, price: e.target.value }))} />
    </div>
  )}

  {/* 🔥 ESTIMATION */}
  {selected && form.quantity && (() => {

    const price = Number(form.type === 'LIMIT' && form.price ? form.price : selected?.price || 0)
    const qty = Number(form.quantity || 0)
    const total = price * qty
    const required = form.side === "BUY"
  ? (form.useMargin ? total * 0.05 : total)
  : 0

    return (
      <div className="order-estimate">

        <div>
          <span className="muted" style={{ fontSize: 12 }}>Estimated Value</span>
          <div className="mono" style={{ fontWeight: 700 }}>
            ₹{total.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
        </div>

        <div style={{ marginTop: 6 }}>
          <span className="muted" style={{ fontSize: 12 }}>
            {form.useMargin ? "Required Margin (5%)" : "Required Amount"}
          </span>

          <div
            className="mono"
            style={{
              fontWeight: 700,
              color: form.useMargin ? 'var(--green)' : 'var(--accent)'
            }}
          >
            ₹{required.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
        </div>

      </div>
    )

  })()}

  {/* BUTTON */}
  <button
    className={form.side === 'BUY' ? 'btn-buy' : 'btn-sell'}
    onClick={handlePlaceOrder}
    disabled={
      placing ||
      !selected ||
      !selected.tradable ||
      (form.side === "BUY" && (
        (form.useMargin
          ? (Number(form.quantity || 0) * (Number(form.price || selected?.price || 0)) * 0.05)
          : (Number(form.quantity || 0) * (Number(form.price || selected?.price || 0)))
        ) > user?.balance
      ))
    }
    style={{
      marginTop: 8,
      opacity: (placing || !selected || !selected.tradable) ? 0.6 : 1,
      cursor: (placing || !selected || !selected.tradable) ? 'not-allowed' : 'pointer'
    }}
  >
    {placing ? 'Placing...' : `${form.side} ${selected?.symbol || ''}`}
  </button>

  {selected && !selected.tradable && (
    <p style={{ fontSize: 12, color: 'var(--red)', textAlign: 'center', marginTop: 8 }}>
      Trading halted for this stock
    </p>
  )}
</div>
      </div>
    </div>
  )
}
