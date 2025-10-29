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

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AIController {
    
    private final AIService aiService;
    
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
    
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<AIRecommendationDTO>> getActiveRecommendations(@PathVariable Long userId) {
        log.info("Solicitud para obtener recomendaciones activas del usuario ID: {}", userId);
        
        List<AIRecommendationDTO> recommendations = aiService.getActiveRecommendations(userId);
        return ResponseEntity.ok(recommendations);
    }
    
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<AIRecommendationDTO>> getRecommendationsByType(
            @PathVariable Long userId,
            @PathVariable AIRecommendation.RecommendationType type) {
        
        log.info("Solicitud para obtener recomendaciones del usuario ID: {} con tipo: {}", userId, type);
        
        List<AIRecommendationDTO> recommendations = aiService.getRecommendationsByType(userId, type);
        return ResponseEntity.ok(recommendations);
    }
    
    @GetMapping("/user/{userId}/high-priority")
    public ResponseEntity<List<AIRecommendationDTO>> getHighPriorityRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "3") Integer minPriority) {
        
        log.info("Solicitud para obtener recomendaciones de alta prioridad del usuario ID: {}", userId);
        
        List<AIRecommendationDTO> recommendations = aiService.getHighPriorityRecommendations(userId, minPriority);
        return ResponseEntity.ok(recommendations);
    }
    
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