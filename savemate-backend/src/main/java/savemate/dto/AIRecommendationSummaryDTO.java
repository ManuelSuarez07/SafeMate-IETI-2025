package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIRecommendationSummaryDTO {
    
    private List<AIRecommendationDTO> activeRecommendations;
    private Map<String, Object> statistics;
    private Integer activeCount;
    private Long totalApplied;
    private Double totalSavings;
    
    public AIRecommendationSummaryDTO(List<AIRecommendationDTO> activeRecommendations, Map<String, Object> statistics) {
        this.activeRecommendations = activeRecommendations;
        this.statistics = statistics;
        this.activeCount = activeRecommendations != null ? activeRecommendations.size() : 0;
        this.totalApplied = statistics != null && statistics.get("totalApplied") != null ? 
                           (Long) statistics.get("totalApplied") : 0L;
        this.totalSavings = statistics != null && statistics.get("totalSavings") != null ? 
                           (Double) statistics.get("totalSavings") : 0.0;
    }
}