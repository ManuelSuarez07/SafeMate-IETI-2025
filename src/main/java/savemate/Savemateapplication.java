package safemate;

/**
 * Clase principal que arranca la aplicacion Spring Boot.
 */
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SaveMateApplication {
    public static void main(String[] args) {
        SpringApplication.run(SaveMateApplication.class, args);
    }
}