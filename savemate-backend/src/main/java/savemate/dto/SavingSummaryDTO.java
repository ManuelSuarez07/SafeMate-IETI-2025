package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingSummaryDTO {
    
    private Double totalCurrentSavings;
    private Long activeGoalsCount;
    private Long completedGoalsCount;
    private Integer totalGoalsCount;
    private List<SavingDTO> activeGoals;
    private List<SavingDTO> allGoals;
    
    public SavingSummaryDTO(Double totalCurrentSavings, Long activeCount, Long completedCount, 
                           List<SavingDTO> allGoals, List<SavingDTO> activeGoals) {
        this.totalCurrentSavings = totalCurrentSavings != null ? totalCurrentSavings : 0.0;
        this.activeGoalsCount = activeCount != null ? activeCount : 0L;
        this.completedGoalsCount = completedCount != null ? completedCount : 0L;
        this.totalGoalsCount = allGoals != null ? allGoals.size() : 0;
        this.activeGoals = activeGoals;
        this.allGoals = allGoals;
    }
}