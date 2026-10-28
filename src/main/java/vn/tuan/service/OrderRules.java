package vn.tuan.service;
import java.util.*;
public final class OrderRules {
 private OrderRules() {}
 public static void require(boolean valid,String message) { if(!valid) throw new IllegalArgumentException(message); }
 public static void vendorTransition(String from,String to) {
  Map<String,Set<String>> edges=Map.of("NEW",Set.of("CONFIRMED","CANCELLED"),"CONFIRMED",Set.of("PREPARING","CANCELLED"),"PREPARING",Set.of("READY"),"RETURN_REQUESTED",Set.of("RETURN_APPROVED","COMPLETED"),"RETURN_APPROVED",Set.of("REFUNDED"));
  require(edges.getOrDefault(from,Set.of()).contains(to),"Không thể chuyển trạng thái từ "+from+" sang "+to);
 }
 public static void quantity(int q,int stock) { require(q>0 && q<=99 && q<=stock,"Số lượng phải từ 1 đến 99 và không vượt tồn kho."); }
}
