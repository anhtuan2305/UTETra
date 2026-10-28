package vn.tuan.service;
import java.math.BigDecimal;import java.util.*;
public final class OptionPricing {
 private OptionPricing(){}
 public static final Map<String,Integer> TOPPINGS=Map.of("Trân châu đen (+5.000đ)",5000,"Thạch sương sáo (+5.000đ)",5000,"Kem Cheese Macchiato (+10.000đ)",10000,"Trân châu hoàng kim (+8.000đ)",8000);
 public static void validate(String size,String sugar,String ice,List<String> toppings){OrderRules.require(size==null || Set.of("Size M","Size L").contains(size),"Size không hợp lệ");OrderRules.require(sugar==null || Set.of("100% Đường","70% Đường","50% Đường","Không Đường").contains(sugar),"Mức đường không hợp lệ");OrderRules.require(ice==null || Set.of("100% Đá","70% Đá","50% Đá","Không Đá").contains(ice),"Mức đá không hợp lệ");if(toppings!=null)OrderRules.require(toppings.size()<=4 && new HashSet<>(toppings).size()==toppings.size() && TOPPINGS.keySet().containsAll(toppings),"Topping không hợp lệ");}
 public static BigDecimal price(BigDecimal base,String options){if(options==null)return base;int extra=options.startsWith("Size L")?6000:0;for(var t:TOPPINGS.entrySet())if(options.contains(t.getKey()))extra+=t.getValue();return base.add(BigDecimal.valueOf(extra));}
}
