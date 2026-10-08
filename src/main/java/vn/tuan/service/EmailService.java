package vn.tuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async // Gửi mail chạy ngầm ở luồng riêng, không làm đứng giao diện web
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Mã xác thực tài khoản UTETra");
            message.setText("Xin chào,\n\nMã OTP kích hoạt tài khoản UTETra của bạn là: " + otp 
                    + "\nMã có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này cho ai.");
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}