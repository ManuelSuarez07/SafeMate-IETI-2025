import 'package:json_annotation/json_annotation.dart';

part 'user.g.dart';

@JsonSerializable()
class User {
  final int? id;
  final String username;
  final String email;
  final String firstName;
  final String lastName;
  final String? phoneNumber;
  final String? bankAccount;
  final String? bankName;
  final SavingType savingType;
  final int roundingMultiple;
  final double savingPercentage;
  final double minSafeBalance;
  final InsufficientBalanceOption insufficientBalanceOption;
  final double totalSaved;
  final double monthlyFeeRate;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  User({
    this.id,
    required this.username,
    required this.email,
    required this.firstName,
    required this.lastName,
    this.phoneNumber,
    this.bankAccount,
    this.bankName,
    this.savingType = SavingType.rounding,
    this.roundingMultiple = 1000,
    this.savingPercentage = 10.0,
    this.minSafeBalance = 0.0,
    this.insufficientBalanceOption = InsufficientBalanceOption.noSaving,
    this.totalSaved = 0.0,
    this.monthlyFeeRate = 2.5,
    this.createdAt,
    this.updatedAt,
  });

  factory User.fromJson(Map<String, dynamic> json) => _$UserFromJson(json);

  Map<String, dynamic> toJson() {
    return {
      if (id != null) 'id': id,
      'username': username,
      'email': email,
      'firstName': firstName,
      'lastName': lastName,
      'phoneNumber': phoneNumber,
      'bankAccount': bankAccount,
      'bankName': bankName,
      'savingType': savingType == SavingType.rounding ? 'ROUNDING' : 'PERCENTAGE',
      'roundingMultiple': roundingMultiple,
      'savingPercentage': savingPercentage,
      'minSafeBalance': minSafeBalance,
      'insufficientBalanceOption': _getInsufficientBalanceOptionString(),
      'totalSaved': totalSaved,
      'monthlyFeeRate': monthlyFeeRate,
      if (createdAt != null) 'createdAt': createdAt!.toIso8601String(),
      if (updatedAt != null) 'updatedAt': updatedAt!.toIso8601String(),
    };
  }

  String _getInsufficientBalanceOptionString() {
    switch (insufficientBalanceOption) {
      case InsufficientBalanceOption.noSaving:
        return 'NO_SAVING';
      case InsufficientBalanceOption.pending:
        return 'PENDING';
      case InsufficientBalanceOption.respectMinBalance:
        return 'RESPECT_MIN_BALANCE';
    }
  }

  // --- Getters para la UI ---

  String get fullName => '$firstName $lastName';

  String get totalSavedDisplay {
    return '\$${totalSaved.toStringAsFixed(2)}';
  }

  String get savingTypeDisplay {
    switch (savingType) {
      case SavingType.rounding:
        return 'Redondeo';
      case SavingType.percentage:
        return 'Porcentaje';
    }
  }

  String get insufficientBalanceOptionDisplay {
    switch (insufficientBalanceOption) {
      case InsufficientBalanceOption.noSaving:
        return 'No ahorrar';
      case InsufficientBalanceOption.pending:
        return 'Pendiente';
      case InsufficientBalanceOption.respectMinBalance:
        return 'Respetar saldo mínimo';
    }
  }
}

enum SavingType {
  @JsonValue('ROUNDING')
  rounding,
  @JsonValue('PERCENTAGE')
  percentage,
}

enum InsufficientBalanceOption {
  @JsonValue('NO_SAVING')
  noSaving,
  @JsonValue('PENDING')
  pending,
  @JsonValue('RESPECT_MIN_BALANCE')
  respectMinBalance,
}