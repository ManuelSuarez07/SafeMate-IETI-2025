import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';

import '../services/auth_service.dart';
import '../services/api_service.dart';
import '../models/saving.dart';

class SavingsScreen extends StatefulWidget {
  const SavingsScreen({Key? key}) : super(key: key);

  @override
  State<SavingsScreen> createState() => _SavingsScreenState();
}

class _SavingsScreenState extends State<SavingsScreen> {
  List<Saving> _allGoals = [];
  List<Saving> _activeGoals = [];
  List<Saving> _completedGoals = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadGoals();
  }

  Future<void> _loadGoals() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final apiService = Provider.of<ApiService>(context, listen: false);

      if (authService.user?.id != null) {
        final goals = await apiService.getSavingGoalsByUserId(authService.user!.id!);
        
        _allGoals = goals;
        _activeGoals = goals.where((g) => g.status == GoalStatus.active).toList();
        _completedGoals = goals.where((g) => g.status == GoalStatus.completed).toList();
      }
    } catch (e) {
      print('Error loading goals: $e');
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          'Metas de Ahorro',
          style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
        ),
        actions: [
          IconButton(
            onPressed: _showStatistics,
            icon: const Icon(Icons.bar_chart),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadGoals,
        child: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : SingleChildScrollView(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Resumen
                    _buildSummaryCard(),
                    
                    const SizedBox(height: 24),
                    
                    // Metas activas
                    _buildSectionTitle('Metas Activas', _activeGoals.length),
                    const SizedBox(height: 16),
                    if (_activeGoals.isEmpty)
                      _buildEmptyState('No hay metas activas', 'Crea tu primera meta de ahorro')
                    else
                      ..._activeGoals.map((goal) => _buildGoalCard(goal)).toList(),
                    
                    const SizedBox(height: 24),
                    
                    // Metas completadas
                    if (_completedGoals.isNotEmpty) ...[
                      _buildSectionTitle('Metas Completadas', _completedGoals.length),
                      const SizedBox(height: 16),
                      ..._completedGoals.map((goal) => _buildGoalCard(goal)).toList(),
                    ],
                    
                    const SizedBox(height: 100),
                  ],
                ),
              ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddGoalDialog,
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildSummaryCard() {
    final totalSaved = _allGoals.fold<double>(0, (sum, goal) => sum + goal.currentAmount);
    final totalTarget = _allGoals.fold<double>(0, (sum, goal) => sum + goal.targetAmount);
    final overallProgress = totalTarget > 0 ? (totalSaved / totalTarget) * 100 : 0;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            Theme.of(context).primaryColor,
            Theme.of(context).primaryColor.withOpacity(0.8),
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Theme.of(context).primaryColor.withOpacity(0.3),
            blurRadius: 10,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Resumen de Ahorros',
            style: GoogleFonts.poppins(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _buildSummaryItem(
                  'Total Ahorrado',
                  '\$${totalSaved.toStringAsFixed(2)}',
                  Colors.white,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: _buildSummaryItem(
                  'Meta Total',
                  '\$${totalTarget.toStringAsFixed(2)}',
                  Colors.white.withOpacity(0.9),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Progreso General',
                    style: GoogleFonts.poppins(
                      fontSize: 14,
                      color: Colors.white.withOpacity(0.9),
                    ),
                  ),
                  Text(
                    '${overallProgress.toStringAsFixed(1)}%',
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
                value: overallProgress / 100,
                backgroundColor: Colors.white.withOpacity(0.3),
                valueColor: const AlwaysStoppedAnimation<Color>(Colors.white),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSummaryItem(String label, String value, Color color) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: GoogleFonts.poppins(
            fontSize: 12,
            color: color.withOpacity(0.9),
          ),
        ),
        Text(
          value,
          style: GoogleFonts.poppins(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
      ],
    );
  }

  Widget _buildSectionTitle(String title, int count) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          title,
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.w600,
          ),
        ),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: Theme.of(context).primaryColor.withOpacity(0.1),
            borderRadius: BorderRadius.circular(20),
          ),
          child: Text(
            '$count',
            style: GoogleFonts.poppins(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: Theme.of(context).primaryColor,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildEmptyState(String title, String subtitle) {
    return Container(
      padding: const EdgeInsets.all(40),
      decoration: BoxDecoration(
        color: Colors.grey[50],
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey[200]!),
      ),
      child: Column(
        children: [
          Icon(
            Icons.savings_outlined,
            size: 64,
            color: Colors.grey[400],
          ),
          const SizedBox(height: 16),
          Text(
            title,
            style: GoogleFonts.poppins(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: Colors.grey[600],
            ),
          ),
          const SizedBox(height: 8),
          Text(
            subtitle,
            style: GoogleFonts.poppins(
              fontSize: 14,
              color: Colors.grey[500],
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildGoalCard(Saving goal) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
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
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    if (goal.description != null) ...[
                      const SizedBox(height: 4),
                      Text(
                        goal.description!,
                        style: GoogleFonts.poppins(
                          fontSize: 14,
                          color: Colors.grey[600],
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: goal.statusColor.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      goal.statusDisplay,
                      style: GoogleFonts.poppins(
                        fontSize: 10,
                        fontWeight: FontWeight.w500,
                        color: goal.statusColor,
                      ),
                    ),
                  ),
                  if (goal.isHighPriority) ...[
                    const SizedBox(width: 8),
                    Icon(
                      Icons.priority_high,
                      color: Colors.red,
                      size: 16,
                    ),
                  ],
                ],
              ),
            ],
          ),
          
          const SizedBox(height: 16),
          
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
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  Text(
                    goal.formattedTargetAmount,
                    style: GoogleFonts.poppins(
                      fontSize: 14,
                      color: Colors.grey[600],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              LinearProgressIndicator(
                value: goal.progressPercentage / 100,
                backgroundColor: Colors.grey[200],
                valueColor: AlwaysStoppedAnimation<Color>(goal.statusColor),
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '${goal.progressPercentage.toStringAsFixed(1)}% completado',
                    style: GoogleFonts.poppins(
                      fontSize: 12,
                      color: Colors.grey[600],
                    ),
                  ),
                  Text(
                    'Faltan ${goal.formattedRemainingAmount}',
                    style: GoogleFonts.poppins(
                      fontSize: 12,
                      color: Colors.grey[600],
                    ),
                  ),
                ],
              ),
            ],
          ),
          
          if (goal.targetDate != null) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: goal.isOverdue 
                    ? Colors.red.withOpacity(0.1) 
                    : Colors.blue.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  Icon(
                    goal.isOverdue ? Icons.warning : Icons.schedule,
                    color: goal.isOverdue ? Colors.red : Colors.blue,
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
                      color: goal.isOverdue ? Colors.red : Colors.blue,
                    ),
                  ),
                ],
              ),
            ),
          ],
          
          const SizedBox(height: 16),
          
          // Acciones
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: () => _showAddAmountDialog(goal),
                  style: OutlinedButton.styleFrom(
                    side: BorderSide(color: Theme.of(context).primaryColor),
                  ),
                  child: Text(
                    'Añadir Ahorro',
                    style: GoogleFonts.poppins(
                      fontSize: 12,
                      color: Theme.of(context).primaryColor,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton(
                  onPressed: () => _showGoalDetails(goal),
                  child: Text(
                    'Ver Detalles',
                    style: GoogleFonts.poppins(fontSize: 12),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  void _showAddGoalDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(
          'Nueva Meta de Ahorro',
          style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
        ),
        content: Text(
          'Funcionalidad en desarrollo',
          style: GoogleFonts.poppins(),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cerrar'),
          ),
        ],
      ),
    );
  }

  void _showAddAmountDialog(Saving goal) {
    final controller = TextEditingController();
    
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(
          'Añadir Ahorro',
          style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Meta: ${goal.name}',
              style: GoogleFonts.poppins(),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: controller,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(
                labelText: 'Monto a añadir',
                prefixText: '\$',
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            onPressed: () async {
              final amount = double.tryParse(controller.text);
              if (amount != null && amount > 0) {
                await _addAmountToGoal(goal, amount);
                Navigator.of(context).pop();
              }
            },
            child: const Text('Añadir'),
          ),
        ],
      ),
    );
  }

  Future<void> _addAmountToGoal(Saving goal, double amount) async {
    try {
      final apiService = Provider.of<ApiService>(context, listen: false);
      await apiService.updateSavingGoalProgress(goal.id!, amount);
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Se han añadido \$${amount.toStringAsFixed(2)} a tu meta'),
          backgroundColor: Colors.green,
        ),
      );
      
      _loadGoals();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error al añadir ahorro: $e'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  void _showGoalDetails(Saving goal) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(
          goal.name,
          style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
        ),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              if (goal.description != null) ...[
                Text(goal.description!),
                const SizedBox(height: 16),
              ],
              _buildDetailRow('Estado', goal.statusDisplay),
              _buildDetailRow('Prioridad', goal.priorityDisplay),
              _buildDetailRow('Actual', goal.formattedCurrentAmount),
              _buildDetailRow('Meta', goal.formattedTargetAmount),
              _buildDetailRow('Restante', goal.formattedRemainingAmount),
              _buildDetailRow('Progreso', '${goal.progressPercentage.toStringAsFixed(1)}%'),
              if (goal.targetDate != null)
                _buildDetailRow('Fecha límite', goal.formattedTargetDate),
              if (goal.monthlyContribution != null)
                _buildDetailRow('Ahorro mensual', '\$${goal.monthlyContribution!.toStringAsFixed(2)}'),
              _buildDetailRow('Colaborativa', goal.isCollaborative ? 'Sí' : 'No'),
              if (goal.createdAt != null)
                _buildDetailRow('Creada', goal.formattedCreatedDate),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cerrar'),
          ),
        ],
      ),
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: GoogleFonts.poppins(
              fontSize: 14,
              color: Colors.grey[600],
            ),
          ),
          Text(
            value,
            style: GoogleFonts.poppins(
              fontSize: 14,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  void _showStatistics() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(
          'Estadísticas de Ahorro',
          style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
        ),
        content: Text(
          'Funcionalidad en desarrollo',
          style: GoogleFonts.poppins(),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cerrar'),
          ),
        ],
      ),
    );
  }
}