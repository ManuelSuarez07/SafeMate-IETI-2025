package savemate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import savemate.dto.TransactionDTO;
import savemate.dto.TransactionSummaryDTO;
import savemate.model.Transaction;
import savemate.service.TransactionService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TransactionController {
    
    private final TransactionService transactionService;
    
    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        log.info("Solicitud para crear transacción para usuario ID: {}", transactionDTO.getUserId());
        
        try {
            TransactionDTO createdTransaction = transactionService.createTransaction(transactionDTO);
            return new ResponseEntity<>(createdTransaction, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error creando transacción: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/from-notification")
    public ResponseEntity<TransactionDTO> processTransactionFromNotification(
            @RequestParam Long userId,
            @RequestParam Double amount,
            @RequestParam String description,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) String notificationSource,
            @RequestParam(required = false) String bankReference) {
        
        log.info("Procesando transacción desde notificación para usuario ID: {}", userId);
        
        try {
            TransactionDTO transaction = transactionService.processTransactionFromNotification(
                userId, amount, description, merchantName, notificationSource, bankReference);
            return new ResponseEntity<>(transaction, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error procesando transacción desde notificación: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/saving-deposit")
    public ResponseEntity<TransactionDTO> createSavingDeposit(
            @RequestParam Long userId,
            @RequestParam Double amount,
            @RequestParam(required = false) String description) {
        
        log.info("Creando depósito de ahorro para usuario ID: {}", userId);
        
        try {
            TransactionDTO transaction = transactionService.createSavingDeposit(userId, amount, description);
            return new ResponseEntity<>(transaction, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error creando depósito de ahorro: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/process-pending/{userId}")
    public ResponseEntity<Void> processPendingTransactions(@PathVariable Long userId) {
        log.info("Procesando transacciones pendientes para usuario ID: {}", userId);
        
        try {
            transactionService.processPendingTransactions(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error procesando transacciones pendientes: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable Long id) {
        log.info("Solicitud para obtener transacción con ID: {}", id);
        
        Optional<TransactionDTO> transaction = transactionService.getTransactionById(id);
        return transaction.map(ResponseEntity::ok)
                         .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByUserId(@PathVariable Long userId) {
        log.info("Solicitud para obtener transacciones del usuario ID: {}", userId);
        
        List<TransactionDTO> transactions = transactionService.getTransactionsByUserId(userId);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByUserIdAndType(
            @PathVariable Long userId,
            @PathVariable Transaction.TransactionType type) {
        
        log.info("Solicitud para obtener transacciones del usuario ID: {} con tipo: {}", userId, type);
        
        List<TransactionDTO> transactions = transactionService.getTransactionsByUserIdAndType(userId, type);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Solicitud para obtener transacciones del usuario ID: {} entre {} y {}", userId, startDate, endDate);
        
        List<TransactionDTO> transactions = transactionService.getTransactionsByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/user/{userId}/statistics/expenses")
    public ResponseEntity<Double> getTotalExpenses(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Solicitud para obtener total de gastos del usuario ID: {} entre {} y {}", userId, startDate, endDate);
        
        Double totalExpenses = transactionService.getTotalExpenses(userId, startDate, endDate);
        return ResponseEntity.ok(totalExpenses != null ? totalExpenses : 0.0);
    }
    
    @GetMapping("/user/{userId}/statistics/savings")
    public ResponseEntity<Double> getTotalSavings(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Solicitud para obtener total de ahorros del usuario ID: {} entre {} y {}", userId, startDate, endDate);
        
        Double totalSavings = transactionService.getTotalSavings(userId, startDate, endDate);
        return ResponseEntity.ok(totalSavings != null ? totalSavings : 0.0);
    }
    
    @GetMapping("/user/{userId}/statistics/summary")
    public ResponseEntity<TransactionSummaryDTO> getTransactionSummary(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Solicitud para obtener resumen de transacciones del usuario ID: {} entre {} y {}", userId, startDate, endDate);
        
        try {
            Double totalExpenses = transactionService.getTotalExpenses(userId, startDate, endDate);
            Double totalSavings = transactionService.getTotalSavings(userId, startDate, endDate);
            List<TransactionDTO> transactions = transactionService.getTransactionsByDateRange(userId, startDate, endDate);
            
            TransactionSummaryDTO summary = new TransactionSummaryDTO(totalExpenses, totalSavings, transactions);
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error obteniendo resumen de transacciones: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}