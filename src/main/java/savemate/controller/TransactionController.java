package safemate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import safemate.model.Transaction;
import safemate.service.TransactionService;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService txService;
    public TransactionController(TransactionService txService) { this.txService = txService; }

    @PostMapping
    public ResponseEntity<Transaction> create(@RequestBody Transaction tx) {
        Transaction saved = txService.save(tx);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Transaction>> byUser(@PathVariable Long userId) {
        return ResponseEntity.ok(txService.findByUser(userId));
    }
}