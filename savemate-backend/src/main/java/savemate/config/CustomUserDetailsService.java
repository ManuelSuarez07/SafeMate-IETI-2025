package savemate.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import savemate.repository.UserRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Cargando usuario por email: {}", email);
        
        return userRepository.findByEmail(email)
                .map(user -> User.builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                        .accountExpired(false)
                        .accountLocked(false)
                        .credentialsExpired(false)
                        .disabled(false)
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));
    }
}