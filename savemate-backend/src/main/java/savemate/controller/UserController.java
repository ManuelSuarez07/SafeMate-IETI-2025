package savemate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import savemate.dto.UserDTO;
import savemate.service.UserService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> register(@RequestBody UserDTO userDTO) {
        log.info("Intento de registro recibido: {}", userDTO);
        try {
            if (userDTO.getEmail() == null || userDTO.getPassword() == null) {
                return ResponseEntity.badRequest().body("Email y contraseña son obligatorios");
            }
            UserDTO created = userService.createUser(userDTO);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error al registrar usuario: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- MÉTODOS QUE FALTABAN PARA SOLUCIONAR EL ERROR 403/404 ---

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        log.info("Solicitud para obtener usuario ID: {}", id);
        Optional<UserDTO> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        log.info("Solicitud para actualizar perfil de usuario ID: {}", id);
        try {
            UserDTO updated = userService.updateUser(id, userDTO);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error actualizando usuario: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}/saving-config")
    public ResponseEntity<UserDTO> updateSavingConfiguration(
            @PathVariable Long id,
            @RequestBody Map<String, Object> config) {
        log.info("Solicitud de actualización de config de ahorro para usuario ID: {}", id);
        try {
            UserDTO updatedUser = userService.updateSavingConfiguration(id, config);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("Error actualizando configuración: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // --- NUEVO ENDPOINT PARA VINCULAR CUENTA ---
    @PutMapping("/{id}/bank-account")
    public ResponseEntity<?> linkBankAccount(@PathVariable Long id, @RequestBody Map<String, String> bankData) {
        log.info("Solicitud para vincular cuenta bancaria usuario ID: {}", id);
        try {
            String bankAccount = bankData.get("bankAccount");
            String bankName = bankData.get("bankName");

            if (bankAccount == null || bankName == null) {
                return ResponseEntity.badRequest().body("Faltan datos bancarios");
            }

            // Llamamos al método específico que ya tienes en UserService
            UserDTO updatedUser = userService.linkBankAccount(id, bankAccount, bankName);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("Error vinculando cuenta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}