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

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingService {

    private final SavingRepository savingRepository;
    private final UserRepository userRepository;
    private final UserService userService;

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

    @Transactional(readOnly = true)
    public Optional<SavingDTO> getSavingGoalById(Long id) {
        return savingRepository.findById(id).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<SavingDTO> getSavingGoalsByUserId(Long userId) {
        return savingRepository.findByUserIdOrderByPriorityLevelDesc(userId).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SavingDTO> getActiveSavingGoals(Long userId) {
        return savingRepository.findByUserIdAndStatusOrderByPriorityLevelDesc(
                        userId, SavingGoal.GoalStatus.ACTIVE).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SavingDTO> getCollaborativeGoals(Long userId) {
        return savingRepository.findCollaborativeGoals(userId).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SavingDTO> getHighPriorityGoals(Long userId, Integer minPriority) {
        return savingRepository.findHighPriorityGoals(userId, minPriority).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getTotalCurrentSavings(Long userId) {
        return savingRepository.sumCurrentSavings(userId);
    }

    @Transactional(readOnly = true)
    public Long countActiveGoals(Long userId) {
        return savingRepository.countActiveGoals(userId);
    }

    @Transactional(readOnly = true)
    public Long countCompletedGoals(Long userId) {
        return savingRepository.countCompletedGoals(userId);
    }

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