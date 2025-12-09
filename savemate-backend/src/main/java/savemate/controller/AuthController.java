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

/**
 * Controlador REST responsable de la autenticación y registro de usuarios.
 *
 * <p>Responsabilidad: Exponer endpoints HTTP bajo {@code /api/auth} para realizar operaciones
 * de inicio de sesión (login) y registro (register). Coordina la autenticación mediante
 * {@link AuthenticationManager}, obtiene detalles de usuario con {@link CustomUserDetailsService},
 * gestiona la generación de tokens JWT con {@link JwtService} y delega la persistencia/creación
 * de usuarios en {@link UserService}.
 */
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

    /**
     * Autentica a un usuario con las credenciales proporcionadas y devuelve tokens JWT.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Autentica las credenciales con {@link AuthenticationManager}.</li>
     *   <li>Si la autenticación es correcta, genera access y refresh tokens con {@link JwtService}.</li>
     *   <li>Devuelve un mapa con tokens, tipo de token, expiración y datos básicos del usuario.</li>
     * </ol>
     *
     * @param loginRequest objeto DTO con los campos {@code email} y {@code password} proporcionados por el cliente
     * @return {@link ResponseEntity} con:
     *         <ul>
     *           <li>HTTP 200 (OK) y un mapa con {@code accessToken}, {@code refreshToken}, {@code tokenType},
     *               {@code expiresIn} y {@code user} cuando las credenciales son válidas.</li>
     *           <li>HTTP 401 (UNAUTHORIZED) con mensaje en caso de credenciales inválidas.</li>
     *           <li>HTTP 500 (INTERNAL_SERVER_ERROR) en caso de error interno inesperado.</li>
     *         </ul>
     */
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
    /**
     * DTO interno que representa la petición de inicio de sesión.
     *
     * <p>Responsabilidad: Transportar los campos necesarios para autenticación
     * (email y password) en el cuerpo de la petición HTTP.
     */
    public static class LoginRequest {
        private String email;
        private String password;

        /**
         * Obtiene el email del usuario que intenta autenticarse.
         *
         * @return cadena con el email (username) proporcionado por el cliente
         */
        public String getEmail() { return email; }

        /**
         * Establece el email del usuario que intenta autenticarse.
         *
         * @param email email (username) proporcionado por el cliente
         */
        public void setEmail(String email) { this.email = email; }

        /**
         * Obtiene la contraseña proporcionada para la autenticación.
         *
         * @return cadena con la contraseña en texto plano recibida en la petición
         */
        public String getPassword() { return password; }

        /**
         * Establece la contraseña proporcionada para la autenticación.
         *
         * @param password contraseña en texto plano recibida en la petición
         */
        public void setPassword(String password) { this.password = password; }
    }


    // ------------------------------------
    //              REGISTER
    // ------------------------------------

    /**
     * Registra un nuevo usuario en el sistema y realiza auto-login devolviendo tokens JWT.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Verifica que el email no esté ya registrado.</li>
     *   <li>Crea el usuario mediante {@link UserService#createUser}.</li>
     *   <li>Genera access y refresh tokens y devuelve el usuario creado junto con los tokens.</li>
     * </ol>
     *
     * @param userDTO DTO con los datos del usuario a crear (por ejemplo: email, password, nombre, etc.)
     * @return {@link ResponseEntity} con:
     *         <ul>
     *           <li>HTTP 201 (CREATED) y un mapa con {@code accessToken}, {@code refreshToken}, {@code tokenType}
     *               y {@code user} cuando el registro es exitoso.</li>
     *           <li>HTTP 400 (BAD_REQUEST) con mensaje en caso de validación o si el email ya existe.</li>
     *         </ul>
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO userDTO) {
        log.info("Intento de registro para el email: {}", userDTO.getEmail());

        try {
            if (userService.getUserByEmail(userDTO.getEmail()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El email ya está registrado"));
            }

            // Crear el usuario
            UserDTO createdUser = userService.createUser(userDTO);

            // Autologin después de registrar
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