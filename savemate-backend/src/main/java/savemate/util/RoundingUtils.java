package savemate.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Componente utilitario de cálculo financiero especializado en operaciones de redondeo y proyección de ahorros.
 * <p>
 * Esta clase encapsula la lógica aritmética necesaria para implementar las estrategias de "Micro-Ahorro" (Spare Change).
 * Utiliza internamente {@link BigDecimal} para garantizar la precisión decimal y evitar errores de coma flotante
 * inherentes a los tipos primitivos cuando se maneja dinero.
 * </p>
 * <p>
 * Sus responsabilidades incluyen:
 * <ul>
 * <li>Cálculo de techos y pisos de redondeo basados en múltiplos configurables (ej. redondear a la siguiente centena o mil).</li>
 * <li>Determinación de deltas de ahorro (diferencia entre monto original y redondeado).</li>
 * <li>Simulación de escenarios y proyección de impacto financiero.</li>
 * <li>Validaciones heurísticas para asegurar la razonabilidad de los montos a debitar.</li>
 * </ul>
 * </p>
 */
@Component
@Slf4j
public class RoundingUtils {

    /**
     * Calcula el redondeo de un monto monetario hacia arriba (Techo) ajustándose al múltiplo base proporcionado.
     * <p>
     * Implementa la lógica principal del ahorro por redondeo ("Spare Change"). Por ejemplo, si el monto es
     * $4,200 y el múltiplo es 1,000, el resultado será $5,000.
     * </p>
     *
     * @param amount   Monto base de la transacción original.
     * @param multiple Base numérica para el redondeo (ej. 100, 1000, 5000). Debe ser mayor a 0.
     * @return El nuevo monto redondeado. Retorna el {@code amount} original si los parámetros son nulos o inválidos.
     */
    public Double roundUpToMultiple(Double amount, Integer multiple) {
        if (amount == null || multiple == null || multiple <= 0) {
            return amount;
        }

        try {
            BigDecimal bdAmount = BigDecimal.valueOf(amount);
            BigDecimal bdMultiple = BigDecimal.valueOf(multiple);

            // Calcular el cociente y redondear hacia arriba
            BigDecimal quotient = bdAmount.divide(bdMultiple, 0, RoundingMode.UP);
            BigDecimal rounded = quotient.multiply(bdMultiple);

            Double result = rounded.doubleValue();
            log.debug("Redondeando {} a múltiplo de {}: {}", amount, multiple, result);

            return result;
        } catch (Exception e) {
            log.error("Error redondeando monto {} a múltiplo de {}: {}", amount, multiple, e.getMessage());
            return amount;
        }
    }

    /**
     * Calcula el redondeo de un monto hacia abajo (Piso) ajustándose al múltiplo base.
     * <p>
     * Útil para estimaciones conservadoras o cálculos de base imponible.
     * Por ejemplo, si el monto es $4,800 y el múltiplo es 1,000, el resultado será $4,000.
     * </p>
     *
     * @param amount   Monto base.
     * @param multiple Base numérica para el redondeo.
     * @return El monto ajustado hacia abajo. Retorna el original ante entradas inválidas.
     */
    public Double roundDownToMultiple(Double amount, Integer multiple) {
        if (amount == null || multiple == null || multiple <= 0) {
            return amount;
        }

        try {
            BigDecimal bdAmount = BigDecimal.valueOf(amount);
            BigDecimal bdMultiple = BigDecimal.valueOf(multiple);

            // Calcular el cociente y redondear hacia abajo
            BigDecimal quotient = bdAmount.divide(bdMultiple, 0, RoundingMode.DOWN);
            BigDecimal rounded = quotient.multiply(bdMultiple);

            Double result = rounded.doubleValue();
            log.debug("Redondeando {} hacia abajo a múltiplo de {}: {}", amount, multiple, result);

            return result;
        } catch (Exception e) {
            log.error("Error redondeando hacia abajo el monto {} a múltiplo de {}: {}", amount, multiple, e.getMessage());
            return amount;
        }
    }

    /**
     * Determina el diferencial monetario exacto generado por la operación de redondeo.
     * <p>
     * Este valor representa el monto real que será transferido a la cuenta de ahorros.
     * Fórmula: {@code roundedAmount - originalAmount}.
     * </p>
     *
     * @param originalAmount Monto real de la transacción.
     * @param roundedAmount  Monto proyectado después del redondeo.
     * @return Valor positivo o cero del ahorro generado. Nunca retorna negativo.
     */
    public Double calculateSavingByRounding(Double originalAmount, Double roundedAmount) {
        if (originalAmount == null || roundedAmount == null) {
            return 0.0;
        }

        Double saving = roundedAmount - originalAmount;
        log.debug("Calculando ahorro por redondeo: {} - {} = {}", roundedAmount, originalAmount, saving);

        return Math.max(0.0, saving);
    }

    /**
     * Calcula el monto de ahorro aplicando una estrategia porcentual directa.
     *
     * @param amount     Monto base de la transacción.
     * @param percentage Porcentaje a aplicar (ej. 10.0 para 10%).
     * @return Valor monetario resultante del porcentaje aplicado.
     */
    public Double calculateSavingByPercentage(Double amount, Double percentage) {
        if (amount == null || percentage == null || percentage < 0) {
            return 0.0;
        }

        Double saving = amount * (percentage / 100.0);
        log.debug("Calculando ahorro por porcentaje: {} * {}% = {}", amount, percentage, saving);

        return Math.max(0.0, saving);
    }

    /**
     * Sugiere un múltiplo de redondeo óptimo basado en la magnitud del gasto.
     * <p>
     * Implementa una lógica escalonada para maximizar el ahorro sin generar un impacto excesivo
     * en el flujo de caja del usuario para transacciones pequeñas.
     * </p>
     *
     * @param amount Monto de la transacción a analizar.
     * @return Entero representando el múltiplo recomendado (1000, 5000, 10000, etc.).
     */
    public Integer findOptimalRoundingMultiple(Double amount) {
        if (amount == null || amount <= 0) {
            return 1000;
        }

        if (amount < 5000) {
            return 1000;
        } else if (amount < 20000) {
            return 5000;
        } else if (amount < 50000) {
            return 10000;
        } else {
            return 20000;
        }
    }

    /**
     * Proyecta el ahorro total acumulado que se generaría al aplicar una regla de redondeo
     * sobre un conjunto histórico de transacciones.
     *
     * @param transactions Lista de montos de transacciones pasadas.
     * @param multiple     Múltiplo de redondeo a simular.
     * @return Suma total estimada del ahorro potencial.
     */
    public Double calculateRoundingImpact(java.util.List<Double> transactions, Integer multiple) {
        if (transactions == null || transactions.isEmpty() || multiple == null) {
            return 0.0;
        }

        Double totalSaving = 0.0;

        for (Double amount : transactions) {
            if (amount != null && amount > 0) {
                Double rounded = roundUpToMultiple(amount, multiple);
                Double saving = calculateSavingByRounding(amount, rounded);
                totalSaving += saving;
            }
        }

        log.debug("Impacto total de redondeo para {} transacciones: {}", transactions.size(), totalSaving);
        return totalSaving;
    }

    /**
     * Ejecuta múltiples estrategias de ahorro en paralelo sobre un mismo monto para fines comparativos.
     * <p>
     * Genera un reporte útil para interfaces de usuario donde se le permite al cliente visualizar
     * "qué pasaría si" configurara diferentes niveles de agresividad en el ahorro.
     * </p>
     *
     * @param amount Monto base para la simulación.
     * @return Mapa estructurado con los resultados de diferentes escenarios (Redondeo a 1k, 5k, 10k y Porcentaje).
     */
    public java.util.Map<String, Object> simulateRoundingScenarios(Double amount) {
        java.util.Map<String, Object> scenarios = new java.util.HashMap<>();

        if (amount == null) {
            return scenarios;
        }

        // Escenario 1: Redondeo a $1,000
        Double rounded1000 = roundUpToMultiple(amount, 1000);
        Double saving1000 = calculateSavingByRounding(amount, rounded1000);

        // Escenario 2: Redondeo a $5,000
        Double rounded5000 = roundUpToMultiple(amount, 5000);
        Double saving5000 = calculateSavingByRounding(amount, rounded5000);

        // Escenario 3: Redondeo a $10,000
        Double rounded10000 = roundUpToMultiple(amount, 10000);
        Double saving10000 = calculateSavingByRounding(amount, rounded10000);

        // Escenario 4: Porcentaje 10%
        Double saving10Percent = calculateSavingByPercentage(amount, 10.0);

        scenarios.put("originalAmount", amount);
        scenarios.put("rounding1000", java.util.Map.of(
                "rounded", rounded1000,
                "saving", saving1000
        ));
        scenarios.put("rounding5000", java.util.Map.of(
                "rounded", rounded5000,
                "saving", saving5000
        ));
        scenarios.put("rounding10000", java.util.Map.of(
                "rounded", rounded10000,
                "saving", saving10000
        ));
        scenarios.put("percentage10", java.util.Map.of(
                "saving", saving10Percent
        ));

        return scenarios;
    }

    /**
     * Aplica una regla heurística de seguridad (Sanity Check) para validar si el monto de ahorro calculado es prudente.
     * <p>
     * Previene situaciones anómalas donde el redondeo podría ser desproporcionado respecto al gasto original
     * (ej. ahorrar más del 50% del valor de la compra), lo cual podría afectar la liquidez inmediata del usuario.
     * </p>
     *
     * @param originalAmount Monto de la transacción.
     * @param savingAmount   Monto calculado para debitar como ahorro.
     * @return {@code true} si el ahorro es menor o igual al 50% del gasto original; {@code false} en caso contrario.
     */
    public boolean isSavingReasonable(Double originalAmount, Double savingAmount) {
        if (originalAmount == null || savingAmount == null) {
            return false;
        }

        // El ahorro no debe exceder el 50% del monto original
        Double maxReasonableSaving = originalAmount * 0.5;

        boolean reasonable = savingAmount <= maxReasonableSaving && savingAmount >= 0;

        if (!reasonable) {
            log.warn("Ahorro no razonable detectado: original={}, saving={}", originalAmount, savingAmount);
        }

        return reasonable;
    }
}