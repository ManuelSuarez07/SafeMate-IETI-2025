package savemate.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de seguridad de la aplicación basada en Spring Security.
 *
 * <p>Responsabilidad: Clase de configuración (\@Configuration, \@EnableWebSecurity) que registra
 * los filtros de autenticación (Firebase y JWT local), define las reglas de autorización para
 * los endpoints, configura la gestión de sesiones como stateless, y expone beans auxiliares
 * necesarios por la capa de seguridad (PasswordEncoder, AuthenticationManager y CORS).
 *
 * <p>Colabora con los componentes {@link JwtAuthenticationFilter} y {@link FirebaseTokenFilter}
 * para establecer la autenticación en el contexto de seguridad y con propiedades externas para
 * la configuración de cifrado de contraseñas y tokens.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final FirebaseTokenFilter firebaseTokenFilter;

    /**
     * Construye la cadena de filtros de seguridad y configura las reglas de autorización y CORS.
     *
     * <p>Configuraciones principales:
     * <ul>
     *   <li>Deshabilita CSRF.</li>
     *   <li>Habilita CORS usando {@link #corsConfigurationSource()}.</li>
     *   <li>Define endpoints públicos y protegidos mediante reglas de autorización.</li>
     *   <li>Establece la política de sesión a {@link SessionCreationPolicy#STATELESS}.</li>
     *   <li>Registra primero el filtro de Firebase y después el filtro JWT local antes de
     *       {@link UsernamePasswordAuthenticationFilter}.</li>
     * </ul>
     *
     * @param http instancia de {@link HttpSecurity} usada para construir la configuración de seguridad
     * @return instancia de {@link SecurityFilterChain} que contiene la configuración aplicada
     * @throws Exception si ocurre un error al construir la configuración de seguridad con HttpSecurity
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers("/api/health").permitAll()

                        // Endpoints protegidos
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/savings/**").authenticated()
                        .requestMatchers("/api/transactions/**").authenticated()
                        .requestMatchers("/api/ai/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 1. Filtro de Firebase (RS256)
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // 2. Filtro JWT Local (HS256) - Se ejecuta después si Firebase no autenticó.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Proporciona un {@link PasswordEncoder} para el hashing de contraseñas.
     *
     * <p>Utiliza BCrypt con la implementación por defecto de Spring Security.
     *
     * @return instancia de {@link PasswordEncoder} (BCryptPasswordEncoder) para uso en servicios de autenticación
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expone el {@link AuthenticationManager} obtenido de la configuración de Spring.
     *
     * <p>Este bean se utiliza por los componentes que realizan autenticación explícita
     * (por ejemplo controladores de login o servicios de autenticación).
     *
     * @param config instancia de {@link AuthenticationConfiguration} proporcionada por Spring
     * @return instancia de {@link AuthenticationManager} delegada por la configuración
     * @throws Exception si no es posible obtener el {@link AuthenticationManager} desde la configuración
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Define la configuración CORS para la aplicación.
     *
     * <p>Configuración principal:
     * <ul>
     *   <li>Permite orígenes con patrón {@code "*"}.</li>
     *   <li>Permite métodos HTTP comunes y OPTIONS.</li>
     *   <li>Permite encabezados {@code Authorization}, {@code Content-Type} y {@code X-Requested-With}.</li>
     *   <li>Expone el encabezado {@code Authorization} y habilita credenciales.</li>
     *   <li>Establece {@code maxAge} a 3600 segundos.</li>
     * </ul>
     *
     * @return instancia de {@link CorsConfigurationSource} que será usada por Spring Security para CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}