package vn.tuan.service;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.support.*;import java.util.*;import vn.tuan.entity.Order;import vn.tuan.config.OrderSocket;import vn.tuan.repository.AccountRepository;
@Service @RequiredArgsConstructor public class OrderEvents {
 private final OrderSocket socket;private final AccountRepository accounts;
 public void changed(Order o){Set<String> names=new HashSet<>();if(o.getAccount()!=null)names.add(o.getAccount().getEmail());if(o.getShipper()!=null)names.add(o.getShipper().getEmail());names.add(o.getShop().getOwner().getEmail());if(o.getShop().getManager()!=null)names.add(o.getShop().getManager().getEmail());if("READY".equals(o.getStatus()))accounts.findByRoleAndIsActiveOrderByCreatedAtDesc("SHIPPER",true).forEach(a->names.add(a.getEmail()));
 if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCommit(){socket.notifyUsers(names);}});else socket.notifyUsers(names);
 }
}
