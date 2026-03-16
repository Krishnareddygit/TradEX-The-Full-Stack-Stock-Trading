package com.trading.platform.user.entity;

import com.trading.platform.common.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private BigDecimal balance;

    private BigDecimal marginMultiplier;

    private BigDecimal usedMargin;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (balance == null) balance = BigDecimal.valueOf(0);
        if (marginMultiplier == null) marginMultiplier = BigDecimal.valueOf(5);
        if (usedMargin == null) usedMargin = BigDecimal.ZERO;
    }
}
