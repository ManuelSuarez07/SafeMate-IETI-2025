package safemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import safemate.model.Transaction;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByOccurredAtDesc(Long userId);
}