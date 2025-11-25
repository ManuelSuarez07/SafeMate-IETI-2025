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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

        // ✅ Encriptar contraseña correctamente
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhoneNumber(userDTO.getPhoneNumber());

        // Configuraciones de ahorro (si no vienen, usa las del modelo User)
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

    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::convertToDTO);
    }

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

    // Sobrecarga 1: Usando DTO (útil para llamadas internas o futuras)
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

    // Sobrecarga 2: Usando Map (usada por el controlador actual para manejar JSON parcial)
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

    // ✅ Método unificado y seguro para actualizar el total ahorrado
    public void updateTotalSaved(Long userId, Double amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Double currentTotal = user.getTotalSaved() != null ? user.getTotalSaved() : 0.0;
        user.setTotalSaved(currentTotal + amount);

        userRepository.save(user);
        log.debug("Total actualizado para usuario {}: {}", userId, user.getTotalSaved());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public Double getTotalSavingsAcrossAllUsers() {
        return userRepository.getTotalSavingsAcrossAllUsers();
    }

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