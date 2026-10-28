package vn.tuan.config;
import org.springframework.web.filter.OncePerRequestFilter;import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.*;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.authority.SimpleGrantedAuthority;import vn.tuan.repository.AccountRepository;
public class SessionAccountFilter extends OncePerRequestFilter {
 private final AccountRepository accounts;public SessionAccountFilter(AccountRepository a){accounts=a;}
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth!=null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())){
   var a=accounts.findByEmail(auth.getName()).orElse(null);
   Object credential=req.getSession(false)==null?null:req.getSession(false).getAttribute("credentialVersion");
   boolean passwordChanged=a!=null && credential!=null && !credential.equals(a.getPassword());
   if(a==null || !Boolean.TRUE.equals(a.getIsActive()) || passwordChanged){SecurityContextHolder.clearContext();if(req.getSession(false)!=null)req.getSession(false).invalidate();res.sendRedirect("/login?error=true");return;}
   var refreshed=new UsernamePasswordAuthenticationToken(auth.getPrincipal(),auth.getCredentials(),java.util.List.of(new SimpleGrantedAuthority("ROLE_"+a.getRole().replace("ROLE_",""))));SecurityContextHolder.getContext().setAuthentication(refreshed);
  }chain.doFilter(req,res);
 }
}
