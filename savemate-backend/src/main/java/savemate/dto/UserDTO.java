package savemate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import savemate.model.User;

import java.time.LocalDateTime;

/**
 * Objeto de Transferencia de Datos (DTO) que centraliza la gestión del perfil de usuario, credenciales y
 * configuraciones paramétricas del sistema de ahorro.
 * Esta clase actúa como la interfaz principal de datos entre el cliente (Frontend/Móvil) y el núcleo de negocio.
 * Sus responsabilidades clave incluyen:
 * <ul>
 * <li>Validación de datos de entrada para registro y actualización de perfil (JSR-380).</li>
 * <li>Protección de credenciales mediante control de serialización (campo password de solo escritura).</li>
 * <li>Transporte de configuraciones financieras (reglas de redondeo, cuentas bancarias).</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String lastName;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String phoneNumber;
    private String bankAccount;
    private String bankName;

    private User.SavingType savingType = User.SavingType.ROUNDING;
    private Integer roundingMultiple = 1000;
    private Double savingPercentage = 10.0;
    private Double minSafeBalance = 0.0;
    private User.InsufficientBalanceOption insufficientBalanceOption = User.InsufficientBalanceOption.NO_SAVING;

    private Double totalSaved = 0.0;
    private Double monthlyFeeRate = 2.5;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Inicializa un DTO parcial con la información básica de contacto e identidad del usuario.
     * <p>
     * Este constructor es útil para operaciones ligeras de consulta de perfil o formularios de
     * actualización donde no se requiere la carga completa de datos financieros o sensibles.
     * </p>
     *
     * @param email       Correo electrónico asociado a la cuenta.
     * @param firstName   Primer nombre del usuario.
     * @param lastName    Apellidos del usuario.
     * @param phoneNumber Número de teléfono de contacto.
     */
    public UserDTO(String email, String firstName, String lastName, String phoneNumber) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Inicializa un DTO especializado para la actualización de la configuración del motor de ahorro.
     * Permite aislar la lógica de configuración financiera (estrategias de redondeo, porcentajes,
     * límites de seguridad) del resto de datos personales, facilitando endpoints dedicados a "Settings".
     *
     * @param id                        Identificador único del usuario a configurar.
     * @param savingType                Modalidad de ahorro seleccionada (ej. REDONDEO, PORCENTAJE).
     * @param roundingMultiple          Base numérica para el cálculo del redondeo (ej. a la centena o mil más cercana).
     * @param savingPercentage          Porcentaje a aplicar si el tipo de ahorro es porcentual.
     * @param minSafeBalance            Límite inferior de saldo en cuenta que bloquea el ahorro automático para evitar sobregiros.
     * @param insufficientBalanceOption Estrategia a seguir cuando el saldo es insuficiente (ej. NO_SAVING, PARTIAL).
     */
    public UserDTO(Long id, User.SavingType savingType, Integer roundingMultiple,
                   Double savingPercentage, Double minSafeBalance,
                   User.InsufficientBalanceOption insufficientBalanceOption) {
        this.id = id;
        this.savingType = savingType;
        this.roundingMultiple = roundingMultiple;
        this.savingPercentage = savingPercentage;
        this.minSafeBalance = minSafeBalance;
        this.insufficientBalanceOption = insufficientBalanceOption;
    }

    /**
     * Inicializa un DTO enfocado exclusivamente en la vinculación de instrumentos bancarios.
     *
     * @param id          Identificador único del usuario.
     * @param bankAccount Número de cuenta bancaria o CLABE interbancaria.
     * @param bankName    Nombre de la institución financiera.
     */
    public UserDTO(Long id, String bankAccount, String bankName) {
        this.id = id;
        this.bankAccount = bankAccount;
        this.bankName = bankName;
    }
}