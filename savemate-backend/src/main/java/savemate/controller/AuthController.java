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
import savemate.service.UserService;

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
    private final UserService userService;

    // ------------------------------------
    //              LOGIN
    // ------------------------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        log.info("Intento de login para el email: {}", loginRequest.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            UserDTO userDto = userService.getUserByEmail(loginRequest.getEmail())
                    .orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtService.getJwtExpiration());
            response.put("user", userDto);

            log.info("Login exitoso");

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error en el servidor"));
        }
    }

    // DTO login
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }


    // ------------------------------------
    //              REGISTER
    // ------------------------------------
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO userDTO) {
        log.info("Intento de registro para el email: {}", userDTO.getEmail());

        try {
            if (userService.getUserByEmail(userDTO.getEmail()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El email ya está registrado"));
            }

            // 🔥 Crear el usuario (aquí ya encripta la contraseña internamente)
            UserDTO createdUser = userService.createUser(userDTO);

            // 🔥 Autologin después de registrar
            UserDetails userDetails = userDetailsService.loadUserByUsername(userDTO.getEmail());
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("user", createdUser);

            log.info("Registro exitoso");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error al registrar usuario", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}
