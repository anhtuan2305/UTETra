package vn.tuan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressDTO {
    private Integer id;

    @NotBlank(message = "Tên người nhận không được để trống")
    private String receiverName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String streetAddress;

    private String city = "TP. Hồ Chí Minh";

    private Boolean isDefault = false;
}