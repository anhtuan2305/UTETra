package vn.tuan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne private Shop shop;
    private LocalDateTime expiresAt;
    private String type = "PRODUCT";

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private Integer discountPercent;

    @Column(precision = 18, scale = 2)
    private BigDecimal maxDiscount;

    @Column(precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    private Boolean isActive = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Promotion(String code, Integer discountPercent, BigDecimal maxDiscount, BigDecimal minOrderAmount, Boolean isActive) {
        this.code = code;
        this.discountPercent = discountPercent;
        this.maxDiscount = maxDiscount;
        this.minOrderAmount = minOrderAmount;
        this.isActive = isActive;
        this.createdAt = LocalDateTime.now();
    }
}