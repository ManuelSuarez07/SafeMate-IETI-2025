package savemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import savemate.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Componente de acceso a datos (Repository) responsable de la gestión y análisis del historial transaccional.
 * <p>
 * Además de las operaciones CRUD estándar, esta interfaz actúa como un motor de reportes ligero.
 * Implementa consultas agregadas optimizadas para:
 * <ul>
 * <li>Calcular balances de flujo de caja (ingresos vs. gastos) en rangos de fecha específicos.</li>
 * <li>Determinar el volumen total de ahorro generado por redondeos o automatización.</li>
 * <li>Identificar patrones de consumo (top comercios) para la inteligencia de negocios.</li>
 * </ul>
 * </p>
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Recupera el historial completo de transacciones de un usuario, ordenado cronológicamente inverso
     * (de lo más reciente a lo más antiguo).
     *
     * @param userId Identificador único del usuario.
     * @return Lista completa de movimientos financieros.
     */
    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    /**
     * Filtra el historial transaccional basándose en la naturaleza del movimiento.
     * Útil para vistas segregadas, como "Solo Gastos" o "Solo Ingresos".
     *
     * @param userId          Identificador del usuario.
     * @param transactionType Tipo de transacción a consultar (ej. {@code EXPENSE}, {@code INCOME}).
     * @return Lista filtrada y ordenada por fecha descendente.
     */
    List<Transaction> findByUserIdAndTransactionTypeOrderByTransactionDateDesc(Long userId, Transaction.TransactionType transactionType);

    /**
     * Obtiene transacciones filtradas por su estado de ciclo de vida.
     * Permite aislar transacciones fallidas, canceladas o completadas.
     *
     * @param userId Identificador del usuario.
     * @param status Estado de procesamiento requerido.
     * @return Lista de transacciones que coinciden con el estado, ordenadas por fecha.
     */
    List<Transaction> findByUserIdAndStatusOrderByTransactionDateDesc(Long userId, Transaction.TransactionStatus status);

    /**
     * Recupera los movimientos ocurridos dentro de una ventana de tiempo específica (Rango de Fechas).
     * Fundamental para la generación de estados de cuenta mensuales o semanales.
     *
     * @param userId    Identificador del usuario.
     * @param startDate Fecha y hora de inicio del periodo (inclusive).
     * @param endDate   Fecha y hora de fin del periodo (inclusive).
     * @return Lista de transacciones dentro del rango, ordenadas de la más reciente a la más antigua.
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndDateRange(@Param("userId") Long userId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Calcula la suma monetaria total de un tipo específico de transacción en un periodo dado.
     * <p>
     * Realiza la agregación directamente en la base de datos para optimizar el rendimiento.
     * Ideal para calcular "Total Gastado este mes" o "Total Ingresos este año".
     * </p>
     *
     * @param userId    Identificador del usuario.
     * @param type      Tipo de transacción a sumar (ej. {@code EXPENSE}).
     * @param startDate Inicio del periodo.
     * @param endDate   Fin del periodo.
     * @return Suma total (Double) de los montos. Puede retornar {@code null} si no existen registros.
     */
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    Double sumTransactionsByTypeAndDateRange(@Param("userId") Long userId,
                                             @Param("type") Transaction.TransactionType type,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Agrega exclusivamente los montos que han sido destinados al ahorro (campo {@code savingAmount}).
     * <p>
     * Esta consulta ignora el monto principal de la transacción y suma solo la porción de "micro-ahorro"
     * o redondeo generado en el rango de fechas.
     * </p>
     *
     * @param userId    Identificador del usuario.
     * @param startDate Inicio del periodo.
     * @param endDate   Fin del periodo.
     * @return Suma total (Double) del capital ahorrado. Retorna {@code null} si no hay datos.
     */
    @Query("SELECT SUM(t.savingAmount) FROM Transaction t WHERE t.user.id = :userId AND t.savingAmount IS NOT NULL AND t.transactionDate BETWEEN :startDate AND :endDate")
    Double sumSavingsByDateRange(@Param("userId") Long userId,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Contabiliza la frecuencia o volumen de operaciones de un tipo específico en un periodo.
     * Métrica útil para analizar el comportamiento del usuario (ej. "¿Cuántas veces usó su tarjeta este mes?").
     *
     * @param userId    Identificador del usuario.
     * @param type      Tipo de transacción a contar.
     * @param startDate Inicio del periodo.
     * @param endDate   Fin del periodo.
     * @return Número entero (Long) con la cantidad de registros encontrados.
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId AND t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    Long countTransactionsByTypeAndDateRange(@Param("userId") Long userId,
                                             @Param("type") Transaction.TransactionType type,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Genera un reporte de inteligencia de gastos agrupado por comercio (Merchant).
     * <p>
     * Identifica en qué lugares gasta más dinero el usuario. Filtra solo transacciones de tipo {@code EXPENSE}.
     * El resultado se ordena descendente por el monto total gastado.
     * </p>
     *
     * @param userId    Identificador del usuario.
     * @param startDate Inicio del periodo de análisis.
     * @param endDate   Fin del periodo de análisis.
     * @return Lista de arreglos de objetos (Proyección), donde cada elemento contiene:
     * [0]: Nombre del comercio (String).
     * [1]: Cantidad de compras realizadas (Long).
     * [2]: Monto total gastado en ese comercio (Double).
     */
    @Query("SELECT t.merchantName, COUNT(t), SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.transactionType = 'EXPENSE' AND t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.merchantName ORDER BY SUM(t.amount) DESC")
    List<Object[]> findTopSpendingCategories(@Param("userId") Long userId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Recupera la cola de transacciones pendientes de procesamiento o conciliación.
     * Se ordenan por fecha de creación ascendente (FIFO - First In, First Out) para asegurar
     * que las transacciones más antiguas se procesen primero.
     *
     * @param userId Identificador del usuario.
     * @return Lista de transacciones con estado {@code PENDING}.
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.status = 'PENDING' ORDER BY t.createdAt ASC")
    List<Transaction> findPendingTransactions(@Param("userId") Long userId);
}