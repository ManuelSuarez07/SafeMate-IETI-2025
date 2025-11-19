package savemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import savemate.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);
    
    List<Transaction> findByUserIdAndTransactionTypeOrderByTransactionDateDesc(Long userId, Transaction.TransactionType transactionType);
    
    List<Transaction> findByUserIdAndStatusOrderByTransactionDateDesc(Long userId, Transaction.TransactionStatus status);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                              @Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    Double sumTransactionsByTypeAndDateRange(@Param("userId") Long userId,
                                            @Param("type") Transaction.TransactionType type,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(t.savingAmount) FROM Transaction t WHERE t.user.id = :userId AND t.savingAmount IS NOT NULL AND t.transactionDate BETWEEN :startDate AND :endDate")
    Double sumSavingsByDateRange(@Param("userId") Long userId,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId AND t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    Long countTransactionsByTypeAndDateRange(@Param("userId") Long userId,
                                            @Param("type") Transaction.TransactionType type,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t.merchantName, COUNT(t), SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.transactionType = 'EXPENSE' AND t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.merchantName ORDER BY SUM(t.amount) DESC")
    List<Object[]> findTopSpendingCategories(@Param("userId") Long userId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.status = 'PENDING' ORDER BY t.createdAt ASC")
    List<Transaction> findPendingTransactions(@Param("userId") Long userId);
}