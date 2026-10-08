package vn.tuan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "Product_Options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "option_group", nullable = false, length = 50)
    private String optionGroup; // Ví dụ: "Size", "Topping", "Đường", "Đá"

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName; // Ví dụ: "Size L", "Trân châu đen"

    @Column(name = "extra_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal extraPrice = BigDecimal.ZERO; // Phụ thu (Size L +10k, trân châu +5k)
}