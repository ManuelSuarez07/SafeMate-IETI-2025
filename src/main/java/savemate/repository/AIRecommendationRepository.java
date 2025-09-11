package safemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import safemate.model.AIRecommendation;
import java.util.List;

public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {
    List<AIRecommendation> findByUserIdOrderByGeneratedAtDesc(Long userId);
}