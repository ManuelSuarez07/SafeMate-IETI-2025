package safemate.dto;

import java.time.LocalDateTime;

public class AIRecommendationDTO {
    public Long id;
    public Long userId;
    public String recommendationType;
    public String message;
    public Long suggestedAmountCents;
    public LocalDateTime generatedAt;
}