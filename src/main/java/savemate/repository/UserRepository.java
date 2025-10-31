package savemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import savemate.model.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}