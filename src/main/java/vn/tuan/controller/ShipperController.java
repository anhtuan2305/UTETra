package vn.tuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.tuan.entity.Order;
import vn.tuan.repository.OrderRepository;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/shipper")
public class ShipperController {

    @Autowired
    private OrderRepository orderRepository;

    // Xem danh sách đơn cần giao (đơn đã chuẩn bị xong hoặc đang đi giao)
    @GetMapping("/orders")
    public String listDeliveries(Model model) {
        // Lấy các đơn hàng có trạng thái PREPARING (quán đã làm xong), SHIPPING (đang giao), COMPLETED
        List<String> activeStatuses = Arrays.asList("PREPARING", "SHIPPING", "COMPLETED");
        List<Order> orders = orderRepository.findByStatusInOrderByCreatedAtDesc(activeStatuses);

        model.addAttribute("orders", orders);
        return "shipper/orders";
    }

    // Cập nhật trạng thái giao hàng
    @PostMapping("/orders/update-status")
    public String updateOrderStatus(@RequestParam("orderId") Integer orderId,
                                    @RequestParam("status") String status,
                                    RedirectAttributes redirectAttributes) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus(status);
            // Nếu giao thành công và là COD thì đánh dấu đã thanh toán
            if ("COMPLETED".equalsIgnoreCase(status) && "COD".equalsIgnoreCase(order.getPaymentMethod())) {
                order.setPaymentStatus("PAID");
            }
            orderRepository.save(order);
            redirectAttributes.addFlashAttribute("message", "Đã cập nhật trạng thái đơn #" + orderId + " thành: " + status);
        }
        return "redirect:/shipper/orders";
    }
}