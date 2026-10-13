package vn.tuan.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = true)
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
    private Boolean isDefault = false;

    public Address() {
    }

    public Address(Integer id, Account account, String receiverName, String phone, String streetAddress, String city, Boolean isDefault) {
        this.id = id;
        this.account = account;
        this.receiverName = receiverName;
        this.phone = phone;
        this.streetAddress = streetAddress;
        this.city = city;
        this.isDefault = isDefault;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}