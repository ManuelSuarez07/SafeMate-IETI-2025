package savemate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import savemate.dto.TransactionDTO;
import savemate.dto.TransactionSummaryDTO;
import savemate.model.Transaction;
import savemate.service.TransactionService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Controlador REST responsable de la gestión de transacciones financieras.
 *
 * <p>Responsabilidad: Exponer endpoints HTTP bajo {@code /api/transactions} para crear,
 * procesar y consultar transacciones del sistema. Traduce solicitudes REST a invocaciones
 * del {@link TransactionService} y devuelve respuestas estándar {@link ResponseEntity}
 * con códigos de estado apropiados.</p>
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Crea una nueva transacción a partir del DTO proporcionado.
     *
     * @param transactionDTO DTO que contiene los datos de la transacción a crear (userId, amount, type, description, etc.)
     * @return {@link ResponseEntity} con el {@link TransactionDTO} creado y HTTP 201 (CREATED) en caso de éxito;
     * HTTP 400 (BAD_REQUEST) si ocurre un error de validación o creación.
     */
    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        log.info("Solicitud para crear transacción para usuario ID: {}", transactionDTO.getUserId());

        try {
            TransactionDTO createdTransaction = transactionService.createTransaction(transactionDTO);
            return new ResponseEntity<>(createdTransaction, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error creando transacción: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Realiza un retiro de fondos para un usuario específico.
     *
     * @param userId identificador del usuario que solicita el retiro
     * @param amount importe a retirar
     * @return {@link ResponseEntity} con el {@link TransactionDTO} del retiro y HTTP 201 (CREATED) en caso de éxito;
     * HTTP 400 (BAD_REQUEST) con mensaje en caso de fondos insuficientes u otro error de negocio.
     */
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdrawFunds(
            @RequestParam Long userId,
            @RequestParam Double amount) {

        log.info("Solicitud de retiro de ${} para usuario ID: {}", amount, userId);

        try {
            TransactionDTO transaction = transactionService.createWithdrawal(userId, amount);
            return new ResponseEntity<>(transaction, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error procesando retiro: {}", e.getMessage());
            // Mensaje si fondos insuficientes
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Procesa una transacción generada a partir de una notificación externa (por ejemplo de banco o proveedor).
     *
     * @param userId identificador del usuario asociado a la transacción
     * @param amount importe de la transacción
     * @param description descripción de la transacción
     * @param merchantName nombre del comercio (opcional)
     * @param notificationSource origen de la notificación (opcional)
     * @param bankReference referencia bancaria asociada (opcional)
     * @return {@link ResponseEntity} con el {@link TransactionDTO} creado y HTTP 201 (CREATED) en caso de éxito;
     * HTTP 400 (BAD_REQUEST) si ocurre un error al procesar la notificación.
     */
    @PostMapping("/from-notification")
    public ResponseEntity<TransactionDTO> processTransactionFromNotification(
            @RequestParam Long userId,
            @RequestParam Double amount,
            @RequestParam String description,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) String notificationSource,
            @RequestParam(required = false) String bankReference) {

        log.info("Procesando transacción desde notificación para usuario ID: {}", userId);

        try {
            TransactionDTO transaction = transactionService.processTransactionFromNotification(
                    userId, amount, description, merchantName, notificationSource, bankReference);
            return new ResponseEntity<>(transaction, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error procesando transacción desde notificación: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Crea un depósito destinado a ahorro para un usuario.
     *
     * @param userId identificador del usuario que realiza el depósito
     * @param amount importe del depósito
     * @param description descripción del depósito (opcional)
     * @return {@link ResponseEntity} con el {@link TransactionDTO} creado y HTTP 201 (CREATED) en caso de éxito;
     * HTTP 400 (BAD_REQUEST) si ocurre un error durante la creación.
     */
    @PostMapping("/saving-deposit")
    public ResponseEntity<TransactionDTO> createSavingDeposit(
            @RequestParam Long userId,
            @RequestParam Double amount,
            @RequestParam(required = false) String description) {

        log.info("Creando depósito de ahorro para usuario ID: {}", userId);

        try {
            TransactionDTO transaction = transactionService.createSavingDeposit(userId, amount, description);
            return new ResponseEntity<>(transaction, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error creando depósito de ahorro: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Procesa las transacciones que se encuentran en estado pendiente para un usuario.
     *
     * @param userId identificador del usuario cuyas transacciones pendientes se procesarán
     * @return {@link ResponseEntity} vacío con HTTP 200 (OK) si la operación se completó correctamente;
     * HTTP 500 (INTERNAL_SERVER_ERROR) si ocurre un error durante el procesamiento.
     */
    @PostMapping("/process-pending/{userId}")
    public ResponseEntity<Void> processPendingTransactions(@PathVariable Long userId) {
        log.info("Procesando transacciones pendientes para usuario ID: {}", userId);

        try {
            transactionService.processPendingTransactions(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error procesando transacciones pendientes: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene una transacción por su identificador.
     *
     * @param id identificador de la transacción solicitada
     * @return {@link ResponseEntity} con el {@link TransactionDTO} y HTTP 200 (OK) si se encuentra;
     * HTTP 404 (NOT_FOUND) si no existe la transacción.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable Long id) {
        log.info("Solicitud para obtener transacción con ID: {}", id);

        Optional<TransactionDTO> transaction = transactionService.getTransactionById(id);
        return transaction.map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Obtiene todas las transacciones de un usuario.
     *
     * @param userId identificador del usuario cuyas transacciones se solicitan
     * @return {@link ResponseEntity} con una lista de {@link TransactionDTO} y HTTP 200 (OK).
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByUserId(@PathVariable Long userId) {
        log.info("Solicitud para obtener transacciones del usuario ID: {}", userId);

        List<TransactionDTO> transactions = transactionService.getTransactionsByUserId(userId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Obtiene las transacciones de un usuario filtradas por tipo.
     *
     * @param userId identificador del usuario
     * @param type tipo de transacción conforme a {@link Transaction.TransactionType}
     * @return {@link ResponseEntity} con una lista de {@link TransactionDTO} filtradas por tipo y HTTP 200 (OK).
     */
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByUserIdAndType(
            @PathVariable Long userId,
            @PathVariable Transaction.TransactionType type) {

        log.info("Solicitud para obtener transacciones del usuario ID: {} con tipo: {}", userId, type);

        List<TransactionDTO> transactions = transactionService.getTransactionsByUserIdAndType(userId, type);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Obtiene las transacciones de un usuario dentro de un rango de fechas.
     *
     * @param userId identificador del usuario
     * @param startDate fecha/hora de inicio (formato ISO_DATE_TIME)
     * @param endDate fecha/hora de fin (formato ISO_DATE_TIME)
     * @return {@link ResponseEntity} con una lista de {@link TransactionDTO} dentro del rango y HTTP 200 (OK).
     */
    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Solicitud para obtener transacciones del usuario ID: {} entre {} y {}", userId, startDate, endDate);

        List<TransactionDTO> transactions = transactionService.getTransactionsByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Calcula el total de gastos de un usuario en un rango de fechas.
     *
     * @param userId identificador del usuario
     * @param startDate fecha/hora de inicio (formato ISO_DATE_TIME)
     * @param endDate fecha/hora de fin (formato ISO_DATE_TIME)
     * @return {@link ResponseEntity} con un {@link Double} que representa el total de gastos; devuelve 0.0 si es nulo.
     */
    @GetMapping("/user/{userId}/statistics/expenses")
    public ResponseEntity<Double> getTotalExpenses(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Solicitud para obtener total de gastos del usuario ID: {} entre {} y {}", userId, startDate, endDate);

        Double totalExpenses = transactionService.getTotalExpenses(userId, startDate, endDate);
        return ResponseEntity.ok(totalExpenses != null ? totalExpenses : 0.0);
    }

    /**
     * Calcula el total de ahorros de un usuario en un rango de fechas.
     *
     * @param userId identificador del usuario
     * @param startDate fecha/hora de inicio (formato ISO_DATE_TIME)
     * @param endDate fecha/hora de fin (formato ISO_DATE_TIME)
     * @return {@link ResponseEntity} con un {@link Double} que representa el total de ahorros; devuelve 0.0 si es nulo.
     */
    @GetMapping("/user/{userId}/statistics/savings")
    public ResponseEntity<Double> getTotalSavings(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Solicitud para obtener total de ahorros del usuario ID: {} entre {} y {}", userId, startDate, endDate);

        Double totalSavings = transactionService.getTotalSavings(userId, startDate, endDate);
        return ResponseEntity.ok(totalSavings != null ? totalSavings : 0.0);
    }

    /**
     * Construye y devuelve un resumen de transacciones, totales y listado para un usuario en un rango de fechas.
     *
     * @param userId identificador del usuario para el cual se genera el resumen
     * @param startDate fecha/hora de inicio (formato ISO_DATE_TIME)
     * @param endDate fecha/hora de fin (formato ISO_DATE_TIME)
     * @return {@link ResponseEntity} con {@link TransactionSummaryDTO} que contiene totales y listado de transacciones;
     * HTTP 200 (OK) en caso de éxito; HTTP 500 (INTERNAL_SERVER_ERROR) en caso de error.
     */
    @GetMapping("/user/{userId}/statistics/summary")
    public ResponseEntity<TransactionSummaryDTO> getTransactionSummary(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Solicitud para obtener resumen de transacciones del usuario ID: {} entre {} y {}", userId, startDate, endDate);

        try {
            Double totalExpenses = transactionService.getTotalExpenses(userId, startDate, endDate);
            Double totalSavings = transactionService.getTotalSavings(userId, startDate, endDate);
            List<TransactionDTO> transactions = transactionService.getTransactionsByDateRange(userId, startDate, endDate);

            TransactionSummaryDTO summary = new TransactionSummaryDTO(totalExpenses, totalSavings, transactions);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error obteniendo resumen de transacciones: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}