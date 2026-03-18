# 📈 TradEX – Stock Trading Simulation Platform

TradEX is a **full-stack stock trading simulation platform** that enables users to practice trading in a realistic environment. It mimics real-world trading features such as portfolio tracking, margin trading, trade execution, and wallet management.

---

# 🚀 Features

### 🔐 Authentication & Security

* User registration and login using **JWT authentication**
* Secure API access with **Spring Security**

---

### 📊 Trading System

* Buy and sell stocks (Market & Limit orders)
* Real-time order validation
* Instant trade execution (system as counterparty)
* Order book support for limit orders

---

### 💼 Portfolio Management

* Track owned stocks
* View quantity and average price
* Real-time portfolio valuation

---

### 💰 Wallet System

* Deposit and withdraw funds
* Automatic balance updates after trades
* Balance validation before placing orders

---

### 📉 Profit & Loss (P&L)

* Track profit/loss per stock
* Aggregated total P&L
* Live updates based on stock price

---

### ⚡ Margin Trading (🔥 NEW)

* Users can enable/disable margin while placing orders
* Default margin: **5% (20x leverage)**
* Only a small portion of capital is required to trade larger amounts

#### Example:

```text
Invest ₹50 → Trade ₹1000 (20x leverage)
```

---

### 🔄 Margin Settlement Logic

* On **profit** → remaining amount credited after repaying borrowed funds
* On **loss** → user loses only the invested margin (no extra deduction)

#### Example:

```text
Buy ₹1000 (₹50 user + ₹950 borrowed)

Sell ₹1100 → User gets ₹150 ✅
Sell ₹900  → User loses ₹50 ❌
```

---

### ⚠️ Liquidation Logic

* Positions are automatically considered **liquidated** when:

```text
Sell Value <= Borrowed Amount
```

* Small losses → no liquidation
* Large losses → full margin loss (position closed)

---

### 📜 Trade History

* Stores all executed trades
* Includes:

  * Stock symbol
  * Quantity
  * Price
  * Timestamp
  * Margin details (if applicable)

---

### ⭐ Watchlist

* Add/remove favorite stocks
* Quick access to tracked stocks

---

### 🏆 Leaderboard

* Displays top-performing traders
* Based on overall P&L

---

### 🟢 Market Status

* Shows whether market is **OPEN / CLOSED**

---

# 🛠️ Tech Stack

## Frontend

* React
* React Router
* CSS
* Lucide Icons
* Recharts (charts)

## Backend

* Spring Boot
* Spring Security
* JWT Authentication
* REST APIs

## Database

* PostgreSQL

## Tools

* Git
* Maven
* Postman

---

# 🏗️ System Architecture

```text
Frontend (React)
        |
        v
   REST APIs
        |
        v
Spring Boot Backend
        |
        v
 PostgreSQL Database
```

---

# 📦 Core Modules

### 🔐 Authentication

* JWT-based login & registration
* Secure endpoints

---

### 📈 Trading Engine

* Buy/Sell execution
* Margin & non-margin support
* Balance validation
* Order matching (limit orders)

---

### 💼 Portfolio

* Tracks holdings
* Calculates total value
* Displays P&L

---

### 💰 Wallet

* Deposit / Withdraw funds
* Auto-update after trades

---

### 📊 Trade Management

* Stores trade history
* Margin tracking (borrowed + invested)

---

# ⚙️ Setup Instructions

## Backend

```bash
cd backend
mvn spring-boot:run
```

Runs on:

```
http://localhost:8080
```

---

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on:

```
http://localhost:5173
```

---

# 🚀 Future Improvements

* 📡 WebSocket-based live market updates
* 📊 Advanced charting (candlestick, indicators)
* ⚠️ Margin call warnings before liquidation
* 🔥 Auto-liquidation scheduler
* 📉 Risk analysis & leverage control

---

# 👨‍💻 Author

**Krishna Reddy Nallamilli**
