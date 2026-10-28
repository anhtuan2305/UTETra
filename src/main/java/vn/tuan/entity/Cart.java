package vn.tuan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Version
    private Long version;

    // Khách vãng lai chưa có tài khoản.
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = true)
    private Account account;

    // Giỏ khách: GUEST_...
    // Giỏ thành viên: MEMBER_...
    @Column(name = "session_id", length = 100)
    private String sessionId;

    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void ensureSessionId() {
        if (sessionId == null || sessionId.isBlank()) {
            String prefix = account != null ? "MEMBER_" : "GUEST_";
            sessionId = prefix + UUID.randomUUID();
        }
    }
}