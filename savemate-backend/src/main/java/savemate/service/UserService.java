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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserDTO createUser(UserDTO userDTO, String rawPassword) {
        log.info("Creando nuevo usuario con email: {}", userDTO.getEmail());
        
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }
        
        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setSavingType(userDTO.getSavingType());
        user.setRoundingMultiple(userDTO.getRoundingMultiple());
        user.setSavingPercentage(userDTO.getSavingPercentage());
        user.setMinSafeBalance(userDTO.getMinSafeBalance());
        user.setInsufficientBalanceOption(userDTO.getInsufficientBalanceOption());
        user.setTotalSaved(0.0);
        user.setMonthlyFeeRate(2.5);
        
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
        
        if (userDTO.getFirstName() != null) {
            user.setFirstName(userDTO.getFirstName());
        }
        if (userDTO.getLastName() != null) {
            user.setLastName(userDTO.getLastName());
        }
        if (userDTO.getPhoneNumber() != null) {
            user.setPhoneNumber(userDTO.getPhoneNumber());
        }
        
        User updatedUser = userRepository.save(user);
        log.info("Usuario actualizado exitosamente");
        
        return convertToDTO(updatedUser);
    }
    
    public UserDTO updateSavingConfiguration(Long id, UserDTO configDTO) {
        log.info("Actualizando configuración de ahorro para usuario ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setSavingType(configDTO.getSavingType());
        user.setRoundingMultiple(configDTO.getRoundingMultiple());
        user.setSavingPercentage(configDTO.getSavingPercentage());
        user.setMinSafeBalance(configDTO.getMinSafeBalance());
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
    
    public void updateTotalSaved(Long userId, Double amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setTotalSaved(user.getTotalSaved() + amount);
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