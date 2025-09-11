package safemate.util;

/**
 * Utilidad con métodos para calcular redondeos y porcentajes.
 */
public class RoundingUtils {
    /**
     * Redondea amountCents al siguiente múltiplo (por ejemplo 1000 = $10.00)
     */
    public static long roundUpTo(long amountCents, long multipleCents) {
        if (multipleCents <= 0) return amountCents;
        long remainder = amountCents % multipleCents;
        if (remainder == 0) return amountCents;
        return amountCents + (multipleCents - remainder);
    }

    public static long percentOf(long amountCents, double percent) {
        return Math.round(amountCents * percent / 100.0);
    }
}