package com.trading.platform.margin.service;

import com.trading.platform.user.entity.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MarginService {

    public BigDecimal totalBuyingPower(User user) {
        return user.getBalance().multiply(user.getMarginMultiplier());
    }

    public BigDecimal availableMargin(User user) {
        return totalBuyingPower(user).subtract(user.getUsedMargin());
    }

    public BigDecimal requiredMargin(BigDecimal orderValue, BigDecimal multiplier) {
        return orderValue.divide(multiplier, 8, RoundingMode.HALF_UP);
    }

    public boolean hasEnoughMargin(User user, BigDecimal orderValue) {

        BigDecimal required = requiredMargin(orderValue, user.getMarginMultiplier());
        BigDecimal available = availableMargin(user);

        return available.compareTo(required) >= 0;
    }

    public void blockMargin(User user, BigDecimal orderValue) {

        BigDecimal required = requiredMargin(orderValue, user.getMarginMultiplier());

        user.setUsedMargin(
                user.getUsedMargin().add(required)
        );
    }

    public void releaseMargin(User user, BigDecimal orderValue) {

        BigDecimal release = requiredMargin(orderValue, user.getMarginMultiplier());

        user.setUsedMargin(
                user.getUsedMargin().subtract(release)
        );
    }
}