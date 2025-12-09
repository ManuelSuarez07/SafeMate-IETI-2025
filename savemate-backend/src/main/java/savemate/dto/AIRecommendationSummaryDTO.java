package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Clase de Transferencia de Datos (DTO) responsable de encapsular el estado consolidado de las
 * recomendaciones generadas por el motor de Inteligencia Artificial.
 * <p>
 * Este componente actúa como un contenedor de transporte entre la capa de negocio y la capa de presentación,
 * agregando tanto la lista detallada de recomendaciones vigentes como las métricas estadísticas calculadas
 * (ahorros totales, conteo histórico) necesarias para el tablero de control del usuario.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIRecommendationSummaryDTO {

    private List<AIRecommendationDTO> activeRecommendations;
    private Map<String, Object> statistics;
    private Integer activeCount;
    private Long totalApplied;
    private Double totalSavings;

    /**
     * Inicializa una nueva instancia del DTO procesando y calculando métricas derivadas a partir de los datos crudos.
     * <p>
     * Este constructor aplica lógica de negocio para la transformación de datos, asegurando la integridad
     * de los campos numéricos mediante validaciones de nulidad (null-safety). Extrae y convierte los valores
     * "totalApplied" y "totalSavings" del mapa de estadísticas para exponerlos como propiedades tipadas.
     * </p>
     *
     * @param activeRecommendations Lista de objetos {@link AIRecommendationDTO} que representan las sugerencias activas.
     * Si el valor es {@code null}, el contador de activos se establece en 0.
     * @param statistics            Mapa de metadatos estadísticos proveniente del servicio de IA o base de datos.
     * Se espera que contenga las claves "totalApplied" y "totalSavings".
     * Los valores faltantes o nulos se tratan como 0.
     */
    public AIRecommendationSummaryDTO(List<AIRecommendationDTO> activeRecommendations, Map<String, Object> statistics) {
        this.activeRecommendations = activeRecommendations;
        this.statistics = statistics;
        this.activeCount = activeRecommendations != null ? activeRecommendations.size() : 0;
        this.totalApplied = statistics != null && statistics.get("totalApplied") != null ?
                (Long) statistics.get("totalApplied") : 0L;
        this.totalSavings = statistics != null && statistics.get("totalSavings") != null ?
                (Double) statistics.get("totalSavings") : 0.0;
    }
}