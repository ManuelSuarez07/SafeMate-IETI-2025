package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import savemate.model.Transaction;

import java.time.LocalDateTime;

/**
 * Objeto de Transferencia de Datos (DTO) diseñado para orquestar el flujo de información de movimientos financieros.
 * <p>
 * Esta clase es central para la captura de operaciones monetarias dentro del sistema. Su responsabilidad abarca
 * desde la ingesta de transacciones crudas provenientes de notificaciones bancarias externas, hasta la
 * estructuración de operaciones complejas de "redondeo" (micro-ahorro) y aportes directos.
 * Actúa como puente validado entre los eventos financieros externos y el modelo de dominio de persistencia.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {

    private Long id;
    private Long userId;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    private Double amount;

    @NotNull(message = "La descripción es obligatoria")
    private String description;

    private String merchantName;
    private LocalDateTime transactionDate;

    @NotNull(message = "El tipo de transacción es obligatorio")
    private Transaction.TransactionType transactionType;

    private Transaction.TransactionStatus status = Transaction.TransactionStatus.COMPLETED;

    private Double originalAmount;
    private Double roundedAmount;
    private Double savingAmount;
    private String notificationSource;
    private String bankReference;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Inicializa una nueva transacción basada en la captura de un evento o notificación externa (ej. SMS bancario, Push).
     * <p>
     * Este constructor se utiliza principalmente en la capa de integración para mapear eventos de pasarelas
     * o lectores de notificaciones hacia una estructura comprensible para el sistema, preservando la trazabilidad
     * del origen y la referencia bancaria.
     * </p>
     *
     * @param userId             Identificador del usuario propietario de la transacción.
     * @param amount             Monto real de la transacción reportada.
     * @param description        Detalle o concepto asociado al movimiento.
     * @param merchantName       Nombre del comercio o entidad donde se realizó la operación.
     * @param transactionDate    Fecha y hora exacta reportada por la entidad financiera.
     * @param transactionType    Clasificación del movimiento (INGRESO, GASTO, AHORRO).
     * @param notificationSource Identificador de la fuente de la información (ej. "SMS_PARSER", "WEBHOOK").
     * @param bankReference      Código único o referencia proporcionada por el banco para conciliación.
     */
    public TransactionDTO(Long userId, Double amount, String description,
                          String merchantName, LocalDateTime transactionDate,
                          Transaction.TransactionType transactionType,
                          String notificationSource, String bankReference) {
        this.userId = userId;
        this.amount = amount;
        this.description = description;
        this.merchantName = merchantName;
        this.transactionDate = transactionDate;
        this.transactionType = transactionType;
        this.notificationSource = notificationSource;
        this.bankReference = bankReference;
    }

    /**
     * Instancia una transacción de tipo GASTO que incluye lógica de redondeo automático para ahorro.
     * <p>
     * Este constructor es fundamental para la funcionalidad de "Micro-Ahorro". Registra la compra original
     * y simultáneamente encapsula los cálculos del redondeo. Establece automáticamente la fecha actual
     * y clasifica la operación como {@code EXPENSE}.
     * </p>
     *
     * @param userId         Identificador del usuario.
     * @param originalAmount Monto base de la compra antes del redondeo.
     * @param roundedAmount  Monto total después de aplicar la regla de redondeo (monto superior).
     * @param savingAmount   La diferencia calculada ({@code roundedAmount - originalAmount}) destinada al ahorro.
     * @param description    Descripción de la compra.
     * @param merchantName   Nombre del comercio.
     */
    public TransactionDTO(Long userId, Double originalAmount, Double roundedAmount,
                          Double savingAmount, String description, String merchantName) {
        this.userId = userId;
        this.originalAmount = originalAmount;
        this.roundedAmount = roundedAmount;
        this.savingAmount = savingAmount;
        this.amount = originalAmount;
        this.description = description;
        this.merchantName = merchantName;
        this.transactionType = Transaction.TransactionType.EXPENSE;
        this.transactionDate = LocalDateTime.now();
    }

    /**
     * Crea una transacción simplificada dedicada exclusivamente a un aporte de ahorro (Depósito).
     * <p>
     * Configura automáticamente el tipo de transacción como {@code SAVING} y establece la marca de tiempo actual.
     * Ideal para aportes voluntarios manuales o transferencias automáticas a metas.
     * </p>
     *
     * @param userId       Identificador del usuario que realiza el ahorro.
     * @param savingAmount Monto total a ingresar en el fondo de ahorro.
     * @param description  Nota o motivo del ahorro.
     */
    public TransactionDTO(Long userId, Double savingAmount, String description) {
        this.userId = userId;
        this.amount = savingAmount;
        this.description = description;
        this.transactionType = Transaction.TransactionType.SAVING;
        this.transactionDate = LocalDateTime.now();
    }
}