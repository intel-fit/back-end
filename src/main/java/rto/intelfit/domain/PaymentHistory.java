package rto.intelfit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "payment_history")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(name = "tid", length = 100)
    private String tid;

    @Column(name = "aid", length = 100)
    private String aid;

    @Column(name = "cid", length = 50)
    private String cid;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column(name = "plan_code", length = 50)
    private String planCode;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "total_amount")
    private Integer totalAmount;

    @Column(name = "tax_free_amount")
    private Integer taxFreeAmount;

    @Column(name = "approved_at")
    private String approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.READY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq")
    private User user;

    // 사용자 로그인 ID를 별도로 보관 (예: "user-1234")
    @Column(name = "user_id", nullable = false, length = 50)
    private String userLoginId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum PaymentStatus {
        READY,
        APPROVED,
        CANCELLED,
        FAILED
    }
}
