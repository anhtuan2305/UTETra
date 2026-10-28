package vn.tuan.controller;
import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;import vn.tuan.service.ApiService;import java.util.Map;
@RestController @RequestMapping("/api") @RequiredArgsConstructor public class ApiController {
 private final ApiService service;
 @PostMapping("/auth/token")public Map<String,Object> token(@RequestBody ApiService.Login input){return service.token(input);}
 @GetMapping("/orders")public Object orders(@AuthenticationPrincipal Jwt jwt){return service.myOrders(jwt);}
}
