package savemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import savemate.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber")
    Optional<User> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    
    @Query("SELECT u FROM User u WHERE u.bankAccount = :bankAccount")
    Optional<User> findByBankAccount(@Param("bankAccount") String bankAccount);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    Long countUsersCreatedAfter(@Param("startDate") java.time.LocalDateTime startDate);
    
    @Query("SELECT SUM(u.totalSaved) FROM User u")
    Double getTotalSavingsAcrossAllUsers();
    
    @Query("SELECT AVG(u.totalSaved) FROM User u WHERE u.totalSaved > 0")
    Double getAverageSavingsPerUser();
}