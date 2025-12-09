package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import savemate.model.SavingGoal;

import java.time.LocalDateTime;

/**
 * Objeto de Transferencia de Datos (DTO) diseñado para encapsular la información y reglas de negocio
 * ligeras asociadas a una meta de ahorro.
 * <p>
 * Su responsabilidad principal es gestionar el transporte de datos para la creación, actualización
 * y seguimiento de metas financieras. Además de contener validaciones de entrada (JSR-380),
 * esta clase centraliza la lógica de cálculo para métricas derivadas como el porcentaje de progreso,
 * el monto restante y la determinación de estados de vencimiento.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingDTO {

    private Long id;
    private Long userId;

    @NotBlank(message = "El nombre de la meta es obligatorio")
    private String name;

    private String description;

    @NotNull(message = "El monto objetivo es obligatorio")
    @Positive(message = "El monto objetivo debe ser positivo")
    private Double targetAmount;

    private Double currentAmount = 0.0;
    private LocalDateTime targetDate;

    private SavingGoal.GoalStatus status = SavingGoal.GoalStatus.ACTIVE;
    private Double monthlyContribution;
    private Integer priorityLevel = 1;
    private Boolean isCollaborative = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    // Campos calculados
    private Double progressPercentage = 0.0;
    private Double remainingAmount = 0.0;

    /**
     * Inicializa una instancia del DTO específica para la creación de una nueva meta de ahorro estándar.
     * Establece por defecto el estado como {@code ACTIVE} y marca la meta como no colaborativa.
     *
     * @param userId              Identificador único del usuario propietario de la meta.
     * @param name                Nombre descriptivo o título de la meta.
     * @param description         Detalles adicionales sobre el propósito del ahorro.
     * @param targetAmount        Monto total monetario que se desea alcanzar.
     * @param targetDate          Fecha límite planificada para alcanzar la meta.
     * @param monthlyContribution Monto sugerido de contribución mensual.
     * @param priorityLevel       Nivel de prioridad de la meta (usualmente 1-5).
     */
    public SavingDTO(Long userId, String name, String description,
                     Double targetAmount, LocalDateTime targetDate,
                     Double monthlyContribution, Integer priorityLevel) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.monthlyContribution = monthlyContribution;
        this.priorityLevel = priorityLevel;
        this.status = SavingGoal.GoalStatus.ACTIVE;
        this.isCollaborative = false;
    }

    /**
     * Inicializa una instancia del DTO configurada específicamente para metas de tipo colaborativo.
     *
     * @param userId          Identificador del usuario creador o administrador de la meta.
     * @param name            Nombre de la meta compartida.
     * @param description     Descripción del objetivo común.
     * @param targetAmount    Monto total objetivo del grupo.
     * @param targetDate      Fecha límite para la recaudación.
     * @param isCollaborative Indicador booleano que define la naturaleza grupal de la meta.
     */
    public SavingDTO(Long userId, String name, String description,
                     Double targetAmount, LocalDateTime targetDate,
                     Boolean isCollaborative) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.isCollaborative = isCollaborative;
        this.status = SavingGoal.GoalStatus.ACTIVE;
    }

    /**
     * Constructor parcial diseñado para operaciones de actualización de saldo.
     * Al instanciar, invoca automáticamente el recálculo de métricas de progreso.
     *
     * @param id            Identificador único de la meta a actualizar.
     * @param currentAmount Nuevo saldo acumulado actual de la meta.
     */
    public SavingDTO(Long id, Double currentAmount) {
        this.id = id;
        this.currentAmount = currentAmount;
        updateProgress();
    }

    /**
     * Constructor parcial para gestionar transiciones de estado en el ciclo de vida de la meta.
     * Si el nuevo estado es {@code COMPLETED}, registra automáticamente la marca de tiempo actual.
     *
     * @param id     Identificador único de la meta.
     * @param status Nuevo estado a asignar (ej. PAUSED, COMPLETED).
     */
    public SavingDTO(Long id, SavingGoal.GoalStatus status) {
        this.id = id;
        this.status = status;
        if (status == SavingGoal.GoalStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    /**
     * Recalcula las métricas derivadas del progreso basándose en el saldo actual y el objetivo.
     * <p>
     * Actualiza {@code progressPercentage} (0-100) y {@code remainingAmount}.
     * Incluye validaciones para evitar divisiones por cero o nulos en {@code targetAmount}.
     * </p>
     */
    public void updateProgress() {
        if (targetAmount != null && targetAmount > 0) {
            this.progressPercentage = (currentAmount / targetAmount) * 100;
            this.remainingAmount = Math.max(0, targetAmount - currentAmount);
        }
    }

    /**
     * Verifica si la meta ha alcanzado o superado su objetivo financiero.
     *
     * @return {@code true} si el monto actual es mayor o igual al monto objetivo;
     * {@code false} en caso contrario o si el objetivo es nulo.
     */
    public boolean isCompleted() {
        return targetAmount != null && currentAmount >= targetAmount;
    }

    /**
     * Determina si la meta se encuentra vencida respecto a su fecha límite planificada.
     * Una meta se considera vencida si la fecha actual es posterior a la fecha objetivo
     * y la meta aún no ha sido completada financieramente.
     *
     * @return {@code true} si la fecha límite ha pasado y no se ha alcanzado el monto;
     * {@code false} en caso contrario.
     */
    public boolean isOverdue() {
        return targetDate != null && LocalDateTime.now().isAfter(targetDate) && !isCompleted();
    }
}