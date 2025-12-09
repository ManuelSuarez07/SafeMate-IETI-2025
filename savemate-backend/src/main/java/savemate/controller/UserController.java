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

/**
 * Controlador REST responsable de la gestión de usuarios.
 *
 * <p>Responsabilidad: Exponer endpoints bajo {@code /api/users} para operaciones de registro,
 * consulta y actualización de usuarios. Traduce solicitudes HTTP a llamadas a {@link UserService}
 * y devuelve respuestas {@link ResponseEntity} con los códigos de estado apropiados.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * <p>Valida la presencia de email y contraseña en el DTO y delega la creación a {@link UserService}.
     *
     * @param userDTO DTO que contiene los datos del usuario a crear (por ejemplo: email, password, nombre)
     * @return {@link ResponseEntity} con:
     *         <ul>
     *           <li>HTTP 201 (CREATED) y el {@link UserDTO} creado en caso de éxito.</li>
     *           <li>HTTP 400 (BAD_REQUEST) con mensaje cuando faltan campos obligatorios o ocurre un error de negocio.</li>
     *         </ul>
     * @throws RuntimeException en caso de errores de negocio durante la creación del usuario.
     */
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

    /**
     * Obtiene un usuario por su identificador.
     *
     * @param id identificador del usuario a recuperar
     * @return {@link ResponseEntity} con:
     *         <ul>
     *           <li>HTTP 200 (OK) y el {@link UserDTO} cuando el usuario existe.</li>
     *           <li>HTTP 404 (NOT_FOUND) cuando no se encuentra el usuario.</li>
     *         </ul>
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        log.info("Solicitud para obtener usuario ID: {}", id);
        Optional<UserDTO> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Actualiza el perfil de un usuario existente.
     *
     * <p>Delegar la lógica de actualización a {@link UserService} y devolver el recurso actualizado.
     *
     * @param id identificador del usuario a actualizar
     * @param userDTO DTO con los campos que se desean actualizar en el perfil del usuario
     * @return {@link ResponseEntity} con:
     *         <ul>
     *           <li>HTTP 200 (OK) y el {@link UserDTO} actualizado en caso de éxito.</li>
     *           <li>HTTP 400 (BAD_REQUEST) si ocurre un error de validación o de negocio.</li>
     *         </ul>
     * @throws RuntimeException si la operación de actualización falla por restricciones de negocio.
     */
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

    /**
     * Actualiza la configuración relacionada con el ahorro del usuario.
     *
     * <p>Espera un mapa con los parámetros de configuración y delega la persistencia en {@link UserService}.
     *
     * @param id identificador del usuario cuya configuración se actualizará
     * @param config mapa con claves/valores que representan la configuración de ahorro a aplicar
     * @return {@link ResponseEntity} con:
     *         <ul>
     *           <li>HTTP 200 (OK) y el {@link UserDTO} actualizado en caso de éxito.</li>
     *           <li>HTTP 400 (BAD_REQUEST) si la actualización falla por validación o lógica de negocio.</li>
     *         </ul>
     * @throws RuntimeException si ocurre un error durante la actualización de la configuración.
     */
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

    /**
     * Vincula una cuenta bancaria al perfil del usuario.
     *
     * <p>Espera un objeto JSON con las claves \"bankAccount\" y \"bankName\". Valida la presencia de ambos
     * antes de delegar la operación a {@link UserService}.
     *
     * @param id identificador del usuario al que se vinculará la cuenta bancaria
     * @param bankData mapa con los datos bancarios, debe contener las claves {@code "bankAccount"} y {@code "bankName"}
     * @return {@link ResponseEntity} con:
     *         <ul>
     *           <li>HTTP 200 (OK) y el {@link UserDTO} actualizado en caso de éxito.</li>
     *           <li>HTTP 400 (BAD_REQUEST) con mensaje cuando faltan datos o ocurre un error.</li>
     *         </ul>
     * @throws RuntimeException si ocurre un error durante la vinculación de la cuenta bancaria.
     */
    @PutMapping("/{id}/bank-account")
    public ResponseEntity<?> linkBankAccount(@PathVariable Long id, @RequestBody Map<String, String> bankData) {
        log.info("Solicitud para vincular cuenta bancaria usuario ID: {}", id);
        try {
            String bankAccount = bankData.get("bankAccount");
            String bankName = bankData.get("bankName");

            if (bankAccount == null || bankName == null) {
                return ResponseEntity.badRequest().body("Faltan datos bancarios");
            }

            UserDTO updatedUser = userService.linkBankAccount(id, bankAccount, bankName);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("Error vinculando cuenta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}