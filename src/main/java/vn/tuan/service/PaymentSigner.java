package vn.tuan.service;
import java.util.*;import java.nio.charset.StandardCharsets;import java.net.URLEncoder;import javax.crypto.Mac;import javax.crypto.spec.SecretKeySpec;import java.security.MessageDigest;
public final class PaymentSigner {
 private PaymentSigner(){}
 public static String query(Map<String,String> fields){return new TreeMap<>(fields).entrySet().stream().filter(e->e.getValue()!=null && !e.getValue().isEmpty()).map(e->URLEncoder.encode(e.getKey(),StandardCharsets.US_ASCII)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.US_ASCII)).collect(java.util.stream.Collectors.joining("&"));}
 public static String sign(String secret,String data){try{Mac mac=Mac.getInstance("HmacSHA512");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Không tạo được chữ ký thanh toán",e);}}
 public static boolean verify(String secret,Map<String,String> fields){if(secret==null || secret.isBlank())return false;String signature=fields.get("vnp_SecureHash");if(signature==null || !signature.matches("[0-9a-fA-F]{128}"))return false;Map<String,String> values=new TreeMap<>(fields);values.remove("vnp_SecureHash");values.remove("vnp_SecureHashType");return MessageDigest.isEqual(HexFormat.of().parseHex(signature),HexFormat.of().parseHex(sign(secret,query(values))));}
}
