package savemate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Punto de entrada principal (Entry Point) y clase de configuración de arranque para la aplicación SaveMate.
 * <p>
 * Esta clase es responsable de inicializar el contexto de Spring Boot (ApplicationContext),
 * disparar el escaneo de componentes, la autoconfiguración de la infraestructura y levantar el servidor web embebido.
 * </p>
 * <p>
 * Adicionalmente, mediante la anotación {@code @EnableJpaAuditing}, activa globalmente el mecanismo de auditoría de JPA.
 * Esto permite que el framework gestione automáticamente los metadatos temporales de las entidades
 * (campos anotados con {@code @CreatedDate} y {@code @LastModifiedDate}) en cada operación de persistencia.
 * </p>
 */
@SpringBootApplication
@EnableJpaAuditing
public class SaveMateApplication {

    /**
     * Método estático estándar que inicia la ejecución de la Máquina Virtual de Java (JVM) y el contenedor de Spring.
     * <p>
     * Delega el control a {@link SpringApplication#run} para realizar el bootstrapping de la aplicación,
     * cargar las propiedades del entorno y establecer la conexión con la base de datos.
     * </p>
     *
     * @param args Argumentos de línea de comandos pasados al iniciar la aplicación.
     * Pueden utilizarse para sobrescribir propiedades de configuración en tiempo de ejecución.
     */
    public static void main(String[] args) {
        SpringApplication.run(SaveMateApplication.class, args);
    }
}