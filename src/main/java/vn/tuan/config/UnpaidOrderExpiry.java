package vn.tuan.config;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;import org.springframework.scheduling.annotation.*;import vn.tuan.service.OrderLifecycleService;import vn.tuan.repository.OrderRepository;import java.util.List;
@Component @EnableScheduling @RequiredArgsConstructor public class UnpaidOrderExpiry {
 private final OrderRepository orders;private final OrderLifecycleService lifecycle;
 @Scheduled(fixedDelay=60000) public void expire(){for(var o:orders.findByPaymentMethodAndPaymentStatusInAndStatus("VNPAY",List.of("UNPAID","FAILED"),"NEW"))lifecycle.expire(o.getId());}
}
