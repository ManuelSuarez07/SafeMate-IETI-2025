package savemate.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "bank_account")
    private String bankAccount;
    
    @Column(name = "bank_name")
    private String bankName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SavingType savingType = SavingType.ROUNDING;
    
    @Column(name = "rounding_multiple")
    private Integer roundingMultiple = 1000;
    
    @Column(name = "saving_percentage")
    private Double savingPercentage = 10.0;
    
    @Column(name = "min_safe_balance")
    private Double minSafeBalance = 0.0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "insufficient_balance_option")
    private InsufficientBalanceOption insufficientBalanceOption = InsufficientBalanceOption.NO_SAVING;
    
    @Column(name = "total_saved")
    private Double totalSaved = 0.0;
    
    @Column(name = "monthly_fee_rate")
    private Double monthlyFeeRate = 2.5;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SavingGoal> savingGoals;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AIRecommendation> aiRecommendations;
    
    public enum SavingType {
        ROUNDING,
        PERCENTAGE
    }
    
    public enum InsufficientBalanceOption {
        NO_SAVING,
        PENDING,
        RESPECT_MIN_BALANCE
    }
}