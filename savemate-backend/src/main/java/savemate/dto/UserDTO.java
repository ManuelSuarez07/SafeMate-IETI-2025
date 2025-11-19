package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import savemate.model.User;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private Long id;
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
    
    // Constructor para creación de usuario (sin password)
    public UserDTO(String email, String firstName, String lastName, String phoneNumber) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }
    
    // Constructor para actualización de configuración
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
    
    // Constructor para vinculación bancaria
    public UserDTO(Long id, String bankAccount, String bankName) {
        this.id = id;
        this.bankAccount = bankAccount;
        this.bankName = bankName;
    }
}