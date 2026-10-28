package vn.tuan.entity;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="product_activity",uniqueConstraints=@UniqueConstraint(columnNames={"account_id","product_id"}))
public class ProductActivity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @ManyToOne(optional=false) @JoinColumn(name="account_id") private Account account;
 @ManyToOne(optional=false) @JoinColumn(name="product_id") private Product product;
 private boolean favorite;private LocalDateTime viewedAt;
}
