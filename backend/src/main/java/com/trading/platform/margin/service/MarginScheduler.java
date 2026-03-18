package com.trading.platform.margin.service;

import com.trading.platform.margin.service.MarginService;
import com.trading.platform.stock.service.StockService;
import com.trading.platform.trade.entity.Trade;
import com.trading.platform.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MarginScheduler {

    private final TradeRepository tradeRepository;
    private final MarginService marginService;
    private final StockService stockService;

    @Scheduled(fixedRate = 5000)
    public void monitorTrades() {

        List<Trade> trades = tradeRepository.findAll();

        for (Trade trade : trades) {

            if (!trade.isMarginTrade() || trade.isAutoSold()) continue;

            BigDecimal currentPrice =
                    stockService.getLivePrice(trade.getStock().getSymbol());

            marginService.checkAndLiquidate(trade, currentPrice);
        }
    }
}