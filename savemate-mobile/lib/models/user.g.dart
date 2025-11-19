// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

User _$UserFromJson(Map<String, dynamic> json) => User(
  id: (json['id'] as num?)?.toInt(),
  email: json['email'] as String,
  firstName: json['firstName'] as String,
  lastName: json['lastName'] as String,
  phoneNumber: json['phoneNumber'] as String?,
  bankAccount: json['bankAccount'] as String?,
  bankName: json['bankName'] as String?,
  savingType:
      $enumDecodeNullable(_$SavingTypeEnumMap, json['savingType']) ??
      SavingType.rounding,
  roundingMultiple: (json['roundingMultiple'] as num?)?.toInt() ?? 1000,
  savingPercentage: (json['savingPercentage'] as num?)?.toDouble() ?? 10.0,
  minSafeBalance: (json['minSafeBalance'] as num?)?.toDouble() ?? 0.0,
  insufficientBalanceOption:
      $enumDecodeNullable(
        _$InsufficientBalanceOptionEnumMap,
        json['insufficientBalanceOption'],
      ) ??
      InsufficientBalanceOption.noSaving,
  totalSaved: (json['totalSaved'] as num?)?.toDouble() ?? 0.0,
  monthlyFeeRate: (json['monthlyFeeRate'] as num?)?.toDouble() ?? 2.5,
  createdAt: json['createdAt'] == null
      ? null
      : DateTime.parse(json['createdAt'] as String),
  updatedAt: json['updatedAt'] == null
      ? null
      : DateTime.parse(json['updatedAt'] as String),
);

Map<String, dynamic> _$UserToJson(User instance) => <String, dynamic>{
  'id': instance.id,
  'email': instance.email,
  'firstName': instance.firstName,
  'lastName': instance.lastName,
  'phoneNumber': instance.phoneNumber,
  'bankAccount': instance.bankAccount,
  'bankName': instance.bankName,
  'savingType': _$SavingTypeEnumMap[instance.savingType]!,
  'roundingMultiple': instance.roundingMultiple,
  'savingPercentage': instance.savingPercentage,
  'minSafeBalance': instance.minSafeBalance,
  'insufficientBalanceOption':
      _$InsufficientBalanceOptionEnumMap[instance.insufficientBalanceOption]!,
  'totalSaved': instance.totalSaved,
  'monthlyFeeRate': instance.monthlyFeeRate,
  'createdAt': instance.createdAt?.toIso8601String(),
  'updatedAt': instance.updatedAt?.toIso8601String(),
};

const _$SavingTypeEnumMap = {
  SavingType.rounding: 'ROUNDING',
  SavingType.percentage: 'PERCENTAGE',
};

const _$InsufficientBalanceOptionEnumMap = {
  InsufficientBalanceOption.noSaving: 'NO_SAVING',
  InsufficientBalanceOption.pending: 'PENDING',
  InsufficientBalanceOption.respectMinBalance: 'RESPECT_MIN_BALANCE',
};
