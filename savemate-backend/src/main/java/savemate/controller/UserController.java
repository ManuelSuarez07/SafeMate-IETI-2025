package savemate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import savemate.dto.UserDTO;
import savemate.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Permitir peticiones desde Flutter
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> register(@RequestBody UserDTO userDTO) {
        log.info("Intento de registro recibido: {}", userDTO); // LOG IMPORTANTE PARA DEPURAR

        try {
            // Validación básica antes de llamar al servicio
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
}