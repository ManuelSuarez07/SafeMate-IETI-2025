import 'package:json_annotation/json_annotation.dart';
import 'package:intl/intl.dart';

part 'ai_recommendation.g.dart';

@JsonSerializable()
class AIRecommendation {
  final int? id;
  final int userId;
  final RecommendationType recommendationType;
  final String title;
  final String description;
  final String? actionText;
  final double? potentialSavings;
  final double? confidenceScore;
  final RecommendationStatus status;
  final bool isApplied;
  final DateTime? appliedAt;
  final DateTime? expiresAt;
  final String? category;
  final int priorityLevel;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  AIRecommendation({
    this.id,
    required this.userId,
    required this.recommendationType,
    required this.title,
    required this.description,
    this.actionText,
    this.potentialSavings,
    this.confidenceScore,
    this.status = RecommendationStatus.pending,
    this.isApplied = false,
    this.appliedAt,
    this.expiresAt,
    this.category,
    this.priorityLevel = 1,
    this.createdAt,
    this.updatedAt,
  });

  factory AIRecommendation.fromJson(Map<String, dynamic> json) => _$AIRecommendationFromJson(json);
  Map<String, dynamic> toJson() => _$AIRecommendationToJson(this);

  String get formattedPotentialSavings => potentialSavings != null 
      ? '\$${potentialSavings!.toStringAsFixed(2)}' 
      : 'N/A';

  String get formattedConfidenceScore => confidenceScore != null 
      ? '${(confidenceScore! * 100).toStringAsFixed(0)}%' 
      : 'N/A';

  String get formattedCreatedDate {
    if (createdAt == null) return '';
    return DateFormat('dd/MM/yyyy').format(createdAt!);
  }

  String get formattedExpiresAt {
    if (expiresAt == null) return 'No expira';
    return DateFormat('dd/MM/yyyy').format(expiresAt!);
  }

  String get recommendationTypeDisplay {
    switch (recommendationType) {
      case RecommendationType.spendingPattern:
        return 'Patrón de Gasto';
      case RecommendationType.savingOptimization:
        return 'Optimización de Ahorro';
      case RecommendationType.goalAdjustment:
        return 'Ajuste de Meta';
      case RecommendationType.roundingConfig:
        return 'Config. Redondeo';
      case RecommendationType.percentageConfig:
        return 'Config. Porcentaje';
      case RecommendationType.expenseReduction:
        return 'Reducción de Gastos';
      case RecommendationType.incomeIncrease:
        return 'Aumento de Ingresos';
    }
  }

  String get statusDisplay {
    switch (status) {
      case RecommendationStatus.pending:
        return 'Pendiente';
      case RecommendationStatus.viewed:
        return 'Vista';
      case RecommendationStatus.applied:
        return 'Aplicada';
      case RecommendationStatus.rejected:
        return 'Rechazada';
      case RecommendationStatus.expired:
        return 'Expirada';
    }
  }

  Color get statusColor {
    switch (status) {
      case RecommendationStatus.pending:
        return Colors.blue;
      case RecommendationStatus.viewed:
        return Colors.grey;
      case RecommendationStatus.applied:
        return Colors.green;
      case RecommendationStatus.rejected:
        return Colors.red;
      case RecommendationStatus.expired:
        return Colors.orange;
    }
  }

  Color get typeColor {
    switch (recommendationType) {
      case RecommendationType.spendingPattern:
        return Colors.purple;
      case RecommendationType.savingOptimization:
        return Colors.green;
      case RecommendationType.goalAdjustment:
        return Colors.blue;
      case RecommendationType.roundingConfig:
        return Colors.orange;
      case RecommendationType.percentageConfig:
        return Colors.teal;
      case RecommendationType.expenseReduction:
        return Colors.red;
      case RecommendationType.incomeIncrease:
        return Colors.indigo;
    }
  }

  bool get isExpired {
    if (expiresAt == null) return false;
    return DateTime.now().isAfter(expiresAt!);
  }

  bool get isHighPriority => priorityLevel >= 3;

  bool get hasHighConfidence => confidenceScore != null && confidenceScore! >= 0.8;

  bool get hasPotentialSavings => potentialSavings != null && potentialSavings! > 0;

  String get priorityDisplay {
    switch (priorityLevel) {
      case 1:
        return 'Baja';
      case 2:
        return 'Media';
      case 3:
        return 'Alta';
      case 4:
        return 'Muy Alta';
      case 5:
        return 'Urgente';
      default:
        return 'Normal';
    }
  }

  Color get priorityColor {
    switch (priorityLevel) {
      case 1:
        return Colors.grey;
      case 2:
        return Colors.blue;
      case 3:
        return Colors.orange;
      case 4:
        return Colors.red;
      case 5:
        return Colors.purple;
      default:
        return Colors.grey;
    }
  }

  String get categoryDisplay {
    if (category == null || category!.isEmpty) {
      return 'General';
    }
    return category!;
  }
}

enum RecommendationType {
  @JsonValue('SPENDING_PATTERN')
  spendingPattern,
  @JsonValue('SAVING_OPTIMIZATION')
  savingOptimization,
  @JsonValue('GOAL_ADJUSTMENT')
  goalAdjustment,
  @JsonValue('ROUNDING_CONFIG')
  roundingConfig,
  @JsonValue('PERCENTAGE_CONFIG')
  percentageConfig,
  @JsonValue('EXPENSE_REDUCTION')
  expenseReduction,
  @JsonValue('INCOME_INCREASE')
  incomeIncrease,
}

enum RecommendationStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('VIEWED')
  viewed,
  @JsonValue('APPLIED')
  applied,
  @JsonValue('REJECTED')
  rejected,
  @JsonValue('EXPIRED')
  expired,
}

import 'package:flutter/material.dart';