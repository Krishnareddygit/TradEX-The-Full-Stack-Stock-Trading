package com.trading.platform.margin.service;

import com.trading.platform.trade.entity.Trade;
import com.trading.platform.trade.repository.TradeRepository;
import com.trading.platform.user.entity.User;
import com.trading.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MarginService {

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;

    public void checkAndLiquidate(Trade trade, BigDecimal currentPrice) {

        if (!trade.isMarginTrade() || trade.isAutoSold()) return;

        BigDecimal currentValue = currentPrice
                .multiply(BigDecimal.valueOf(trade.getQuantity()));

        BigDecimal originalValue = trade.getPrice()
                .multiply(BigDecimal.valueOf(trade.getQuantity()));

        BigDecimal loss = originalValue.subtract(currentValue);

        if (loss.compareTo(trade.getBorrowedAmount()) >= 0) {
            autoSell(trade, currentPrice);
        }
    }

    private void autoSell(Trade trade, BigDecimal currentPrice) {

        User user = trade.getBuyer();

        BigDecimal sellValue = currentPrice
                .multiply(BigDecimal.valueOf(trade.getQuantity()));

        BigDecimal remaining = sellValue.subtract(trade.getBorrowedAmount());

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            user.setBalance(user.getBalance().add(remaining));
        }

        trade.setAutoSold(true);

        userRepository.save(user);
        tradeRepository.save(trade);

        System.out.println("🚨 AUTO SELL: Trade " + trade.getId());
    }
}