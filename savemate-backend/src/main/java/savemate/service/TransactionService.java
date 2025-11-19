package savemate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import savemate.dto.TransactionDTO;
import savemate.model.Transaction;
import savemate.model.User;
import savemate.repository.TransactionRepository;
import savemate.repository.UserRepository;
import savemate.util.RoundingUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RoundingUtils roundingUtils;
    
    @Transactional
    public TransactionDTO createTransaction(TransactionDTO transactionDTO) {
        log.info("Creando transacción para usuario ID: {}", transactionDTO.getUserId());
        
        User user = userRepository.findById(transactionDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setDescription(transactionDTO.getDescription());
        transaction.setMerchantName(transactionDTO.getMerchantName());
        transaction.setTransactionDate(transactionDTO.getTransactionDate() != null ? 
                                     transactionDTO.getTransactionDate() : LocalDateTime.now());
        transaction.setTransactionType(transactionDTO.getTransactionType());
        transaction.setStatus(transactionDTO.getStatus());
        transaction.setNotificationSource(transactionDTO.getNotificationSource());
        transaction.setBankReference(transactionDTO.getBankReference());
        
        // Si es un gasto, calcular ahorro automático
        if (transactionDTO.getTransactionType() == Transaction.TransactionType.EXPENSE) {
            calculateAndApplySaving(transaction, user);
        }
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transacción creada exitosamente con ID: {}", savedTransaction.getId());
        
        return convertToDTO(savedTransaction);
    }
    
    @Transactional
    public TransactionDTO processTransactionFromNotification(Long userId, Double amount, String description,
                                                           String merchantName, String notificationSource,
                                                           String bankReference) {
        log.info("Procesando transacción desde notificación para usuario ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setMerchantName(merchantName);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setTransactionType(Transaction.TransactionType.EXPENSE);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setNotificationSource(notificationSource);
        transaction.setBankReference(bankReference);
        
        // Calcular y aplicar ahorro automático
        calculateAndApplySaving(transaction, user);
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Actualizar total ahorrado del usuario
        if (savedTransaction.getSavingAmount() != null && savedTransaction.getSavingAmount() > 0) {
            userService.updateTotalSaved(userId, savedTransaction.getSavingAmount());
        }
        
        log.info("Transacción procesada exitosamente con ID: {}", savedTransaction.getId());
        
        return convertToDTO(savedTransaction);
    }
    
    @Transactional
    public TransactionDTO createSavingDeposit(Long userId, Double amount, String description) {
        log.info("Creando depósito de ahorro para usuario ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setDescription(description != null ? description : "Depósito de ahorro manual");
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setTransactionType(Transaction.TransactionType.SAVING);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Actualizar total ahorrado del usuario
        userService.updateTotalSaved(userId, amount);
        
        log.info("Depósito de ahorro creado exitosamente con ID: {}", savedTransaction.getId());
        
        return convertToDTO(savedTransaction);
    }
    
    @Transactional
    public void processPendingTransactions(Long userId) {
        log.info("Procesando transacciones pendientes para usuario ID: {}", userId);
        
        List<Transaction> pendingTransactions = transactionRepository.findPendingTransactions(userId);
        
        for (Transaction transaction : pendingTransactions) {
            try {
                // Lógica para procesar transacción pendiente
                processPendingTransaction(transaction);
            } catch (Exception e) {
                log.error("Error procesando transacción pendiente ID: {}", transaction.getId(), e);
                transaction.setStatus(Transaction.TransactionStatus.FAILED);
                transactionRepository.save(transaction);
            }
        }
    }
    
    private void calculateAndApplySaving(Transaction transaction, User user) {
        Double savingAmount = null;
        Double roundedAmount = null;
        
        if (user.getSavingType() == User.SavingType.ROUNDING) {
            // Calcular por redondeo
            Double originalAmount = transaction.getAmount();
            roundedAmount = roundingUtils.roundUpToMultiple(originalAmount, user.getRoundingMultiple());
            savingAmount = roundedAmount - originalAmount;
            
            transaction.setOriginalAmount(originalAmount);
            transaction.setRoundedAmount(roundedAmount);
            transaction.setSavingAmount(savingAmount);
            
        } else if (user.getSavingType() == User.SavingType.PERCENTAGE) {
            // Calcular por porcentaje
            savingAmount = transaction.getAmount() * (user.getSavingPercentage() / 100);
            transaction.setSavingAmount(savingAmount);
        }
        
        // Verificar saldo mínimo seguro
        if (savingAmount != null && savingAmount > 0) {
            if (!hasSufficientBalance(user, savingAmount)) {
                handleInsufficientBalance(transaction, user, savingAmount);
            }
        }
    }
    
    private boolean hasSufficientBalance(User user, Double savingAmount) {
        // Lógica simplificada - en un caso real se verificaría el saldo bancario real
        return user.getMinSafeBalance() == null || savingAmount <= user.getMinSafeBalance();
    }
    
    private void handleInsufficientBalance(Transaction transaction, User user, Double savingAmount) {
        switch (user.getInsufficientBalanceOption()) {
            case NO_SAVING:
                transaction.setSavingAmount(0.0);
                transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                break;
            case PENDING:
                transaction.setStatus(Transaction.TransactionStatus.PENDING);
                break;
            case RESPECT_MIN_BALANCE:
                // Ajustar monto de ahorro para respetar saldo mínimo
                Double adjustedSaving = Math.max(0, savingAmount - user.getMinSafeBalance());
                transaction.setSavingAmount(adjustedSaving);
                break;
        }
    }
    
    private void processPendingTransaction(Transaction transaction) {
        // Lógica para procesar transacción pendiente
        // En un caso real, se intentaría el cobro nuevamente
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);
        
        if (transaction.getSavingAmount() != null && transaction.getSavingAmount() > 0) {
            userService.updateTotalSaved(transaction.getUser().getId(), transaction.getSavingAmount());
        }
    }
    
    @Transactional(readOnly = true)
    public Optional<TransactionDTO> getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .map(this::convertToDTO);
    }
    
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByUserId(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByUserIdAndType(Long userId, Transaction.TransactionType type) {
        return transactionRepository.findByUserIdAndTransactionTypeOrderByTransactionDateDesc(userId, type).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByUserIdAndDateRange(userId, startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Double getTotalExpenses(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.sumTransactionsByTypeAndDateRange(
                userId, Transaction.TransactionType.EXPENSE, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public Double getTotalSavings(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.sumSavingsByDateRange(userId, startDate, endDate);
    }
    
    private TransactionDTO convertToDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setUserId(transaction.getUser().getId());
        dto.setAmount(transaction.getAmount());
        dto.setDescription(transaction.getDescription());
        dto.setMerchantName(transaction.getMerchantName());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setStatus(transaction.getStatus());
        dto.setOriginalAmount(transaction.getOriginalAmount());
        dto.setRoundedAmount(transaction.getRoundedAmount());
        dto.setSavingAmount(transaction.getSavingAmount());
        dto.setNotificationSource(transaction.getNotificationSource());
        dto.setBankReference(transaction.getBankReference());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdatedAt(transaction.getUpdatedAt());
        return dto;
    }
}