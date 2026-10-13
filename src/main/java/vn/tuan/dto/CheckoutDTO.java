package vn.tuan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CheckoutDTO {

    @NotNull(message = "Vui lòng chọn chi nhánh quán phục vụ")
    private Integer shopId;

    @NotBlank(message = "Tên người nhận không được để trống")
    private String receiverName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String streetAddress;

    @NotBlank(message = "Tỉnh / Thành phố không được để trống")
    private String city = "TP. Hồ Chí Minh";

    private String paymentMethod = "COD";

    private String note;

    public CheckoutDTO() {
    }

    public CheckoutDTO(Integer shopId, String receiverName, String phone, String streetAddress, String city, String paymentMethod, String note) {
        this.shopId = shopId;
        this.receiverName = receiverName;
        this.phone = phone;
        this.streetAddress = streetAddress;
        this.city = city;
        this.paymentMethod = paymentMethod;
        this.note = note;
    }

    public Integer getShopId() {
        return shopId;
    }

    public void setShopId(Integer shopId) {
        this.shopId = shopId;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}