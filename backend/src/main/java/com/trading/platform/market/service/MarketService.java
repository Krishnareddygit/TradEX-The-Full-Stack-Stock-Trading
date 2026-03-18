package com.trading.platform.market.service;

import com.trading.platform.market.model.MarketStatus;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class MarketService {

    private final LocalTime marketOpen = LocalTime.of(9, 15);
    private final LocalTime marketClose = LocalTime.of(15, 30);

    public boolean isMarketOpen() {

        LocalDate today = LocalDate.now();
        DayOfWeek day = today.getDayOfWeek();

        // Market closed on weekends
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime now = LocalTime.now();

        return !now.isBefore(marketOpen) && !now.isAfter(marketClose);
    }

    public MarketStatus getMarketStatus() {
        return isMarketOpen() ? MarketStatus.OPEN : MarketStatus.CLOSED;
    }
}