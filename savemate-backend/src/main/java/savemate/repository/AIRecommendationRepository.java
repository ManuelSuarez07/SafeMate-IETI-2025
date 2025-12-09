package savemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import savemate.model.AIRecommendation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Componente de acceso a datos (Repository) encargado de la persistencia y recuperación de las
 * recomendaciones generadas por el motor de Inteligencia Artificial.
 * <p>
 * Extiende de {@link JpaRepository} para proporcionar operaciones CRUD estándar, e implementa
 * consultas JPQL personalizadas para soportar la lógica de negocio compleja, como el filtrado
 * de recomendaciones vigentes, detección de caducidad, cálculo de métricas agregadas de ahorro
 * y análisis estadístico de la efectividad del motor de IA.
 * </p>
 */
@Repository
public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {

    /**
     * Recupera el historial completo de recomendaciones asociadas a un usuario específico.
     * Los resultados se ordenan cronológicamente de forma descendente (las más recientes primero).
     *
     * @param userId Identificador único del usuario.
     * @return Lista completa de recomendaciones históricas del usuario.
     */
    List<AIRecommendation> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Busca recomendaciones filtrando por su estado actual en el ciclo de vida.
     * Útil para obtener vistas específicas como "Historial de aplicadas" o "Papelera de rechazadas".
     *
     * @param userId Identificador único del usuario.
     * @param status Estado enumerado por el cual filtrar (ej. {@code APPLIED}, {@code REJECTED}).
     * @return Lista de recomendaciones que coinciden con el estado y usuario solicitados.
     */
    List<AIRecommendation> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, AIRecommendation.RecommendationStatus status);

    /**
     * Obtiene sugerencias basadas en su tipología o naturaleza de optimización.
     * Permite segmentar estrategias, por ejemplo, aislar solo las sugerencias de "Configuración de Redondeo".
     *
     * @param userId             Identificador único del usuario.
     * @param recommendationType Tipo de recomendación a consultar.
     * @return Lista de recomendaciones clasificadas por el tipo especificado.
     */
    List<AIRecommendation> findByUserIdAndRecommendationTypeOrderByCreatedAtDesc(Long userId, AIRecommendation.RecommendationType recommendationType);

    /**
     * Recupera las recomendaciones que son actualmente "accionables" para el usuario.
     * <p>
     * La consulta aplica una lógica compuesta:
     * 1. Pertenecen al usuario indicado.
     * 2. Su estado es {@code PENDING} (no han sido ni aceptadas ni rechazadas).
     * 3. Están vigentes temporalmente (fecha de expiración nula o fecha de expiración futura respecto a {@code now}).
     * </p>
     * Los resultados se priorizan primero por nivel de importancia ({@code priorityLevel}) y luego por fecha de creación.
     *
     * @param userId Identificador único del usuario.
     * @param now    Fecha y hora actual de referencia para evaluar la expiración.
     * @return Lista priorizada de recomendaciones activas listas para ser mostradas en el feed principal.
     */
    @Query("SELECT ar FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.status = 'PENDING' AND (ar.expiresAt IS NULL OR ar.expiresAt > :now) ORDER BY ar.priorityLevel DESC, ar.createdAt DESC")
    List<AIRecommendation> findActiveRecommendations(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Identifica recomendaciones cuya fecha de validez ha caducado pero cuyo estado interno
     * aún no refleja la condición de {@code EXPIRED}.
     * Utilizado habitualmente por procesos en segundo plano (Cron Jobs) para mantenimiento de la base de datos.
     *
     * @param userId Identificador del usuario.
     * @param now    Fecha y hora actual contra la cual comparar la fecha de expiración.
     * @return Lista de recomendaciones técnicamente vencidas que requieren actualización de estado.
     */
    @Query("SELECT ar FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.expiresAt <= :now AND ar.status != 'EXPIRED'")
    List<AIRecommendation> findExpiredRecommendations(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Contabiliza el número total de recomendaciones que el usuario ha aceptado y aplicado exitosamente.
     * Métrica clave para determinar el nivel de interacción (engagement) del usuario con la IA.
     *
     * @param userId Identificador único del usuario.
     * @return Número entero (Long) que representa la cantidad de recomendaciones aplicadas.
     */
    @Query("SELECT COUNT(ar) FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.isApplied = true")
    Long countAppliedRecommendations(@Param("userId") Long userId);

    /**
     * Calcula la suma total del ahorro potencial proyectado de todas las recomendaciones aplicadas.
     * Métrica utilizada para mostrar al usuario cuánto dinero ha ahorrado gracias al asistente inteligente.
     *
     * @param userId Identificador único del usuario.
     * @return Valor monetario total (Double) de los ahorros potenciales aceptados. Puede ser nulo si no hay registros.
     */
    @Query("SELECT SUM(ar.potentialSavings) FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.isApplied = true AND ar.potentialSavings IS NOT NULL")
    Double sumAppliedRecommendationsSavings(@Param("userId") Long userId);

    /**
     * Genera un reporte estadístico agrupado por tipo de recomendación para analizar el comportamiento del sistema.
     *
     * @param userId Identificador del usuario.
     * @return Una lista de arreglos de objetos (Tuplas), donde cada arreglo contiene:
     * [0]: Tipo de recomendación (Enum).
     * [1]: Cantidad total de recomendaciones de ese tipo (Count).
     * [2]: Promedio del puntaje de confianza (Confidence Score) de ese tipo.
     */
    @Query("SELECT ar.recommendationType, COUNT(ar), AVG(ar.confidenceScore) FROM AIRecommendation ar WHERE ar.user.id = :userId GROUP BY ar.recommendationType ORDER BY COUNT(ar) DESC")
    List<Object[]> getRecommendationStatistics(@Param("userId") Long userId);

    /**
     * Filtra las recomendaciones por una etiqueta de categoría textual arbitraria.
     *
     * @param userId   Identificador del usuario.
     * @param category Cadena de texto exacta de la categoría a buscar.
     * @return Lista de recomendaciones que coinciden con la categoría.
     */
    @Query("SELECT ar FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.category = :category ORDER BY ar.createdAt DESC")
    List<AIRecommendation> findByCategory(@Param("userId") Long userId, @Param("category") String category);

    /**
     * Selecciona recomendaciones que superan un umbral de prioridad específico.
     * Utilizado para alertas críticas o notificaciones push de oportunidades de alto valor.
     *
     * @param userId      Identificador del usuario.
     * @param minPriority Nivel mínimo de prioridad (inclusivo) para ser considerado en la búsqueda.
     * @return Lista de recomendaciones de alta prioridad ordenadas descendentemente.
     */
    @Query("SELECT ar FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.priorityLevel >= :minPriority ORDER BY ar.priorityLevel DESC, ar.createdAt DESC")
    List<AIRecommendation> findHighPriorityRecommendations(@Param("userId") Long userId, @Param("minPriority") Integer minPriority);

    /**
     * Ejecuta una operación de limpieza masiva eliminando físicamente registros de recomendaciones
     * que han expirado y cuyo estado ya ha sido marcado como {@code EXPIRED}.
     * <p>
     * <strong>Precaución:</strong> Esta operación es destructiva e irreversible.
     * </p>
     *
     * @param userId Identificador del usuario.
     * @param now    Fecha de corte; se eliminarán los registros expirados antes o en este momento.
     */
    @Query("DELETE FROM AIRecommendation ar WHERE ar.user.id = :userId AND ar.expiresAt <= :now AND ar.status = 'EXPIRED'")
    void deleteExpiredRecommendations(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}