package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import savemate.model.Transaction;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    
    private Long id;
    private Long userId;
    
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    private Double amount;
    
    @NotNull(message = "La descripción es obligatoria")
    private String description;
    
    private String merchantName;
    private LocalDateTime transactionDate;
    
    @NotNull(message = "El tipo de transacción es obligatorio")
    private Transaction.TransactionType transactionType;
    
    private Transaction.TransactionStatus status = Transaction.TransactionStatus.COMPLETED;
    
    private Double originalAmount;
    private Double roundedAmount;
    private Double savingAmount;
    private String notificationSource;
    private String bankReference;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructor para creación desde notificación
    public TransactionDTO(Long userId, Double amount, String description, 
                         String merchantName, LocalDateTime transactionDate,
                         Transaction.TransactionType transactionType, 
                         String notificationSource, String bankReference) {
        this.userId = userId;
        this.amount = amount;
        this.description = description;
        this.merchantName = merchantName;
        this.transactionDate = transactionDate;
        this.transactionType = transactionType;
        this.notificationSource = notificationSource;
        this.bankReference = bankReference;
    }
    
    // Constructor para transacción con ahorro
    public TransactionDTO(Long userId, Double originalAmount, Double roundedAmount, 
                         Double savingAmount, String description, String merchantName) {
        this.userId = userId;
        this.originalAmount = originalAmount;
        this.roundedAmount = roundedAmount;
        this.savingAmount = savingAmount;
        this.amount = originalAmount;
        this.description = description;
        this.merchantName = merchantName;
        this.transactionType = Transaction.TransactionType.EXPENSE;
        this.transactionDate = LocalDateTime.now();
    }
    
    // Constructor para depósito de ahorro
    public TransactionDTO(Long userId, Double savingAmount, String description) {
        this.userId = userId;
        this.amount = savingAmount;
        this.description = description;
        this.transactionType = Transaction.TransactionType.SAVING;
        this.transactionDate = LocalDateTime.now();
    }
}