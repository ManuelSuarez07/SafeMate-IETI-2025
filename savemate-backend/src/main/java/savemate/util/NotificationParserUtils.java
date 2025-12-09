package savemate.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Componente utilitario encargado del análisis sintáctico (parsing) y extracción estructurada de información
 * a partir de notificaciones bancarias de texto no estructurado (SMS, Push Notifications).
 * <p>
 * Esta clase implementa un motor de reglas basado en Expresiones Regulares (Regex) adaptables por entidad bancaria.
 * Su responsabilidad principal es transformar mensajes de texto crudos en mapas de datos clave-valor
 * normalizados (Monto, Comercio, Fecha, Tipo) para su posterior procesamiento en el sistema de transacciones.
 * Incluye soporte específico para formatos de bancos locales (Bancolombia, Daviplata, Nequi) y un mecanismo de
 * "fallback" genérico.
 * </p>
 */
@Component
@Slf4j
public class NotificationParserUtils {

    // Se omiten atributos estáticos según instrucciones

    /**
     * Orquesta el proceso completo de interpretación de una notificación bancaria.
     * <p>
     * Este método selecciona la estrategia de patrones adecuada según el banco, extrae los datos core
     * (monto, comercio, referencia), enriquece la información con metadatos adicionales y
     * clasifica la naturaleza de la transacción (Ingreso/Gasto).
     * </p>
     *
     * @param notificationText El cuerpo del mensaje de texto de la notificación a procesar.
     * @param bankName         Nombre de la entidad bancaria para optimizar la selección de patrones (opcional).
     * Si es nulo o desconocido, se utilizan patrones genéricos.
     * @return Un {@link Map} mutable que contiene los datos extraídos. Las claves estándar incluyen:
     * "amount" (Double), "merchant" (String), "transactionType" (String), "success" (Boolean).
     * En caso de fallo, incluye la clave "error".
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
     * Extrae el valor monetario de la transacción aplicando limpieza de formato.
     * Elimina símbolos de moneda y separadores de miles para obtener un valor numérico puro.
     *
     * @param text     Texto completo de la notificación.
     * @param patterns Mapa de patrones regex configurado para el banco específico.
     * @return El monto como {@code Double}, o {@code null} si no se encuentra un patrón coincidente.
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
                amountStr = matcher.group(2);
            }

            try {
                amountStr = amountStr.replace(",", "").replace("$", "").trim();
                return Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                log.warn("No se pudo parsear el monto: {}", amountStr);
            }
        }

        return null;
    }

    /**
     * Identifica y extrae el nombre del establecimiento comercial o destinatario.
     * <p>
     * Utiliza una estrategia de múltiples intentos: primero busca patrones específicos del banco,
     * y si falla, itera sobre una lista de patrones semánticos comunes (ej. "compra en...", "pago a...").
     * </p>
     *
     * @param text     Texto completo de la notificación.
     * @param patterns Mapa de patrones regex.
     * @return El nombre del comercio extraído o "Comercio no identificado" como valor por defecto.
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
     * Busca el código de referencia único o número de autorización en el texto.
     *
     * @param text     Texto completo de la notificación.
     * @param patterns Mapa de patrones regex.
     * @return La referencia como cadena de texto o {@code null} si no está presente.
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
     * Método auxiliar que agrupa la extracción de metadatos secundarios.
     * Delega en métodos específicos para fecha, tarjeta, teléfono y tipos especiales.
     *
     * @param text   Texto de la notificación.
     * @param result Mapa de resultados donde se inyectarán los datos encontrados.
     */
    private void extractAdditionalInfo(String text, Map<String, Object> result) {
        // Extraer fecha si está presente
        extractDate(text, result);

        // Extraer tipo de tarjeta
        extractCardInfo(text, result);

        // Extraer número de teléfono
        extractPhoneInfo(text, result);

        // Detectar si es recarga o retiro
        detectSpecialTransactionTypes(text, result);
    }

    /**
     * Intenta identificar patrones de fecha dentro del texto para registrar el momento de la operación.
     * Soporta formatos comunes como DD/MM/YYYY o YYYY/MM/DD con separadores de barra o guion.
     *
     * @param text   Texto de la notificación.
     * @param result Mapa de resultados.
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
                    break;
                } catch (Exception e) {
                    log.debug("No se pudo parsear fecha: {}", matcher.group(0));
                }
            }
        }
    }

    /**
     * Extrae los últimos 4 dígitos de la tarjeta si están presentes en la notificación enmascarada.
     * Útil para identificar el instrumento de pago utilizado.
     *
     * @param text   Texto de la notificación.
     * @param result Mapa de resultados.
     */
    private void extractCardInfo(String text, Map<String, Object> result) {
        Pattern pattern = Pattern.compile("tarjeta \\*\\*(\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            result.put("cardLast4", matcher.group(1));
        }
    }

    /**
     * Busca números de teléfono de 10 dígitos, común en billeteras digitales (Nequi, Daviplata).
     *
     * @param text   Texto de la notificación.
     * @param result Mapa de resultados.
     */
    private void extractPhoneInfo(String text, Map<String, Object> result) {
        Pattern pattern = Pattern.compile("(\\d{10})");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            result.put("phoneNumber", matcher.group(1));
        }
    }

    /**
     * Analiza el texto en busca de palabras clave que denoten operaciones especiales.
     * Identifica: Recargas, Retiros, Transferencias y Depósitos.
     *
     * @param text   Texto de la notificación.
     * @param result Mapa de resultados.
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
     * Clasifica la transacción como GASTO (EXPENSE) o INGRESO (INCOME) basándose en un análisis semántico.
     * Evalúa la presencia de palabras clave positivas (abono, recibir) vs negativas (compra, pago).
     *
     * @param text   Texto de la notificación.
     * @param amount Monto extraído previamente (necesario para validar integridad).
     * @return "EXPENSE", "INCOME" o "UNKNOWN".
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

        // Si no hay palabras clave claras, asumir que es un gasto
        return "EXPENSE";
    }

    /**
     * Construye una descripción legible y amigable para el usuario final combinando los datos extraídos.
     *
     * @param parsedData Mapa con los datos ya procesados.
     * @return Cadena formateada (ej. "Compra en Starbucks por $12,500.00").
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
     * Selecciona el conjunto de patrones regex adecuado según el banco detectado.
     * Implementa lógica de fallback a patrones genéricos si el banco no tiene reglas específicas definidas.
     *
     * @param bankName Nombre de la entidad bancaria.
     * @return Mapa de expresiones regulares.
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
     * Realiza una validación preliminar para determinar si un texto tiene la estructura de una notificación financiera.
     * Verifica la presencia de símbolos de moneda, palabras clave financieras o códigos.
     *
     * @param text Texto a validar.
     * @return {@code true} si parece ser una notificación válida; {@code false} en caso contrario.
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
     * Aplica heurística simple sobre el texto para intentar adivinar la entidad bancaria emisora.
     *
     * @param text Texto de la notificación.
     * @return Identificador normalizado del banco (ej. "bancolombia", "nequi") o "generic" si no se detecta.
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