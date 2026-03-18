package com.trading.platform.order.service;

import com.trading.platform.engine.OrderBook;
import com.trading.platform.engine.OrderBookManager;
import com.trading.platform.engine.service.MatchingEngine;
import com.trading.platform.order.dto.OrderRequest;
import com.trading.platform.order.entity.Order;
import com.trading.platform.order.model.*;
import com.trading.platform.order.repository.OrderRepository;
import com.trading.platform.stock.entity.Stock;
import com.trading.platform.stock.repository.StockRepository;
import com.trading.platform.trade.service.TradeService;
import com.trading.platform.user.entity.User;
import com.trading.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.trading.platform.trade.entity.Trade;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository  orderRepository;
    private final OrderBookManager orderBookManager;
    private final MatchingEngine   matchingEngine;
    private final StockRepository  stockRepository;
    private final UserRepository   userRepository;
    private final TradeService     tradeService;

    @Transactional
    public Order placeOrder(String username, OrderRequest request) {

        // 🔹 Fetch user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isTradingEnabled()) {
            throw new RuntimeException("Trading is paused for this user");
        }

        // 🔹 Fetch stock
        Stock stock = stockRepository.findBySymbol(request.getSymbol())
                .orElseThrow(() -> new RuntimeException("Stock not found: " + request.getSymbol()));

        if (!stock.isTradable()) {
            throw new RuntimeException("Trading is disabled for " + request.getSymbol());
        }

        // 🔹 Parse enums
        OrderSide side = OrderSide.valueOf(request.getSide().toUpperCase());
        OrderType type = OrderType.valueOf(request.getType().toUpperCase());

        BigDecimal price = (type == OrderType.MARKET)
                ? stock.getPrice()
                : request.getPrice();

        Long quantity = request.getQuantity();

        // ============================
        // 🔥 BUY LOGIC (WITH MARGIN)
        // ============================
        if (side == OrderSide.BUY) {

            BigDecimal totalCost = price.multiply(BigDecimal.valueOf(quantity));

            boolean useMargin = request.getUseMargin() != null && request.getUseMargin();

            User systemSeller = getSystemAccount();

            if (!useMargin) {
                // ✅ NORMAL BUY (STRICT CHECK)

                if (user.getBalance().compareTo(totalCost) < 0) {
                    throw new RuntimeException("Insufficient balance. Need ₹" + totalCost);
                }

                // 🚫 NO margin here
                tradeService.executeTrade(user, systemSeller, stock, quantity, price);

            } else {
                // 🔥 MARGIN BUY

                int marginPercent = 5;

                BigDecimal investedAmount = totalCost
                        .multiply(BigDecimal.valueOf(marginPercent))
                        .divide(BigDecimal.valueOf(100));

                BigDecimal borrowedAmount = totalCost.subtract(investedAmount);

                if (user.getBalance().compareTo(investedAmount) < 0) {
                    throw new RuntimeException("Insufficient margin. Need ₹" + investedAmount);
                }

                int multiplier = 100 / marginPercent;

                tradeService.executeTradeWithMargin(
                        user,
                        systemSeller,
                        stock,
                        quantity,
                        price,
                        multiplier,
                        investedAmount,
                        borrowedAmount
                );
            }
        }

        // ============================
        // 🔥 SELL LOGIC
        // ============================
        if (side == OrderSide.SELL) {

            Long portfolioQty = tradeService.getPortfolioQuantity(user, stock);

            if (portfolioQty < quantity)
                throw new RuntimeException("Insufficient shares");

            User systemBuyer = getSystemAccount();

            // 🔥 CHECK FOR MARGIN TRADE
            Trade trade = tradeService.getLatestTrade(user, stock);

            if (trade != null && trade.isMarginTrade() && !trade.isAutoSold()) {

                tradeService.sellWithMargin(user, stock, quantity, price, trade);

            } else {

                tradeService.executeTrade(systemBuyer, user, stock, quantity, price);
            }
        }

        // ============================
        // 🔥 SAVE ORDER
        // ============================
        Order order = Order.builder()
                .user(user)
                .stock(stock)
                .side(side)
                .type(type)
                .price(price)
                .quantity(quantity)
                .remainingQuantity(0L)
                .status(OrderStatus.FILLED)
                .build();

        orderRepository.save(order);

        // ============================
        // 🔥 ORDER BOOK (LIMIT ORDERS)
        // ============================
        if (type == OrderType.LIMIT) {
            OrderBook orderBook = orderBookManager.getOrderBook(stock.getSymbol());
            ReentrantLock lock = orderBook.getLock();

            lock.lock();
            try {
                orderBook.addOrder(order);
                matchingEngine.match(orderBook);
            } finally {
                lock.unlock();
            }
        }

        log.info("[OrderService] {} {} x {} @ ₹{} executed for {}",
                side, quantity, stock.getSymbol(), price, username);

        return order;
    }

    // 🔹 SYSTEM ACCOUNT (MARKET MAKER)
    private User getSystemAccount() {
        return userRepository.findByUsername("system")
                .orElseGet(() -> {
                    User system = User.builder()
                            .username("system")
                            .email("system@tradepro.internal")
                            .password("$2a$10$disabled")
                            .balance(BigDecimal.valueOf(999999999))
                            .build();
                    return userRepository.save(system);
                });
    }

    // 🔹 FETCH USER ORDERS
    public List<Order> getOrdersForUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}