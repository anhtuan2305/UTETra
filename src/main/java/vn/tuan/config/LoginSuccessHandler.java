package vn.tuan.config;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;import org.springframework.security.web.authentication.AuthenticationSuccessHandler;import org.springframework.security.core.Authentication;import jakarta.servlet.http.*;import java.io.IOException;import vn.tuan.repository.AccountRepository;import vn.tuan.service.CartService;
@Component @RequiredArgsConstructor public class LoginSuccessHandler implements AuthenticationSuccessHandler {
 private final AccountRepository accounts;private final CartService carts;
 public void onAuthenticationSuccess(HttpServletRequest req,HttpServletResponse res,Authentication auth)throws IOException{var a=accounts.findByEmail(auth.getName()).orElseThrow();req.getSession().setAttribute("credentialVersion",a.getPassword());Object guest=req.getSession().getAttribute("guestCartKey");if(guest!=null)carts.mergeGuest(auth.getName(),guest.toString());res.sendRedirect("/");}
}
