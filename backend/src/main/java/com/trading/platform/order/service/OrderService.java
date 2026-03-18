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
import com.trading.platform.trade.entity.Trade;
import com.trading.platform.trade.service.TradeService;
import com.trading.platform.user.entity.User;
import com.trading.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderBookManager orderBookManager;
    private final MatchingEngine matchingEngine;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final TradeService tradeService;

    @Transactional
    public Order placeOrder(String username, OrderRequest request) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isTradingEnabled()) {
            throw new RuntimeException("Trading is paused");
        }

        Stock stock = stockRepository.findBySymbol(request.getSymbol())
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        if (!stock.isTradable()) {
            throw new RuntimeException("Stock not tradable");
        }

        OrderSide side = OrderSide.valueOf(request.getSide().toUpperCase());
        OrderType type = OrderType.valueOf(request.getType().toUpperCase());

        BigDecimal price = (type == OrderType.MARKET)
                ? stock.getPrice()
                : request.getPrice();

        Long quantity = request.getQuantity();
        BigDecimal totalCost = price.multiply(BigDecimal.valueOf(quantity));

        // 🔥 FIXED
        boolean useMargin = Boolean.TRUE.equals(request.getUseMargin());

        // ================= BUY =================
        if (side == OrderSide.BUY) {

            User systemSeller = getSystemAccount();

            if (!useMargin) {
                if (user.getBalance().compareTo(totalCost) < 0) {
                    throw new RuntimeException("Insufficient balance");
                }

                tradeService.executeTrade(user, systemSeller, stock, quantity, price);

            } else {
                int marginPercent = 5;

                BigDecimal investedAmount = totalCost
                        .multiply(BigDecimal.valueOf(marginPercent))
                        .divide(BigDecimal.valueOf(100));

                BigDecimal borrowedAmount = totalCost.subtract(investedAmount);
                int multiplier = 100 / marginPercent;

                if (user.getBalance().compareTo(investedAmount) < 0) {
                    throw new RuntimeException("Insufficient margin funds");
                }

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

        // ================= SELL =================
        if (side == OrderSide.SELL) {

            Long portfolioQty = tradeService.getPortfolioQuantity(user, stock);

            if (portfolioQty < quantity)
                throw new RuntimeException("Insufficient shares");

            User systemBuyer = getSystemAccount();

            Trade marginTrade = tradeService.getActiveMarginTrade(user, stock);

            if (marginTrade != null) {
                tradeService.sellWithMargin(user, stock, quantity, price, marginTrade);
            } else {
                tradeService.executeTrade(systemBuyer, user, stock, quantity, price);
            }
        }

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

        // OPTIONAL ORDER BOOK
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

        return order;
    }

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

    public List<Order> getOrdersForUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}