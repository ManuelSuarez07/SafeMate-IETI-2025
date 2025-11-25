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

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
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