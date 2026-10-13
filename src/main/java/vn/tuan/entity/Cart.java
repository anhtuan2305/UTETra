package vn.tuan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Cho phép null đối với khách vãng lai
    @OneToOne
    @JoinColumn(name = "account_id", nullable = true)
    private Account account;

    // Định danh phiên làm việc cho khách chưa đăng nhập
    @Column(name = "session_id", length = 100)
    private String sessionId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();
}