package vn.tuan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Shops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Mỗi shop thuộc về 1 tài khoản Vendor
    @OneToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "id", unique = true, nullable = false)
    private Account owner;

    @Column(name = "shop_name", nullable = false, length = 150)
    private String shopName;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ACTIVE, LOCKED

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.valueOf(5.0);

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}