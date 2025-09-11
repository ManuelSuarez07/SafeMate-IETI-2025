package safemate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import safemate.model.SavingGoal;
import safemate.service.SavingService;

@RestController
@RequestMapping("/api/savings")
public class SavingController {
    private final SavingService savingService;
    public SavingController(SavingService savingService) { this.savingService = savingService; }

    @PostMapping
    public ResponseEntity<SavingGoal> create(@RequestBody SavingGoal goal) {
        return ResponseEntity.ok(savingService.create(goal));
    }
}