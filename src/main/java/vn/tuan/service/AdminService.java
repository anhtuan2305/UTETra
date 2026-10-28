package vn.tuan.service;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.data.domain.*;import vn.tuan.entity.*;import vn.tuan.repository.*;import java.util.*;
@Service @RequiredArgsConstructor public class AdminService {
 private final AccountRepository accounts;private final ShopRepository shops;
 public Page<Account> users(String key,int page){return accounts.findByEmailContainingIgnoreCase(key==null?"":key.trim(),PageRequest.of(Math.max(0,page),20,Sort.by("id").descending()));}
 public Page<Shop> shops(String key,int page){return shops.findByShopNameContainingIgnoreCase(key==null?"":key.trim(),PageRequest.of(Math.max(0,page),20,Sort.by("id").descending()));}
 @Transactional public void toggle(Integer id){Account a=accounts.findById(id).orElseThrow();OrderRules.require(!a.getRole().contains("ADMIN"),"Không khóa Admin qua chức năng này");OrderRules.require(Boolean.TRUE.equals(a.getEmailVerified()),"Tài khoản chưa xác minh email");a.setIsActive(!Boolean.TRUE.equals(a.getIsActive()));}
 @Transactional public void shopStatus(Integer id,String status){OrderRules.require(Set.of("ACTIVE","PENDING","BANNED").contains(status),"Trạng thái quán không hợp lệ");Shop s=shops.findById(id).orElseThrow();OrderRules.require(Set.of("USER","VENDOR").contains(s.getOwner().getRole().replace("ROLE_","")),"Vai trò chủ quán không hợp lệ");s.setStatus(status);s.getOwner().setRole("ACTIVE".equals(status)?"VENDOR":"USER");}
 public List<Account> shippers(boolean active){return accounts.findByRoleAndIsActiveOrderByCreatedAtDesc("SHIPPER",active);}
 @Transactional public void shipperStatus(Integer id,boolean active){Account a=accounts.findById(id).orElseThrow();OrderRules.require("SHIPPER".equals(a.getRole().replace("ROLE_","")),"Tài khoản không phải Shipper");OrderRules.require(!active || Boolean.TRUE.equals(a.getEmailVerified()),"Shipper chưa xác minh email");a.setIsActive(active);}
}
