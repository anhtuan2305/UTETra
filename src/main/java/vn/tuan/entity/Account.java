package vn.tuan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Builder.Default private Boolean emailVerified = false;
    private String otpPurpose;
    @Builder.Default private Integer otpAttempts = 0;
    private LocalDateTime otpSentAt;

    @Column(nullable = false, unique = true, length = 100)
    private String email;
    @Column(length=100) private String fullName;
    @Column(length=20) private String phone;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 20)
    private String role; // USER, VENDOR, MANAGER, ADMIN, SHIPPER

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = false;
        }
    }
}