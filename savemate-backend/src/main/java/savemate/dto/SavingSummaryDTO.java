package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Objeto de Transferencia de Datos (DTO) diseñado para proporcionar una visión panorámica y consolidada
 * de la situación financiera de ahorro del usuario.
 * <p>
 * Esta clase actúa como un agregador de información, encapsulando tanto métricas cuantitativas
 * (saldos totales acumulados, contadores de estado) como los detalles granulares de las metas (listas de DTOs),
 * optimizando así la entrega de datos para la renderización de tableros de control (dashboards) y pantallas de resumen.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingSummaryDTO {

    private Double totalCurrentSavings;
    private Long activeGoalsCount;
    private Long completedGoalsCount;
    private Integer totalGoalsCount;
    private List<SavingDTO> activeGoals;
    private List<SavingDTO> allGoals;

    /**
     * Construye una instancia del resumen financiero procesando los datos crudos y calculando totales derivados.
     * <p>
     * Este constructor implementa validaciones de seguridad contra nulos (null-safety) para los campos numéricos,
     * garantizando que los contadores y montos monetarios nunca sean nulos (default a 0) para el consumo del cliente.
     * Además, deriva automáticamente el conteo total histórico de metas basándose en el tamaño de la colección proporcionada.
     * </p>
     *
     * @param totalCurrentSavings Suma monetaria total del dinero ahorrado en todas las metas existentes.
     * Si es {@code null}, se inicializa en 0.0.
     * @param activeCount         Número de metas que se encuentran actualmente en estado activo.
     * Si es {@code null}, se inicializa en 0.
     * @param completedCount      Número de metas que han alcanzado su objetivo financiero.
     * Si es {@code null}, se inicializa en 0.
     * @param allGoals            Colección completa del historial de metas del usuario (activas, completadas, pausadas, etc.).
     * Si es {@code null}, el contador total de metas se establece en 0.
     * @param activeGoals         Subconjunto filtrado que contiene únicamente las metas vigentes para acceso rápido.
     */
    public SavingSummaryDTO(Double totalCurrentSavings, Long activeCount, Long completedCount,
                            List<SavingDTO> allGoals, List<SavingDTO> activeGoals) {
        this.totalCurrentSavings = totalCurrentSavings != null ? totalCurrentSavings : 0.0;
        this.activeGoalsCount = activeCount != null ? activeCount : 0L;
        this.completedGoalsCount = completedCount != null ? completedCount : 0L;
        this.totalGoalsCount = allGoals != null ? allGoals.size() : 0;
        this.activeGoals = activeGoals;
        this.allGoals = allGoals;
    }
}