package savemate.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad raíz de persistencia que representa al usuario registrado en el ecosistema financiero.
 * <p>
 * Esta clase centraliza tres responsabilidades críticas del dominio:
 * <ol>
 * <li><strong>Identidad y Acceso:</strong> Gestión de credenciales (hash de contraseña) y datos de contacto verificados.</li>
 * <li><strong>Configuración del Motor de Ahorro:</strong> Almacena los parámetros que dictan cómo se ejecutan los algoritmos de ahorro automático (estrategia de redondeo vs. porcentaje, umbrales de seguridad).</li>
 * <li><strong>Relacionamiento:</strong> Actúa como la entidad ancla (Owner) para la integridad referencial de transacciones, metas y recomendaciones de IA.</li>
 * </ol>
 * </p>
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "bank_name")
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SavingType savingType = SavingType.ROUNDING;

    @Column(name = "rounding_multiple")
    private Integer roundingMultiple = 1000;

    @Column(name = "saving_percentage")
    private Double savingPercentage = 10.0;

    @Column(name = "min_safe_balance")
    private Double minSafeBalance = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "insufficient_balance_option")
    private InsufficientBalanceOption insufficientBalanceOption = InsufficientBalanceOption.NO_SAVING;

    @Column(name = "total_saved")
    private Double totalSaved = 0.0;

    @Column(name = "monthly_fee_rate")
    private Double monthlyFeeRate = 2.5;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SavingGoal> savingGoals;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AIRecommendation> aiRecommendations;

    /**
     * Define la estrategia algorítmica fundamental utilizada para calcular el monto a debitar en cada operación.
     * Determina si el sistema opera bajo lógica de redondeo (Spare Change) o deducción proporcional.
     */
    public enum SavingType {
        ROUNDING,
        PERCENTAGE
    }

    /**
     * Establece la política de manejo de excepciones financieras cuando el usuario carece de liquidez.
     * <p>
     * Controla si el sistema debe abortar el ahorro, intentar ejecutarlo parcialmente hasta el límite de seguridad,
     * o encolarlo para un reintento posterior.
     * </p>
     */
    public enum InsufficientBalanceOption {
        NO_SAVING,
        PENDING,
        RESPECT_MIN_BALANCE
    }
}