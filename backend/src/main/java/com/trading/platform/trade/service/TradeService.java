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

    // ============================
    // NORMAL TRADE
    // ============================
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

        User freshBuyer = userRepository.findById(buyer.getId()).orElse(buyer);
        User freshSeller = userRepository.findById(seller.getId()).orElse(seller);

        freshBuyer.setBalance(freshBuyer.getBalance().subtract(totalPrice));
        freshSeller.setBalance(freshSeller.getBalance().add(totalPrice));

        userRepository.save(freshBuyer);
        userRepository.save(freshSeller);

        portfolioService.addStock(freshBuyer, stock, quantity);

        if (!"system".equals(freshSeller.getUsername())) {
            portfolioService.removeStock(freshSeller, stock, quantity);
        }

        log.info("[TradeService] Normal trade executed");
    }

    // ============================
    // MARGIN BUY
    // ============================
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

        log.info("[TradeService] Margin BUY executed");
    }

    // ============================
    // 🔥 GET LATEST TRADE
    // ============================
    public Trade getLatestTrade(User user, Stock stock) {
        return tradeRepository
                .findTopByBuyerAndStockOrderByExecutedAtDesc(user, stock)
                .orElse(null);
    }

    // ============================
    // 🔥 FINAL MARGIN SELL (CORRECT)
    // ============================
    @Transactional
    public void sellWithMargin(User user,
                               Stock stock,
                               Long quantity,
                               BigDecimal price,
                               Trade trade) {

        BigDecimal sellValue = price.multiply(BigDecimal.valueOf(quantity));

        BigDecimal borrowed = trade.getBorrowedAmount();
        BigDecimal invested = trade.getInvestedAmount();

        BigDecimal remaining = sellValue.subtract(borrowed);

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            // ✅ PROFIT CASE
            user.setBalance(user.getBalance().add(remaining));
        } else {
            // ❌ LOSS CASE
            // user already lost invested amount
            // no additional deduction needed
        }

        trade.setAutoSold(true);

        userRepository.save(user);
        tradeRepository.save(trade);

        portfolioService.removeStock(user, stock, quantity);

        notificationService.createNotification(user,
                "📉 Margin position closed for " + stock.getSymbol());

        log.info("[TradeService] Margin SELL executed");
    }

    // ============================
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