package vn.tuan.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.tuan.entity.*; import vn.tuan.repository.*;
@Service @RequiredArgsConstructor
public class ActorService {
 private final AccountRepository accounts; private final ShopRepository shops;
 public Account account(String email) { Account a=accounts.findByEmail(email).orElseThrow(()->new IllegalArgumentException("Không tìm thấy tài khoản")); OrderRules.require(Boolean.TRUE.equals(a.getIsActive()),"Tài khoản chưa được kích hoạt hoặc bị khóa");return a; }
 public String role(Account a) {return a.getRole().replace("ROLE_","");}
 public boolean manages(Account a,Shop s) {return "ADMIN".equals(role(a)) || ("VENDOR".equals(role(a)) && s.getOwner().getId().equals(a.getId())) || ("MANAGER".equals(role(a)) && s.getManager()!=null && s.getManager().getId().equals(a.getId()));}
 public Shop shop(String email,Integer id) {Account a=account(email);Shop s=shops.findById(id).orElseThrow();OrderRules.require(manages(a,s),"Bạn không quản lý cửa hàng này");OrderRules.require("ADMIN".equals(role(a)) || "ACTIVE".equals(s.getStatus()),"Cửa hàng chưa hoạt động");return s;}
}
