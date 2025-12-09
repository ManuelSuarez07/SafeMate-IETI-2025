package savemate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración MVC responsable de definir las políticas CORS para la API.
 *
 * <p>Responsabilidad: Clase de configuración (\@Configuration) que implementa {@link WebMvcConfigurer}
 * y registra mapeos CORS globales para los endpoints bajo {@code /api/**}. Permite orígenes,
 * métodos y cabeceras necesarias para que clientes externos consuman la API y evita rechazos
 * por políticas de mismo origen en peticiones cross-origin.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Registra las reglas de CORS para las rutas de la API.
     *
     * <p>Configuración aplicada:
     * <ul>
     *   <li>Se mapean las rutas bajo {@code /api/**}.</li>
     *   <li>Se permiten orígenes desde cualquier procedencia ({@code "*"}).</li>
     *   <li>Se permiten los métodos HTTP {@code GET}, {@code POST}, {@code PUT}, {@code DELETE} y {@code OPTIONS}.</li>
     *   <li>Se permiten todas las cabeceras ({@code "*"}).</li>
     *   <li>Se establece un {@code maxAge} de 3600 segundos para las preflight requests.</li>
     * </ul>
     *
     * @param registry instancia de {@link CorsRegistry} utilizada para registrar los mappings CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}