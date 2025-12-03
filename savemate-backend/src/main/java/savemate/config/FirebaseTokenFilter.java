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
import java.util.ArrayList;
import java.util.Collections; // Usaremos Collections.emptyList()

@Component
@Slf4j // Usar el logger
public class FirebaseTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Si ya está autenticado (por ejemplo, por el filtro JWT local), no hacer nada.
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

                // NOTA: Una vez que Firebase autentica, el contexto ya no es nulo,
                // por lo que el JwtAuthenticationFilter (si se ejecuta después) lo ignorará.

            } catch (FirebaseAuthException e) {
                // Si la validación de Firebase falla, no lanzamos excepción, solo limpiamos
                // y dejamos que el siguiente filtro (JwtAuthenticationFilter) lo intente,
                // o que la petición sea rechazada al final.
                log.warn("Error al validar Token de Firebase (posible token local o expirado): {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}