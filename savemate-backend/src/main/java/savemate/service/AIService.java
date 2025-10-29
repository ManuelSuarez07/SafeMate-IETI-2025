package savemate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import savemate.dto.AIRecommendationDTO;
import savemate.model.AIRecommendation;
import savemate.model.Transaction;
import savemate.model.User;
import savemate.repository.AIRecommendationRepository;
import savemate.repository.TransactionRepository;
import savemate.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {
    
    private final AIRecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    
    @Transactional
    public AIRecommendationDTO createRecommendation(AIRecommendationDTO recommendationDTO) {
        log.info("Creando recomendación de IA para usuario ID: {}", recommendationDTO.getUserId());
        
        User user = userRepository.findById(recommendationDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        AIRecommendation recommendation = new AIRecommendation();
        recommendation.setUser(user);
        recommendation.setRecommendationType(recommendationDTO.getRecommendationType());
        recommendation.setTitle(recommendationDTO.getTitle());
        recommendation.setDescription(recommendationDTO.getDescription());
        recommendation.setActionText(recommendationDTO.getActionText());
        recommendation.setPotentialSavings(recommendationDTO.getPotentialSavings());
        recommendation.setConfidenceScore(recommendationDTO.getConfidenceScore());
        recommendation.setStatus(AIRecommendation.RecommendationStatus.PENDING);
        recommendation.setIsApplied(false);
        recommendation.setExpiresAt(recommendationDTO.getExpiresAt());
        recommendation.setCategory(recommendationDTO.getCategory());
        recommendation.setPriorityLevel(recommendationDTO.getPriorityLevel());
        
        AIRecommendation savedRecommendation = recommendationRepository.save(recommendation);
        log.info("Recomendación creada exitosamente con ID: {}", savedRecommendation.getId());
        
        return convertToDTO(savedRecommendation);
    }
    
    @Transactional
    public void generateSpendingPatternRecommendations(Long userId) {
        log.info("Generando recomendaciones de patrón de gasto para usuario ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(3);
        
        List<Object[]> spendingCategories = transactionRepository.findTopSpendingCategories(userId, startDate, endDate);
        
        for (Object[] category : spendingCategories) {
            String merchantName = (String) category[0];
            Long transactionCount = (Long) category[1];
            Double totalAmount = (Double) category[2];
            
            if (totalAmount > 50000 && transactionCount > 5) { // Umbral para recomendación
                AIRecommendation recommendation = new AIRecommendation();
                recommendation.setUser(user);
                recommendation.setRecommendationType(AIRecommendation.RecommendationType.SPENDING_PATTERN);
                recommendation.setTitle("Alto gasto en " + merchantName);
                recommendation.setDescription(String.format(
                    "Has gastado $%.2f en %s en los últimos 3 meses (%d transacciones). " +
                    "Considera reducir este gasto o buscar alternativas más económicas.",
                    totalAmount, merchantName, transactionCount));
                recommendation.setActionText("Revisar hábitos de gasto");
                recommendation.setPotentialSavings(totalAmount * 0.2); // Estimación de 20% de ahorro
                recommendation.setConfidenceScore(0.8);
                recommendation.setStatus(AIRecommendation.RecommendationStatus.PENDING);
                recommendation.setCategory(merchantName);
                recommendation.setPriorityLevel(2);
                recommendation.setExpiresAt(LocalDateTime.now().plusDays(30));
                
                recommendationRepository.save(recommendation);
            }
        }
    }
    
    @Transactional
    public void generateSavingOptimizationRecommendations(Long userId) {
        log.info("Generando recomendaciones de optimización de ahorro para usuario ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Analizar configuración actual y sugerir optimizaciones
        if (user.getSavingType() == User.SavingType.ROUNDING) {
            if (user.getRoundingMultiple() > 1000) {
                AIRecommendation recommendation = new AIRecommendation();
                recommendation.setUser(user);
                recommendation.setRecommendationType(AIRecommendation.RecommendationType.ROUNDING_CONFIG);
                recommendation.setTitle("Optimiza tu redondeo");
                recommendation.setDescription("Considera reducir el múltiplo de redondeo a $1,000 para generar ahorros más frecuentes y mantener mejor control de tus gastos.");
                recommendation.setActionText("Ajustar configuración de redondeo");
                recommendation.setPotentialSavings(5000.0); // Estimación mensual
                recommendation.setConfidenceScore(0.9);
                recommendation.setStatus(AIRecommendation.RecommendationStatus.PENDING);
                recommendation.setCategory("configuration");
                recommendation.setPriorityLevel(1);
                recommendation.setExpiresAt(LocalDateTime.now().plusDays(15));
                
                recommendationRepository.save(recommendation);
            }
        } else if (user.getSavingType() == User.SavingType.PERCENTAGE) {
            if (user.getSavingPercentage() < 10.0) {
                AIRecommendation recommendation = new AIRecommendation();
                recommendation.setUser(user);
                recommendation.setRecommendationType(AIRecommendation.RecommendationType.PERCENTAGE_CONFIG);
                recommendation.setTitle("Aumenta tu porcentaje de ahorro");
                recommendation.setDescription("Tu porcentaje de ahorro actual es del " + user.getSavingPercentage() + "%. " +
                    "Considera aumentarlo al 10% para alcanzar tus metas más rápido.");
                recommendation.setActionText("Ajustar porcentaje de ahorro");
                recommendation.setPotentialSavings(2000.0); // Estimación mensual
                recommendation.setConfidenceScore(0.85);
                recommendation.setStatus(AIRecommendation.RecommendationStatus.PENDING);
                recommendation.setCategory("configuration");
                recommendation.setPriorityLevel(1);
                recommendation.setExpiresAt(LocalDateTime.now().plusDays(15));
                
                recommendationRepository.save(recommendation);
            }
        }
    }
    
    @Transactional
    public void generateGoalAdjustmentRecommendations(Long userId) {
        log.info("Generando recomendaciones de ajuste de metas para usuario ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(1);
        
        Double monthlySavings = transactionRepository.sumSavingsByDateRange(userId, startDate, endDate);
        
        if (monthlySavings != null && monthlySavings > 0) {
            AIRecommendation recommendation = new AIRecommendation();
            recommendation.setUser(user);
            recommendation.setRecommendationType(AIRecommendation.RecommendationType.GOAL_ADJUSTMENT);
            recommendation.setTitle("Ajusta tus metas según tu ritmo de ahorro");
            recommendation.setDescription(String.format(
                "Basado en tu ahorro mensual de $%.2f, podrías considerar ajustar tus metas " +
                "para que sean más realistas y alcanzables.", monthlySavings));
            recommendation.setActionText("Revisar y ajustar metas");
            recommendation.setConfidenceScore(0.75);
            recommendation.setStatus(AIRecommendation.RecommendationStatus.PENDING);
            recommendation.setCategory("goals");
            recommendation.setPriorityLevel(2);
            recommendation.setExpiresAt(LocalDateTime.now().plusDays(20));
            
            recommendationRepository.save(recommendation);
        }
    }
    
    @Transactional
    public void generatePredictiveRecommendations(Long userId) {
        log.info("Generando recomendaciones predictivas para usuario ID: {}", userId);
        
        // Analizar patrones históricos para predecir gastos futuros
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(6);
        
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        
        // Agrupar gastos por categoría y mes
        Map<String, Map<Integer, Double>> categoryMonthlySpending = new HashMap<>();
        
        for (Transaction transaction : transactions) {
            if (transaction.getTransactionType() == Transaction.TransactionType.EXPENSE) {
                String category = transaction.getMerchantName() != null ? 
                    transaction.getMerchantName() : "Otros";
                int month = transaction.getTransactionDate().getMonthValue();
                
                categoryMonthlySpending
                    .computeIfAbsent(category, k -> new HashMap<>())
                    .merge(month, transaction.getAmount(), Double::sum);
            }
        }
        
        // Generar recomendaciones basadas en predicciones
        for (Map.Entry<String, Map<Integer, Double>> entry : categoryMonthlySpending.entrySet()) {
            String category = entry.getKey();
            Map<Integer, Double> monthlyData = entry.getValue();
            
            if (monthlyData.size() >= 3) { // Necesitamos al menos 3 meses de datos
                Double average = monthlyData.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
                
                if (average > 30000) { // Umbral para categorías significativas
                    AIRecommendation recommendation = new AIRecommendation();
                    recommendation.setUser(userRepository.findById(userId).get());
                    recommendation.setRecommendationType(AIRecommendation.RecommendationType.EXPENSE_REDUCTION);
                    recommendation.setTitle("Previsión de gasto en " + category);
                    recommendation.setDescription(String.format(
                        "Basado en tu historial, es probable que gastes alrededor de $%.2f mensuales en %s. " +
                        "Considera establecer un límite o buscar alternativas.", average, category));
                    recommendation.setActionText("Establecer límite de gasto");
                    recommendation.setPotentialSavings(average * 0.15);
                    recommendation.setConfidenceScore(0.7);
                    recommendation.setStatus(AIRecommendation.RecommendationStatus.PENDING);
                    recommendation.setCategory(category);
                    recommendation.setPriorityLevel(2);
                    recommendation.setExpiresAt(LocalDateTime.now().plusDays(25));
                    
                    recommendationRepository.save(recommendation);
                }
            }
        }
    }
    
    @Transactional
    public AIRecommendationDTO applyRecommendation(Long recommendationId) {
        log.info("Aplicando recomendación ID: {}", recommendationId);
        
        AIRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Recomendación no encontrada"));
        
        recommendation.setIsApplied(true);
        recommendation.setAppliedAt(LocalDateTime.now());
        recommendation.setStatus(AIRecommendation.RecommendationStatus.APPLIED);
        
        AIRecommendation savedRecommendation = recommendationRepository.save(recommendation);
        log.info("Recomendación aplicada exitosamente");
        
        return convertToDTO(savedRecommendation);
    }
    
    @Transactional
    public void cleanupExpiredRecommendations() {
        log.info("Limpiando recomendaciones expiradas");
        
        LocalDateTime now = LocalDateTime.now();
        List<AIRecommendation> expiredRecommendations = recommendationRepository.findAll();
        
        for (AIRecommendation recommendation : expiredRecommendations) {
            if (recommendation.isExpired() && recommendation.getStatus() != AIRecommendation.RecommendationStatus.EXPIRED) {
                recommendation.setStatus(AIRecommendation.RecommendationStatus.EXPIRED);
                recommendationRepository.save(recommendation);
            }
        }
        
        // Eliminar recomendaciones expiradas antiguas (más de 30 días)
        List<User> users = userRepository.findAll();
        for (User user : users) {
            recommendationRepository.deleteExpiredRecommendations(user.getId(), now.minusDays(30));
        }
    }
    
    @Transactional(readOnly = true)
    public List<AIRecommendationDTO> getActiveRecommendations(Long userId) {
        return recommendationRepository.findActiveRecommendations(userId, LocalDateTime.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<AIRecommendationDTO> getRecommendationsByType(Long userId, AIRecommendation.RecommendationType type) {
        return recommendationRepository.findByUserIdAndRecommendationTypeOrderByCreatedAtDesc(userId, type).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<AIRecommendationDTO> getHighPriorityRecommendations(Long userId, Integer minPriority) {
        return recommendationRepository.findHighPriorityRecommendations(userId, minPriority).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getRecommendationStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        Long totalApplied = recommendationRepository.countAppliedRecommendations(userId);
        Double totalSavings = recommendationRepository.sumAppliedRecommendationsSavings(userId);
        List<Object[]> typeStats = recommendationRepository.getRecommendationStatistics(userId);
        
        stats.put("totalApplied", totalApplied);
        stats.put("totalSavings", totalSavings != null ? totalSavings : 0.0);
        stats.put("typeStatistics", typeStats);
        
        return stats;
    }
    
    private AIRecommendationDTO convertToDTO(AIRecommendation recommendation) {
        AIRecommendationDTO dto = new AIRecommendationDTO();
        dto.setId(recommendation.getId());
        dto.setUserId(recommendation.getUser().getId());
        dto.setRecommendationType(recommendation.getRecommendationType());
        dto.setTitle(recommendation.getTitle());
        dto.setDescription(recommendation.getDescription());
        dto.setActionText(recommendation.getActionText());
        dto.setPotentialSavings(recommendation.getPotentialSavings());
        dto.setConfidenceScore(recommendation.getConfidenceScore());
        dto.setStatus(recommendation.getStatus());
        dto.setIsApplied(recommendation.getIsApplied());
        dto.setAppliedAt(recommendation.getAppliedAt());
        dto.setExpiresAt(recommendation.getExpiresAt());
        dto.setCategory(recommendation.getCategory());
        dto.setPriorityLevel(recommendation.getPriorityLevel());
        dto.setCreatedAt(recommendation.getCreatedAt());
        dto.setUpdatedAt(recommendation.getUpdatedAt());
        return dto;
    }
}