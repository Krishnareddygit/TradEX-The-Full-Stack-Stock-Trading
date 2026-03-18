package com.trading.platform.trade.repository;

import com.trading.platform.stock.entity.Stock;
import com.trading.platform.trade.entity.Trade;
import com.trading.platform.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByBuyerIdOrSellerIdOrderByExecutedAtDesc(Long buyerId, Long sellerId);

    @Modifying
    @Query("DELETE FROM Trade t WHERE t.buyer.id = :userId OR t.seller.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    Optional<Trade> findTopByBuyerAndStockOrderByExecutedAtDesc(User buyer, Stock stock);
}
