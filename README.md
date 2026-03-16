
# TradeHub – Stock Trading Simulation Platform

TradeHub is a full-stack stock trading simulation platform that allows users to practice buying and selling stocks in a simulated environment.
The platform provides portfolio tracking, trade history, and wallet management features similar to real trading applications.

---

## Features

* User authentication and authorization
* Buy and sell stocks
* Portfolio management
* Profit and loss (P&L) calculation
* Wallet system (deposit and withdraw funds)
* Trade history tracking
* Favorites watchlist
* Leaderboard for top traders
* Market status indicator

---

## Tech Stack

### Frontend

* React
* React Router
* CSS
* Lucide Icons

### Backend

* Spring Boot
* Spring Security
* JWT Authentication
* REST APIs

### Database

* Postgres

### Tools

* Git
* Maven
* Postman

---

## System Architecture

```
Frontend (React)
        |
        v
REST APIs
        |
        v
Spring Boot Backend
        |
        v
MySQL Database
```

---

## Core Modules

### Authentication

* JWT-based login and registration
* Secure API access using Spring Security

### Trading System

* Users can place buy and sell orders
* Orders are validated based on available balance
* Transactions update portfolio holdings

### Portfolio

* Displays user holdings
* Calculates portfolio value
* Shows profit and loss (P&L)

### Wallet

* Users can deposit funds
* Users can withdraw funds
* Balance updates automatically after trades

### Trade History

* Stores executed trades
* Displays stock symbol, quantity, price, and execution time

---


### Backend Setup

```
cd backend
mvn spring-boot:run
```

Backend runs on:

```
http://localhost:8080
```

---

### Frontend Setup

```
cd frontend
npm install
npm run dev
```

Frontend runs on:

```
http://localhost:5173
```


---

## Future Improvements

* Real-time stock price updates
* Advanced stock charts
* Margin trading support
* WebSocket-based live market feed

---

## Author

Krishna Reddy Nallamilli
