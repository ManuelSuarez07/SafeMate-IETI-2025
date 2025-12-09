// java
package savemate.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de seguridad responsable de autenticar peticiones HTTP mediante tokens JWT.
 *
 * <p>Responsabilidad: Componente de infraestructura de seguridad (\@Component) que extiende
 * {@link OncePerRequestFilter} y realiza la extracción y validación de tokens JWT presentes
 * en el encabezado {@code Authorization} (esquema Bearer). Si el token es válido, carga
 * los detalles del usuario mediante {@link UserDetailsService}, crea una instancia de
 * {@link UsernamePasswordAuthenticationToken} y la establece en el {@link SecurityContextHolder}
 * para que el resto de la aplicación trabaje con la identidad autenticada.
 *
 * <p>Colabora con {@link JwtService} para operaciones relacionadas con el token (extracción
 * de username y validación).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Procesa la petición HTTP para autenticar al usuario mediante un token JWT.
     *
     * <p>Flujo principal:
     * <ol>
     *   <li>Extrae el encabezado {@code Authorization} y comprueba que tenga el prefijo {@code Bearer }.</li>
     *   <li>Si existe un token, extrae el email/username con {@link JwtService#extractUsername}.</li>
     *   <li>Si el usuario no está autenticado en el contexto, carga {@link UserDetails} y valida el token
     *       con {@link JwtService#isTokenValid}. Si es válido, crea y establece un
     *       {@link UsernamePasswordAuthenticationToken} con las authorities del usuario.</li>
     *   <li>Registra eventos relevantes mediante logs y continúa la cadena de filtros.</li>
     * </ol>
     *
     * @param request petición HTTP entrante que puede contener el encabezado Authorization con el token JWT
     * @param response respuesta HTTP asociada a la petición
     * @param filterChain cadena de filtros a la que delegar la petición después del procesamiento
     * @throws ServletException si ocurre un error de tipo servlet durante el filtrado
     * @throws IOException si ocurre un error de E/S al procesar la petición/respuesta
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Verificar si el header está presente y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No se encontró token JWT en el header Authorization");
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token
        jwt = authHeader.substring(7);

        try {
            // Extraer el email del token
            userEmail = jwtService.extractUsername(jwt);

            // Verificar si el usuario no está autenticado
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Cargar los detalles del usuario
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Validar el token
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // Crear el token de autenticación
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    // Establecer detalles de la autenticación
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Establecer la autenticación en el contexto de seguridad
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Usuario {} autenticado exitosamente", userEmail);
                } else {
                    log.warn("Token JWT inválido para el usuario: {}", userEmail);
                }
            }
        } catch (Exception e) {
            log.error("Error procesando token JWT: {}", e.getMessage());
            // No lanzar excepción para no interrumpir el flujo
        }

        filterChain.doFilter(request, response);
    }
}