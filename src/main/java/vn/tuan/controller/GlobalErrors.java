package vn.tuan.controller;
import org.springframework.web.bind.annotation.*;import org.springframework.ui.Model;import org.springframework.http.HttpStatus;
@ControllerAdvice public class GlobalErrors {
 @ExceptionHandler({IllegalArgumentException.class,java.util.NoSuchElementException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
 public String invalid(Exception e,Model m){m.addAttribute("message",e.getMessage()==null?"Yêu cầu không hợp lệ":e.getMessage());return "business-error";}
 @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class,org.springframework.dao.CannotAcquireLockException.class}) @ResponseStatus(HttpStatus.CONFLICT)
 public String conflict(Exception e,Model m){m.addAttribute("message","Dữ liệu vừa thay đổi. Vui lòng tải lại trang và thử lại.");return "business-error";}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) @ResponseStatus(HttpStatus.CONFLICT)
 public String duplicate(Exception e,Model m){m.addAttribute("message","Dữ liệu bị trùng hoặc đang được sử dụng. Hãy kiểm tra mã/email và tải lại trang.");return "business-error";}
}
