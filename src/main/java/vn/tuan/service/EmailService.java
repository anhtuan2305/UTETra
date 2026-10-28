package vn.tuan.service;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service;import org.springframework.beans.factory.annotation.Value;import org.springframework.mail.*;import org.springframework.mail.javamail.JavaMailSender;
@Service @RequiredArgsConstructor public class EmailService {
 private final JavaMailSender mail; @Value("${spring.mail.username:}") private String from; @Value("${app.mail.enabled:false}") private boolean enabled; @Value("${app.mail.demo:false}")private boolean demo;
 public void sendOtpEmail(String email,String otp){if(!enabled){if(demo){System.out.println("[DEMO OTP] "+email+": "+otp);return;}throw new IllegalArgumentException("Chưa cấu hình SMTP. Hãy liên hệ quản trị viên.");}SimpleMailMessage m=new SimpleMailMessage();m.setFrom(from);m.setTo(email);m.setSubject("UTETra - Mã xác thực");m.setText("Mã xác thực của bạn: "+otp+". Có hiệu lực 5 phút. Không chia sẻ cho người khác.");try{mail.send(m);}catch(MailException e){throw new IllegalArgumentException("Không gửi được email. Vui lòng thử lại hoặc kiểm tra cấu hình SMTP.");}}
}
