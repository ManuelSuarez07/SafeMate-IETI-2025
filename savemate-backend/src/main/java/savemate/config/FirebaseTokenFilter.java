package savemate.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j; // Importar Slf4j para logs
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections; // Usaremos Collections.emptyList()

/**
 * Filtro de seguridad que valida tokens de Firebase y establece la autenticación en Spring Security.
 *
 * <p>Responsabilidad: Componente de infraestructura de seguridad (\@Component) que intercepta
 * cada petición HTTP (extiende {@link OncePerRequestFilter}), extrae el encabezado
 * {@code Authorization} con esquema Bearer, verifica el token mediante {@link FirebaseAuth}
 * y, si es válido, construye un objeto de autenticación {@link UsernamePasswordAuthenticationToken}
 * usando el email del usuario como principal. Si ya existe una autenticación en el contexto,
 * el filtro delega sin realizar nuevas comprobaciones. Los errores de validación de Firebase
 * se registran y la petición continúa sin autenticación.
 */
@Component
@Slf4j
public class FirebaseTokenFilter extends OncePerRequestFilter {

    /**
     * Intercepta la petición HTTP y valida el token de Firebase si está presente.
     *
     * <p>Flujo principal:
     * <ol>
     *   <li>Si ya existe una autenticación en el {@link SecurityContextHolder}, delega al siguiente filtro.</li>
     *   <li>Lee el encabezado {@code Authorization} y, si contiene un token Bearer, intenta verificarlo con {@link FirebaseAuth#verifyIdToken}.</li>
     *   <li>Si la verificación es correcta, extrae el email del {@link FirebaseToken}, crea un {@link UsernamePasswordAuthenticationToken}
     *       con autoridades vacías y lo establece en el contexto de seguridad.</li>
     *   <li>Si ocurre una excepción de Firebase, se registra una advertencia y la cadena de filtros continua sin establecer autenticación.</li>
     * </ol>
     *
     * @param request petición HTTP entrante que puede contener el encabezado Authorization
     * @param response respuesta HTTP asociada a la petición
     * @param filterChain cadena de filtros a la que delegar la petición después del procesamiento
     * @throws ServletException si ocurre un error de tipo servlet durante el filtrado
     * @throws IOException si ocurre un error de E/S al procesar la petición/respuesta
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Verificar token con Firebase (RS256)
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                String email = decodedToken.getEmail();

                log.debug("Token Firebase válido. UID: {}, Email: {}", decodedToken.getUid(), email);

                // Crear sesión en Spring Security. Usamos email como Principal.
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.emptyList()); // Usar Collections.emptyList() para roles vacíos

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (FirebaseAuthException e) {
                log.warn("Error al validar Token de Firebase (posible token local o expirado): {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}