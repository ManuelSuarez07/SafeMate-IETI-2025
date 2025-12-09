import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../models/saving.dart';

/// Widget que representa visualmente una tarjeta detallada para una meta de ahorro [Saving].
///
/// Esta clase es responsable de:
/// 1. Mostrar el progreso actual de la meta mediante barras y porcentajes.
/// 2. Visualizar el estado de la meta (Activa, Completada, Vencida) mediante colores y gradientes dinámicos.
/// 3. Proveer botones de acción rápida para añadir fondos ([onAddAmount]) o ver detalles ([onEdit]).
/// 4. Adaptar su diseño visual basado en la prioridad y el estado de la meta.
class SavingGoalCard extends StatelessWidget {
  /// La meta de ahorro a visualizar.
  final Saving goal;

  /// Callback que se ejecuta al tocar la tarjeta completa.
  final VoidCallback? onTap;

  /// Callback para la acción de añadir fondos rápidamente.
  final VoidCallback? onAddAmount;

  /// Callback para la acción de editar o ver detalles completos.
  final VoidCallback? onEdit;

  /// Determina si se deben mostrar los botones de acción en la parte inferior.
  final bool showActions;

  const SavingGoalCard({
    Key? key,
    required this.goal,
    this.onTap,
    this.onAddAmount,
    this.onEdit,
    this.showActions = true,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      elevation: 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(20),
        child: Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(20),
            gradient: _getCardGradient(),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          goal.name,
                          style: GoogleFonts.poppins(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        if (goal.description != null) ...[
                          const SizedBox(height: 4),
                          Text(
                            goal.description!,
                            style: GoogleFonts.poppins(
                              fontSize: 14,
                              color: Colors.white.withOpacity(0.9),
                            ),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ],
                    ),
                  ),
                  Row(
                    children: [
                      // Badge de estado
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.2),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          goal.statusDisplay,
                          style: GoogleFonts.poppins(
                            fontSize: 10,
                            fontWeight: FontWeight.w600,
                            color: Colors.white,
                          ),
                        ),
                      ),
                      if (goal.isHighPriority) ...[
                        const SizedBox(width: 8),
                        Icon(
                          Icons.priority_high,
                          color: Colors.white,
                          size: 16,
                        ),
                      ],
                    ],
                  ),
                ],
              ),

              const SizedBox(height: 20),

              // Progreso
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        goal.formattedCurrentAmount,
                        style: GoogleFonts.poppins(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                      Text(
                        '${goal.progressPercentage.toStringAsFixed(1)}%',
                        style: GoogleFonts.poppins(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: Colors.white,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  LinearProgressIndicator(
                    value: goal.progressPercentage / 100,
                    backgroundColor: Colors.white.withOpacity(0.3),
                    valueColor: const AlwaysStoppedAnimation<Color>(Colors.white),
                    minHeight: 6,
                  ),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Meta: ${goal.formattedTargetAmount}',
                        style: GoogleFonts.poppins(
                          fontSize: 12,
                          color: Colors.white.withOpacity(0.9),
                        ),
                      ),
                      Text(
                        'Faltan: ${goal.formattedRemainingAmount}',
                        style: GoogleFonts.poppins(
                          fontSize: 12,
                          color: Colors.white.withOpacity(0.9),
                        ),
                      ),
                    ],
                  ),
                ],
              ),

              // Información adicional
              if (_showAdditionalInfo(goal)) ...[
                const SizedBox(height: 16),
                _buildAdditionalInfo(goal),
              ],

              // Acciones
              if (showActions && goal.status == GoalStatus.active) ...[
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: ElevatedButton(
                        onPressed: onAddAmount,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.white,
                          foregroundColor: _getPrimaryColor(),
                          padding: const EdgeInsets.symmetric(vertical: 8),
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(Icons.add, size: 16),
                            const SizedBox(width: 4),
                            Text(
                              'Añadir',
                              style: GoogleFonts.poppins(
                                fontSize: 12,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: OutlinedButton(
                        onPressed: onEdit,
                        style: OutlinedButton.styleFrom(
                          side: const BorderSide(color: Colors.white),
                          padding: const EdgeInsets.symmetric(vertical: 8),
                        ),
                        child: Text(
                          'Detalles',
                          style: GoogleFonts.poppins(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  /// Construye la sección de información adicional si existen datos relevantes.
  ///
  /// Muestra alertas visuales si la meta está vencida ([goal.isOverdue]) o
  /// muestra la fecha de vencimiento y la contribución mensual sugerida.
  Widget _buildAdditionalInfo(Saving goal) {
    if (goal.targetDate != null) {
      return Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(0.1),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          children: [
            Icon(
              goal.isOverdue ? Icons.warning : Icons.schedule,
              color: Colors.white,
              size: 16,
            ),
            const SizedBox(width: 8),
            Text(
              goal.isOverdue
                  ? 'Meta vencida'
                  : 'Vence: ${goal.daysRemainingDisplay}',
              style: GoogleFonts.poppins(
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: Colors.white,
              ),
            ),
            if (goal.monthlyContribution != null) ...[
              const Spacer(),
              Text(
                'Mensual: \$${goal.monthlyContribution!.toStringAsFixed(2)}',
                style: GoogleFonts.poppins(
                  fontSize: 12,
                  color: Colors.white.withOpacity(0.9),
                ),
              ),
            ],
          ],
        ),
      );
    }

    return const SizedBox.shrink();
  }

  /// Determina el gradiente de fondo de la tarjeta basado en el [GoalStatus] y estado de la meta.
  ///
  /// Retorna:
  /// - Verde: Si la meta está completada.
  /// - Naranja: Si la meta está activa pero vencida.
  /// - Color Primario (variable): Si la meta está activa y a tiempo.
  /// - Gris: Si la meta está pausada.
  /// - Rojo: Si la meta está cancelada.
  LinearGradient _getCardGradient() {
    switch (goal.status) {
      case GoalStatus.completed:
        return const LinearGradient(
          colors: [Color(0xFF4CAF50), Color(0xFF45A049)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        );
      case GoalStatus.active:
        if (goal.isOverdue) {
          return const LinearGradient(
            colors: [Color(0xFFFF9800), Color(0xFFF57C00)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          );
        }
        return LinearGradient(
          colors: [_getPrimaryColor(), _getPrimaryColor().withOpacity(0.8)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        );
      case GoalStatus.paused:
        return const LinearGradient(
          colors: [Color(0xFF9E9E9E), Color(0xFF757575)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        );
      case GoalStatus.cancelled:
        return const LinearGradient(
          colors: [Color(0xFFF44336), Color(0xFFD32F2F)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        );
    }
  }

  /// Retorna el color primario visual basado en el nivel de prioridad ([Saving.priorityLevel]).
  ///
  /// Usado para determinar el color base del gradiente en metas activas.
  Color _getPrimaryColor() {
    switch (goal.priorityLevel) {
      case 5:
        return Colors.purple;
      case 4:
        return Colors.red;
      case 3:
        return Colors.orange;
      case 2:
        return Colors.blue;
      case 1:
      default:
        return Colors.teal;
    }
  }

  bool _showAdditionalInfo(Saving goal) {
    return goal.targetDate != null || goal.monthlyContribution != null;
  }
}

/// Versión compacta y resumida de la tarjeta de meta de ahorro.
///
/// Ideal para listas donde el espacio vertical es limitado o para mostrar
/// un resumen rápido del progreso.
///
/// Muestra:
/// - Nombre de la meta.
/// - Porcentaje de progreso (Badge y Barra lineal).
/// - Monto actual y meta total.
class CompactSavingGoalCard extends StatelessWidget {
  final Saving goal;
  final VoidCallback? onTap;

  const CompactSavingGoalCard({
    Key? key,
    required this.goal,
    this.onTap,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(16),
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.05),
                  blurRadius: 10,
                  offset: const Offset(0, 2),
                ),
              ],
              border: Border.all(
                color: goal.statusColor.withOpacity(0.2),
                width: 1,
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Text(
                        goal.name,
                        style: GoogleFonts.poppins(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: goal.statusColor.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        goal.progressPercentage.toStringAsFixed(0) + '%',
                        style: GoogleFonts.poppins(
                          fontSize: 10,
                          fontWeight: FontWeight.w600,
                          color: goal.statusColor,
                        ),
                      ),
                    ),
                  ],
                ),

                const SizedBox(height: 12),

                LinearProgressIndicator(
                  value: goal.progressPercentage / 100,
                  backgroundColor: Colors.grey[200],
                  valueColor: AlwaysStoppedAnimation<Color>(goal.statusColor),
                  minHeight: 4,
                ),

                const SizedBox(height: 8),

                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      goal.formattedCurrentAmount,
                      style: GoogleFonts.poppins(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    Text(
                      goal.formattedTargetAmount,
                      style: GoogleFonts.poppins(
                        fontSize: 12,
                        color: Colors.grey[600],
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}