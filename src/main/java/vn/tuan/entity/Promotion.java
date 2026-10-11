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
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id") // Nếu null thì là mã sàn của Admin, có ID là mã riêng của Shop
    private Shop shop;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private Integer discountPercent;

    @Column(precision = 18, scale = 2)
    private BigDecimal maxDiscount;

    @Column(precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
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