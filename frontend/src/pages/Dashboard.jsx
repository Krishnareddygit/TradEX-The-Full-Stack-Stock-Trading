import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { useAuth } from "../context/AuthContext"
import { getStocks, getPortfolio, getPnL, getMarketStatus } from "../services/api"

import {
TrendingUp,
TrendingDown,
DollarSign,
Briefcase,
Activity,
ArrowRight
} from "lucide-react"

import {
LineChart,
Line,
XAxis,
YAxis,
Tooltip,
ResponsiveContainer
} from "recharts"

import "./Dashboard.css"


function StatCard({ label, value, sub, color, icon: Icon }) {
return (
<div className="stat-card">
<div className="stat-header">
<span className="stat-title">{label}</span>
<div className="stat-icon" style={{ color }}>
<Icon size={18}/>
</div>
</div>

<div className="stat-value" style={{ color }}>
{value}
</div>

{sub && <div className="stat-sub">{sub}</div>}
</div>
)
}


export default function Dashboard() {

const { user, refreshUser } = useAuth()
const navigate = useNavigate()

const [stocks,setStocks] = useState([])
const [portfolio,setPortfolio] = useState([])
const [pnl,setPnl] = useState({})
const [marketStatus,setMarketStatus] = useState("CLOSED")
const [loading,setLoading] = useState(true)


// 🔥 DEFAULT MARGIN = 5%
const marginPercent = 5

// 🔥 BUYING POWER CALCULATION
const buyingPower = (user?.balance || 0) * (100 / marginPercent)


const loadData = () => {

Promise.all([
getStocks(),
getPortfolio(),
getPnL(),
getMarketStatus()
])
.then(([stocksRes,portfolioRes,pnlRes,marketRes]) => {
setStocks(stocksRes.data)
setPortfolio(portfolioRes.data)
setPnl(pnlRes.data)
setMarketStatus(marketRes.data)
})
.finally(()=>setLoading(false))

}


useEffect(()=>{

loadData()
refreshUser()

const interval = setInterval(()=>{
loadData()
refreshUser()
},8000)

return ()=>clearInterval(interval)

},[])



const totalPnl = pnl["TOTAL"] || 0

const portfolioValue = portfolio.reduce((sum,p)=>{
return sum + (Number(p.stock?.price)||0)*(Number(p.quantity)||0)
},0)


const chartData = portfolio.map(p=>({
name:p.stock?.symbol,
value:(p.stock?.price||0)*(p.quantity||0)
}))


const gainers = [...stocks].sort((a,b)=>b.price-a.price).slice(0,5)
const losers = [...stocks].sort((a,b)=>a.price-b.price).slice(0,5)


if(loading){
return(
<div className="page">
<div className="spinner"/>
</div>
)
}


return(
<div className="page">


{/* MARKET TICKER */}
<div className="ticker">
{stocks.slice(0,12).map(stock=>(
<div key={stock.id} className="ticker-item">
<span className="ticker-symbol">{stock.symbol}</span>
<span className="ticker-price">
₹{Number(stock.price).toFixed(2)}
</span>
</div>
))}
</div>


{/* HEADER */}
<div className="dashboard-header">

<div>
<h1 className="page-title">
Good {getGreeting()}, {user?.username} 👋
</h1>
<p className="subtitle">
Your trading overview
</p>
</div>

<div className={`market-badge ${marketStatus==="OPEN"?"open":"closed"}`}>
<span className={`dot ${marketStatus==="OPEN"?"open":"closed"}`}/>
Market {marketStatus}
</div>

</div>


{/* STATS */}
<div className="stats-grid">

<StatCard
label="Available Funds"
value={`₹${Number(user?.balance||0).toLocaleString("en-IN",{minimumFractionDigits:2})}`}
sub={`Max Buying Power: ₹${Number(buyingPower).toLocaleString("en-IN",{minimumFractionDigits:2})} (20x)`}
icon={DollarSign}
color="#3b82f6"
/>

<StatCard
label="Investment Value"
value={`₹${portfolioValue.toLocaleString("en-IN",{minimumFractionDigits:2})}`}
icon={Briefcase}
color="#38bdf8"
/>

<StatCard
label="Net P/L"
value={`${totalPnl>=0?"+":""}₹${Number(totalPnl).toFixed(2)}`}
icon={totalPnl>=0?TrendingUp:TrendingDown}
color={totalPnl>=0?"#22c55e":"#ef4444"}
/>

<StatCard
label="Positions"
value={portfolio.length}
sub={`${portfolio.length} active`}
icon={Activity}
color="#f59e0b"
/>

</div>


{/* PORTFOLIO CHART */}
<div className="card">

<div className="card-header">
<span className="card-title">
Portfolio Performance
</span>
</div>

<ResponsiveContainer width="100%" height={260}>
<LineChart data={chartData}>
<XAxis dataKey="name"/>
<YAxis/>
<Tooltip/>
<Line
type="monotone"
dataKey="value"
stroke="#3b82f6"
strokeWidth={2}
/>
</LineChart>
</ResponsiveContainer>

</div>


{/* MAIN GRID */}
<div className="main-grid">


{/* MARKET WATCH */}
<div className="card">

<div className="card-header">
<span className="card-title">
Market Watch
</span>

<button
className="btn"
onClick={()=>navigate("/trade")}
>
Place Order
<ArrowRight size={14}/>
</button>

</div>


<table>
<thead>
<tr>
<th>Symbol</th>
<th>Company</th>
<th>Price</th>
<th>Status</th>
</tr>
</thead>

<tbody>
{stocks
.filter(s=>s.symbol!=="system" && s.companyName)
.slice(0,8)
.map(stock=>(
<tr
key={stock.id}
onClick={()=>navigate(`/trade/${stock.symbol}`)}
className="clickable"
>
<td className="symbol">{stock.symbol}</td>
<td className="company">{stock.companyName}</td>
<td className="mono">
₹{Number(stock.price).toFixed(2)}
</td>
<td>
<span className={`tag ${stock.tradable?"green":"red"}`}>
{stock.tradable?"Active":"Halted"}
</span>
</td>
</tr>
))}
</tbody>
</table>

</div>


{/* POSITIONS */}
<div className="card">

<div className="card-header">
<span className="card-title">
My Positions
</span>

<button
className="btn"
onClick={()=>navigate("/portfolio")}
>
View Portfolio
<ArrowRight size={14}/>
</button>

</div>


{portfolio.length===0? (

<div className="empty">
<Briefcase size={32}/>
<p>No positions yet</p>

<button
className="btn-primary"
onClick={()=>navigate("/trade")}
>
Start Trading
</button>

</div>

) : (

<table>
<thead>
<tr>
<th>Symbol</th>
<th>Qty</th>
<th>Avg</th>
<th>P/L</th>
</tr>
</thead>

<tbody>
{portfolio.slice(0,6).map(p=>{
const pl = pnl[p.stock?.symbol] || 0

return(
<tr key={p.id}>
<td>{p.stock?.symbol}</td>
<td>{p.quantity}</td>
<td>₹{Number(p.averagePrice).toFixed(2)}</td>
<td className={pl>=0?"positive":"negative"}>
{pl>=0?"+":""}₹{Number(pl).toFixed(2)}
</td>
</tr>
)
})}
</tbody>
</table>

)}

</div>

</div>


{/* GAINERS LOSERS */}
<div className="gainers-losers">

<div className="card">
<div className="card-header">
<span className="card-title">Top Gainers</span>
</div>

{gainers.map(s=>(
<div key={s.id} className="row">
<span>{s.symbol}</span>
<span className="positive">
₹{Number(s.price).toFixed(2)}
</span>
</div>
))}
</div>


<div className="card">
<div className="card-header">
<span className="card-title">Top Losers</span>
</div>

{losers.map(s=>(
<div key={s.id} className="row">
<span>{s.symbol}</span>
<span className="negative">
₹{Number(s.price).toFixed(2)}
</span>
</div>
))}
</div>

</div>

</div>
)
}


function getGreeting(){
const h = new Date().getHours()
if(h<12) return "morning"
if(h<17) return "afternoon"
return "evening"
}