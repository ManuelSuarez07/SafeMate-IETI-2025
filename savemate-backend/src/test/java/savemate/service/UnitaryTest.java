package savemate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import savemate.dto.UserDTO;
import savemate.model.User;
import savemate.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

// La clase User ya está disponible en el classpath, no necesitamos TestUser
@ExtendWith(MockitoExtension.class)
class UnitaryTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserDTO baseUserDTO;
    // Usamos directamente la clase User del modelo
    private User baseUser;
    private final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // 1. DTO de usuario base para las pruebas
        baseUserDTO = new UserDTO();
        baseUserDTO.setEmail("test@example.com");
        baseUserDTO.setUsername("testuser");
        baseUserDTO.setPassword("password123");
        baseUserDTO.setFirstName("Juan");
        baseUserDTO.setLastName("Perez");

        // 2. Entidad de usuario base para los mocks
        baseUser = new User();
        baseUser.setId(TEST_USER_ID);
        baseUser.setEmail("test@example.com");
        baseUser.setUsername("testuser");
        baseUser.setPassword("encodedPassword");
        baseUser.setFirstName("Juan");
        baseUser.setLastName("Perez");
        // Aseguramos que los campos numéricos estén inicializados para evitar NullPointerException en el service
        baseUser.setTotalSaved(0.0);
        baseUser.setRoundingMultiple(1000);
        baseUser.setSavingPercentage(10.0);
        baseUser.setMinSafeBalance(0.0);
        baseUser.setSavingType(User.SavingType.ROUNDING);
        baseUser.setInsufficientBalanceOption(User.InsufficientBalanceOption.NO_SAVING);
    }

    // =========================================================================
    //                            TESTS: createUser
    // =========================================================================

    @Test
    void test1_createUser_Success() {
        // GIVEN: El email no existe y el encoder funciona
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(baseUser);

        // WHEN: Llamamos al servicio
        UserDTO result = userService.createUser(baseUserDTO);

        // THEN: Verificamos creación exitosa y encriptación
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    void test2_createUser_EmailAlreadyExists_ThrowsException() {
        // GIVEN: El email YA existe
        when(userRepository.existsByEmail(baseUserDTO.getEmail())).thenReturn(true);

        // WHEN & THEN: Esperamos una excepción de tiempo de ejecución
        assertThrows(RuntimeException.class, () -> {
            userService.createUser(baseUserDTO);
        }, "El email ya está registrado");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void test3_createUser_MissingUsername_ThrowsException() {
        // GIVEN: Falta el nombre de usuario
        baseUserDTO.setUsername(null);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // WHEN & THEN: Esperamos una excepción con el mensaje correcto
        assertThrows(RuntimeException.class, () -> {
            userService.createUser(baseUserDTO);
        }, "El nombre de usuario es obligatorio");
    }

    @Test
    void test4_createUser_MissingPassword_ThrowsException() {
        // GIVEN: Falta la contraseña
        baseUserDTO.setPassword(null);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // WHEN & THEN: Esperamos una excepción con el mensaje correcto
        assertThrows(RuntimeException.class, () -> {
            userService.createUser(baseUserDTO);
        }, "La contraseña es obligatoria");
    }

    @Test
    void test5_createUser_ExplicitSavingConfig_IsSet() {
        // GIVEN: Usuario con configuración explícita
        // Usamos los ENUMS correctos del modelo
        baseUserDTO.setSavingType(User.SavingType.PERCENTAGE);
        baseUserDTO.setSavingPercentage(5.0);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // Configuramos el mock para devolver una entidad que refleje los cambios
        User userWithConfig = baseUser;
        userWithConfig.setSavingType(User.SavingType.PERCENTAGE);
        userWithConfig.setSavingPercentage(5.0);
        when(userRepository.save(any(User.class))).thenReturn(userWithConfig);

        // WHEN
        UserDTO result = userService.createUser(baseUserDTO);

        // THEN
        assertEquals(User.SavingType.PERCENTAGE, result.getSavingType());
        assertEquals(5.0, result.getSavingPercentage());
    }


    // =========================================================================
    //                            TESTS: Getters
    // =========================================================================

    @Test
    void test6_getUserById_Success() {
        // GIVEN
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(baseUser));

        // WHEN
        Optional<UserDTO> result = userService.getUserById(TEST_USER_ID);

        // THEN
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void test7_getUserById_NotFound() {
        // GIVEN
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // WHEN
        Optional<UserDTO> result = userService.getUserById(99L);

        // THEN
        assertFalse(result.isPresent());
    }

    @Test
    void test8_getUserByEmail_Success() {
        // GIVEN
        when(userRepository.findByEmail(baseUserDTO.getEmail())).thenReturn(Optional.of(baseUser));

        // WHEN
        Optional<UserDTO> result = userService.getUserByEmail(baseUserDTO.getEmail());

        // THEN
        assertTrue(result.isPresent());
        assertEquals(TEST_USER_ID, result.get().getId());
    }


    // =========================================================================
    //                            TESTS: updateUser
    // =========================================================================

    @Test
    void test9_updateUser_UpdateFirstName() {
        // GIVEN
        UserDTO updateDTO = new UserDTO();
        updateDTO.setFirstName("Pedro");

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(baseUser));

        // Configuramos el mock para reflejar el cambio antes de devolverlo
        User updatedUser = baseUser;
        updatedUser.setFirstName("Pedro");
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // WHEN
        UserDTO result = userService.updateUser(TEST_USER_ID, updateDTO);

        // THEN
        assertEquals("Pedro", result.getFirstName());
        assertEquals("Perez", result.getLastName(), "Otros campos no deberían cambiar");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void test10_updateUser_NotFound_ThrowsException() {
        // GIVEN
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> {
            userService.updateUser(99L, new UserDTO());
        }, "Usuario no encontrado");
    }

    // =========================================================================
    //                       TESTS: updateSavingConfiguration (DTO)
    // =========================================================================

    @Test
    void test11_updateSavingConfiguration_UpdateAllFields() {
        // GIVEN
        UserDTO configDTO = new UserDTO();
        // Usamos los ENUMS correctos del modelo
        configDTO.setSavingType(User.SavingType.ROUNDING);
        configDTO.setRoundingMultiple(10);
        configDTO.setMinSafeBalance(500.0);
        configDTO.setInsufficientBalanceOption(User.InsufficientBalanceOption.PENDING);

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(baseUser));

        // Configuramos el mock para reflejar los 4 cambios
        User updatedUser = baseUser;
        updatedUser.setSavingType(User.SavingType.ROUNDING);
        updatedUser.setRoundingMultiple(10);
        updatedUser.setMinSafeBalance(500.0);
        updatedUser.setInsufficientBalanceOption(User.InsufficientBalanceOption.PENDING);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // WHEN
        UserDTO result = userService.updateSavingConfiguration(TEST_USER_ID, configDTO);

        // THEN
        assertEquals(User.SavingType.ROUNDING, result.getSavingType());
        assertEquals(10, result.getRoundingMultiple());
        assertEquals(500.0, result.getMinSafeBalance());
        assertEquals(User.InsufficientBalanceOption.PENDING, result.getInsufficientBalanceOption());
    }

    // =========================================================================
    //                       TESTS: updateSavingConfiguration (Map)
    // =========================================================================

    @Test
    void test12_updateSavingConfiguration_PartialUpdateRoundingMultiple() {
        // GIVEN
        Map<String, Object> updates = Map.of("roundingMultiple", 5);

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(baseUser));

        // Reflejamos solo el cambio de rounding
        User updatedUser = baseUser;
        updatedUser.setRoundingMultiple(5);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // WHEN
        UserDTO result = userService.updateSavingConfiguration(TEST_USER_ID, updates);

        // THEN
        assertEquals(5, result.getRoundingMultiple());
        // El tipo de ahorro no debería ser modificado, por lo que debería mantener el valor predeterminado del setup
        assertEquals(User.SavingType.ROUNDING, result.getSavingType());
    }

    @Test
    void test13_updateSavingConfiguration_PartialUpdateSavingType() {
        // GIVEN
        // Usamos un valor de enum válido (PERCENTAGE)
        Map<String, Object> updates = Map.of("savingType", "PERCENTAGE");

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(baseUser));

        // Reflejamos el cambio de tipo de ahorro
        User updatedUser = baseUser;
        updatedUser.setSavingType(User.SavingType.PERCENTAGE);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // WHEN
        UserDTO result = userService.updateSavingConfiguration(TEST_USER_ID, updates);

        // THEN
        assertEquals(User.SavingType.PERCENTAGE, result.getSavingType());
    }

    // =========================================================================
    //                            TESTS: linkBankAccount
    // =========================================================================

    @Test
    void test14_linkBankAccount_Success() {
        // GIVEN
        String accountNumber = "1234567890";
        String bankName = "Test Bank";

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(baseUser));

        // Reflejamos los datos de la cuenta vinculada
        User updatedUser = baseUser;
        updatedUser.setBankAccount(accountNumber);
        updatedUser.setBankName(bankName);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);


        // WHEN
        UserDTO result = userService.linkBankAccount(TEST_USER_ID, accountNumber, bankName);

        // THEN
        assertEquals(accountNumber, result.getBankAccount());
        assertEquals(bankName, result.getBankName());
        verify(userRepository, times(1)).save(any(User.class));
    }


    // =========================================================================
    //                            TESTS: updateTotalSaved
    // =========================================================================

    @Test
    void test15_updateTotalSaved_InitialSave() {
        // GIVEN: El usuario tiene el campo totalSaved como 0.0 (simulado en el setup)
        Double initialAmount = 15.50;

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(baseUser));

        // WHEN
        userService.updateTotalSaved(TEST_USER_ID, initialAmount);

        // THEN: Se verifica que el valor guardado en la entidad sea el correcto
        // FIX: La lambda debe devolver un booleano (true si coincide)
        verify(userRepository).save(argThat(user -> initialAmount.equals(user.getTotalSaved())));
    }

    @Test
    void test16_updateTotalSaved_IncrementSave() {
        // GIVEN: El usuario ya tiene un saldo ahorrado
        baseUser.setTotalSaved(50.0);
        Double incrementAmount = 25.0;
        Double expectedTotal = 75.0;

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(baseUser));

        // WHEN
        userService.updateTotalSaved(TEST_USER_ID, incrementAmount);

        // THEN: Se espera que el nuevo total sea 75.0 (50.0 + 25.0)
        // FIX: La lambda debe devolver un booleano (true si coincide)
        verify(userRepository).save(argThat(user -> expectedTotal.equals(user.getTotalSaved())));
    }

    @Test
    void test17_updateTotalSaved_NegativeAmount_ReducesTotal() {
        // GIVEN: El usuario ya tiene un saldo, y se resta una cantidad (retiro o corrección)
        baseUser.setTotalSaved(100.0);
        Double decrementAmount = -30.0;
        Double expectedTotal = 70.0;

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(baseUser));

        // WHEN
        userService.updateTotalSaved(TEST_USER_ID, decrementAmount);

        // THEN: Se espera que el nuevo total sea 70.0 (100.0 - 30.0)
        // FIX: La lambda debe devolver un booleano (true si coincide)
        verify(userRepository).save(argThat(user -> expectedTotal.equals(user.getTotalSaved())));
    }

    @Test
    void test18_updateTotalSaved_UserNotFound_ThrowsException() {
        // GIVEN
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> {
            userService.updateTotalSaved(99L, 10.0);
        }, "Usuario no encontrado");
        verify(userRepository, never()).save(any(User.class));
    }
}