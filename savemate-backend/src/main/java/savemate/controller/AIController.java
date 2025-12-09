package savemate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import savemate.dto.AIRecommendationDTO;
import savemate.dto.AIRecommendationSummaryDTO;
import savemate.model.AIRecommendation;
import savemate.service.AIService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST responsable de exponer endpoints relacionados con las recomendaciones generadas por la
 * capa de IA del sistema.
 *
 * <p>Responsabilidad: Recibir y enrutar solicitudes HTTP relacionadas con la creación, generación,
 * aplicación, limpieza y consulta de recomendaciones de IA. Traduce las solicitudes REST a llamadas sobre
 * {@link AIService} y devuelve responses estándar {@link ResponseEntity} con los códigos de estado apropiados.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AIController {

    private final AIService aiService;

    /**
     * Crea una nueva recomendación de IA a partir del DTO proporcionado.
     *
     * @param recommendationDTO DTO que contiene los datos necesarios para crear la recomendación (incluye userId, tipo, prioridad, etc.)
     * @return {@link ResponseEntity} con el {@link AIRecommendationDTO} creado y estado HTTP 201 (CREATED) en caso de éxito;
     *         HTTP 400 (BAD_REQUEST) en caso de error de validación o creación
     */
    @PostMapping
    public ResponseEntity<AIRecommendationDTO> createRecommendation(@Valid @RequestBody AIRecommendationDTO recommendationDTO) {
        log.info("Solicitud para crear recomendación de IA para usuario ID: {}", recommendationDTO.getUserId());

        try {
            AIRecommendationDTO createdRecommendation = aiService.createRecommendation(recommendationDTO);
            return new ResponseEntity<>(createdRecommendation, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error creando recomendación: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Genera recomendaciones de patrones de gasto para el usuario indicado.
     *
     * @param userId identificador del usuario para el cual se generarán las recomendaciones
     * @return {@link ResponseEntity} con estado HTTP 200 (OK) si la generación se lanzó correctamente;
     *         HTTP 400 (BAD_REQUEST) en caso de error durante la generación
     */
    @PostMapping("/generate/spending-patterns/{userId}")
    public ResponseEntity<Void> generateSpendingPatternRecommendations(@PathVariable Long userId) {
        log.info("Generando recomendaciones de patrón de gasto para usuario ID: {}", userId);

        try {
            aiService.generateSpendingPatternRecommendations(userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error generando recomendaciones de patrón de gasto: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Genera recomendaciones de optimización de ahorro para el usuario indicado.
     *
     * @param userId identificador del usuario para el cual se generarán las recomendaciones de ahorro
     * @return {@link ResponseEntity} con estado HTTP 200 (OK) si la operación se completó correctamente;
     *         HTTP 400 (BAD_REQUEST) en caso de error
     */
    @PostMapping("/generate/saving-optimization/{userId}")
    public ResponseEntity<Void> generateSavingOptimizationRecommendations(@PathVariable Long userId) {
        log.info("Generando recomendaciones de optimización de ahorro para usuario ID: {}", userId);

        try {
            aiService.generateSavingOptimizationRecommendations(userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error generando recomendaciones de optimización: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Genera recomendaciones de ajuste de metas para el usuario indicado.
     *
     * @param userId identificador del usuario para el cual se generarán las recomendaciones de ajuste de metas
     * @return {@link ResponseEntity} con estado HTTP 200 (OK) si la generación se lanzó correctamente;
     *         HTTP 400 (BAD_REQUEST) en caso de error
     */
    @PostMapping("/generate/goal-adjustment/{userId}")
    public ResponseEntity<Void> generateGoalAdjustmentRecommendations(@PathVariable Long userId) {
        log.info("Generando recomendaciones de ajuste de metas para usuario ID: {}", userId);

        try {
            aiService.generateGoalAdjustmentRecommendations(userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error generando recomendaciones de ajuste de metas: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Genera recomendaciones predictivas para el usuario indicado.
     *
     * @param userId identificador del usuario para el cual se generarán recomendaciones predictivas
     * @return {@link ResponseEntity} con estado HTTP 200 (OK) si la solicitud se procesó correctamente;
     *         HTTP 400 (BAD_REQUEST) en caso de error
     */
    @PostMapping("/generate/predictive/{userId}")
    public ResponseEntity<Void> generatePredictiveRecommendations(@PathVariable Long userId) {
        log.info("Generando recomendaciones predictivas para usuario ID: {}", userId);

        try {
            aiService.generatePredictiveRecommendations(userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error generando recomendaciones predictivas: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Genera todos los tipos de recomendaciones disponibles para el usuario indicado.
     *
     * @param userId identificador del usuario para el cual se generarán todas las recomendaciones
     * @return {@link ResponseEntity} con estado HTTP 200 (OK) si todas las operaciones se lanzaron correctamente;
     *         HTTP 400 (BAD_REQUEST) en caso de error
     */
    @PostMapping("/generate/all/{userId}")
    public ResponseEntity<Void> generateAllRecommendations(@PathVariable Long userId) {
        log.info("Generando todo tipo de recomendaciones para usuario ID: {}", userId);

        try {
            aiService.generateSpendingPatternRecommendations(userId);
            aiService.generateSavingOptimizationRecommendations(userId);
            aiService.generateGoalAdjustmentRecommendations(userId);
            aiService.generatePredictiveRecommendations(userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error generando recomendaciones: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Aplica una recomendación existente identificada por su ID.
     *
     * @param recommendationId identificador de la recomendación a aplicar
     * @return {@link ResponseEntity} con el {@link AIRecommendationDTO} aplicado y estado HTTP 200 (OK) si existe;
     *         HTTP 404 (NOT_FOUND) si la recomendación no se encuentra o no puede aplicarse
     */
    @PutMapping("/apply/{recommendationId}")
    public ResponseEntity<AIRecommendationDTO> applyRecommendation(@PathVariable Long recommendationId) {
        log.info("Aplicando recomendación ID: {}", recommendationId);

        try {
            AIRecommendationDTO appliedRecommendation = aiService.applyRecommendation(recommendationId);
            return ResponseEntity.ok(appliedRecommendation);
        } catch (RuntimeException e) {
            log.error("Error aplicando recomendación: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Elimina o marca como expiradas las recomendaciones cuyo tiempo de validez haya finalizado.
     *
     * @return {@link ResponseEntity} con estado HTTP 200 (OK) si la limpieza se completó correctamente;
     *         HTTP 500 (INTERNAL_SERVER_ERROR) en caso de error durante la operación
     */
    @PostMapping("/cleanup-expired")
    public ResponseEntity<Void> cleanupExpiredRecommendations() {
        log.info("Limpiando recomendaciones expiradas");

        try {
            aiService.cleanupExpiredRecommendations();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error limpiando recomendaciones expiradas: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene las recomendaciones activas de un usuario.
     *
     * @param userId identificador del usuario cuyas recomendaciones activas se solicitan
     * @return {@link ResponseEntity} con una lista de {@link AIRecommendationDTO} y estado HTTP 200 (OK)
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<AIRecommendationDTO>> getActiveRecommendations(@PathVariable Long userId) {
        log.info("Solicitud para obtener recomendaciones activas del usuario ID: {}", userId);

        List<AIRecommendationDTO> recommendations = aiService.getActiveRecommendations(userId);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Obtiene las recomendaciones de un usuario filtradas por tipo.
     *
     * @param userId identificador del usuario
     * @param type tipo de recomendación conforme a {@link AIRecommendation.RecommendationType}
     * @return {@link ResponseEntity} con una lista de {@link AIRecommendationDTO} filtradas por tipo y estado HTTP 200 (OK)
     */
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<AIRecommendationDTO>> getRecommendationsByType(
            @PathVariable Long userId,
            @PathVariable AIRecommendation.RecommendationType type) {

        log.info("Solicitud para obtener recomendaciones del usuario ID: {} con tipo: {}", userId, type);

        List<AIRecommendationDTO> recommendations = aiService.getRecommendationsByType(userId, type);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Obtiene recomendaciones de alta prioridad para un usuario, aplicando un umbral mínimo.
     *
     * @param userId identificador del usuario
     * @param minPriority umbral mínimo de prioridad (por defecto 3)
     * @return {@link ResponseEntity} con una lista de {@link AIRecommendationDTO} de prioridad alta y estado HTTP 200 (OK)
     */
    @GetMapping("/user/{userId}/high-priority")
    public ResponseEntity<List<AIRecommendationDTO>> getHighPriorityRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "3") Integer minPriority) {

        log.info("Solicitud para obtener recomendaciones de alta prioridad del usuario ID: {}", userId);

        List<AIRecommendationDTO> recommendations = aiService.getHighPriorityRecommendations(userId, minPriority);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Obtiene estadísticas agregadas sobre las recomendaciones de un usuario.
     *
     * @param userId identificador del usuario para el cual se solicitan las estadísticas
     * @return {@link ResponseEntity} con un mapa que contiene métricas y estadísticos (por ejemplo: contadores, tasas)
     *         y estado HTTP 200 (OK) en caso de éxito; HTTP 500 (INTERNAL_SERVER_ERROR) en caso de error
     */
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<Map<String, Object>> getRecommendationStatistics(@PathVariable Long userId) {
        log.info("Solicitud para obtener estadísticas de recomendaciones del usuario ID: {}", userId);

        try {
            Map<String, Object> statistics = aiService.getRecommendationStatistics(userId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas de recomendaciones: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Construye y devuelve un resumen de recomendaciones activas y estadísticas para un usuario.
     *
     * @param userId identificador del usuario para el que se genera el resumen
     * @return {@link ResponseEntity} con {@link AIRecommendationSummaryDTO} que contiene el listado de recomendaciones activas
     *         y las estadísticas asociadas; estado HTTP 200 (OK) en caso de éxito; HTTP 500 (INTERNAL_SERVER_ERROR) en caso de error
     */
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<AIRecommendationSummaryDTO> getRecommendationSummary(@PathVariable Long userId) {
        log.info("Solicitud para obtener resumen de recomendaciones del usuario ID: {}", userId);

        try {
            List<AIRecommendationDTO> activeRecommendations = aiService.getActiveRecommendations(userId);
            Map<String, Object> statistics = aiService.getRecommendationStatistics(userId);

            AIRecommendationSummaryDTO summary = new AIRecommendationSummaryDTO(activeRecommendations, statistics);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error obteniendo resumen de recomendaciones: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}