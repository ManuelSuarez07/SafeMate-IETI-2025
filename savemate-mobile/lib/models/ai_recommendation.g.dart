// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'ai_recommendation.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AIRecommendation _$AIRecommendationFromJson(Map<String, dynamic> json) =>
    AIRecommendation(
      id: (json['id'] as num?)?.toInt(),
      userId: (json['userId'] as num).toInt(),
      recommendationType: $enumDecode(
        _$RecommendationTypeEnumMap,
        json['recommendationType'],
      ),
      title: json['title'] as String,
      description: json['description'] as String,
      actionText: json['actionText'] as String?,
      potentialSavings: (json['potentialSavings'] as num?)?.toDouble(),
      confidenceScore: (json['confidenceScore'] as num?)?.toDouble(),
      status:
          $enumDecodeNullable(_$RecommendationStatusEnumMap, json['status']) ??
          RecommendationStatus.pending,
      isApplied: json['isApplied'] as bool? ?? false,
      appliedAt: json['appliedAt'] == null
          ? null
          : DateTime.parse(json['appliedAt'] as String),
      expiresAt: json['expiresAt'] == null
          ? null
          : DateTime.parse(json['expiresAt'] as String),
      category: json['category'] as String?,
      priorityLevel: (json['priorityLevel'] as num?)?.toInt() ?? 1,
      createdAt: json['createdAt'] == null
          ? null
          : DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$AIRecommendationToJson(AIRecommendation instance) =>
    <String, dynamic>{
      'id': instance.id,
      'userId': instance.userId,
      'recommendationType':
          _$RecommendationTypeEnumMap[instance.recommendationType]!,
      'title': instance.title,
      'description': instance.description,
      'actionText': instance.actionText,
      'potentialSavings': instance.potentialSavings,
      'confidenceScore': instance.confidenceScore,
      'status': _$RecommendationStatusEnumMap[instance.status]!,
      'isApplied': instance.isApplied,
      'appliedAt': instance.appliedAt?.toIso8601String(),
      'expiresAt': instance.expiresAt?.toIso8601String(),
      'category': instance.category,
      'priorityLevel': instance.priorityLevel,
      'createdAt': instance.createdAt?.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

const _$RecommendationTypeEnumMap = {
  RecommendationType.spendingPattern: 'SPENDING_PATTERN',
  RecommendationType.savingOptimization: 'SAVING_OPTIMIZATION',
  RecommendationType.goalAdjustment: 'GOAL_ADJUSTMENT',
  RecommendationType.roundingConfig: 'ROUNDING_CONFIG',
  RecommendationType.percentageConfig: 'PERCENTAGE_CONFIG',
  RecommendationType.expenseReduction: 'EXPENSE_REDUCTION',
  RecommendationType.incomeIncrease: 'INCOME_INCREASE',
};

const _$RecommendationStatusEnumMap = {
  RecommendationStatus.pending: 'PENDING',
  RecommendationStatus.viewed: 'VIEWED',
  RecommendationStatus.applied: 'APPLIED',
  RecommendationStatus.rejected: 'REJECTED',
  RecommendationStatus.expired: 'EXPIRED',
};
