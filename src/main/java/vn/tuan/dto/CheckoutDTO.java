package vn.tuan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckoutDTO {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String receiverName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String streetAddress;

    @NotBlank(message = "Tỉnh / Thành phố không được để trống")
    private String city = "TP. Hồ Chí Minh";

    private String paymentMethod = "COD"; // COD hoặc VNPAY

    private String note;
}