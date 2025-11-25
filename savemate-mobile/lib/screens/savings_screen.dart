import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';

import '../services/auth_service.dart';
import '../services/api_service.dart';
import '../models/saving.dart';

class SavingsScreen extends StatefulWidget {
  const SavingsScreen({Key? key}) : super(key: key);

  @override
  State<SavingsScreen> createState() => SavingsScreenState();
}

class SavingsScreenState extends State<SavingsScreen> {
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
    if (!mounted) return;
    setState(() {
      _isLoading = true;
    });

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final apiService = Provider.of<ApiService>(context, listen: false);

      if (authService.user?.id != null) {
        final goals = await apiService.getSavingGoalsByUserId(authService.user!.id!);

        if (mounted) {
          setState(() {
            _allGoals = goals;
            _activeGoals = goals.where((g) => g.status == GoalStatus.active).toList();
            _completedGoals = goals.where((g) => g.status == GoalStatus.completed).toList();
            _isLoading = false;
          });
        }
      }
    } catch (e) {
      print('Error loading goals: $e');
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
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
            onPressed: showStatistics,
            icon: const Icon(Icons.bar_chart),
            tooltip: 'Ver estadísticas',
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadGoals,
        child: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
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

              // Espacio extra para el FAB
              const SizedBox(height: 100),
            ],
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: showAddGoalDialog,
        backgroundColor: Theme.of(context).primaryColor,
        child: const Icon(Icons.add, color: Colors.white),
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

  // --- MÉTODOS PÚBLICOS Y DIÁLOGOS ---

  void showAddGoalDialog() {
    showDialog(
      context: context,
      builder: (context) => const AddSavingGoalDialog(),
    ).then((value) {
      if (value == true) {
        _loadGoals(); // Recargar si se creó exitosamente
      }
    });
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
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Meta: ${goal.name}',
              style: GoogleFonts.poppins(color: Colors.grey[700]),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: controller,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              style: GoogleFonts.poppins(fontSize: 18),
              decoration: InputDecoration(
                labelText: 'Monto a añadir',
                prefixText: '\$ ',
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
              final amount = double.tryParse(controller.text.replaceAll(',', '.'));
              if (amount != null && amount > 0) {
                Navigator.of(context).pop(); // Cerrar diálogo primero
                await _addAmountToGoal(goal, amount);
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

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Se han añadido \$${amount.toStringAsFixed(2)} a tu meta'),
            backgroundColor: Colors.green,
          ),
        );
        _loadGoals();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error al añadir ahorro: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  void _showGoalDetails(Saving goal) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text(
          goal.name,
          style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
        ),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              if (goal.description != null && goal.description!.isNotEmpty) ...[
                Text(
                  goal.description!,
                  style: GoogleFonts.poppins(color: Colors.grey[700]),
                ),
                const Divider(height: 24),
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
      padding: const EdgeInsets.symmetric(vertical: 6),
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

  void showStatistics() {
    showDialog(
      context: context,
      builder: (context) => SavingsStatisticsDialog(
        allGoals: _allGoals,
        activeGoals: _activeGoals,
        completedGoals: _completedGoals,
      ),
    );
  }
}

// ----------------------------------------------
// WIDGETS AUXILIARES
// ----------------------------------------------

class AddSavingGoalDialog extends StatefulWidget {
  const AddSavingGoalDialog({Key? key}) : super(key: key);

  @override
  State<AddSavingGoalDialog> createState() => _AddSavingGoalDialogState();
}

class _AddSavingGoalDialogState extends State<AddSavingGoalDialog> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _amountController = TextEditingController();
  final _descriptionController = TextEditingController();

  DateTime? _targetDate;
  // Variable para guardar la prioridad seleccionada (1 = Baja, 3 = Alta, etc.)
  int _selectedPriority = 2; // Valor por defecto: Media (2)
  bool _isSaving = false;

  @override
  void dispose() {
    _nameController.dispose();
    _amountController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _selectDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: DateTime.now().add(const Duration(days: 30)),
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365 * 10)),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: ColorScheme.light(
              primary: Theme.of(context).primaryColor,
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) {
      setState(() {
        _targetDate = picked;
      });
    }
  }

  Future<void> _submitForm() async {
    if (_formKey.currentState!.validate()) {
      setState(() {
        _isSaving = true;
      });

      try {
        final authService = Provider.of<AuthService>(context, listen: false);
        final apiService = Provider.of<ApiService>(context, listen: false);

        if (authService.user?.id == null) throw Exception("Usuario no identificado");

        final targetAmount = double.parse(_amountController.text.replaceAll(',', '.'));

        // Crear objeto Saving
        final newGoal = Saving(
          userId: authService.user!.id!,
          name: _nameController.text,
          description: _descriptionController.text.isEmpty ? null : _descriptionController.text,
          targetAmount: targetAmount,
          currentAmount: 0,
          targetDate: _targetDate,
          status: GoalStatus.active,
          priorityLevel: _selectedPriority, // Usar la prioridad seleccionada
        );

        await apiService.createSavingGoal(newGoal);

        if (mounted) {
          Navigator.of(context).pop(true);
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('¡Meta creada exitosamente!'),
              backgroundColor: Colors.green,
            ),
          );
        }
      } catch (e) {
        if (mounted) {
          setState(() {
            _isSaving = false;
          });
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Error al crear meta: ${e.toString()}'),
              backgroundColor: Colors.red,
            ),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(
        'Nueva Meta',
        style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
      ),
      content: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _nameController,
                decoration: InputDecoration(
                  labelText: 'Nombre de la meta',
                  hintText: 'Ej. Viaje a Japón, Auto Nuevo',
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  prefixIcon: const Icon(Icons.flag_outlined),
                ),
                textCapitalization: TextCapitalization.sentences,
                validator: (v) => v!.isEmpty ? 'Ingresa un nombre' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _amountController,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                  labelText: 'Monto objetivo',
                  prefixText: '\$ ',
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  prefixIcon: const Icon(Icons.attach_money),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return 'Ingresa un monto';
                  if (double.tryParse(v.replaceAll(',', '.')) == null) return 'Monto inválido';
                  return null;
                },
              ),
              const SizedBox(height: 16),
              // Selector de Prioridad (Nuevo)
              DropdownButtonFormField<int>(
                value: _selectedPriority,
                decoration: InputDecoration(
                  labelText: 'Prioridad',
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  prefixIcon: const Icon(Icons.sort),
                ),
                items: const [
                  DropdownMenuItem(value: 1, child: Text('Baja')),
                  DropdownMenuItem(value: 2, child: Text('Media')),
                  DropdownMenuItem(value: 3, child: Text('Alta')),
                  DropdownMenuItem(value: 4, child: Text('Muy Alta')),
                  DropdownMenuItem(value: 5, child: Text('Urgente')),
                ],
                onChanged: (val) {
                  setState(() {
                    _selectedPriority = val!;
                  });
                },
              ),
              const SizedBox(height: 16),
              InkWell(
                onTap: () => _selectDate(context),
                borderRadius: BorderRadius.circular(12),
                child: InputDecorator(
                  decoration: InputDecoration(
                    labelText: 'Fecha límite (Opcional)',
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                    prefixIcon: const Icon(Icons.calendar_today),
                  ),
                  child: Text(
                    _targetDate == null
                        ? 'Seleccionar fecha'
                        : DateFormat('dd/MM/yyyy').format(_targetDate!),
                    style: GoogleFonts.poppins(
                      color: _targetDate == null ? Colors.grey[600] : Colors.black,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _descriptionController,
                maxLines: 2,
                decoration: InputDecoration(
                  labelText: 'Descripción (Opcional)',
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  prefixIcon: const Icon(Icons.description_outlined),
                ),
                textCapitalization: TextCapitalization.sentences,
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _isSaving ? null : () => Navigator.of(context).pop(),
          child: Text('Cancelar', style: GoogleFonts.poppins(color: Colors.grey[600])),
        ),
        ElevatedButton(
          onPressed: _isSaving ? null : _submitForm,
          style: ElevatedButton.styleFrom(
            backgroundColor: Theme.of(context).primaryColor,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
          ),
          child: _isSaving
              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
              : Text('Crear Meta', style: GoogleFonts.poppins(fontWeight: FontWeight.w600, color: Colors.white)),
        ),
      ],
    );
  }
}

class SavingsStatisticsDialog extends StatelessWidget {
  final List<Saving> allGoals;
  final List<Saving> activeGoals;
  final List<Saving> completedGoals;

  const SavingsStatisticsDialog({
    Key? key,
    required this.allGoals,
    required this.activeGoals,
    required this.completedGoals,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final totalSaved = allGoals.fold<double>(0, (sum, g) => sum + g.currentAmount);
    final totalTarget = allGoals.fold<double>(0, (sum, g) => sum + g.targetAmount);
    final overallPercentage = totalTarget > 0 ? (totalSaved / totalTarget) * 100 : 0.0;

    final averageActiveProgress = activeGoals.isNotEmpty
        ? activeGoals.fold<double>(0, (sum, g) => sum + g.progressPercentage) / activeGoals.length
        : 0.0;

    return AlertDialog(
      title: Text(
        'Estadísticas',
        style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
      ),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _buildStatCard(
              context,
              'Total Ahorrado',
              '\$${totalSaved.toStringAsFixed(2)}',
              'de \$${totalTarget.toStringAsFixed(2)}',
              Colors.green,
              Icons.savings,
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _buildStatCard(
                    context,
                    'Activas',
                    activeGoals.length.toString(),
                    '${averageActiveProgress.toStringAsFixed(1)}% avg',
                    Colors.blue,
                    Icons.trending_up,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _buildStatCard(
                    context,
                    'Completadas',
                    completedGoals.length.toString(),
                    '${allGoals.isNotEmpty ? ((completedGoals.length / allGoals.length) * 100).toStringAsFixed(0) : 0}% total',
                    Colors.purple,
                    Icons.check_circle_outline,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Text(
              'Progreso Global',
              style: GoogleFonts.poppins(fontSize: 14, fontWeight: FontWeight.w500),
            ),
            const SizedBox(height: 8),
            LinearProgressIndicator(
              value: overallPercentage / 100,
              backgroundColor: Colors.grey[200],
              color: Theme.of(context).primaryColor,
              minHeight: 10,
              borderRadius: BorderRadius.circular(5),
            ),
            const SizedBox(height: 4),
            Text(
              'Has alcanzado el ${overallPercentage.toStringAsFixed(1)}% de todo lo que te has propuesto ahorrar.',
              style: GoogleFonts.poppins(fontSize: 12, color: Colors.grey[600]),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cerrar'),
        ),
      ],
    );
  }

  Widget _buildStatCard(BuildContext context, String title, String value, String subtitle, Color color, IconData icon) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 18, color: color),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  title,
                  style: GoogleFonts.poppins(
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                    color: color,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            value,
            style: GoogleFonts.poppins(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
          Text(
            subtitle,
            style: GoogleFonts.poppins(
              fontSize: 11,
              color: Colors.grey[600],
            ),
          ),
        ],
      ),
    );
  }
}