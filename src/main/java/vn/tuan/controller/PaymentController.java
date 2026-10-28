package vn.tuan.controller;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.*;import org.springframework.ui.Model;import java.util.*;import vn.tuan.service.PaymentService;
@Controller @RequiredArgsConstructor public class PaymentController {
 private final PaymentService payments;
 @GetMapping("/payment/ipn") @ResponseBody public Map<String,String> ipn(@RequestParam Map<String,String> params){return payments.ipn(params);}
 @GetMapping("/payment/return") public String result(@RequestParam Map<String,String> params,Model m){m.addAttribute("message",payments.valid(params)?"Đã nhận kết quả từ cổng thanh toán. Trạng thái đơn chỉ được xác nhận sau thông báo IPN của VNPAY. Hãy kiểm tra lịch sử hoặc tra cứu đơn.":"Chữ ký phản hồi thanh toán không hợp lệ.");return "payment-result";}
}
