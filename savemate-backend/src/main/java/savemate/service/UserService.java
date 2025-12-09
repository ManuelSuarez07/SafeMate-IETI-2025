package savemate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import savemate.dto.UserDTO;
import savemate.model.User;
import savemate.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de dominio responsable de la gestión del ciclo de vida de los usuarios, seguridad de credenciales
 * y configuración de preferencias financieras.
 * <p>
 * Este componente actúa como la fachada principal para todas las operaciones relacionadas con la identidad
 * y el perfil del usuario. Sus responsabilidades incluyen:
 * <ul>
 * <li>Registro de nuevos usuarios con validación de unicidad y encriptación de contraseñas.</li>
 * <li>Actualización de datos demográficos y vinculación de instrumentos bancarios.</li>
 * <li>Gestión granular de la configuración del motor de ahorro (estrategias de redondeo, umbrales de seguridad).</li>
 * <li>Sincronización del saldo contable total del usuario.</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo usuario en la plataforma, estableciendo su identidad y configuración inicial.
     * <p>
     * Este método realiza validaciones críticas de negocio (unicidad de email, obligatoriedad de campos)
     * y se encarga de proteger la contraseña utilizando un algoritmo de hash seguro (vía {@link PasswordEncoder})
     * antes de la persistencia.
     * </p>
     *
     * @param userDTO Objeto de transferencia con los datos de registro (nombre, email, password plano, config inicial).
     * @return El DTO del usuario creado, excluyendo la contraseña pero incluyendo el ID generado y fechas de auditoría.
     * @throws RuntimeException Si el email ya existe o si faltan credenciales obligatorias.
     */
    public UserDTO createUser(UserDTO userDTO) {
        log.info("Creando nuevo usuario con email: {}", userDTO.getEmail());

        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        if (userDTO.getUsername() == null || userDTO.getUsername().trim().isEmpty()) {
            throw new RuntimeException("El nombre de usuario es obligatorio");
        }

        if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
            throw new RuntimeException("La contraseña es obligatoria");
        }

        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setUsername(userDTO.getUsername());

        // Encriptar contraseña
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhoneNumber(userDTO.getPhoneNumber());

        // Configuraciones de ahorro
        if (userDTO.getSavingType() != null)
            user.setSavingType(userDTO.getSavingType());

        if (userDTO.getRoundingMultiple() != null)
            user.setRoundingMultiple(userDTO.getRoundingMultiple());

        if (userDTO.getSavingPercentage() != null)
            user.setSavingPercentage(userDTO.getSavingPercentage());

        if (userDTO.getMinSafeBalance() != null)
            user.setMinSafeBalance(userDTO.getMinSafeBalance());

        if (userDTO.getInsufficientBalanceOption() != null)
            user.setInsufficientBalanceOption(userDTO.getInsufficientBalanceOption());

        User savedUser = userRepository.save(user);
        log.info("Usuario creado exitosamente con ID: {}", savedUser.getId());

        return convertToDTO(savedUser);
    }

    /**
     * Recupera la información detallada de un usuario basado en su identificador único.
     *
     * @param id Identificador primario del usuario.
     * @return Un {@link Optional} conteniendo el DTO del usuario si existe.
     */
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::convertToDTO);
    }

    /**
     * Busca un usuario por su dirección de correo electrónico.
     * Método esencial para procesos de autenticación y recuperación de cuenta.
     *
     * @param email Correo electrónico a buscar.
     * @return Un {@link Optional} conteniendo el DTO del usuario si existe.
     */
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::convertToDTO);
    }

    /**
     * Actualiza la información básica de perfil (demográfica y de contacto).
     * <p>
     * Ignora campos sensibles como contraseña o configuración financiera, los cuales
     * deben actualizarse a través de sus métodos especializados.
     * </p>
     *
     * @param id      Identificador del usuario a modificar.
     * @param userDTO DTO conteniendo los nuevos valores para nombre, apellido o teléfono.
     * @return El DTO con la información actualizada persistida.
     * @throws RuntimeException Si el usuario no es encontrado.
     */
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        log.info("Actualizando usuario con ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (userDTO.getFirstName() != null)
            user.setFirstName(userDTO.getFirstName());

        if (userDTO.getLastName() != null)
            user.setLastName(userDTO.getLastName());

        if (userDTO.getPhoneNumber() != null)
            user.setPhoneNumber(userDTO.getPhoneNumber());

        User updatedUser = userRepository.save(user);
        log.info("Usuario actualizado exitosamente");

        return convertToDTO(updatedUser);
    }

    /**
     * Actualiza los parámetros del algoritmo de ahorro automático utilizando un objeto tipado.
     * <p>
     * Permite reconfigurar la estrategia (Redondeo vs Porcentaje), los multiplicadores y
     * los umbrales de seguridad financiera (Saldo mínimo).
     * </p>
     *
     * @param id        Identificador del usuario.
     * @param configDTO DTO que encapsula la nueva configuración financiera.
     * @return El DTO del usuario con la configuración aplicada.
     */
    public UserDTO updateSavingConfiguration(Long id, UserDTO configDTO) {
        log.info("Actualizando configuración de ahorro para usuario ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (configDTO.getSavingType() != null)
            user.setSavingType(configDTO.getSavingType());

        if (configDTO.getRoundingMultiple() != null)
            user.setRoundingMultiple(configDTO.getRoundingMultiple());

        if (configDTO.getSavingPercentage() != null)
            user.setSavingPercentage(configDTO.getSavingPercentage());

        if (configDTO.getMinSafeBalance() != null)
            user.setMinSafeBalance(configDTO.getMinSafeBalance());

        if (configDTO.getInsufficientBalanceOption() != null)
            user.setInsufficientBalanceOption(configDTO.getInsufficientBalanceOption());

        User updatedUser = userRepository.save(user);
        log.info("Configuración de ahorro actualizada exitosamente");

        return convertToDTO(updatedUser);
    }

    /**
     * Vincula un instrumento bancario externo al perfil del usuario.
     * Necesario para habilitar la funcionalidad de retiros de fondos.
     *
     * @param id          Identificador del usuario.
     * @param bankAccount Número de cuenta o CLABE interbancaria.
     * @param bankName    Nombre de la institución financiera.
     * @return El DTO actualizado.
     */
    public UserDTO linkBankAccount(Long id, String bankAccount, String bankName) {
        log.info("Vinculando cuenta bancaria para usuario ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setBankAccount(bankAccount);
        user.setBankName(bankName);

        User updatedUser = userRepository.save(user);
        log.info("Cuenta bancaria vinculada exitosamente");

        return convertToDTO(updatedUser);
    }

    /**
     * Actualiza dinámicamente la configuración de ahorro mediante un mapa de valores (estilo PATCH).
     * <p>
     * Este método ofrece flexibilidad para interfaces de usuario que envían actualizaciones parciales,
     * parseando y convirtiendo los valores del mapa a los tipos de datos del modelo de dominio.
     * Soporta la actualización de Enumeraciones y valores numéricos.
     * </p>
     *
     * @param userId  Identificador del usuario.
     * @param updates Mapa clave-valor con los campos a modificar.
     * @return El DTO del usuario actualizado.
     */
    public UserDTO updateSavingConfiguration(Long userId, Map<String, Object> updates) {
        log.info("Actualizando configuración de ahorro para usuario ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (updates.containsKey("savingType")) {
            user.setSavingType(User.SavingType.valueOf((String) updates.get("savingType")));
        }

        if (updates.containsKey("roundingMultiple")) {
            Object val = updates.get("roundingMultiple");
            if (val instanceof Number) {
                user.setRoundingMultiple(((Number) val).intValue());
            }
        }

        if (updates.containsKey("savingPercentage")) {
            Object val = updates.get("savingPercentage");
            if (val instanceof Number) {
                user.setSavingPercentage(((Number) val).doubleValue());
            }
        }

        if (updates.containsKey("minSafeBalance")) {
            Object val = updates.get("minSafeBalance");
            if (val instanceof Number) {
                user.setMinSafeBalance(((Number) val).doubleValue());
            }
        }

        if (updates.containsKey("insufficientBalanceOption")) {
            user.setInsufficientBalanceOption(
                    User.InsufficientBalanceOption.valueOf((String) updates.get("insufficientBalanceOption"))
            );
        }

        if (updates.containsKey("bankAccount")) {
            user.setBankAccount((String) updates.get("bankAccount"));
        }

        if (updates.containsKey("bankName")) {
            user.setBankName((String) updates.get("bankName"));
        }

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    /**
     * Actualiza el saldo contable total acumulado por el usuario.
     * <p>
     * Este método es de uso interno (invocado principalmente por el servicio de transacciones)
     * y maneja tanto incrementos (ahorros) como decrementos (retiros) mediante la suma algebraica.
     * </p>
     *
     * @param userId Identificador del usuario.
     * @param amount Cantidad a sumar al saldo actual (puede ser negativo para restas).
     */
    public void updateTotalSaved(Long userId, Double amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Double currentTotal = user.getTotalSaved() != null ? user.getTotalSaved() : 0.0;
        user.setTotalSaved(currentTotal + amount);

        userRepository.save(user);
        log.debug("Total actualizado para usuario {}: {}", userId, user.getTotalSaved());
    }

    /**
     * Recupera la lista completa de usuarios registrados en el sistema.
     * Método administrativo para reportes generales.
     *
     * @return Lista de DTOs de todos los usuarios.
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Verifica la existencia de un usuario mediante su correo electrónico.
     *
     * @param email Correo a verificar.
     * @return {@code true} si el correo ya está en uso.
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Calcula la suma total de dinero gestionado por la plataforma (todos los usuarios).
     *
     * @return Monto total acumulado global.
     */
    @Transactional(readOnly = true)
    public Double getTotalSavingsAcrossAllUsers() {
        return userRepository.getTotalSavingsAcrossAllUsers();
    }

    /**
     * Contabiliza el crecimiento de usuarios a partir de una fecha específica.
     *
     * @param startDate Fecha de corte.
     * @return Cantidad de nuevos usuarios desde la fecha dada.
     */
    @Transactional(readOnly = true)
    public Long countUsersCreatedAfter(LocalDateTime startDate) {
        return userRepository.countUsersCreatedAfter(startDate);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setBankAccount(user.getBankAccount());
        dto.setBankName(user.getBankName());
        dto.setSavingType(user.getSavingType());
        dto.setRoundingMultiple(user.getRoundingMultiple());
        dto.setSavingPercentage(user.getSavingPercentage());
        dto.setMinSafeBalance(user.getMinSafeBalance());
        dto.setInsufficientBalanceOption(user.getInsufficientBalanceOption());
        dto.setTotalSaved(user.getTotalSaved());
        dto.setMonthlyFeeRate(user.getMonthlyFeeRate());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}