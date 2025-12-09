package savemate.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidad de persistencia que modela un objetivo financiero específico definido por el usuario.
 * <p>
 * Esta clase representa la unidad fundamental de la estrategia de ahorro en el sistema. Su responsabilidad
 * es mantener el estado persistente de una meta, rastreando el progreso acumulado (`currentAmount`)
 * frente al objetivo total (`targetAmount`), gestionando los plazos temporales y controlando el ciclo
 * de vida de la meta (activa, completada, pausada).
 * </p>
 */
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

    /**
     * Enumeración que define los posibles estados operativos en el ciclo de vida de una meta de ahorro.
     * Permite controlar la visibilidad y la lógica de aportes permitidos para cada meta.
     */
    public enum GoalStatus {
        ACTIVE,
        COMPLETED,
        PAUSED,
        CANCELLED
    }

    /**
     * Calcula el porcentaje de avance actual respecto al monto objetivo total.
     * <p>
     * Este método contiene lógica de seguridad para evitar divisiones por cero o nulos.
     * </p>
     *
     * @return Un valor {@code Double} que representa el porcentaje (0.0 a 100.0+).
     * Retorna 0.0 si el monto objetivo no está definido o es cero.
     */
    public Double getProgressPercentage() {
        if (targetAmount == null || targetAmount == 0) return 0.0;
        return (currentAmount / targetAmount) * 100;
    }

    /**
     * Determina el monto monetario pendiente para alcanzar la meta financiera.
     * <p>
     * Realiza un cálculo aritmético simple asegurando que el resultado nunca sea negativo
     * (en caso de haber excedido la meta).
     * </p>
     *
     * @return Un valor {@code Double} positivo o cero indicando cuánto falta por ahorrar.
     * Retorna 0.0 si el monto objetivo es nulo.
     */
    public Double getRemainingAmount() {
        if (targetAmount == null) return 0.0;
        return Math.max(0, targetAmount - currentAmount);
    }
}