package com.trading.platform.stock.service;

import com.trading.platform.admin.dto.CreateStockRequest;
import com.trading.platform.stock.entity.Stock;
import com.trading.platform.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    public void createStock(CreateStockRequest request) {
        Stock stock = Stock.builder()
                .symbol(request.getSymbol())
                .companyName(request.getCompanyName())
                .price(request.getPrice())
                .totalShares(request.getTotalShares())
                .tradable(true)
                .build();
        stockRepository.save(stock);
    }

    public void deleteStock(Long stockId) {
        stockRepository.deleteById(stockId);
    }

    public void enableTrading(Long stockId) {
        Stock stock = stockRepository.findById(stockId).orElseThrow();
        stock.setTradable(true);
        stockRepository.save(stock);
    }

    public void disableTrading(Long stockId) {
        Stock stock = stockRepository.findById(stockId).orElseThrow();
        stock.setTradable(false);
        stockRepository.save(stock);
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    public Stock getStockBySymbol(String symbol) {
        return stockRepository.findBySymbol(symbol).orElseThrow();
    }

    public BigDecimal getLivePrice(String symbol) {
        return stockRepository.findBySymbol(symbol)
                .orElseThrow()
                .getPrice();
    }

}
