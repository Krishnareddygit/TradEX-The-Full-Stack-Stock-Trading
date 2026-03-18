package com.trading.platform.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderRequest {
    private String symbol;
    private String side;
    private String type;
    private Long quantity;
    private BigDecimal price;

    // 🔥 NEW
    private Boolean useMargin = true;     // default ON
    private Integer marginPercent = 5;   // default 5%
}