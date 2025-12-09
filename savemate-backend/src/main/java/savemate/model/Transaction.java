package savemate.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidad de persistencia que representa un registro inmutable de un movimiento financiero individual.
 * <p>
 * Esta clase constituye el núcleo del historial financiero del usuario. Su responsabilidad es almacenar
 * la evidencia transaccional con alto nivel de detalle, soportando tanto operaciones estándar (ingresos/egresos)
 * como la lógica específica de "Micro-Ahorro" (almacenando el desglose entre monto original, redondeo y ahorro).
 * Integra mecanismos de auditoría automática para garantizar la trazabilidad temporal de la creación y modificación de registros.
 * </p>
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String description;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @Column(name = "original_amount")
    private Double originalAmount;

    @Column(name = "rounded_amount")
    private Double roundedAmount;

    @Column(name = "saving_amount")
    private Double savingAmount;

    @Column(name = "notification_source")
    private String notificationSource;

    @Column(name = "bank_reference")
    private String bankReference;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Categorización contable que define la naturaleza y dirección del flujo monetario.
     * Utilizada para la generación de balances, gráficos de gastos y aplicación de reglas de ahorro.
     */
    public enum TransactionType {
        EXPENSE,
        INCOME,
        SAVING,
        FEE,
        WITHDRAWAL
    }

    /**
     * Define los estados posibles en el ciclo de vida de procesamiento de una transacción.
     * Fundamental para garantizar la consistencia eventual en procesos asíncronos (como la lectura de SMS)
     * y la conciliación bancaria.
     */
    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}