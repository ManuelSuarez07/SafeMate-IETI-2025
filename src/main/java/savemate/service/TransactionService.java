package safemate.service;

import org.springframework.stereotype.Service;
import safemate.model.Transaction;
import safemate.repository.TransactionRepository;

import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository txRepo;
    public TransactionService(TransactionRepository txRepo) { this.txRepo = txRepo; }

    public Transaction save(Transaction tx) { return txRepo.save(tx); }
    public List<Transaction> findByUser(Long userId) { return txRepo.findByUserIdOrderByOccurredAtDesc(userId); }
}