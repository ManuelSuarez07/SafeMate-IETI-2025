import 'package:json_annotation/json_annotation.dart';
import 'package:intl/intl.dart';

part 'transaction.g.dart';

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

  String get formattedAmount => '\$${amount.toStringAsFixed(2)}';
  
  String get formattedSavingAmount => savingAmount != null 
      ? '\$${savingAmount!.toStringAsFixed(2)}' 
      : '\$0.00';

  String get formattedDate {
    final date = transactionDate ?? createdAt ?? DateTime.now();
    return DateFormat('dd/MM/yyyy HH:mm').format(date);
  }

  String get formattedShortDate {
    final date = transactionDate ?? createdAt ?? DateTime.now();
    return DateFormat('dd/MM/yyyy').format(date);
  }

  String get transactionTypeDisplay {
    switch (transactionType) {
      case TransactionType.expense:
        return 'Gasto';
      case TransactionType.income:
        return 'Ingreso';
      case TransactionType.saving:
        return 'Ahorro';
      case TransactionType.fee:
        return 'Comisión';
    }
  }

  String get statusDisplay {
    switch (status) {
      case TransactionStatus.pending:
        return 'Pendiente';
      case TransactionStatus.completed:
        return 'Completado';
      case TransactionStatus.failed:
        return 'Fallido';
      case TransactionStatus.cancelled:
        return 'Cancelado';
    }
  }

  Color get statusColor {
    switch (status) {
      case TransactionStatus.pending:
        return Colors.orange;
      case TransactionStatus.completed:
        return Colors.green;
      case TransactionStatus.failed:
        return Colors.red;
      case TransactionStatus.cancelled:
        return Colors.grey;
    }
  }

  Color get transactionTypeColor {
    switch (transactionType) {
      case TransactionType.expense:
        return Colors.red;
      case TransactionType.income:
        return Colors.green;
      case TransactionType.saving:
        return Colors.blue;
      case TransactionType.fee:
        return Colors.purple;
    }
  }

  bool get hasSaving => savingAmount != null && savingAmount! > 0;
  
  bool get isExpense => transactionType == TransactionType.expense;
  
  bool get isIncome => transactionType == TransactionType.income;
  
  bool get isSaving => transactionType == TransactionType.saving;
}

enum TransactionType {
  @JsonValue('EXPENSE')
  expense,
  @JsonValue('INCOME')
  income,
  @JsonValue('SAVING')
  saving,
  @JsonValue('FEE')
  fee,
}

enum TransactionStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('FAILED')
  failed,
  @JsonValue('CANCELLED')
  cancelled,
}

import 'package:flutter/material.dart';