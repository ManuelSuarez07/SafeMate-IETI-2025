package savemate.service;

import org.springframework.stereotype.Service;
import savemate.model.User;
import savemate.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository) { this.userRepository = userRepository; }

    public User create(User user) { return userRepository.save(user); }
    public Optional<User> findById(Long id) { return userRepository.findById(id); }
    public Optional<User> findByEmail(String email) { return userRepository.findByEmail(email); }
    public List<User> findAll() {
        return userRepository.findAll();
    }
    public boolean deleteById(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<User> updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(updatedUser.getUsername());
            user.setEmail(updatedUser.getEmail());
            user.setPassword(updatedUser.getPassword());
            user.setName(updatedUser.getName());
            user.setPasswordHash(updatedUser.getPasswordHash());
            return userRepository.save(user);
        });
    }


}