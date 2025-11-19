// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'saving.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Saving _$SavingFromJson(Map<String, dynamic> json) => Saving(
  id: (json['id'] as num?)?.toInt(),
  userId: (json['userId'] as num).toInt(),
  name: json['name'] as String,
  description: json['description'] as String?,
  targetAmount: (json['targetAmount'] as num).toDouble(),
  currentAmount: (json['currentAmount'] as num?)?.toDouble() ?? 0.0,
  targetDate: json['targetDate'] == null
      ? null
      : DateTime.parse(json['targetDate'] as String),
  status:
      $enumDecodeNullable(_$GoalStatusEnumMap, json['status']) ??
      GoalStatus.active,
  monthlyContribution: (json['monthlyContribution'] as num?)?.toDouble(),
  priorityLevel: (json['priorityLevel'] as num?)?.toInt() ?? 1,
  isCollaborative: json['isCollaborative'] as bool? ?? false,
  createdAt: json['createdAt'] == null
      ? null
      : DateTime.parse(json['createdAt'] as String),
  updatedAt: json['updatedAt'] == null
      ? null
      : DateTime.parse(json['updatedAt'] as String),
  completedAt: json['completedAt'] == null
      ? null
      : DateTime.parse(json['completedAt'] as String),
);

Map<String, dynamic> _$SavingToJson(Saving instance) => <String, dynamic>{
  'id': instance.id,
  'userId': instance.userId,
  'name': instance.name,
  'description': instance.description,
  'targetAmount': instance.targetAmount,
  'currentAmount': instance.currentAmount,
  'targetDate': instance.targetDate?.toIso8601String(),
  'status': _$GoalStatusEnumMap[instance.status]!,
  'monthlyContribution': instance.monthlyContribution,
  'priorityLevel': instance.priorityLevel,
  'isCollaborative': instance.isCollaborative,
  'createdAt': instance.createdAt?.toIso8601String(),
  'updatedAt': instance.updatedAt?.toIso8601String(),
  'completedAt': instance.completedAt?.toIso8601String(),
};

const _$GoalStatusEnumMap = {
  GoalStatus.active: 'ACTIVE',
  GoalStatus.completed: 'COMPLETED',
  GoalStatus.paused: 'PAUSED',
  GoalStatus.cancelled: 'CANCELLED',
};
