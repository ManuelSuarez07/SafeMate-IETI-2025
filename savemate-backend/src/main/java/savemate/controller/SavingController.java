package savemate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import savemate.dto.SavingDTO;
import savemate.dto.SavingSummaryDTO;
import savemate.model.SavingGoal;
import savemate.service.SavingService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map; // <--- ASEGÚRATE DE TENER ESTE IMPORT
import java.util.Optional;

/**
 * Controlador REST responsable de la gestión de metas de ahorro.
 *
 * <p>Responsabilidad: Exponer endpoints HTTP bajo {@code /api/savings} para crear, actualizar,
 * consultar y ejecutar operaciones de negocio relacionadas con las metas de ahorro del sistema.
 * Traduce solicitudes HTTP a invocaciones del {@link SavingService} y devuelve respuestas estándar
 * {@link ResponseEntity} con códigos de estado apropiados.
 */
@RestController
@RequestMapping("/api/savings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SavingController {

    private final SavingService savingService;

    /**
     * Crea una nueva meta de ahorro a partir del DTO proporcionado.
     *
     * @param savingDTO DTO con los datos de la meta de ahorro a crear (incluye userId, objetivo, monto, fecha, prioridad, etc.)
     * @return {@link ResponseEntity} con el {@link SavingDTO} creado y estado HTTP 201 (CREATED) en caso de éxito;
     *         HTTP 400 (BAD_REQUEST) si ocurre un error de validación o creación.
     */
    @PostMapping
    public ResponseEntity<SavingDTO> createSavingGoal(@Valid @RequestBody SavingDTO savingDTO) {
        log.info("Solicitud para crear meta de ahorro para usuario ID: {}", savingDTO.getUserId());

        try {
            SavingDTO createdGoal = savingService.createSavingGoal(savingDTO);
            return new ResponseEntity<>(createdGoal, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error creando meta de ahorro: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Actualiza el progreso (monto ahorrado) de una meta de ahorro existente.
     *
     * @param id identificador de la meta de ahorro a actualizar
     * @param payload mapa que debe contener la clave {@code "amount"} con el monto a añadir al progreso
     * @return {@link ResponseEntity} con el {@link SavingDTO} actualizado y estado HTTP 200 (OK) si existe;
     *         HTTP 400 (BAD_REQUEST) si el payload no contiene el monto; HTTP 404 (NOT_FOUND) si la meta no existe.
     */
    @PutMapping("/{id}/progress")
    public ResponseEntity<SavingDTO> updateSavingGoalProgress(
            @PathVariable Long id,
            @RequestBody Map<String, Double> payload) {

        Double amount = payload.get("amount");

        log.info("Actualizando progreso de meta de ahorro ID: {} con monto: {}", id, amount);

        if (amount == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            SavingDTO updatedGoal = savingService.updateSavingGoalProgress(id, amount);
            return ResponseEntity.ok(updatedGoal);
        } catch (RuntimeException e) {
            log.error("Error actualizando progreso de meta: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Actualiza el estado de una meta de ahorro (por ejemplo: ACTIVE, COMPLETED, OVERDUE).
     *
     * @param id identificador de la meta de ahorro a modificar
     * @param status nuevo {@link SavingGoal.GoalStatus} que se asignará a la meta
     * @return {@link ResponseEntity} con el {@link SavingDTO} actualizado y estado HTTP 200 (OK) si existe;
     *         HTTP 404 (NOT_FOUND) si la meta no se encuentra.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<SavingDTO> updateSavingGoalStatus(
            @PathVariable Long id,
            @RequestParam SavingGoal.GoalStatus status) {

        log.info("Actualizando estado de meta de ahorro ID: {} a: {}", id, status);

        try {
            SavingDTO updatedGoal = savingService.updateSavingGoalStatus(id, status);
            return ResponseEntity.ok(updatedGoal);
        } catch (RuntimeException e) {
            log.error("Error actualizando estado de meta: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Distribuye un importe total entre las metas de ahorro de un usuario siguiendo la lógica de negocio.
     *
     * @param userId identificador del usuario cuyas metas recibirán la distribución
     * @param totalAmount importe total a distribuir entre las metas del usuario
     * @return {@link ResponseEntity} vacío con HTTP 200 (OK) si la distribución se ejecutó correctamente;
     *         HTTP 400 (BAD_REQUEST) en caso de error de negocio.
     */
    @PostMapping("/distribute/{userId}")
    public ResponseEntity<Void> distributeSavingsToGoals(
            @PathVariable Long userId,
            @RequestParam Double totalAmount) {

        log.info("Distribuyendo ahorros de {} a metas del usuario ID: {}", totalAmount, userId);

        try {
            savingService.distributeSavingsToGoals(userId, totalAmount);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error distribuyendo ahorros: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Verifica metas vencidas y actualiza su estado según la lógica definida.
     *
     * @return {@link ResponseEntity} vacío con HTTP 200 (OK) si la operación se completó correctamente;
     *         HTTP 500 (INTERNAL_SERVER_ERROR) si ocurre un error durante la verificación.
     */
    @PostMapping("/check-overdue")
    public ResponseEntity<Void> checkOverdueGoals() {
        log.info("Verificando metas de ahorro vencidas");

        try {
            savingService.checkAndUpdateOverdueGoals();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error verificando metas vencidas: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verifica metas completadas y ejecuta acciones asociadas (notificaciones, marcación, etc.).
     *
     * @return {@link ResponseEntity} vacío con HTTP 200 (OK) si la operación se completó correctamente;
     *         HTTP 500 (INTERNAL_SERVER_ERROR) si ocurre un error durante la verificación.
     */
    @PostMapping("/check-completed")
    public ResponseEntity<Void> checkCompletedGoals() {
        log.info("Verificando metas completadas");

        try {
            savingService.checkCompletedGoals();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error verificando metas completadas: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene una meta de ahorro por su identificador.
     *
     * @param id identificador de la meta solicitada
     * @return {@link ResponseEntity} con el {@link SavingDTO} y HTTP 200 (OK) si se encuentra;
     *         HTTP 404 (NOT_FOUND) si no existe la meta.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SavingDTO> getSavingGoalById(@PathVariable Long id) {
        log.info("Solicitud para obtener meta de ahorro con ID: {}", id);

        Optional<SavingDTO> goal = savingService.getSavingGoalById(id);
        return goal.map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Obtiene todas las metas de ahorro asociadas a un usuario.
     *
     * @param userId identificador del usuario cuyas metas se solicitan
     * @return {@link ResponseEntity} con una lista de {@link SavingDTO} y HTTP 200 (OK).
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SavingDTO>> getSavingGoalsByUserId(@PathVariable Long userId) {
        log.info("Solicitud para obtener metas de ahorro del usuario ID: {}", userId);

        List<SavingDTO> goals = savingService.getSavingGoalsByUserId(userId);
        return ResponseEntity.ok(goals);
    }

    /**
     * Obtiene las metas activas de un usuario.
     *
     * @param userId identificador del usuario
     * @return {@link ResponseEntity} con la lista de {@link SavingDTO} activas y HTTP 200 (OK).
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<SavingDTO>> getActiveSavingGoals(@PathVariable Long userId) {
        log.info("Solicitud para obtener metas activas del usuario ID: {}", userId);

        List<SavingDTO> goals = savingService.getActiveSavingGoals(userId);
        return ResponseEntity.ok(goals);
    }

    /**
     * Obtiene las metas colaborativas (compartidas) de un usuario.
     *
     * @param userId identificador del usuario
     * @return {@link ResponseEntity} con la lista de metas colaborativas ({@link SavingDTO}) y HTTP 200 (OK).
     */
    @GetMapping("/user/{userId}/collaborative")
    public ResponseEntity<List<SavingDTO>> getCollaborativeGoals(@PathVariable Long userId) {
        log.info("Solicitud para obtener metas colaborativas del usuario ID: {}", userId);

        List<SavingDTO> goals = savingService.getCollaborativeGoals(userId);
        return ResponseEntity.ok(goals);
    }

    /**
     * Obtiene metas de alta prioridad para un usuario aplicando un umbral mínimo.
     *
     * @param userId identificador del usuario
     * @param minPriority umbral mínimo de prioridad (valor por defecto: 3) utilizado para filtrar las metas
     * @return {@link ResponseEntity} con la lista de {@link SavingDTO} que cumplen la prioridad y HTTP 200 (OK).
     */
    @GetMapping("/user/{userId}/high-priority")
    public ResponseEntity<List<SavingDTO>> getHighPriorityGoals(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "3") Integer minPriority) {

        log.info("Solicitud para obtener metas de alta prioridad del usuario ID: {}", userId);

        List<SavingDTO> goals = savingService.getHighPriorityGoals(userId, minPriority);
        return ResponseEntity.ok(goals);
    }

    /**
     * Obtiene metas que vencen en los próximos días indicados.
     *
     * @param userId identificador del usuario
     * @param daysAhead número de días hacia adelante para considerar la ventana de vencimiento (por defecto: 7)
     * @return {@link ResponseEntity} con la lista de {@link SavingDTO} que vencen pronto y HTTP 200 (OK).
     */
    @GetMapping("/user/{userId}/due-soon")
    public ResponseEntity<List<SavingDTO>> getGoalsDueSoon(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "7") Integer daysAhead) {

        log.info("Solicitud para obtener metas que vencen pronto del usuario ID: {}", userId);

        List<SavingDTO> goals = savingService.getGoalsDueSoon(userId, daysAhead);
        return ResponseEntity.ok(goals);
    }

    /**
     * Obtiene el total de ahorros actuales del usuario.
     *
     * @param userId identificador del usuario
     * @return {@link ResponseEntity} con un {@link Double} que representa el total de ahorros actuales;
     *         devuelve 0.0 si el total es nulo.
     */
    @GetMapping("/user/{userId}/statistics/current-savings")
    public ResponseEntity<Double> getTotalCurrentSavings(@PathVariable Long userId) {
        log.info("Solicitud para obtener total de ahorros actuales del usuario ID: {}", userId);

        Double totalSavings = savingService.getTotalCurrentSavings(userId);
        return ResponseEntity.ok(totalSavings != null ? totalSavings : 0.0);
    }

    /**
     * Obtiene el conteo de metas activas de un usuario.
     *
     * @param userId identificador del usuario
     * @return {@link ResponseEntity} con un {@link Long} representando el conteo de metas activas; devuelve 0 si es nulo.
     */
    @GetMapping("/user/{userId}/statistics/active-count")
    public ResponseEntity<Long> getActiveGoalsCount(@PathVariable Long userId) {
        log.info("Solicitud para obtener conteo de metas activas del usuario ID: {}", userId);

        Long count = savingService.countActiveGoals(userId);
        return ResponseEntity.ok(count != null ? count : 0L);
    }

    /**
     * Obtiene el conteo de metas completadas de un usuario.
     *
     * @param userId identificador del usuario
     * @return {@link ResponseEntity} con un {@link Long} representando el conteo de metas completadas; devuelve 0 si es nulo.
     */
    @GetMapping("/user/{userId}/statistics/completed-count")
    public ResponseEntity<Long> getCompletedGoalsCount(@PathVariable Long userId) {
        log.info("Solicitud para obtener conteo de metas completadas del usuario ID: {}", userId);

        Long count = savingService.countCompletedGoals(userId);
        return ResponseEntity.ok(count != null ? count : 0L);
    }

    /**
     * Construye y devuelve un resumen agregando métricas y listados relevantes de metas de un usuario.
     *
     * @param userId identificador del usuario para el cual se genera el resumen
     * @return {@link ResponseEntity} con {@link SavingSummaryDTO} que contiene métricas agregadas y listados;
     *         HTTP 200 (OK) en caso de éxito; HTTP 500 (INTERNAL_SERVER_ERROR) en caso de error.
     */
    @GetMapping("/user/{userId}/statistics/summary")
    public ResponseEntity<SavingSummaryDTO> getSavingSummary(@PathVariable Long userId) {
        log.info("Solicitud para obtener resumen de ahorros del usuario ID: {}", userId);

        try {
            List<SavingDTO> allGoals = savingService.getSavingGoalsByUserId(userId);
            List<SavingDTO> activeGoals = savingService.getActiveSavingGoals(userId);
            Double totalCurrentSavings = savingService.getTotalCurrentSavings(userId);
            Long activeCount = savingService.countActiveGoals(userId);
            Long completedCount = savingService.countCompletedGoals(userId);

            SavingSummaryDTO summary = new SavingSummaryDTO(totalCurrentSavings, activeCount, completedCount, allGoals, activeGoals);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error obteniendo resumen de ahorros: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}