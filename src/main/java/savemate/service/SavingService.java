package safemate.service;

import org.springframework.stereotype.Service;
import safemate.model.SavingGoal;
import safemate.repository.SavingRepository;

import java.util.List;

@Service
public class SavingService {
    private final SavingRepository savingRepository;
    public SavingService(SavingRepository savingRepository) { this.savingRepository = savingRepository; }

    public SavingGoal create(SavingGoal goal) { return savingRepository.save(goal); }
    public List<SavingGoal> getByUser(Long userId) { return savingRepository.findByUserId(userId); }
}