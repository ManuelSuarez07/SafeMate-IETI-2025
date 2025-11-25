package savemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import savemate.model.SavingGoal;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SavingRepository extends JpaRepository<SavingGoal, Long> {
    
    List<SavingGoal> findByUserIdOrderByPriorityLevelDesc(Long userId);
    
    List<SavingGoal> findByUserIdAndStatusOrderByPriorityLevelDesc(Long userId, SavingGoal.GoalStatus status);
    
    @Query("SELECT sg FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.targetDate <= :date AND sg.status = 'ACTIVE'")
    List<SavingGoal> findGoalsDueByDate(@Param("userId") Long userId, @Param("date") LocalDateTime date);
    
    @Query("SELECT sg FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.currentAmount >= sg.targetAmount AND sg.status != 'COMPLETED'")
    List<SavingGoal> findCompletedGoalsNotMarked(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(sg) FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.status = 'ACTIVE'")
    Long countActiveGoals(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(sg) FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.status = 'COMPLETED'")
    Long countCompletedGoals(@Param("userId") Long userId);
    
    @Query("SELECT SUM(sg.targetAmount) FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.status = 'ACTIVE'")
    Double sumActiveGoalsTargetAmount(@Param("userId") Long userId);
    
    @Query("SELECT SUM(sg.currentAmount) FROM SavingGoal sg WHERE sg.user.id = :userId")
    Double sumCurrentSavings(@Param("userId") Long userId);
    
    @Query("SELECT sg FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.isCollaborative = true ORDER BY sg.createdAt DESC")
    List<SavingGoal> findCollaborativeGoals(@Param("userId") Long userId);
    
    @Query("SELECT sg FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.priorityLevel >= :minPriority ORDER BY sg.priorityLevel DESC, sg.targetDate ASC")
    List<SavingGoal> findHighPriorityGoals(@Param("userId") Long userId, @Param("minPriority") Integer minPriority);
}