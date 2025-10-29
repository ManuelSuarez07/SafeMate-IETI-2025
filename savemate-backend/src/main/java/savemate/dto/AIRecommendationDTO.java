package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import savemate.model.AIRecommendation;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIRecommendationDTO {
    
    private Long id;
    private Long userId;
    
    @NotNull(message = "El tipo de recomendación es obligatorio")
    private AIRecommendation.RecommendationType recommendationType;
    
    @NotBlank(message = "El título es obligatorio")
    private String title;
    
    @NotBlank(message = "La descripción es obligatoria")
    private String description;
    
    private String actionText;
    private Double potentialSavings;
    private Double confidenceScore;
    
    private AIRecommendation.RecommendationStatus status = AIRecommendation.RecommendationStatus.PENDING;
    private Boolean isApplied = false;
    private LocalDateTime appliedAt;
    private LocalDateTime expiresAt;
    private String category;
    private Integer priorityLevel = 1;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructor para creación de recomendación
    public AIRecommendationDTO(Long userId, AIRecommendation.RecommendationType recommendationType,
                              String title, String description, String actionText,
                              Double potentialSavings, Double confidenceScore,
                              String category, Integer priorityLevel) {
        this.userId = userId;
        this.recommendationType = recommendationType;
        this.title = title;
        this.description = description;
        this.actionText = actionText;
        this.potentialSavings = potentialSavings;
        this.confidenceScore = confidenceScore;
        this.category = category;
        this.priorityLevel = priorityLevel;
        this.status = AIRecommendation.RecommendationStatus.PENDING;
        this.isApplied = false;
        
        // Establecer fecha de expiración (30 días por defecto)
        this.expiresAt = LocalDateTime.now().plusDays(30);
    }
    
    // Constructor para recomendación de optimización de ahorro
    public AIRecommendationDTO(Long userId, String title, String description,
                              Double potentialSavings, Double confidenceScore) {
        this(userId, AIRecommendation.RecommendationType.SAVING_OPTIMIZATION,
             title, description, "Aplicar recomendación", potentialSavings,
             confidenceScore, "optimization", 1);
    }
    
    // Constructor para recomendación de patrón de gasto
    public AIRecommendationDTO(Long userId, String title, String description,
                              String category, Double potentialSavings) {
        this(userId, AIRecommendation.RecommendationType.SPENDING_PATTERN,
             title, description, "Revisar hábitos de gasto", potentialSavings,
             0.8, category, 2);
    }
    
    // Constructor para recomendación de configuración
    public AIRecommendationDTO(Long userId, AIRecommendation.RecommendationType recommendationType,
                              String title, String description, String actionText) {
        this(userId, recommendationType, title, description, actionText,
             null, 0.9, "configuration", 1);
    }
    
    // Constructor para aplicar recomendación
    public AIRecommendationDTO(Long id, Boolean isApplied) {
        this.id = id;
        this.isApplied = isApplied;
        if (isApplied) {
            this.appliedAt = LocalDateTime.now();
            this.status = AIRecommendation.RecommendationStatus.APPLIED;
        }
    }
    
    // Constructor para cambiar estado
    public AIRecommendationDTO(Long id, AIRecommendation.RecommendationStatus status) {
        this.id = id;
        this.status = status;
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isHighPriority() {
        return priorityLevel != null && priorityLevel >= 3;
    }
    
    public boolean hasHighConfidence() {
        return confidenceScore != null && confidenceScore >= 0.8;
    }
}