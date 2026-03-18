package com.trading.platform.trade.entity;

import com.trading.platform.stock.entity.Stock;
import com.trading.platform.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User buyer;

    @ManyToOne
    private User seller;

    @ManyToOne
    private Stock stock;

    private Long quantity;
    private BigDecimal price;
    private BigDecimal brokerageFee;
    private LocalDateTime executedAt;

    private Integer marginMultiplier;
    private BigDecimal investedAmount;
    private BigDecimal borrowedAmount;
    private boolean marginTrade;
    private boolean autoSold;
}