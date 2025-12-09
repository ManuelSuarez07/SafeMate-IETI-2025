package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Objeto de Transferencia de Datos (DTO) que consolida el resumen operativo de las transacciones financieras
 * de un usuario en un periodo determinado.
 * <p>
 * Su responsabilidad es agrupar métricas clave de flujo de caja (total de gastos frente a total de ahorros)
 * junto con el detalle transaccional. Es utilizado principalmente para alimentar vistas de historial,
 * balances rápidos y reportes, centralizando la lógica de agregación y evitando cálculos repetitivos
 * en la capa de presentación.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummaryDTO {

    private Double totalExpenses;
    private Double totalSavings;
    private Integer transactionCount;
    private List<TransactionDTO> transactions;

    /**
     * Inicializa el objeto de resumen aplicando reglas de integridad de datos y cálculo de metadatos.
     * <p>
     * Este constructor implementa lógica de saneamiento (sanitization) para asegurar que los valores
     * monetarios acumulados nunca sean nulos (estableciendo 0.0 por defecto). Adicionalmente,
     * deriva automáticamente la cardinalidad del conjunto de datos calculando el tamaño de la lista
     * proporcionada.
     * </p>
     *
     * @param totalExpenses Suma acumulada de los egresos o gastos detectados.
     * Si el valor es {@code null}, se inicializa en 0.0.
     * @param totalSavings  Suma acumulada de los montos destinados a ahorro.
     * Si el valor es {@code null}, se inicializa en 0.0.
     * @param transactions  Lista detallada de objetos {@link TransactionDTO} que componen el historial.
     * Si es {@code null}, el contador de transacciones se establece en 0.
     */
    public TransactionSummaryDTO(Double totalExpenses, Double totalSavings, List<TransactionDTO> transactions) {
        this.totalExpenses = totalExpenses != null ? totalExpenses : 0.0;
        this.totalSavings = totalSavings != null ? totalSavings : 0.0;
        this.transactionCount = transactions != null ? transactions.size() : 0;
        this.transactions = transactions;
    }
}