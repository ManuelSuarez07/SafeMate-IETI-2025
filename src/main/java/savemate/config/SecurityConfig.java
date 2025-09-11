package safemate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // Desactiva CSRF para pruebas con Postman/Flutter
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll() //Permite todos los endpoints del backend
                        .anyRequest().authenticated()
                )
                .httpBasic(); // Autenticación básica

        return http.build();
    }
}