// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'transaction.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Transaction _$TransactionFromJson(Map<String, dynamic> json) => Transaction(
  id: (json['id'] as num?)?.toInt(),
  userId: (json['userId'] as num).toInt(),
  amount: (json['amount'] as num).toDouble(),
  description: json['description'] as String,
  merchantName: json['merchantName'] as String?,
  transactionDate: json['transactionDate'] == null
      ? null
      : DateTime.parse(json['transactionDate'] as String),
  transactionType: $enumDecode(
    _$TransactionTypeEnumMap,
    json['transactionType'],
  ),
  status:
      $enumDecodeNullable(_$TransactionStatusEnumMap, json['status']) ??
      TransactionStatus.completed,
  originalAmount: (json['originalAmount'] as num?)?.toDouble(),
  roundedAmount: (json['roundedAmount'] as num?)?.toDouble(),
  savingAmount: (json['savingAmount'] as num?)?.toDouble(),
  notificationSource: json['notificationSource'] as String?,
  bankReference: json['bankReference'] as String?,
  createdAt: json['createdAt'] == null
      ? null
      : DateTime.parse(json['createdAt'] as String),
  updatedAt: json['updatedAt'] == null
      ? null
      : DateTime.parse(json['updatedAt'] as String),
);

Map<String, dynamic> _$TransactionToJson(Transaction instance) =>
    <String, dynamic>{
      'id': instance.id,
      'userId': instance.userId,
      'amount': instance.amount,
      'description': instance.description,
      'merchantName': instance.merchantName,
      'transactionDate': instance.transactionDate?.toIso8601String(),
      'transactionType': _$TransactionTypeEnumMap[instance.transactionType]!,
      'status': _$TransactionStatusEnumMap[instance.status]!,
      'originalAmount': instance.originalAmount,
      'roundedAmount': instance.roundedAmount,
      'savingAmount': instance.savingAmount,
      'notificationSource': instance.notificationSource,
      'bankReference': instance.bankReference,
      'createdAt': instance.createdAt?.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

const _$TransactionTypeEnumMap = {
  TransactionType.expense: 'EXPENSE',
  TransactionType.income: 'INCOME',
  TransactionType.saving: 'SAVING',
  TransactionType.fee: 'FEE',
};

const _$TransactionStatusEnumMap = {
  TransactionStatus.pending: 'PENDING',
  TransactionStatus.completed: 'COMPLETED',
  TransactionStatus.failed: 'FAILED',
  TransactionStatus.cancelled: 'CANCELLED',
};
