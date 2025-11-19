package savemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import savemate.model.AIRecommendation;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {
    
    List<AIRecommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<AIRecommendation> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, AIRecommendation.RecommendationStatus status);
    
    List<AIRecommendation> findByUserIdAndRecommendationTypeOrderByCreatedAtDesc(Long userId, AIRecommendation.RecommendationType recommendationType);
    
    @Query("SELECT ar FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.status = 'PENDING' AND (ar.expiresAt IS NULL OR ar.expiresAt > :now) ORDER BY ar.priorityLevel DESC, ar.createdAt DESC")
    List<AIRecommendation> findActiveRecommendations(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT ar FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.expiresAt <= :now AND ar.status != 'EXPIRED'")
    List<AIRecommendation> findExpiredRecommendations(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(ar) FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.isApplied = true")
    Long countAppliedRecommendations(@Param("userId") Long userId);
    
    @Query("SELECT SUM(ar.potentialSavings) FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.isApplied = true AND ar.potentialSavings IS NOT NULL")
    Double sumAppliedRecommendationsSavings(@Param("userId") Long userId);
    
    @Query("SELECT ar.recommendationType, COUNT(ar), AVG(ar.confidenceScore) FROM AIRecommendation ar WHERE ar.user.id = :userId GROUP BY ar.recommendationType ORDER BY COUNT(ar) DESC")
    List<Object[]> getRecommendationStatistics(@Param("userId") Long userId);
    
    @Query("SELECT ar FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.category = :category ORDER BY ar.createdAt DESC")
    List<AIRecommendation> findByCategory(@Param("userId") Long userId, @Param("category") String category);
    
    @Query("SELECT ar FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.priorityLevel >= :minPriority ORDER BY ar.priorityLevel DESC, ar.createdAt DESC")
    List<AIRecommendation> findHighPriorityRecommendations(@Param("userId") Long userId, @Param("minPriority") Integer minPriority);
    
    @Query("DELETE FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.expiresAt <= :now AND ar.status = 'EXPIRED'")
    void deleteExpiredRecommendations(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}