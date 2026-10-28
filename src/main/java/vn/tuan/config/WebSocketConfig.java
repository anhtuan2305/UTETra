package vn.tuan.config;
import lombok.RequiredArgsConstructor;import org.springframework.context.annotation.Configuration;import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocket @RequiredArgsConstructor public class WebSocketConfig implements WebSocketConfigurer {
 private final OrderSocket socket;public void registerWebSocketHandlers(WebSocketHandlerRegistry r){r.addHandler(socket,"/ws/orders");}
}
