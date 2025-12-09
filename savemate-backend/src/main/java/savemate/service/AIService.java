package savemate.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
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
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Transactional
    public AIRecommendationDTO createRecommendation(AIRecommendationDTO recommendationDTO) {
        log.info("Creando recomendación manual para usuario ID: {}", recommendationDTO.getUserId());

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
        recommendation.setCreatedAt(LocalDateTime.now()); // Asegurar fecha de creación

        AIRecommendation savedRecommendation = recommendationRepository.save(recommendation);
        log.info("Recomendación creada exitosamente con ID: {}", savedRecommendation.getId());

        return convertToDTO(savedRecommendation);
    }

    @Transactional
    public void generateSpendingPatternRecommendations(Long userId) {
        log.info("Iniciando análisis de IA con Gemini para usuario ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Obtener transacciones recientes (últimos 3 meses)
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(3);
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        if (transactions.isEmpty()) {
            log.info("No hay transacciones suficientes para generar análisis de IA.");
            return;
        }

        // 2. Formatear datos para el prompt
        String financialData = formatTransactionsForPrompt(transactions);

        // 3. Construir el prompt para Gemini
        String prompt = buildGeminiPrompt(financialData);

        try {
            // 4. Llamar a la API
            String jsonResponse = callGeminiApi(prompt);

            // 5. Parsear y Guardar
            List<AIRecommendationDTO> aiSuggestions = parseGeminiResponse(jsonResponse);

            int savedCount = 0;
            for (AIRecommendationDTO dto : aiSuggestions) {
                // Validar duplicados básicos o guardar
                AIRecommendation rec = new AIRecommendation();
                rec.setUser(user);
                rec.setRecommendationType(dto.getRecommendationType());
                rec.setTitle(dto.getTitle());
                rec.setDescription(dto.getDescription());
                rec.setActionText(dto.getActionText());
                rec.setPotentialSavings(dto.getPotentialSavings());
                rec.setConfidenceScore(dto.getConfidenceScore());
                rec.setCategory(dto.getCategory());
                rec.setPriorityLevel(dto.getPriorityLevel());

                rec.setStatus(AIRecommendation.RecommendationStatus.PENDING);
                rec.setIsApplied(false);
                rec.setCreatedAt(LocalDateTime.now());
                rec.setExpiresAt(LocalDateTime.now().plusDays(30));

                recommendationRepository.save(rec);
                savedCount++;
            }
            log.info("IA Gemini generó y guardó {} nuevas recomendaciones.", savedCount);

        } catch (Exception e) {
            log.error("Error al comunicarse con Gemini AI: {}", e.getMessage(), e);
        }
    }

    // Métodos de Ayuda para la IA

    private String formatTransactionsForPrompt(List<Transaction> transactions) {
        // Agrupamos por categoría/comercio y sumamos montos
        Map<String, Double> summary = transactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        t -> t.getMerchantName() != null ? t.getMerchantName() : "Otros",
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        StringBuilder sb = new StringBuilder();
        sb.append("Resumen de gastos últimos 3 meses:\n");
        summary.forEach((k, v) -> sb.append("- ").append(k).append(": $").append(String.format("%.2f", v)).append("\n"));
        return sb.toString();
    }

    private String buildGeminiPrompt(String financialData) {
        return "Actúa como un asesor financiero experto para la app 'SafeMate'. " +
                "Analiza los siguientes gastos de un usuario y genera 3 recomendaciones prácticas para ahorrar dinero. " +
                "DATOS:\n" + financialData + "\n\n" +
                "REGLAS OBLIGATORIAS:\n" +
                "1. Responde ÚNICAMENTE con un JSON válido. No incluyas texto antes ni después del JSON.\n" +
                "2. El formato debe ser una lista de objetos con esta estructura exacta:\n" +
                "[\n" +
                "  {\n" +
                "    \"recommendationType\": \"SPENDING_PATTERN\" (o EXPENSE_REDUCTION, SAVING_OPTIMIZATION),\n" +
                "    \"title\": \"Título corto y motivador\",\n" +
                "    \"description\": \"Explicación clara de por qué sugieres esto y cómo ayuda\",\n" +
                "    \"actionText\": \"Acción concreta (ej: Reducir salidas)\",\n" +
                "    \"potentialSavings\": 0.0 (número estimado de ahorro),\n" +
                "    \"confidenceScore\": 0.9 (entre 0.0 y 1.0),\n" +
                "    \"category\": \"Nombre de la categoría\",\n" +
                "    \"priorityLevel\": 2 (1=baja, 5=urgente)\n" +
                "  }\n" +
                "]";
    }

    private String callGeminiApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Estructura específica para Gemini API
        Map<String, Object> part = Collections.singletonMap("text", prompt);
        Map<String, Object> content = Collections.singletonMap("parts", Collections.singletonList(part));
        Map<String, Object> requestBody = Collections.singletonMap("contents", Collections.singletonList(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = geminiApiUrl + "?key=" + geminiApiKey;

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return response.getBody();
    }

    private List<AIRecommendationDTO> parseGeminiResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            // Navegar la respuesta de Gemini
            String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // Limpiar bloques de código markdown si la IA los incluyó
            text = text.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(text, new TypeReference<List<AIRecommendationDTO>>(){});
        } catch (Exception e) {
            log.error("Error parseando JSON de IA. Respuesta raw: {}", rawResponse);
            throw new RuntimeException("Error procesando respuesta de IA", e);
        }
    }

    // --- Métodos Legacy / Específicos
    @Transactional
    public void generateSavingOptimizationRecommendations(Long userId) {
        generateSpendingPatternRecommendations(userId);
    }

    @Transactional
    public void generateGoalAdjustmentRecommendations(Long userId) {
        generateSpendingPatternRecommendations(userId);
    }

    @Transactional
    public void generatePredictiveRecommendations(Long userId) {
        generateSpendingPatternRecommendations(userId);
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

        List<AIRecommendation> pending = recommendationRepository.findAll();
        for (AIRecommendation rec : pending) {
            if (rec.isExpired() && rec.getStatus() == AIRecommendation.RecommendationStatus.PENDING) {
                rec.setStatus(AIRecommendation.RecommendationStatus.EXPIRED);
                recommendationRepository.save(rec);
            }
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