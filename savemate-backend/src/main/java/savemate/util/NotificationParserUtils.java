package savemate.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
public class NotificationParserUtils {
    
    // Patrones regex para diferentes bancos
    private static final Map<String, Map<String, String>> BANK_PATTERNS = new HashMap<>();
    
    static {
        // Patrones para Bancolombia
        Map<String, String> bancolombiaPatterns = new HashMap<>();
        bancolombiaPatterns.put("amount", "\\$([\\d,]+\\.?\\d*)");
        bancolombiaPatterns.put("merchant", "en ([^.]+)");
        bancolombiaPatterns.put("card", "tarjeta \\*\\*(\\d{4})");
        bancolombiaPatterns.put("reference", "No\\. (\\d+)");
        BANK_PATTERNS.put("bancolombia", bancolombiaPatterns);
        
        // Patrones para Daviplata
        Map<String, String> daviplataPatterns = new HashMap<>();
        daviplataPatterns.put("amount", "\\$([\\d,]+\\.?\\d*)");
        daviplataPatterns.put("merchant", "a ([^.]+)");
        daviplataPatterns.put("phone", "(\\d{10})");
        BANK_PATTERNS.put("daviplata", daviplataPatterns);
        
        // Patrones para Nequi
        Map<String, String> nequiPatterns = new HashMap<>();
        nequiPatterns.put("amount", "\\$([\\d,]+\\.?\\d*)");
        nequiPatterns.put("merchant", "a ([^.]+)");
        nequiPatterns.put("phone", "(\\d{10})");
        BANK_PATTERNS.put("nequi", nequiPatterns);
        
        // Patrones generales
        Map<String, String> genericPatterns = new HashMap<>();
        genericPatterns.put("amount", "\\$([\\d,]+\\.?\\d*)|([\\d,]+\\.?\\d*)\\s*pesos");
        genericPatterns.put("merchant", "en ([^.]+)|a ([^.]+)");
        genericPatterns.put("date", "(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})");
        BANK_PATTERNS.put("generic", genericPatterns);
    }
    
    /**
     * Parsea una notificación de transacción bancaria
     * @param notificationText Texto de la notificación
     * @param bankName Nombre del banco (opcional)
     * @return Mapa con los datos extraídos
     */
    public Map<String, Object> parseTransactionNotification(String notificationText, String bankName) {
        log.info("Parseando notificación del banco: {}", bankName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("originalText", notificationText);
        result.put("bankName", bankName);
        
        if (notificationText == null || notificationText.trim().isEmpty()) {
            result.put("error", "Texto de notificación vacío");
            return result;
        }
        
        try {
            // Determinar qué patrones usar
            Map<String, String> patterns = getPatternsForBank(bankName);
            
            // Extraer monto
            Double amount = extractAmount(notificationText, patterns);
            result.put("amount", amount);
            
            // Extraer comerciante
            String merchant = extractMerchant(notificationText, patterns);
            result.put("merchant", merchant);
            
            // Extraer referencia
            String reference = extractReference(notificationText, patterns);
            result.put("reference", reference);
            
            // Extraer información adicional
            extractAdditionalInfo(notificationText, result);
            
            // Determinar tipo de transacción
            String transactionType = determineTransactionType(notificationText, amount);
            result.put("transactionType", transactionType);
            
            // Generar descripción
            String description = generateDescription(result);
            result.put("description", description);
            
            result.put("success", true);
            log.info("Notificación parseada exitosamente: {}", result);
            
        } catch (Exception e) {
            log.error("Error parseando notificación: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            result.put("success", false);
        }
        
        return result;
    }
    
    /**
     * Extrae el monto de la notificación
     */
    private Double extractAmount(String text, Map<String, String> patterns) {
        String amountPattern = patterns.get("amount");
        if (amountPattern == null) {
            amountPattern = "\\$([\\d,]+\\.?\\d*)";
        }
        
        Pattern pattern = Pattern.compile(amountPattern);
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            String amountStr = matcher.group(1);
            if (amountStr == null) {
                amountStr = matcher.group(2); // Para patrones alternativos
            }
            
            try {
                // Limpiar el string y convertir
                amountStr = amountStr.replace(",", "").replace("$", "").trim();
                return Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                log.warn("No se pudo parsear el monto: {}", amountStr);
            }
        }
        
        return null;
    }
    
    /**
     * Extrae el nombre del comerciante
     */
    private String extractMerchant(String text, Map<String, String> patterns) {
        String merchantPattern = patterns.get("merchant");
        if (merchantPattern == null) {
            merchantPattern = "en ([^.]+)";
        }
        
        Pattern pattern = Pattern.compile(merchantPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            String merchant = matcher.group(1);
            if (merchant != null) {
                return merchant.trim();
            }
        }
        
        // Intentar patrones alternativos
        String[] alternativePatterns = {
            "compra en ([^.]+)",
            "pago en ([^.]+)",
            "transacción en ([^.]+)"
        };
        
        for (String altPattern : alternativePatterns) {
            pattern = Pattern.compile(altPattern, Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(text);
            
            if (matcher.find()) {
                String merchant = matcher.group(1);
                if (merchant != null) {
                    return merchant.trim();
                }
            }
        }
        
        return "Comercio no identificado";
    }
    
    /**
     * Extrae el número de referencia
     */
    private String extractReference(String text, Map<String, String> patterns) {
        String referencePattern = patterns.get("reference");
        if (referencePattern == null) {
            referencePattern = "No\\. (\\d+)|ref (\\d+)|referencia (\\d+)";
        }
        
        Pattern pattern = Pattern.compile(referencePattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String reference = matcher.group(i);
                if (reference != null) {
                    return reference.trim();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extrae información adicional de la notificación
     */
    private void extractAdditionalInfo(String text, Map<String, Object> result) {
        // Extraer fecha si está presente
        extractDate(text, result);
        
        // Extraer tipo de tarjeta
        extractCardInfo(text, result);
        
        // Extraer número de teléfono (para apps de pago)
        extractPhoneInfo(text, result);
        
        // Detectar si es recarga o retiro
        detectSpecialTransactionTypes(text, result);
    }
    
    /**
     * Extrae fecha del texto
     */
    private void extractDate(String text, Map<String, Object> result) {
        String[] datePatterns = {
            "(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})",
            "(\\d{2,4})[/-](\\d{1,2})[/-](\\d{1,2})"
        };
        
        for (String datePattern : datePatterns) {
            Pattern pattern = Pattern.compile(datePattern);
            Matcher matcher = pattern.matcher(text);
            
            if (matcher.find()) {
                try {
                    String dateStr = matcher.group(0);
                    result.put("dateString", dateStr);
                    // Aquí se podría parsear a LocalDateTime si se necesita
                    break;
                } catch (Exception e) {
                    log.debug("No se pudo parsear fecha: {}", matcher.group(0));
                }
            }
        }
    }
    
    /**
     * Extrae información de tarjeta
     */
    private void extractCardInfo(String text, Map<String, Object> result) {
        Pattern pattern = Pattern.compile("tarjeta \\*\\*(\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            result.put("cardLast4", matcher.group(1));
        }
    }
    
    /**
     * Extrae información de teléfono
     */
    private void extractPhoneInfo(String text, Map<String, Object> result) {
        Pattern pattern = Pattern.compile("(\\d{10})");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            result.put("phoneNumber", matcher.group(1));
        }
    }
    
    /**
     * Detecta tipos especiales de transacción
     */
    private void detectSpecialTransactionTypes(String text, Map<String, Object> result) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("recarga") || lowerText.contains("top up")) {
            result.put("specialType", "RECARGA");
        } else if (lowerText.contains("retiro") || lowerText.contains("withdrawal")) {
            result.put("specialType", "RETIRO");
        } else if (lowerText.contains("transferencia") || lowerText.contains("transfer")) {
            result.put("specialType", "TRANSFERENCIA");
        } else if (lowerText.contains("depósito") || lowerText.contains("deposit")) {
            result.put("specialType", "DEPOSITO");
        }
    }
    
    /**
     * Determina el tipo de transacción basado en el texto
     */
    private String determineTransactionType(String text, Double amount) {
        if (amount == null) {
            return "UNKNOWN";
        }
        
        String lowerText = text.toLowerCase();
        
        // Palabras clave para gastos
        String[] expenseKeywords = {
            "compra", "pago", "consumo", "débito", "debito", "gasto", "compra en"
        };
        
        // Palabras clave para ingresos
        String[] incomeKeywords = {
            "abono", "crédito", "credito", "deposito", "depósito", "recibiste", "recibido"
        };
        
        for (String keyword : expenseKeywords) {
            if (lowerText.contains(keyword)) {
                return "EXPENSE";
            }
        }
        
        for (String keyword : incomeKeywords) {
            if (lowerText.contains(keyword)) {
                return "INCOME";
            }
        }
        
        // Si no hay palabras clave claras, asumir que es un gasto (más común)
        return "EXPENSE";
    }
    
    /**
     * Genera una descripción amigable para la transacción
     */
    private String generateDescription(Map<String, Object> parsedData) {
        StringBuilder description = new StringBuilder();
        
        String merchant = (String) parsedData.get("merchant");
        String specialType = (String) parsedData.get("specialType");
        Double amount = (Double) parsedData.get("amount");
        
        if (specialType != null) {
            description.append(specialType);
        } else if (merchant != null && !merchant.equals("Comercio no identificado")) {
            description.append("Compra en ").append(merchant);
        } else {
            description.append("Transacción bancaria");
        }
        
        if (amount != null) {
            description.append(" por $").append(String.format("%.2f", amount));
        }
        
        return description.toString();
    }
    
    /**
     * Obtiene los patrones para un banco específico
     */
    private Map<String, String> getPatternsForBank(String bankName) {
        if (bankName == null || bankName.trim().isEmpty()) {
            return BANK_PATTERNS.get("generic");
        }
        
        String normalizedBankName = bankName.toLowerCase().trim();
        
        // Buscar coincidencias exactas
        if (BANK_PATTERNS.containsKey(normalizedBankName)) {
            return BANK_PATTERNS.get(normalizedBankName);
        }
        
        // Buscar coincidencias parciales
        for (String bank : BANK_PATTERNS.keySet()) {
            if (normalizedBankName.contains(bank) || bank.contains(normalizedBankName)) {
                return BANK_PATTERNS.get(bank);
            }
        }
        
        // Usar patrones genéricos por defecto
        return BANK_PATTERNS.get("generic");
    }
    
    /**
     * Valida si el texto parece ser una notificación bancaria válida
     */
    public boolean isValidBankNotification(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        
        // Debe contener símbolos de moneda o palabras clave financieras
        boolean hasCurrency = lowerText.contains("$") || lowerText.contains("pesos") || lowerText.contains("cop");
        boolean hasKeywords = lowerText.contains("compra") || lowerText.contains("pago") || 
                             lowerText.contains("transacción") || lowerText.contains("banco") ||
                             lowerText.contains("tarjeta") || lowerText.contains("cuenta");
        
        return hasCurrency || hasKeywords;
    }
    
    /**
     * Detecta el banco basado en el texto de la notificación
     */
    public String detectBank(String text) {
        if (text == null) {
            return "unknown";
        }
        
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("bancolombia")) {
            return "bancolombia";
        } else if (lowerText.contains("daviplata")) {
            return "daviplata";
        } else if (lowerText.contains("nequi")) {
            return "nequi";
        } else if (lowerText.contains("bbva")) {
            return "bbva";
        } else if (lowerText.contains("banco de bogotá")) {
            return "bancodebogota";
        } else if (lowerText.contains("aval")) {
            return "aval";
        } else {
            return "generic";
        }
    }
}