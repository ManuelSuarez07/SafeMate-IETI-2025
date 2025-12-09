package savemate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import savemate.dto.SavingDTO;
import savemate.model.SavingGoal;
import savemate.model.User;
import savemate.repository.SavingRepository;
import savemate.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio encargado de la gestión integral del ciclo de vida de las metas de ahorro.
 * Este servicio orquesta las operaciones relacionadas con la planificación financiera a largo plazo del usuario.
 * Sus responsabilidades principales incluyen:
 * <ul>
 * <li>Creación y configuración de objetivos de ahorro (individuales o colaborativos).</li>
 * <li>Actualización de progresos y detección automática de cumplimiento de metas.</li>
 * <li>Algoritmos de distribución inteligente de fondos excedentes entre múltiples metas basadas en prioridad.</li>
 * <li>Generación de reportes y métricas de desempeño financiero (metas vencidas, totales acumulados).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SavingService {

    private final SavingRepository savingRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Registra una nueva meta de ahorro en el sistema asociada a un usuario existente.
     * Inicializa la meta con estado {@code ACTIVE} y establece valores por defecto para campos opcionales
     * como el monto actual (0.0) y el nivel de prioridad (1) si no son proporcionados.
     *
     * @param savingDTO Objeto de transferencia con la definición de la meta (nombre, monto objetivo, fecha límite).
     * @return El DTO de la meta persistida, incluyendo el ID generado y las marcas de tiempo de auditoría.
     * @throws RuntimeException Si el ID de usuario proporcionado no corresponde a un usuario registrado.
     */
    @Transactional
    public SavingDTO createSavingGoal(SavingDTO savingDTO) {
        log.info("Creando meta de ahorro para usuario ID: {}", savingDTO.getUserId());

        User user = userRepository.findById(savingDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        SavingGoal savingGoal = new SavingGoal();
        savingGoal.setUser(user);
        savingGoal.setName(savingDTO.getName());
        savingGoal.setDescription(savingDTO.getDescription());
        savingGoal.setTargetAmount(savingDTO.getTargetAmount());
        savingGoal.setCurrentAmount(savingDTO.getCurrentAmount() != null ? savingDTO.getCurrentAmount() : 0.0);
        savingGoal.setTargetDate(savingDTO.getTargetDate());
        savingGoal.setStatus(SavingGoal.GoalStatus.ACTIVE);
        savingGoal.setMonthlyContribution(savingDTO.getMonthlyContribution());
        savingGoal.setPriorityLevel(savingDTO.getPriorityLevel() != null ? savingDTO.getPriorityLevel() : 1);
        savingGoal.setIsCollaborative(savingDTO.getIsCollaborative() != null ? savingDTO.getIsCollaborative() : false);

        SavingGoal savedGoal = savingRepository.save(savingGoal);
        log.info("Meta de ahorro creada exitosamente con ID: {}", savedGoal.getId());

        return convertToDTO(savedGoal);
    }

    /**
     * Incrementa el saldo acumulado de una meta específica y verifica su finalización.
     * Este método es transaccional y atómico: actualiza el saldo de la meta y simultáneamente
     * sincroniza el total ahorrado global del perfil del usuario (vía {@link UserService}).
     * Si el nuevo saldo iguala o supera el objetivo, la meta cambia automáticamente a estado {@code COMPLETED}.
     *
     * @param goalId           Identificador único de la meta a financiar.
     * @param additionalAmount Monto monetario positivo a agregar al saldo actual.
     * @return El DTO actualizado reflejando el nuevo progreso y estado.
     * @throws RuntimeException Si la meta no existe en la base de datos.
     */
    @Transactional
    public SavingDTO updateSavingGoalProgress(Long goalId, Double additionalAmount) {
        log.info("Actualizando progreso de meta de ahorro ID: {} con monto: {}", goalId, additionalAmount);

        SavingGoal savingGoal = savingRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Meta de ahorro no encontrada"));

        savingGoal.setCurrentAmount(savingGoal.getCurrentAmount() + additionalAmount);

        // Verificar si se completó la meta
        if (savingGoal.getCurrentAmount() >= savingGoal.getTargetAmount()) {
            savingGoal.setStatus(SavingGoal.GoalStatus.COMPLETED);
            savingGoal.setCompletedAt(LocalDateTime.now());
            log.info("Meta de ahorro completada: {}", savingGoal.getName());
        }

        SavingGoal updatedGoal = savingRepository.save(savingGoal);
        userService.updateTotalSaved(savingGoal.getUser().getId(), additionalAmount);

        return convertToDTO(updatedGoal);
    }

    /**
     * Modifica manualmente el estado administrativo de una meta.
     * Útil para operaciones de pausado, cancelación o reactivación de objetivos.
     *
     * @param goalId    Identificador de la meta.
     * @param newStatus Nuevo estado a asignar (ej. {@code PAUSED}, {@code CANCELLED}).
     * @return El DTO actualizado.
     * @throws RuntimeException Si la meta no es encontrada.
     */
    @Transactional
    public SavingDTO updateSavingGoalStatus(Long goalId, SavingGoal.GoalStatus newStatus) {
        log.info("Actualizando estado de meta de ahorro ID: {} a: {}", goalId, newStatus);

        SavingGoal savingGoal = savingRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Meta de ahorro no encontrada"));

        savingGoal.setStatus(newStatus);

        if (newStatus == SavingGoal.GoalStatus.COMPLETED) {
            savingGoal.setCompletedAt(LocalDateTime.now());
        }

        SavingGoal updatedGoal = savingRepository.save(savingGoal);
        return convertToDTO(updatedGoal);
    }

    /**
     * Ejecuta un algoritmo de distribución de fondos para asignar un monto global entre múltiples metas activas.
     * La lógica de distribución prioriza las metas según su {@code priorityLevel}.
     * El cálculo intenta equilibrar el aporte pero otorga un peso adicional (multiplicador)
     * a las metas más importantes. Si una meta se completa durante la distribución,
     * el estado se actualiza y el remanente se intenta asignar a las siguientes.
     *
     * @param userId      Identificador del usuario propietario de los fondos.
     * @param totalAmount Monto total disponible para repartir (bolsa de ahorro).
     */
    @Transactional
    public void distributeSavingsToGoals(Long userId, Double totalAmount) {
        log.info("Distribuyendo ahorros de {} a metas del usuario ID: {}", totalAmount, userId);

        List<SavingGoal> activeGoals = savingRepository.findByUserIdAndStatusOrderByPriorityLevelDesc(
                userId, SavingGoal.GoalStatus.ACTIVE);

        if (activeGoals.isEmpty()) {
            log.info("No hay metas activas para distribuir ahorros");
            return;
        }

        Double remainingAmount = totalAmount;

        for (SavingGoal goal : activeGoals) {
            if (remainingAmount <= 0) break;

            Double neededToComplete = goal.getTargetAmount() - goal.getCurrentAmount();
            if (neededToComplete <= 0) continue;

            Double amountForThisGoal = Math.min(remainingAmount, neededToComplete);

            Double priorityMultiplier = 1.0 + (goal.getPriorityLevel() * 0.2);
            Double calculatedAmount = Math.min(amountForThisGoal,
                    (totalAmount / activeGoals.size()) * priorityMultiplier);

            goal.setCurrentAmount(goal.getCurrentAmount() + calculatedAmount);
            remainingAmount -= calculatedAmount;

            if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
                goal.setStatus(SavingGoal.GoalStatus.COMPLETED);
                goal.setCompletedAt(LocalDateTime.now());
            }

            savingRepository.save(goal);
        }
    }

    /**
     * Proceso de monitoreo (potencialmente programado) que identifica metas cuya fecha límite ha expirado.
     * Actualmente registra los hallazgos en el log para auditoría o alertas.
     */
    @Transactional
    public void checkAndUpdateOverdueGoals() {
        LocalDateTime now = LocalDateTime.now();
        List<SavingGoal> allGoals = savingRepository.findAll();
        for (SavingGoal goal : allGoals) {
            if (goal.getStatus() == SavingGoal.GoalStatus.ACTIVE &&
                    goal.getTargetDate() != null &&
                    now.isAfter(goal.getTargetDate())) {
                log.info("Meta vencida identificada: {}", goal.getName());
            }
        }
    }

    /**
     * Proceso de saneamiento de datos que busca y corrige inconsistencias de estado.
     * Detecta metas que financieramente han alcanzado su objetivo ({@code current >= target})
     * pero que no figuran como {@code COMPLETED}, actualizándolas automáticamente.
     */
    @Transactional
    public void checkCompletedGoals() {
        List<SavingGoal> allGoals = savingRepository.findAll();
        for (SavingGoal goal : allGoals) {
            if (goal.getStatus() != SavingGoal.GoalStatus.COMPLETED &&
                    goal.getCurrentAmount() >= goal.getTargetAmount()) {
                goal.setStatus(SavingGoal.GoalStatus.COMPLETED);
                goal.setCompletedAt(LocalDateTime.now());
                savingRepository.save(goal);
            }
        }
    }

    /**
     * Recupera una meta específica por su identificador.
     *
     * @param id Identificador de la meta.
     * @return Un {@link Optional} conteniendo el DTO si se encuentra.
     */
    @Transactional(readOnly = true)
    public Optional<SavingDTO> getSavingGoalById(Long id) {
        return savingRepository.findById(id).map(this::convertToDTO);
    }

    /**
     * Obtiene todas las metas de un usuario ordenadas por prioridad.
     *
     * @param userId Identificador del usuario.
     * @return Lista de metas.
     */
    @Transactional(readOnly = true)
    public List<SavingDTO> getSavingGoalsByUserId(Long userId) {
        return savingRepository.findByUserIdOrderByPriorityLevelDesc(userId).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Filtra y retorna únicamente las metas que se encuentran en estado ACTIVO.
     *
     * @param userId Identificador del usuario.
     * @return Lista de metas activas.
     */
    @Transactional(readOnly = true)
    public List<SavingDTO> getActiveSavingGoals(Long userId) {
        return savingRepository.findByUserIdAndStatusOrderByPriorityLevelDesc(
                        userId, SavingGoal.GoalStatus.ACTIVE).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Recupera las metas marcadas como colaborativas o grupales.
     *
     * @param userId Identificador del usuario.
     * @return Lista de metas compartidas.
     */
    @Transactional(readOnly = true)
    public List<SavingDTO> getCollaborativeGoals(Long userId) {
        return savingRepository.findCollaborativeGoals(userId).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Obtiene metas que superan un umbral de prioridad específico.
     *
     * @param userId      Identificador del usuario.
     * @param minPriority Prioridad mínima requerida.
     * @return Lista de metas de alta prioridad.
     */
    @Transactional(readOnly = true)
    public List<SavingDTO> getHighPriorityGoals(Long userId, Integer minPriority) {
        return savingRepository.findHighPriorityGoals(userId, minPriority).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Calcula la suma total del dinero reservado en todas las metas del usuario.
     *
     * @param userId Identificador del usuario.
     * @return Monto total acumulado (Double).
     */
    @Transactional(readOnly = true)
    public Double getTotalCurrentSavings(Long userId) {
        return savingRepository.sumCurrentSavings(userId);
    }

    /**
     * Cuenta el número de metas actualmente en curso.
     *
     * @param userId Identificador del usuario.
     * @return Cantidad de metas activas.
     */
    @Transactional(readOnly = true)
    public Long countActiveGoals(Long userId) {
        return savingRepository.countActiveGoals(userId);
    }

    /**
     * Cuenta el número histórico de metas completadas exitosamente.
     *
     * @param userId Identificador del usuario.
     * @return Cantidad de metas completadas.
     */
    @Transactional(readOnly = true)
    public Long countCompletedGoals(Long userId) {
        return savingRepository.countCompletedGoals(userId);
    }

    /**
     * Identifica las metas próximas a vencer dentro de un horizonte temporal dado.
     * Útil para generar alertas tempranas o notificaciones push.
     *
     * @param userId    Identificador del usuario.
     * @param daysAhead Número de días a proyectar hacia el futuro para la búsqueda.
     * @return Lista de metas que vencen en el rango [hoy, hoy + daysAhead].
     */
    @Transactional(readOnly = true)
    public List<SavingDTO> getGoalsDueSoon(Long userId, int daysAhead) {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(daysAhead);
        return savingRepository.findGoalsDueByDate(userId, futureDate).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    private SavingDTO convertToDTO(SavingGoal savingGoal) {
        SavingDTO dto = new SavingDTO();
        dto.setId(savingGoal.getId());
        dto.setUserId(savingGoal.getUser().getId());
        dto.setName(savingGoal.getName());
        dto.setDescription(savingGoal.getDescription());
        dto.setTargetAmount(savingGoal.getTargetAmount());
        dto.setCurrentAmount(savingGoal.getCurrentAmount());
        dto.setTargetDate(savingGoal.getTargetDate());
        dto.setStatus(savingGoal.getStatus());
        dto.setMonthlyContribution(savingGoal.getMonthlyContribution());
        dto.setPriorityLevel(savingGoal.getPriorityLevel());
        dto.setIsCollaborative(savingGoal.getIsCollaborative());
        dto.setCreatedAt(savingGoal.getCreatedAt());
        dto.setUpdatedAt(savingGoal.getUpdatedAt());
        dto.setCompletedAt(savingGoal.getCompletedAt());
        dto.updateProgress();
        return dto;
    }
}