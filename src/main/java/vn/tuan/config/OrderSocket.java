package vn.tuan.config;
import org.springframework.stereotype.Component;import org.springframework.web.socket.*;import org.springframework.web.socket.handler.TextWebSocketHandler;import java.util.concurrent.*;import java.util.*;
@Component public class OrderSocket extends TextWebSocketHandler {
 private final Set<WebSocketSession> sessions=ConcurrentHashMap.newKeySet();
 @Override public void afterConnectionEstablished(WebSocketSession s)throws Exception{if(s.getPrincipal()==null){s.close(CloseStatus.POLICY_VIOLATION);return;}sessions.add(s);}
 @Override public void afterConnectionClosed(WebSocketSession s,CloseStatus status){sessions.remove(s);}
 public void notifyUsers(Set<String> emails){for(var s:sessions)if(s.isOpen() && s.getPrincipal()!=null && emails.contains(s.getPrincipal().getName()))try{synchronized(s){s.sendMessage(new TextMessage("{\"event\":\"orders-changed\"}"));}}catch(Exception e){sessions.remove(s);}}
}
