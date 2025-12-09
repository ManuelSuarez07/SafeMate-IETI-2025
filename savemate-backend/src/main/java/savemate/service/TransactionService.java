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

        if (transactionDTO.getTransactionType() == Transaction.TransactionType.EXPENSE) {
            calculateAndApplySaving(transaction, user);
        }
        else if (transactionDTO.getTransactionType() == Transaction.TransactionType.INCOME) {
            transaction.setSavingAmount(transactionDTO.getAmount());
            log.info("Ingreso de ${} registrado como ahorro total.", transactionDTO.getAmount());
        }

        Transaction savedTransaction = transactionRepository.save(transaction);

        if (savedTransaction.getSavingAmount() != null && savedTransaction.getSavingAmount() > 0) {
            userService.updateTotalSaved(user.getId(), savedTransaction.getSavingAmount());
        }

        return convertToDTO(savedTransaction);
    }

    // MÉTODO PARA RETIROS
    @Transactional
    public TransactionDTO createWithdrawal(Long userId, Double amount) {
        log.info("Procesando retiro de ${} para usuario ID: {}", amount, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Verificar fondos suficientes
        if (user.getTotalSaved() == null || user.getTotalSaved() < amount) {
            throw new RuntimeException("Fondos insuficientes para realizar el retiro");
        }

        // 2. Crear transacción de retiro
        Transaction t = new Transaction();
        t.setUser(user);
        t.setAmount(amount);
        t.setDescription("Retiro a cuenta vinculada " + (user.getBankName() != null ? user.getBankName() : ""));
        t.setTransactionDate(LocalDateTime.now());
        t.setTransactionType(Transaction.TransactionType.WITHDRAWAL);
        t.setStatus(Transaction.TransactionStatus.COMPLETED);

        Transaction saved = transactionRepository.save(t);

        // 3. Restar del saldo del usuario
        userService.updateTotalSaved(userId, -amount);

        log.info("Retiro exitoso. Nuevo saldo: {}", user.getTotalSaved() - amount);

        return convertToDTO(saved);
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

        calculateAndApplySaving(transaction, user);

        Transaction savedTransaction = transactionRepository.save(transaction);

        if (savedTransaction.getSavingAmount() != null && savedTransaction.getSavingAmount() > 0) {
            userService.updateTotalSaved(userId, savedTransaction.getSavingAmount());
        }
        return convertToDTO(savedTransaction);
    }
    private void calculateAndApplySaving(Transaction transaction, User user) {
        Double savingAmount = 0.0;
        Double roundedAmount = 0.0;

        if (user.getSavingType() != null) {
            if (user.getSavingType() == User.SavingType.ROUNDING) {
                Double originalAmount = transaction.getAmount();
                roundedAmount = roundingUtils.roundUpToMultiple(originalAmount, user.getRoundingMultiple());
                savingAmount = roundedAmount - originalAmount;

                transaction.setOriginalAmount(originalAmount);
                transaction.setRoundedAmount(roundedAmount);
                transaction.setSavingAmount(savingAmount);

            } else if (user.getSavingType() == User.SavingType.PERCENTAGE) {
                savingAmount = transaction.getAmount() * (user.getSavingPercentage() / 100);
                transaction.setSavingAmount(savingAmount);
            }
        }

        if (savingAmount > 0) {
            if (!hasSufficientBalance(user, savingAmount)) {
                handleInsufficientBalance(transaction, user, savingAmount);
            }
        }
    }

    private boolean hasSufficientBalance(User user, Double savingAmount) {
        return user.getMinSafeBalance() == null || savingAmount <= user.getMinSafeBalance();
    }

    private void handleInsufficientBalance(Transaction transaction, User user, Double savingAmount) {
        switch (user.getInsufficientBalanceOption()) {
            case NO_SAVING: transaction.setSavingAmount(0.0); break;
            case PENDING: transaction.setStatus(Transaction.TransactionStatus.PENDING); break;
            case RESPECT_MIN_BALANCE:
                Double adjusted = Math.max(0, savingAmount - user.getMinSafeBalance());
                transaction.setSavingAmount(adjusted);
                break;
        }
    }

    @Transactional
    public void processPendingTransactions(Long userId) {
        List<Transaction> pending = transactionRepository.findPendingTransactions(userId);
        for (Transaction t : pending) {
            t.setStatus(Transaction.TransactionStatus.COMPLETED);
            transactionRepository.save(t);
            if (t.getSavingAmount() != null && t.getSavingAmount() > 0) {
                userService.updateTotalSaved(userId, t.getSavingAmount());
            }
        }
    }

    @Transactional
    public TransactionDTO createSavingDeposit(Long userId, Double amount, String description) {
        log.info("Creando depósito de ahorro para usuario ID: {}", userId);
        User user = userRepository.findById(userId).orElseThrow();

        Transaction t = new Transaction();
        t.setUser(user);
        t.setAmount(amount);
        t.setDescription(description != null ? description : "Depósito manual");
        t.setTransactionDate(LocalDateTime.now());
        t.setTransactionType(Transaction.TransactionType.SAVING);
        t.setStatus(Transaction.TransactionStatus.COMPLETED);

        Transaction saved = transactionRepository.save(t);
        userService.updateTotalSaved(userId, amount); // Suma positiva

        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public Optional<TransactionDTO> getTransactionById(Long id) {
        return transactionRepository.findById(id).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByUserId(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByUserIdAndType(Long userId, Transaction.TransactionType type) {
        return transactionRepository.findByUserIdAndTransactionTypeOrderByTransactionDateDesc(userId, type).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByUserIdAndDateRange(userId, startDate, endDate).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
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