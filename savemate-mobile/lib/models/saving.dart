import 'package:json_annotation/json_annotation.dart';
import 'package:intl/intl.dart';

part 'saving.g.dart';

@JsonSerializable()
class Saving {
  final int? id;
  final int userId;
  final String name;
  final String? description;
  final double targetAmount;
  final double currentAmount;
  final DateTime? targetDate;
  final GoalStatus status;
  final double? monthlyContribution;
  final int priorityLevel;
  final bool isCollaborative;
  final DateTime? createdAt;
  final DateTime? updatedAt;
  final DateTime? completedAt;

  Saving({
    this.id,
    required this.userId,
    required this.name,
    this.description,
    required this.targetAmount,
    this.currentAmount = 0.0,
    this.targetDate,
    this.status = GoalStatus.active,
    this.monthlyContribution,
    this.priorityLevel = 1,
    this.isCollaborative = false,
    this.createdAt,
    this.updatedAt,
    this.completedAt,
  });

  factory Saving.fromJson(Map<String, dynamic> json) => _$SavingFromJson(json);
  Map<String, dynamic> toJson() => _$SavingToJson(this);

  String get formattedTargetAmount => '\$${targetAmount.toStringAsFixed(2)}';
  String get formattedCurrentAmount => '\$${currentAmount.toStringAsFixed(2)}';
  String get formattedRemainingAmount => '\$${remainingAmount.toStringAsFixed(2)}';

  String get formattedTargetDate {
    if (targetDate == null) return 'Sin fecha límite';
    return DateFormat('dd/MM/yyyy').format(targetDate!);
  }

  String get formattedCreatedDate {
    if (createdAt == null) return '';
    return DateFormat('dd/MM/yyyy').format(createdAt!);
  }

  String get statusDisplay {
    switch (status) {
      case GoalStatus.active:
        return 'Activa';
      case GoalStatus.completed:
        return 'Completada';
      case GoalStatus.paused:
        return 'Pausada';
      case GoalStatus.cancelled:
        return 'Cancelada';
    }
  }

  Color get statusColor {
    switch (status) {
      case GoalStatus.active:
        return Colors.blue;
      case GoalStatus.completed:
        return Colors.green;
      case GoalStatus.paused:
        return Colors.orange;
      case GoalStatus.cancelled:
        return Colors.red;
    }
  }

  double get progressPercentage {
    if (targetAmount == 0) return 0.0;
    return (currentAmount / targetAmount) * 100;
  }

  double get remainingAmount => targetAmount - currentAmount;

  bool get isCompleted => currentAmount >= targetAmount;
  
  bool get isOverdue {
    if (targetDate == null) return false;
    return DateTime.now().isAfter(targetDate!) && !isCompleted;
  }

  bool get isHighPriority => priorityLevel >= 3;

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

  int get daysRemaining {
    if (targetDate == null) return -1;
    final now = DateTime.now();
    final difference = targetDate!.difference(now);
    return difference.inDays;
  }

  String get daysRemainingDisplay {
    if (targetDate == null) return 'Sin límite';
    
    final days = daysRemaining;
    if (days < 0) {
      return 'Vencida';
    } else if (days == 0) {
      return 'Vence hoy';
    } else if (days == 1) {
      return 'Mañana';
    } else if (days <= 7) {
      return '$days días';
    } else if (days <= 30) {
      return '${(days / 7).floor()} semanas';
    } else {
      return '${(days / 30).floor()} meses';
    }
  }
}

enum GoalStatus {
  @JsonValue('ACTIVE')
  active,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('PAUSED')
  paused,
  @JsonValue('CANCELLED')
  cancelled,
}

import 'package:flutter/material.dart';