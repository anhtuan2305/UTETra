package vn.tuan.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Cart_Items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    // Lưu các lựa chọn Topping/Size dưới dạng chuỗi Text hoặc JSON
    @Column(name = "options_json", columnDefinition = "NVARCHAR(500)")
    private String optionsJson;
}