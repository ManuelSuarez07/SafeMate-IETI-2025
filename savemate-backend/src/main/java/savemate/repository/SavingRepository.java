package savemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import savemate.model.SavingGoal;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Componente de acceso a datos (Repository) encargado de la persistencia y gestión del ciclo de vida
 * de las metas de ahorro ({@link SavingGoal}).
 * <p>
 * Esta interfaz extiende las capacidades CRUD estándar de JPA con consultas estratégicas diseñadas para:
 * <ul>
 * <li>El monitoreo del progreso financiero (sumas de saldos y objetivos).</li>
 * <li>La gestión de alertas temporales (metas próximas a vencer).</li>
 * <li>La detección de inconsistencias de estado (metas completadas financieramente pero no marcadas).</li>
 * <li>La generación de métricas para el tablero de control del usuario.</li>
 * </ul>
 * </p>
 */
@Repository
public interface SavingRepository extends JpaRepository<SavingGoal, Long> {

    /**
     * Recupera la totalidad de las metas de ahorro asociadas a un usuario, ordenadas por su relevancia estratégica.
     * Útil para presentar una lista general priorizada en la interfaz principal.
     *
     * @param userId Identificador único del usuario propietario.
     * @return Lista de metas ordenadas descendentemente por nivel de prioridad.
     */
    List<SavingGoal> findByUserIdOrderByPriorityLevelDesc(Long userId);

    /**
     * Obtiene un subconjunto de metas filtradas por su estado operativo actual.
     * Permite aislar vistas como "Metas en Progreso" o "Historial de Logros".
     *
     * @param userId Identificador del usuario.
     * @param status Estado específico a consultar (ej. {@code ACTIVE}, {@code COMPLETED}).
     * @return Lista de metas que coinciden con el estado, ordenadas por prioridad.
     */
    List<SavingGoal> findByUserIdAndStatusOrderByPriorityLevelDesc(Long userId, SavingGoal.GoalStatus status);

    /**
     * Identifica las metas activas cuya fecha límite de cumplimiento es anterior o igual a una fecha dada.
     * <p>
     * Esta consulta es crítica para el sistema de notificaciones y alertas tempranas, permitiendo avisar
     * al usuario sobre objetivos que requieren atención inmediata o están en riesgo de incumplimiento.
     * </p>
     *
     * @param userId Identificador del usuario.
     * @param date   Fecha de corte para la evaluación (usualmente {@code LocalDateTime.now()}).
     * @return Lista de metas activas que vencen en o antes de la fecha especificada.
     */
    @Query("SELECT sg FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.targetDate <= :date AND sg.status = 'ACTIVE'")
    List<SavingGoal> findGoalsDueByDate(@Param("userId") Long userId, @Param("date") LocalDateTime date);

    /**
     * Detecta metas que han alcanzado su objetivo financiero pero cuyo estado no ha sido actualizado.
     * <p>
     * Utilizada habitualmente por procesos de mantenimiento o sincronización para garantizar la consistencia
     * eventual de los datos, cerrando automáticamente metas que ya cumplieron su propósito monetario.
     * </p>
     *
     * @param userId Identificador del usuario.
     * @return Lista de metas con saldo suficiente ({@code current >= target}) que aún no están en estado {@code COMPLETED}.
     */
    @Query("SELECT sg FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.currentAmount >= sg.targetAmount AND sg.status != 'COMPLETED'")
    List<SavingGoal> findCompletedGoalsNotMarked(@Param("userId") Long userId);

    /**
     * Calcula la cantidad total de metas que el usuario está persiguiendo activamente.
     * Métrica clave (KPI) para el dashboard principal.
     *
     * @param userId Identificador del usuario.
     * @return Número entero (Long) de metas en estado {@code ACTIVE}.
     */
    @Query("SELECT COUNT(sg) FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.status = 'ACTIVE'")
    Long countActiveGoals(@Param("userId") Long userId);

    /**
     * Contabiliza el número histórico de metas que el usuario ha finalizado con éxito.
     *
     * @param userId Identificador del usuario.
     * @return Número entero (Long) de metas en estado {@code COMPLETED}.
     */
    @Query("SELECT COUNT(sg) FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.status = 'COMPLETED'")
    Long countCompletedGoals(@Param("userId") Long userId);

    /**
     * Calcula la suma total del dinero proyectado necesario para cumplir todas las metas activas.
     * Representa la "Deuda de Ahorro" o el objetivo macro del usuario.
     *
     * @param userId Identificador del usuario.
     * @return Suma total (Double) de los {@code targetAmount} de las metas activas. Puede ser nulo.
     */
    @Query("SELECT SUM(sg.targetAmount) FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.status = 'ACTIVE'")
    Double sumActiveGoalsTargetAmount(@Param("userId") Long userId);

    /**
     * Agrega el capital total real acumulado por el usuario a través de todas sus metas.
     * <p>
     * Suma los saldos actuales ({@code currentAmount}) de todas las metas, independientemente de su estado,
     * proporcionando una visión del patrimonio líquido total reservado en la plataforma.
     * </p>
     *
     * @param userId Identificador del usuario.
     * @return Suma total (Double) del dinero ahorrado actualmente. Puede ser nulo.
     */
    @Query("SELECT SUM(sg.currentAmount) FROM SavingGoal sg WHERE sg.user.id = :userId")
    Double sumCurrentSavings(@Param("userId") Long userId);

    /**
     * Recupera exclusivamente las metas configuradas como grupales o colaborativas.
     *
     * @param userId Identificador del usuario.
     * @return Lista de metas colaborativas ordenadas por fecha de creación reciente.
     */
    @Query("SELECT sg FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.isCollaborative = true ORDER BY sg.createdAt DESC")
    List<SavingGoal> findCollaborativeGoals(@Param("userId") Long userId);

    /**
     * Filtra y recupera metas que superan un umbral de prioridad específico.
     * <p>
     * Los resultados se ordenan primariamente por prioridad (de mayor a menor) y secundariamente
     * por fecha de vencimiento (las más próximas primero), facilitando la toma de decisiones sobre
     * dónde asignar fondos disponibles.
     * </p>
     *
     * @param userId      Identificador del usuario.
     * @param minPriority Nivel mínimo de prioridad (inclusive) para incluir en la búsqueda.
     * @return Lista de metas de alta prioridad.
     */
    @Query("SELECT sg FROM SavingGoal sg WHERE sg.user.id = :userId AND sg.priorityLevel >= :minPriority ORDER BY sg.priorityLevel DESC, sg.targetDate ASC")
    List<SavingGoal> findHighPriorityGoals(@Param("userId") Long userId, @Param("minPriority") Integer minPriority);
}