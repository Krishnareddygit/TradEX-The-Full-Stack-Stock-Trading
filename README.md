# TradePro – Real-Time Stock Trading Simulation Platform

TradePro is a **Spring Boot–based stock trading simulation platform** that allows users to place buy and sell orders, view portfolios, and simulate real-time trading with margin leverage.
The platform implements a **matching engine, order book, margin trading, and portfolio management**, similar to modern brokerage systems.

---

## Features

### User Management

* User registration and authentication using **JWT**
* Role-based access control
* Account balance and margin tracking

### Trading Engine

* Real-time **order placement (BUY / SELL)**
* **Market orders** and **Limit orders**
* **Order book management**
* Price-time priority order matching

### Margin Trading

* Configurable **margin multiplier**
* Used margin and available margin tracking
* Leveraged trading support

### Portfolio Tracking

* User stock holdings
* Portfolio quantity validation before selling
* Trade history tracking

### Market Simulation

* Simulated market execution using a **system account**
* Real-time trade execution
* Order book exposure for each stock

---

## Tech Stack

Backend:

* Java 17
* Spring Boot
* Spring Security
* Spring Data JPA
* Hibernate

Database:

* MySQL / PostgreSQL

Other Tools:

* Lombok
* Maven
* JWT Authentication

---

## Project Architecture

```
Controller
   ↓
Service Layer
   ↓
Matching Engine
   ↓
Order Book Manager
   ↓
Repositories (JPA)
   ↓
Database
```

Key modules:

```
user
stock
order
trade
engine
market
security
```

---

## Order Execution Flow

1. User sends order request.
2. System validates:

   * market status
   * available margin
   * portfolio quantity
3. Order is stored in the order book.
4. Matching engine finds matching orders.
5. Trade executes and updates:

   * balances
   * margin usage
   * portfolios
6. Order status becomes **FILLED / PARTIAL / OPEN**.


---


## Market Timing

Market trading hours follow **Indian stock market timings**:

```
Open  : 09:15 AM
Close : 03:30 PM
Closed on Saturday & Sunday
```

---


## Future Improvements

* WebSocket live price updates
* Advanced matching engine
* Stop-loss and bracket orders

---

## Author

Krishna Reddy Nallamilli

---

## License

This project is intended for **educational and simulation purposes only**.
