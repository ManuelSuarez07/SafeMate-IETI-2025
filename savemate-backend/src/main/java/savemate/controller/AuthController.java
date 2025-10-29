package savemate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import savemate.config.JwtService;
import savemate.config.CustomUserDetailsService;
import savemate.dto.UserDTO;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        log.info("Intento de login para el email: {}", loginRequest.getEmail());
        
        try {
            // Autenticar al usuario
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );
            
            // Cargar detalles del usuario
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
            
            // Generar tokens
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);
            
            // Crear respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtService.getJwtExpiration());
            response.put("email", userDetails.getUsername());
            
            log.info("Login exitoso para el email: {}", loginRequest.getEmail());
            
            return ResponseEntity.ok(response);
            
        } catch (BadCredentialsException e) {
            log.warn("Credenciales inválidas para el email: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        } catch (Exception e) {
            log.error("Error durante el login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error en el servidor"));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("Solicitud de refresco de token");
        
        try {
            String refreshToken = request.getRefreshToken();
            String username = jwtService.extractUsername(refreshToken);
            
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                if (jwtService.isTokenValid(refreshToken, userDetails)) {
                    String newAccessToken = jwtService.generateToken(userDetails);
                    String newRefreshToken = jwtService.generateRefreshToken(userDetails);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("accessToken", newAccessToken);
                    response.put("refreshToken", newRefreshToken);
                    response.put("tokenType", "Bearer");
                    response.put("expiresIn", jwtService.getJwtExpiration());
                    
                    return ResponseEntity.ok(response);
                }
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token de refresco inválido"));
            
        } catch (Exception e) {
            log.error("Error refrescando token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error en el servidor"));
        }
    }
    
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody ValidateTokenRequest request) {
        log.info("Validando token");
        
        try {
            String token = request.getToken();
            
            if (jwtService.isTokenValid(token)) {
                Map<String, Object> userInfo = jwtService.extractUserInfo(token);
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "userInfo", userInfo
                ));
            } else {
                return ResponseEntity.ok(Map.of("valid", false));
            }
            
        } catch (Exception e) {
            log.error("Error validando token: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("valid", false));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "OK", "service", "Auth API"));
    }
    
    // DTOs para las peticiones
    public static class LoginRequest {
        private String email;
        private String password;
        
        // Getters y setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class RefreshTokenRequest {
        private String refreshToken;
        
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }
    
    public static class ValidateTokenRequest {
        private String token;
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}