import vn.tuan.service.*;import java.util.*;import java.math.*;
public class CoreRegression {
 static int count;static void check(boolean b){count++;if(!b)throw new AssertionError("Check "+count);}
 static void reject(Runnable r){try{r.run();throw new AssertionError("Expected rejection");}catch(IllegalArgumentException expected){count++;}}
 public static void main(String[]args){
 String[][] valid={{"NEW","CONFIRMED"},{"NEW","CANCELLED"},{"CONFIRMED","PREPARING"},{"CONFIRMED","CANCELLED"},{"PREPARING","READY"},{"RETURN_REQUESTED","RETURN_APPROVED"},{"RETURN_APPROVED","REFUNDED"}};
 for(var pair:valid){OrderRules.vendorTransition(pair[0],pair[1]);count++;}
 for(String st:List.of("NEW","CONFIRMED","PREPARING","READY","SHIPPING","COMPLETED","CANCELLED","REFUNDED"))reject(()->OrderRules.vendorTransition(st,"SHIPPING"));
 reject(()->OrderRules.vendorTransition("NEW","COMPLETED"));reject(()->OrderRules.vendorTransition("CANCELLED","CONFIRMED"));reject(()->OrderRules.quantity(-1,10));reject(()->OrderRules.quantity(0,10));reject(()->OrderRules.quantity(3,2));reject(()->OrderRules.quantity(100,100));OrderRules.quantity(2,2);count++;
 check(OptionPricing.price(new BigDecimal("25000"),"Size L | 100% Đường | 100% Đá | Topping: Trân châu đen (+5.000đ)").equals(new BigDecimal("36000")));check(OptionPricing.price(new BigDecimal("25000"),"Size M").equals(new BigDecimal("25000")));reject(()->OptionPricing.validate("Size XXL",null,null,null));reject(()->OptionPricing.validate(null,null,null,List.of("free")));reject(()->OptionPricing.validate(null,null,null,List.of("Trân châu đen (+5.000đ)","Trân châu đen (+5.000đ)")));
 String key="sandbox-secret";Map<String,String> p=new HashMap<>();p.put("vnp_TxnRef","abc");p.put("vnp_Amount","5100000");p.put("vnp_ResponseCode","00");String signature=PaymentSigner.sign(key,PaymentSigner.query(p));p.put("vnp_SecureHash",signature);check(PaymentSigner.verify(key,p));p.put("vnp_Amount","100");check(!PaymentSigner.verify(key,p));check(!PaymentSigner.verify("",p));p.put("vnp_SecureHash","invalid");check(!PaymentSigner.verify(key,p));check(PaymentSigner.query(Map.of("b","two words","a","1")).equals("a=1&b=two+words"));
 System.out.println("PASS: "+count+" core regression assertions (state transitions, quantities, pricing, signed payment tamper detection).");
 }
}
