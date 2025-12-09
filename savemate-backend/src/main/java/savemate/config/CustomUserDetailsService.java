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

/**
 * Servicio de Spring Security responsable de proporcionar los detalles de autenticación
 * de un usuario a partir de su email.
 *
 * <p>Responsabilidad: Servicio de negocio que implementa {@link UserDetailsService} para
 * recuperar información de autenticación de la capa de persistencia ({@link savemate.repository.UserRepository}).
 * Convierte la entidad de usuario de la aplicación en un {@link UserDetails} utilizado por el
 * framework de seguridad (por ejemplo, construye un usuario con la autoridad {@code ROLE_USER}).
 *
 * <p>La implementación usa inyección por constructor (anotación {@code @RequiredArgsConstructor})
 * para obtener el repositorio de usuarios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carga los detalles de autenticación de un usuario por su email.
     *
     * <p>Busca la entidad de usuario en {@link UserRepository} usando el email proporcionado.
     * Si se encuentra, construye y devuelve un {@link UserDetails} con el nombre de usuario,
     * contraseña y las autoridades necesarias para la autenticación en Spring Security.
     *
     * @param email email del usuario que se desea cargar (identificador único para la autenticación)
     * @return un objeto {@link UserDetails} que contiene username, password y authorities para uso por Spring Security
     * @throws UsernameNotFoundException si no se encuentra ningún usuario con el email proporcionado
     */
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