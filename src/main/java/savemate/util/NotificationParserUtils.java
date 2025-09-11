package safemate.util;

/**
 * Esqueleto para parsear notificaciones/SMS bancarios y extraer montos, comercio, fecha.
 */
public class NotificationParserUtils {

    public static ParsedTransaction parse(String rawNotification) {
        // implementar parsing real según formatos de bancos
        return new ParsedTransaction();
    }

    public static class ParsedTransaction {
        public String merchant;
        public long amountCents;
        public String category;
    }
}