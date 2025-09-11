package safemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import safemate.model.SavingGoal;
import java.util.List;

public interface SavingRepository extends JpaRepository<SavingGoal, Long> {
    List<SavingGoal> findByUserId(Long userId);
}