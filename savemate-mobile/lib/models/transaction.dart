import 'package:flutter/material.dart';
import 'package:json_annotation/json_annotation.dart';
import 'package:intl/intl.dart';

part 'transaction.g.dart';

enum TransactionType {
  @JsonValue('EXPENSE')
  expense,
  @JsonValue('INCOME')
  income,
  @JsonValue('SAVING')
  saving,
  @JsonValue('FEE')
  fee,
  @JsonValue('WITHDRAWAL')
  withdrawal
}

enum TransactionStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('FAILED')
  failed,
  @JsonValue('CANCELLED')
  cancelled
}

@JsonSerializable()
class Transaction {
  final int? id;
  final int userId;
  final double amount;
  final String description;
  final String? merchantName;
  final DateTime? transactionDate;
  final TransactionType transactionType;
  final TransactionStatus status;

  final double? originalAmount;
  final double? roundedAmount;
  final double? savingAmount;

  final String? notificationSource;
  final String? bankReference;

  final DateTime? createdAt;
  final DateTime? updatedAt;

  Transaction({
    this.id,
    required this.userId,
    required this.amount,
    required this.description,
    this.merchantName,
    this.transactionDate,
    required this.transactionType,
    this.status = TransactionStatus.completed,
    this.originalAmount,
    this.roundedAmount,
    this.savingAmount,
    this.notificationSource,
    this.bankReference,
    this.createdAt,
    this.updatedAt,
  });

  factory Transaction.fromJson(Map<String, dynamic> json) => _$TransactionFromJson(json);
  Map<String, dynamic> toJson() => _$TransactionToJson(this);

  // --- UI Helpers ---

  String get formattedAmount {
    final currencyFormat = NumberFormat.currency(symbol: '\$', decimalDigits: 0);
    return currencyFormat.format(amount);
  }

  String get formattedSavingAmount {
    if (savingAmount == null) return '';
    final currencyFormat = NumberFormat.currency(symbol: '\$', decimalDigits: 0);
    return currencyFormat.format(savingAmount);
  }

  String get formattedDate {
    if (transactionDate == null) return '';
    return DateFormat('dd MMM yyyy, hh:mm a').format(transactionDate!);
  }

  String get formattedShortDate {
    if (transactionDate == null) return '';
    // Ejemplo: "Hoy, 10:30 AM" o "25 Nov"
    final now = DateTime.now();
    final diff = now.difference(transactionDate!);

    if (diff.inDays == 0 && now.day == transactionDate!.day) {
      return 'Hoy, ${DateFormat('hh:mm a').format(transactionDate!)}';
    } else if (diff.inDays == 1) {
      return 'Ayer, ${DateFormat('hh:mm a').format(transactionDate!)}';
    } else {
      return DateFormat('dd MMM, hh:mm a').format(transactionDate!);
    }
  }

  bool get hasSaving => savingAmount != null && savingAmount! > 0;

  // Helper getters for logic
  bool get isExpense => transactionType == TransactionType.expense;
  bool get isIncome => transactionType == TransactionType.income;
  bool get isSaving => transactionType == TransactionType.saving;
  bool get isWithdrawal => transactionType == TransactionType.withdrawal;

  Color get transactionTypeColor {
    switch (transactionType) {
      case TransactionType.expense:
        return Colors.orange;
      case TransactionType.income:
        return Colors.green;
      case TransactionType.saving:
        return Colors.blue;
      case TransactionType.fee:
        return Colors.red;
      case TransactionType.withdrawal:
        return Colors.purple;
    }
  }

  String get typeDisplay {
    switch (transactionType) {
      case TransactionType.expense: return 'Gasto';
      case TransactionType.income: return 'Ingreso';
      case TransactionType.saving: return 'Ahorro';
      case TransactionType.fee: return 'Tarifa';
      case TransactionType.withdrawal: return 'Retiro';
    }
  }

  String get statusDisplay {
    switch (status) {
      case TransactionStatus.pending: return 'Pendiente';
      case TransactionStatus.completed: return 'Completado';
      case TransactionStatus.failed: return 'Fallido';
      case TransactionStatus.cancelled: return 'Cancelado';
    }
  }

  Color get statusColor {
    switch (status) {
      case TransactionStatus.pending: return Colors.orange;
      case TransactionStatus.completed: return Colors.green;
      case TransactionStatus.failed: return Colors.red;
      case TransactionStatus.cancelled: return Colors.grey;
    }
  }
}