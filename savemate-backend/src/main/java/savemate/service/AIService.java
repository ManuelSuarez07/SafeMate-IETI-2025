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

/**
 * Servicio de lógica de negocio responsable de la orquestación e integración con motores de
 * Inteligencia Artificial Generativa (específicamente Google Gemini).
 * Este servicio actúa como un puente entre los datos financieros transaccionales del usuario y
 * los modelos de lenguaje grande (LLM). Sus responsabilidades principales incluyen:
 * <ul>
 * <li>Recopilación y anonimización parcial de datos financieros históricos.</li>
 * <li>Construcción de prompts (Ingeniería de Prompts) para análisis financiero contextual.</li>
 * <li>Gestión del ciclo de vida de las recomendaciones (generación, persistencia, aplicación y expiración).</li>
 * <li>Comunicación HTTP con la API externa de Gemini y parseo resiliente de respuestas JSON.</li>
 * </ul>
 */
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

    /**
     * Crea y persiste una recomendación financiera de forma manual o explícita.
     * Útil para administradores o para inyectar sugerencias predefinidas por el sistema sin usar la IA.
     *
     * @param recommendationDTO Objeto de transferencia con los datos de la sugerencia a crear.
     * @return El DTO de la recomendación persistida, incluyendo su ID generado y marcas de tiempo.
     * @throws RuntimeException Si el usuario especificado en el DTO no existe.
     */
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
        recommendation.setCreatedAt(LocalDateTime.now());

        AIRecommendation savedRecommendation = recommendationRepository.save(recommendation);
        log.info("Recomendación creada exitosamente con ID: {}", savedRecommendation.getId());

        return convertToDTO(savedRecommendation);
    }

    /**
     * Motor principal de análisis financiero. Orquesta el flujo completo de generación de insights con IA.
     * El flujo de ejecución es:
     * 1. Recupera el historial transaccional de los últimos 3 meses.
     * 2. Agrega y resume los datos para minimizar el consumo de tokens.
     * 3. Construye un prompt especializado solicitando una respuesta en formato JSON estricto.
     * 4. Invoca la API de Gemini y procesa la respuesta para persistir las nuevas sugerencias.
     *
     * @param userId Identificador del usuario sobre el cual se realizará el análisis.
     * @throws RuntimeException Si el usuario no existe. Los errores de la API de IA se capturan y loguean (no relanzan excepción).
     */
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

    /**
     * Condensa el historial de transacciones en un resumen textual agrupado por categorías.
     * Optimiza el uso de tokens en la petición a la IA enviando solo datos agregados en lugar de registros crudos.
     *
     * @param transactions Lista de transacciones a procesar.
     * @return Cadena de texto con el resumen de gastos formateado para lectura humana/IA.
     */
    private String formatTransactionsForPrompt(List<Transaction> transactions) {
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

    /**
     * Construye el prompt de sistema (System Prompt) definiendo la "personalidad" del asistente y las reglas de salida.
     * Incluye instrucciones estrictas para forzar la salida en formato JSON compatible con {@link AIRecommendationDTO}.
     *
     * @param financialData Datos financieros ya formateados.
     * @return String con el prompt completo listo para enviar a la API.
     */
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

    /**
     * Ejecuta la llamada HTTP POST a la API REST de Google Gemini.
     * Encapsula la autenticación mediante API Key y la estructura del cuerpo de la petición (parts/contents).
     *
     * @param prompt Texto del prompt a enviar.
     * @return El cuerpo de la respuesta cruda (JSON string) devuelto por Gemini.
     */
    private String callGeminiApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> part = Collections.singletonMap("text", prompt);
        Map<String, Object> content = Collections.singletonMap("parts", Collections.singletonList(part));
        Map<String, Object> requestBody = Collections.singletonMap("contents", Collections.singletonList(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = geminiApiUrl + "?key=" + geminiApiKey;

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return response.getBody();
    }

    /**
     * Procesa la respuesta compleja de la API de Gemini para extraer y deserializar la lista de recomendaciones.
     * Incluye lógica de limpieza (sanitización) para eliminar bloques de código Markdown (```json) que los LLMs
     * suelen incluir accidentalmente.
     *
     * @param rawResponse Respuesta JSON cruda de la API de Google.
     * @return Lista de objetos DTO deserializados.
     * @throws RuntimeException Si falla la navegación por el árbol JSON o la deserialización.
     */
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

    /**
     * Genera recomendaciones enfocadas en la optimización del ahorro.
     * Actualmente delega en la estrategia general de análisis de patrones de gasto.
     *
     * @param userId Identificador del usuario.
     */
    @Transactional
    public void generateSavingOptimizationRecommendations(Long userId) {
        generateSpendingPatternRecommendations(userId);
    }

    /**
     * Analiza el progreso de las metas para sugerir ajustes.
     * Actualmente delega en la estrategia general de análisis de patrones de gasto.
     *
     * @param userId Identificador del usuario.
     */
    @Transactional
    public void generateGoalAdjustmentRecommendations(Long userId) {
        generateSpendingPatternRecommendations(userId);
    }

    /**
     * Genera predicciones financieras basadas en comportamiento histórico.
     * Actualmente delega en la estrategia general de análisis de patrones de gasto.
     *
     * @param userId Identificador del usuario.
     */
    @Transactional
    public void generatePredictiveRecommendations(Long userId) {
        generateSpendingPatternRecommendations(userId);
    }

    /**
     * Marca una recomendación existente como "Aplicada" o aceptada por el usuario.
     * Esto actualiza su estado y registra la fecha de aplicación para futuras métricas de ahorro.
     *
     * @param recommendationId ID de la recomendación a aplicar.
     * @return El DTO actualizado reflejando el nuevo estado.
     * @throws RuntimeException Si la recomendación no existe.
     */
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

    /**
     * Tarea de mantenimiento que busca recomendaciones pendientes cuya fecha de validez ha expirado
     * y actualiza su estado a {@code EXPIRED}.
     * Se recomienda invocar este método mediante un programador de tareas (ej. @Scheduled).
     */
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

    /**
     * Recupera todas las recomendaciones vigentes y no resueltas para el usuario.
     *
     * @param userId Identificador del usuario.
     * @return Lista de DTOs de recomendaciones activas.
     */
    @Transactional(readOnly = true)
    public List<AIRecommendationDTO> getActiveRecommendations(Long userId) {
        return recommendationRepository.findActiveRecommendations(userId, LocalDateTime.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Filtra recomendaciones históricas por su tipología.
     *
     * @param userId Identificador del usuario.
     * @param type   Tipo de recomendación deseada.
     * @return Lista de DTOs filtrada y ordenada por fecha.
     */
    @Transactional(readOnly = true)
    public List<AIRecommendationDTO> getRecommendationsByType(Long userId, AIRecommendation.RecommendationType type) {
        return recommendationRepository.findByUserIdAndRecommendationTypeOrderByCreatedAtDesc(userId, type).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene recomendaciones de alta prioridad que requieren atención inmediata.
     *
     * @param userId      Identificador del usuario.
     * @param minPriority Nivel mínimo de prioridad para incluir en la lista.
     * @return Lista de sugerencias críticas.
     */
    @Transactional(readOnly = true)
    public List<AIRecommendationDTO> getHighPriorityRecommendations(Long userId, Integer minPriority) {
        return recommendationRepository.findHighPriorityRecommendations(userId, minPriority).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Consolida métricas de rendimiento sobre la interacción del usuario con el módulo de IA.
     *
     * @param userId Identificador del usuario.
     * @return Mapa conteniendo claves como "totalApplied", "totalSavings" y "typeStatistics".
     */
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