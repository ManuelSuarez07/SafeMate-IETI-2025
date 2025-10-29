package savemate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import savemate.dto.UserDTO;
import savemate.service.UserService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO, 
                                             @RequestParam String password) {
        log.info("Solicitud para crear usuario con email: {}", userDTO.getEmail());
        
        try {
            UserDTO createdUser = userService.createUser(userDTO, password);
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error creando usuario: {}", e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        log.info("Solicitud para obtener usuario con ID: {}", id);
        
        Optional<UserDTO> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                  .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        log.info("Solicitud para obtener usuario con email: {}", email);
        
        Optional<UserDTO> user = userService.getUserByEmail(email);
        return user.map(ResponseEntity::ok)
                  .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, 
                                             @Valid @RequestBody UserDTO userDTO) {
        log.info("Solicitud para actualizar usuario con ID: {}", id);
        
        try {
            UserDTO updatedUser = userService.updateUser(id, userDTO);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("Error actualizando usuario: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/{id}/saving-config")
    public ResponseEntity<UserDTO> updateSavingConfiguration(@PathVariable Long id, 
                                                           @Valid @RequestBody UserDTO configDTO) {
        log.info("Solicitud para actualizar configuración de ahorro del usuario ID: {}", id);
        
        try {
            UserDTO updatedUser = userService.updateSavingConfiguration(id, configDTO);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("Error actualizando configuración de ahorro: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/{id}/bank-account")
    public ResponseEntity<UserDTO> linkBankAccount(@PathVariable Long id,
                                                  @RequestParam String bankAccount,
                                                  @RequestParam String bankName) {
        log.info("Solicitud para vincular cuenta bancaria del usuario ID: {}", id);
        
        try {
            UserDTO updatedUser = userService.linkBankAccount(id, bankAccount, bankName);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("Error vinculando cuenta bancaria: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        log.info("Solicitud para obtener todos los usuarios");
        
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/exists/email/{email}")
    public ResponseEntity<Boolean> checkEmailExists(@PathVariable String email) {
        log.info("Verificando si existe el email: {}", email);
        
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(exists);
    }
    
    @GetMapping("/statistics/total-savings")
    public ResponseEntity<Double> getTotalSavingsAcrossAllUsers() {
        log.info("Solicitud para obtener total de ahorros de todos los usuarios");
        
        Double totalSavings = userService.getTotalSavingsAcrossAllUsers();
        return ResponseEntity.ok(totalSavings != null ? totalSavings : 0.0);
    }
    
    @GetMapping("/statistics/new-users")
    public ResponseEntity<Long> getNewUsersCount(@RequestParam String startDate) {
        log.info("Solicitud para obtener conteo de nuevos usuarios desde: {}", startDate);
        
        try {
            java.time.LocalDateTime start = java.time.LocalDateTime.parse(startDate);
            Long count = userService.countUsersCreatedAfter(start);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error parseando fecha: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}