package savemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import savemate.model.User;

import java.util.Optional;

/**
 * Componente de acceso a datos (Repository) encargado de la gestión de identidades de usuario y
 * métricas globales de la plataforma.
 * <p>
 * Esta interfaz es fundamental para el módulo de seguridad, proporcionando los mecanismos de búsqueda
 * necesarios para la autenticación (Login) y la validación de unicidad durante el registro.
 * Adicionalmente, expone consultas analíticas de alto nivel para monitorear el crecimiento de la
 * base de usuarios y el volumen total de capital gestionado por la aplicación.
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Recupera una entidad de usuario basándose en su dirección de correo electrónico.
     * <p>
     * Este método es la piedra angular del proceso de autenticación (ej. en {@code UserDetailsService}),
     * permitiendo cargar las credenciales y roles del usuario a partir de su identificador principal.
     * </p>
     *
     * @param email Dirección de correo electrónico a buscar. Debe ser una coincidencia exacta.
     * @return Un {@link Optional} que contiene el usuario si existe, o vacío si no se encuentra.
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica de manera eficiente si una dirección de correo electrónico ya está registrada en el sistema.
     * <p>
     * Utilizado principalmente en los validadores de registro (Sign Up) para prevenir la duplicidad de cuentas
     * antes de intentar una operación de persistencia.
     * </p>
     *
     * @param email Dirección de correo electrónico a verificar.
     * @return {@code true} si el correo ya está asociado a una cuenta; {@code false} en caso contrario.
     */
    boolean existsByEmail(String email);

    /**
     * Realiza una búsqueda inversa de usuario utilizando su número de teléfono móvil.
     * Útil para funcionalidades de recuperación de cuenta, autenticación de dos factores (2FA)
     * o búsqueda de contactos.
     *
     * @param phoneNumber Número de teléfono normalizado.
     * @return Un {@link Optional} con el usuario asociado al teléfono, si existe.
     */
    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber")
    Optional<User> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * Busca un usuario asociado a un número de cuenta bancaria o CLABE específica.
     * <p>
     * Esta consulta es crítica para garantizar que una misma cuenta bancaria no sea vinculada
     * fraudulentamente a múltiples perfiles de usuario diferentes.
     * </p>
     *
     * @param bankAccount Identificador de la cuenta bancaria.
     * @return Un {@link Optional} con el usuario propietario de la cuenta bancaria.
     */
    @Query("SELECT u FROM User u WHERE u.bankAccount = :bankAccount")
    Optional<User> findByBankAccount(@Param("bankAccount") String bankAccount);

    /**
     * Calcula la métrica de adquisición de usuarios a partir de una fecha determinada.
     * Utilizado para reportes de crecimiento y efectividad de campañas de marketing.
     *
     * @param startDate Fecha de corte desde la cual contar los nuevos registros.
     * @return Número total (Long) de usuarios registrados en o después de la fecha indicada.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    Long countUsersCreatedAfter(@Param("startDate") java.time.LocalDateTime startDate);

    /**
     * Calcula la suma global del dinero ahorrado por todos los usuarios en la plataforma.
     * <p>
     * Esta métrica representa el "Valor Total Bloqueado" (TVL) o el impacto financiero total
     * generado por la aplicación.
     * </p>
     *
     * @return Suma total (Double) de los saldos ahorrados. Puede ser nulo si no hay usuarios.
     */
    @Query("SELECT SUM(u.totalSaved) FROM User u")
    Double getTotalSavingsAcrossAllUsers();

    /**
     * Obtiene el promedio de ahorro por usuario activo.
     * <p>
     * Filtra aquellos usuarios que tienen un ahorro mayor a cero para obtener una media
     * representativa de los usuarios que utilizan activamente la funcionalidad de ahorro.
     * </p>
     *
     * @return Valor promedio (Double) del ahorro.
     */
    @Query("SELECT AVG(u.totalSaved) FROM User u WHERE u.totalSaved > 0")
    Double getAverageSavingsPerUser();
}