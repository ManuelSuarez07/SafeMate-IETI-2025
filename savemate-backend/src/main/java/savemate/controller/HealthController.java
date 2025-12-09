package savemate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST responsable de exponer un endpoint de comprobación de estado (health-check).
 *
 * <p>Responsabilidad: Proveer un endpoint simple bajo {@code /api/health} que permita a
 * sistemas de monitorización, balanceadores de carga y orquestadores verificar la disponibilidad
 * y obtener metadatos básicos de la aplicación (marca temporal, nombre y versión).
 */
@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    /**
     * Devuelve el estado operativo de la aplicación junto con metadatos relevantes.
     *
     * <p>Construye y devuelve un mapa con las siguientes claves:
     * <ul>
     *   <li>{@code status}: estado de disponibilidad de la aplicación (por ejemplo: "UP").</li>
     *   <li>{@code timestamp}: marca temporal de la respuesta.</li>
     *   <li>{@code application}: nombre de la aplicación.</li>
     *   <li>{@code version}: versión desplegada de la aplicación.</li>
     * </ul>
     *
     * @return {@link ResponseEntity} que envuelve un {@link Map} con el estado y metadatos de la aplicación.
     *         Responde con HTTP 200 (OK) cuando la información se construye correctamente.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("application", "SaveMate Backend");
        response.put("version", "1.0.0");

        return ResponseEntity.ok(response);
    }
}