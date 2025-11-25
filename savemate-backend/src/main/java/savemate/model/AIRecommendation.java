package savemate.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_recommendations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AIRecommendation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationType recommendationType;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String actionText;
    
    @Column(name = "potential_savings")
    private Double potentialSavings;
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationStatus status = RecommendationStatus.PENDING;
    
    @Column(name = "is_applied")
    private Boolean isApplied = false;
    
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "priority_level")
    private Integer priorityLevel = 1;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum RecommendationType {
        SPENDING_PATTERN,
        SAVING_OPTIMIZATION,
        GOAL_ADJUSTMENT,
        ROUNDING_CONFIG,
        PERCENTAGE_CONFIG,
        EXPENSE_REDUCTION,
        INCOME_INCREASE
    }
    
    public enum RecommendationStatus {
        PENDING,
        VIEWED,
        APPLIED,
        REJECTED,
        EXPIRED
    }
    
    public Boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}