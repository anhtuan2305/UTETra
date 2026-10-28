package vn.tuan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "carriers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Carrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne private Shop shop;

    // Map cột name
    @Column(name = "name", length = 100)
    private String name;

    // Map thêm cột carrier_name để thỏa mãn cả 2 cột NOT NULL trong DB
    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    @Column(name = "base_fee", precision = 18, scale = 2)
    private BigDecimal baseFee;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Carrier(String name, BigDecimal baseFee, Boolean isActive) {
        this.name = name;
        this.carrierName = name; // Đồng bộ cả 2 trường cùng giá trị
        this.baseFee = baseFee;
        this.isActive = isActive;
        this.createdAt = LocalDateTime.now();
    }
}