package savemate.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public class RoundingUtils {
    
    /**
     * Redondea un monto hacia arriba al múltiplo especificado
     * @param amount Monto a redondear
     * @param multiple Múltiplo al cual redondear
     * @return Monto redondeado
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
     * Redondea un monto hacia abajo al múltiplo especificado
     * @param amount Monto a redondear
     * @param multiple Múltiplo al cual redondear
     * @return Monto redondeado
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
     * Calcula el ahorro por redondeo
     * @param originalAmount Monto original
     * @param roundedAmount Monto redondeado
     * @return Ahorro generado
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
     * Calcula el ahorro por porcentaje
     * @param amount Monto original
     * @param percentage Porcentaje de ahorro
     * @return Ahorro generado
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
     * Encuentra el mejor múltiplo de redondeo basado en el rango del monto
     * @param amount Monto a analizar
     * @return Múltiplo recomendado
     */
    public Integer findOptimalRoundingMultiple(Double amount) {
        if (amount == null || amount <= 0) {
            return 1000; // Valor por defecto
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
     * Calcula el impacto del redondeo en un período
     * @param transactions Lista de montos de transacciones
     * @param multiple Múltiplo de redondeo
     * @return Ahorro total estimado
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
     * Simula diferentes escenarios de redondeo
     * @param amount Monto base
     * @return Mapa con diferentes escenarios
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
     * Valida si un monto de ahorro es razonable
     * @param originalAmount Monto original
     * @param savingAmount Monto de ahorro
     * @return true si es razonable
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