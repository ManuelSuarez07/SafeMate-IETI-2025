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

@RestController
@RequestMapping("/api/savings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SavingController {

    private final SavingService savingService;

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

    @GetMapping("/{id}")
    public ResponseEntity<SavingDTO> getSavingGoalById(@PathVariable Long id) {
        log.info("Solicitud para obtener meta de ahorro con ID: {}", id);

        Optional<SavingDTO> goal = savingService.getSavingGoalById(id);
        return goal.map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SavingDTO>> getSavingGoalsByUserId(@PathVariable Long userId) {
        log.info("Solicitud para obtener metas de ahorro del usuario ID: {}", userId);

        List<SavingDTO> goals = savingService.getSavingGoalsByUserId(userId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<SavingDTO>> getActiveSavingGoals(@PathVariable Long userId) {
        log.info("Solicitud para obtener metas activas del usuario ID: {}", userId);

        List<SavingDTO> goals = savingService.getActiveSavingGoals(userId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/user/{userId}/collaborative")
    public ResponseEntity<List<SavingDTO>> getCollaborativeGoals(@PathVariable Long userId) {
        log.info("Solicitud para obtener metas colaborativas del usuario ID: {}", userId);

        List<SavingDTO> goals = savingService.getCollaborativeGoals(userId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/user/{userId}/high-priority")
    public ResponseEntity<List<SavingDTO>> getHighPriorityGoals(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "3") Integer minPriority) {

        log.info("Solicitud para obtener metas de alta prioridad del usuario ID: {}", userId);

        List<SavingDTO> goals = savingService.getHighPriorityGoals(userId, minPriority);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/user/{userId}/due-soon")
    public ResponseEntity<List<SavingDTO>> getGoalsDueSoon(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "7") Integer daysAhead) {

        log.info("Solicitud para obtener metas que vencen pronto del usuario ID: {}", userId);

        List<SavingDTO> goals = savingService.getGoalsDueSoon(userId, daysAhead);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/user/{userId}/statistics/current-savings")
    public ResponseEntity<Double> getTotalCurrentSavings(@PathVariable Long userId) {
        log.info("Solicitud para obtener total de ahorros actuales del usuario ID: {}", userId);

        Double totalSavings = savingService.getTotalCurrentSavings(userId);
        return ResponseEntity.ok(totalSavings != null ? totalSavings : 0.0);
    }

    @GetMapping("/user/{userId}/statistics/active-count")
    public ResponseEntity<Long> getActiveGoalsCount(@PathVariable Long userId) {
        log.info("Solicitud para obtener conteo de metas activas del usuario ID: {}", userId);

        Long count = savingService.countActiveGoals(userId);
        return ResponseEntity.ok(count != null ? count : 0L);
    }

    @GetMapping("/user/{userId}/statistics/completed-count")
    public ResponseEntity<Long> getCompletedGoalsCount(@PathVariable Long userId) {
        log.info("Solicitud para obtener conteo de metas completadas del usuario ID: {}", userId);

        Long count = savingService.countCompletedGoals(userId);
        return ResponseEntity.ok(count != null ? count : 0L);
    }

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