package savemate.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * Configuración de Spring que provee un bean de {@link FirebaseApp} para la aplicación.
 *
 * <p>Responsabilidad: Clase de configuración (\@Configuration) que inicializa y proporciona
 * una instancia de {@link FirebaseApp} usando las credenciales situadas en el classpath
 * (archivo {@code service-account-file.json}). Garantiza que la aplicación Firebase se
 * inicialice una sola vez y que el bean esté disponible para inyección en otros componentes.
 */
@Configuration
public class FirebaseConfig {

    /**
     * Proporciona un bean de {@link FirebaseApp}.
     *
     * <p>Si no existe ninguna instancia de {@link FirebaseApp} ya inicializada, lee las
     * credenciales desde el recurso de clase {@code service-account-file.json}, construye
     * un {@link FirebaseOptions} y inicializa la aplicación Firebase. Si ya existe una
     * instancia inicializada, devuelve la instancia existente.
     *
     * @return instancia de {@link FirebaseApp} inicializada y lista para su uso por otros componentes
     * @throws IOException si ocurre un error al leer el archivo de credenciales desde el classpath
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            ClassPathResource resource = new ClassPathResource("service-account-file.json");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                    .build();

            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }
}