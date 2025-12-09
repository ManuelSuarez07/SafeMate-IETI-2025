package savemate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "savemate.repository")
public class JpaConfig {

}// java
package savemate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Clase de configuración de Spring responsable de habilitar las capacidades de
 * JPA en la aplicación.
 *
 * <p>Responsabilidad: Clase de configuración (\@Configuration) que activa la auditoría
 * automática de entidades JPA mediante {@link EnableJpaAuditing} y habilita la
 * detección de repositorios JPA en el paquete {@code savemate.repository} mediante
 * {@link EnableJpaRepositories}. Esta clase no expone métodos públicos; su única
 * responsabilidad es registrar las anotaciones de configuración necesarias para
 * la capa de persistencia.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "savemate.repository")
public class JpaConfig {

    /**
     * Constructor privado para evitar la instanciación de la clase de configuración.
     *
     * <p>La clase se utiliza únicamente como contenedor de anotaciones de configuración
     * y no debe ser instanciada manualmente.
     */
    private JpaConfig() {
        // Prevent instantiation
    }
}