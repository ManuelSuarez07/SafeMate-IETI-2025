package savemate.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidad de persistencia que representa una recomendación individual generada por el motor de inteligencia artificial.
 * <p>
 * Esta clase actúa como el registro histórico y operativo de las sugerencias financieras (insights)
 * ofrecidas al usuario. Gestiona no solo el contenido de la recomendación (título, descripción, impacto monetario),
 * sino también su ciclo de vida completo (pendiente, aplicada, rechazada) y su vigencia temporal.
 * Es fundamental para el módulo de asesoramiento financiero automatizado y la trazabilidad de la interacción usuario-IA.
 * </p>
 */
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

    /**
     * Define las categorías taxonómicas de las sugerencias generadas por el modelo predictivo.
     * Clasifica la naturaleza de la optimización (ej. reducción de gastos, ajuste de configuración)
     * para facilitar el filtrado y la priorización.
     */
    public enum RecommendationType {
        SPENDING_PATTERN,
        SAVING_OPTIMIZATION,
        GOAL_ADJUSTMENT,
        ROUNDING_CONFIG,
        PERCENTAGE_CONFIG,
        EXPENSE_REDUCTION,
        INCOME_INCREASE
    }

    /**
     * Gestiona el ciclo de vida o máquina de estados de una recomendación desde su creación
     * hasta su resolución final por parte del usuario o el sistema.
     */
    public enum RecommendationStatus {
        PENDING,
        VIEWED,
        APPLIED,
        REJECTED,
        EXPIRED
    }

    /**
     * Evalúa la validez temporal de la recomendación en el momento de la consulta.
     * <p>
     * Verifica si la fecha de expiración ha sido establecida y si dicha fecha es anterior
     * al instante actual del sistema, lo cual indicaría que la oportunidad de ahorro ya no es aplicable.
     * </p>
     *
     * @return {@code true} si la recomendación tiene fecha límite y esta ya ha pasado;
     * {@code false} si sigue vigente o si no tiene fecha de vencimiento asignada.
     */
    public Boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}