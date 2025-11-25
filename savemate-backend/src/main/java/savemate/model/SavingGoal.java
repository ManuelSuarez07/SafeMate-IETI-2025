package savemate.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "saving_goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SavingGoal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "target_amount", nullable = false)
    private Double targetAmount;
    
    @Column(name = "current_amount")
    private Double currentAmount = 0.0;
    
    @Column(name = "target_date")
    private LocalDateTime targetDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status = GoalStatus.ACTIVE;
    
    @Column(name = "monthly_contribution")
    private Double monthlyContribution;
    
    @Column(name = "priority_level")
    private Integer priorityLevel = 1;
    
    @Column(name = "is_collaborative")
    private Boolean isCollaborative = false;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    public enum GoalStatus {
        ACTIVE,
        COMPLETED,
        PAUSED,
        CANCELLED
    }
    
    public Double getProgressPercentage() {
        if (targetAmount == null || targetAmount == 0) return 0.0;
        return (currentAmount / targetAmount) * 100;
    }
    
    public Double getRemainingAmount() {
        if (targetAmount == null) return 0.0;
        return Math.max(0, targetAmount - currentAmount);
    }
}