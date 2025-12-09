package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import savemate.model.AIRecommendation;

import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) que representa una recomendación generada por IA.
 *
 * <p>Responsabilidad: Transporte de datos entre capas (controlador/servicio/persistencia)
 * para las recomendaciones generadas por el subsistema de IA. Contiene campos
 * de metadatos, estado, puntuaciones y utilidades de conveniencia para consultas
 * simples (p. ej. comprobar expiración o prioridad alta).
 *
 * @see AIRecommendation
 */
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

    /**
     * Constructor principal para creación de una recomendación.
     *
     * @param userId identificador del usuario al que se asocia la recomendación
     * @param recommendationType tipo de recomendación (p. ej. SAVING_OPTIMIZATION, SPENDING_PATTERN)
     * @param title título breve de la recomendación
     * @param description descripción detallada de la recomendación
     * @param actionText texto de la acción sugerida al usuario (p. ej. "Aplicar recomendación")
     * @param potentialSavings valor estimado de ahorro potencial asociado a la recomendación (puede ser null)
     * @param confidenceScore puntuación de confianza de la recomendación (0.0 - 1.0)
     * @param category categoría o etiqueta de la recomendación (p. ej. "optimization", "configuration")
     * @param priorityLevel nivel de prioridad de la recomendación (valores mayores indican mayor prioridad)
     */
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

    /**
     * Constructor de conveniencia para recomendación de optimización de ahorro.
     *
     * @param userId identificador del usuario
     * @param title título de la recomendación
     * @param description descripción de la recomendación
     * @param potentialSavings ahorro potencial estimado
     * @param confidenceScore puntuación de confianza de la recomendación
     */
    public AIRecommendationDTO(Long userId, String title, String description,
                               Double potentialSavings, Double confidenceScore) {
        this(userId, AIRecommendation.RecommendationType.SAVING_OPTIMIZATION,
                title, description, "Aplicar recomendación", potentialSavings,
                confidenceScore, "optimization", 1);
    }

    /**
     * Constructor de conveniencia para recomendación basada en patrón de gasto.
     *
     * @param userId identificador del usuario
     * @param title título de la recomendación
     * @param description descripción de la recomendación
     * @param category categoría asociada al patrón de gasto
     * @param potentialSavings ahorro potencial estimado
     */
    public AIRecommendationDTO(Long userId, String title, String description,
                               String category, Double potentialSavings) {
        this(userId, AIRecommendation.RecommendationType.SPENDING_PATTERN,
                title, description, "Revisar hábitos de gasto", potentialSavings,
                0.8, category, 2);
    }

    /**
     * Constructor para recomendaciones que corresponden a cambios de configuración.
     *
     * @param userId identificador del usuario
     * @param recommendationType tipo de recomendación (ej. CONFIGURATION)
     * @param title título de la recomendación
     * @param description descripción de la recomendación
     * @param actionText texto de acción asociado
     */
    public AIRecommendationDTO(Long userId, AIRecommendation.RecommendationType recommendationType,
                               String title, String description, String actionText) {
        this(userId, recommendationType, title, description, actionText,
                null, 0.9, "configuration", 1);
    }

    /**
     * Constructor utilizado para marcar una recomendación como aplicada o no aplicada.
     *
     * <p>Si {@code isApplied} es {@code true} establece la marca temporal {@code appliedAt}
     * y el estado a {@link AIRecommendation.RecommendationStatus#APPLIED}.
     *
     * @param id identificador de la recomendación
     * @param isApplied indicador que señala si la recomendación ya fue aplicada
     */
    public AIRecommendationDTO(Long id, Boolean isApplied) {
        this.id = id;
        this.isApplied = isApplied;
        if (isApplied) {
            this.appliedAt = LocalDateTime.now();
            this.status = AIRecommendation.RecommendationStatus.APPLIED;
        }
    }

    /**
     * Constructor para cambiar el estado de una recomendación.
     *
     * @param id identificador de la recomendación
     * @param status nuevo estado que se asignará a la recomendación
     */
    public AIRecommendationDTO(Long id, AIRecommendation.RecommendationStatus status) {
        this.id = id;
        this.status = status;
    }

    /**
     * Indica si la recomendación ha expirado según la propiedad {@code expiresAt}.
     *
     * @return {@code true} si {@code expiresAt} está definida y la fecha actual es posterior a {@code expiresAt};
     *         {@code false} en caso contrario.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Indica si la recomendación tiene prioridad alta.
     *
     * @return {@code true} si {@code priorityLevel} es no nulo y mayor o igual a 3; {@code false} en caso contrario.
     */
    public boolean isHighPriority() {
        return priorityLevel != null && priorityLevel >= 3;
    }

    /**
     * Indica si la recomendación tiene una puntuación de confianza alta.
     *
     * @return {@code true} si {@code confidenceScore} es no nulo y mayor o igual a 0.8; {@code false} en caso contrario.
     */
    public boolean hasHighConfidence() {
        return confidenceScore != null && confidenceScore >= 0.8;
    }
}