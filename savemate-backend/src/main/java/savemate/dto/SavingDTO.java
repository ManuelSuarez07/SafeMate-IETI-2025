package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import savemate.model.SavingGoal;

import java.time.LocalDateTime;

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
    
    // Constructor para creación de meta
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
    
    // Constructor para meta colaborativa
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
    
    // Constructor para actualización de progreso
    public SavingDTO(Long id, Double currentAmount) {
        this.id = id;
        this.currentAmount = currentAmount;
        updateProgress();
    }
    
    // Constructor para cambio de estado
    public SavingDTO(Long id, SavingGoal.GoalStatus status) {
        this.id = id;
        this.status = status;
        if (status == SavingGoal.GoalStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
    }
    
    public void updateProgress() {
        if (targetAmount != null && targetAmount > 0) {
            this.progressPercentage = (currentAmount / targetAmount) * 100;
            this.remainingAmount = Math.max(0, targetAmount - currentAmount);
        }
    }
    
    public boolean isCompleted() {
        return targetAmount != null && currentAmount >= targetAmount;
    }
    
    public boolean isOverdue() {
        return targetDate != null && LocalDateTime.now().isAfter(targetDate) && !isCompleted();
    }
}