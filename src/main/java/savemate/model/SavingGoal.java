package safemate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saving_goals")
public class SavingGoal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String title;
    private Long targetAmountCents;
    private Long currentAmountCents = 0L;
    private LocalDateTime createdAt = LocalDateTime.now();

    // getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getTargetAmountCents() { return targetAmountCents; }
    public void setTargetAmountCents(Long targetAmountCents) { this.targetAmountCents = targetAmountCents; }
    public Long getCurrentAmountCents() { return currentAmountCents; }
    public void setCurrentAmountCents(Long currentAmountCents) { this.currentAmountCents = currentAmountCents; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}