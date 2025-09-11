package safemate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_recommendations")
public class AIRecommendation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String recommendationType;
    private String message;
    private Long suggestedAmountCents;
    private LocalDateTime generatedAt = LocalDateTime.now();

    // getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getSuggestedAmountCents() { return suggestedAmountCents; }
    public void setSuggestedAmountCents(Long suggestedAmountCents) { this.suggestedAmountCents = suggestedAmountCents; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}