package com.trading.platform.trade.service;

import com.trading.platform.notification.service.NotificationService;
import com.trading.platform.portfolio.entity.Portfolio;
import com.trading.platform.portfolio.repository.PortfolioRepository;
import com.trading.platform.portfolio.service.PortfolioService;
import com.trading.platform.stock.entity.Stock;
import com.trading.platform.trade.entity.Trade;
import com.trading.platform.trade.repository.TradeRepository;
import com.trading.platform.user.entity.User;
import com.trading.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final PortfolioService portfolioService;
    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ================= NORMAL TRADE =================
    @Transactional
    public void executeTrade(User buyer, User seller, Stock stock,
                             Long quantity, BigDecimal price) {

        BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(quantity));

        Trade trade = Trade.builder()
                .buyer(buyer)
                .seller(seller)
                .stock(stock)
                .quantity(quantity)
                .price(price)
                .executedAt(LocalDateTime.now())
                .marginTrade(false)
                .autoSold(false)
                .build();

        tradeRepository.save(trade);

        buyer.setBalance(buyer.getBalance().subtract(totalPrice));
        seller.setBalance(seller.getBalance().add(totalPrice));

        userRepository.save(buyer);
        userRepository.save(seller);

        portfolioService.addStock(buyer, stock, quantity);

        notificationService.createNotification(buyer,
                "✅ Bought " + quantity + " shares of " + stock.getSymbol() +
                        " @ ₹" + price);

        if (!"system".equals(seller.getUsername())) {
            portfolioService.removeStock(seller, stock, quantity);

            notificationService.createNotification(seller,
                    "💰 Sold " + quantity + " shares of " + stock.getSymbol() +
                            " @ ₹" + price);
        }
    }

    // ================= MARGIN BUY =================
    @Transactional
    public void executeTradeWithMargin(User buyer,
                                       User seller,
                                       Stock stock,
                                       Long quantity,
                                       BigDecimal price,
                                       int multiplier,
                                       BigDecimal investedAmount,
                                       BigDecimal borrowedAmount) {

        BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(quantity));

        buyer.setBalance(buyer.getBalance().subtract(investedAmount));
        seller.setBalance(seller.getBalance().add(totalPrice));

        userRepository.save(buyer);
        userRepository.save(seller);

        Trade trade = Trade.builder()
                .buyer(buyer)
                .seller(seller)
                .stock(stock)
                .quantity(quantity)
                .price(price)
                .executedAt(LocalDateTime.now())
                .marginMultiplier(multiplier)
                .investedAmount(investedAmount)
                .borrowedAmount(borrowedAmount)
                .marginTrade(true)
                .autoSold(false)
                .build();

        tradeRepository.save(trade);

        portfolioService.addStock(buyer, stock, quantity);

        notificationService.createNotification(buyer,
                "⚡ Margin BUY: " + quantity + " shares of " + stock.getSymbol() +
                        " @ ₹" + price + " (x" + multiplier + ")");
    }

    // ================= ACTIVE MARGIN =================
    public Trade getActiveMarginTrade(User user, Stock stock) {
        return tradeRepository
                .findTopByBuyerAndStockAndMarginTradeTrueAndAutoSoldFalseOrderByExecutedAtDesc(user, stock)
                .orElse(null);
    }

    // ================= MARGIN SELL =================
    @Transactional
    public void sellWithMargin(User user,
                               Stock stock,
                               Long quantity,
                               BigDecimal price,
                               Trade trade) {

        BigDecimal sellValue = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal borrowed = trade.getBorrowedAmount();

        BigDecimal remaining = sellValue.subtract(borrowed);

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            user.setBalance(user.getBalance().add(remaining));
        }

        // PARTIAL SELL FIX
        if (quantity.equals(trade.getQuantity())) {
            trade.setAutoSold(true);
        } else {
            trade.setQuantity(trade.getQuantity() - quantity);
        }

        userRepository.save(user);
        tradeRepository.save(trade);

        portfolioService.removeStock(user, stock, quantity);

        // 🔥 CREATE SELL TRADE
        Trade sellTrade = Trade.builder()
                .buyer(getSystemAccount())
                .seller(user)
                .stock(stock)
                .quantity(quantity)
                .price(price)
                .executedAt(LocalDateTime.now())
                .marginTrade(true)
                .autoSold(true)
                .build();

        tradeRepository.save(sellTrade);

        notificationService.createNotification(user,
                "📉 Margin position closed for " + stock.getSymbol());
    }

    private User getSystemAccount() {
        return userRepository.findByUsername("system").orElseThrow();
    }

    public List<Trade> getTradesForUser(Long userId) {
        return tradeRepository.findByBuyerIdOrSellerIdOrderByExecutedAtDesc(userId, userId);
    }

    public Long getPortfolioQuantity(User user, Stock stock) {
        return portfolioRepository
                .findByUserIdAndStockId(user.getId(), stock.getId())
                .map(Portfolio::getQuantity)
                .orElse(0L);
    }
}