package vn.tuan.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "street_address", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String streetAddress;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;
}