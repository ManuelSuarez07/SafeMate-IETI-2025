import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';

import '../services/auth_service.dart';
import '../services/api_service.dart';
import '../models/transaction.dart';

/// Pantalla principal para visualizar el historial de transacciones financieras.
///
/// Esta clase es responsable de:
/// 1. Listar transacciones clasificadas por pestañas (Todas, Gastos, Ingresos, Ahorros).
/// 2. Mostrar un resumen financiero (Dashboard) con totales calculados dinámicamente.
/// 3. Proveer herramientas de filtrado y actualización manual (Pull-to-refresh).
/// 4. Permitir la creación de nuevas transacciones mediante [AddTransactionDialog].
class TransactionsScreen extends StatefulWidget {
  const TransactionsScreen({Key? key}) : super(key: key);

  @override
  State<TransactionsScreen> createState() => TransactionsScreenState();
}

class TransactionsScreenState extends State<TransactionsScreen>
    with SingleTickerProviderStateMixin, AutomaticKeepAliveClientMixin {

  late TabController _tabController;

  List<Transaction> _allTransactions = [];
  List<Transaction> _expenses = [];
  List<Transaction> _income = [];
  List<Transaction> _savings = [];
  bool _isLoading = true;
  String _selectedFilter = 'Todas';

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
    _loadTransactions();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  /// Obtiene y clasifica las transacciones del usuario desde el backend.
  ///
  /// Realiza una llamada HTTP mediante [ApiService.getTransactionsByUserId].
  ///
  /// Acciones principales:
  /// - Ordena las transacciones por fecha descendente (de la más reciente a la más antigua).
  /// - Segrega la lista principal en sub-listas ([_expenses], [_income], [_savings]) para
  ///   alimentar las diferentes pestañas de la interfaz.
  ///
  /// Retorna un [Future<void>].
  Future<void> _loadTransactions() async {
    if (!mounted) return;
    setState(() {
      _isLoading = true;
    });

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final apiService = Provider.of<ApiService>(context, listen: false);

      if (authService.user?.id != null) {
        final transactions = await apiService.getTransactionsByUserId(authService.user!.id!);

        // Ordenar por fecha descendente
        transactions.sort((a, b) {
          final dateA = a.transactionDate ?? a.createdAt ?? DateTime.now();
          final dateB = b.transactionDate ?? b.createdAt ?? DateTime.now();
          return dateB.compareTo(dateA);
        });

        if (mounted) {
          setState(() {
            _allTransactions = transactions;
            _expenses = transactions.where((t) => t.transactionType == TransactionType.expense).toList();
            _income = transactions.where((t) => t.transactionType == TransactionType.income).toList();
            _savings = transactions.where((t) => t.transactionType == TransactionType.saving).toList();
            _isLoading = false;
          });
        }
      } else {
        print('Advertencia: Usuario sin ID al cargar transacciones');
        if (mounted) {
          setState(() {
            _isLoading = false;
          });
        }
      }
    } catch (e) {
      print('Error loading transactions: $e');
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  /// Retorna la lista de transacciones correspondiente al filtro seleccionado en el diálogo.
  List<Transaction> get _filteredTransactions {
    switch (_selectedFilter) {
      case 'Gastos':
        return _expenses;
      case 'Ingresos':
        return _income;
      case 'Ahorros':
        return _savings;
      default:
        return _allTransactions;
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(
          'Transacciones',
          style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
        ),
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          labelStyle: GoogleFonts.poppins(fontWeight: FontWeight.w600),
          unselectedLabelStyle: GoogleFonts.poppins(),
          tabs: const [
            Tab(text: 'Todas'),
            Tab(text: 'Gastos'),
            Tab(text: 'Ingresos'),
            Tab(text: 'Ahorros'),
          ],
        ),
        actions: [
          IconButton(
            onPressed: _showFilterDialog,
            icon: const Icon(Icons.filter_list),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadTransactions,
        child: TabBarView(
          controller: _tabController,
          children: [
            _buildTransactionsList(_allTransactions),
            _buildTransactionsList(_expenses),
            _buildTransactionsList(_income),
            _buildTransactionsList(_savings),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: showAddTransactionDialog,
        backgroundColor: Theme.of(context).primaryColor,
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }

  /// Construye la lista visual de transacciones o el estado vacío.
  ///
  /// Recibe una lista de [Transaction] y renderiza un [ListView] con tarjetas
  /// de detalle y un encabezado de resumen.
  Widget _buildTransactionsList(List<Transaction> transactions) {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (transactions.isEmpty) {
      return Center(
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.receipt_long,
                size: 64,
                color: Colors.grey[400],
              ),
              const SizedBox(height: 16),
              Text(
                'No hay transacciones',
                style: GoogleFonts.poppins(
                  fontSize: 18,
                  color: Colors.grey[600],
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'Añade tu primera transacción',
                style: GoogleFonts.poppins(
                  fontSize: 14,
                  color: Colors.grey[500],
                ),
              ),
            ],
          ),
        ),
      );
    }

    return Column(
      children: [
        // Resumen
        _buildSummaryCard(transactions),

        // Lista de transacciones
        Expanded(
          child: ListView.builder(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 80),
            itemCount: transactions.length,
            itemBuilder: (context, index) {
              final transaction = transactions[index];
              return _buildTransactionCard(transaction);
            },
          ),
        ),
      ],
    );
  }

  /// Construye la tarjeta de resumen financiero (Dashboard).
  ///
  /// Calcula y muestra los totales de:
  /// - Gastos totales.
  /// - Ingresos totales.
  /// - Ahorros directos.
  /// - Ahorros generados por redondeo automático.
  Widget _buildSummaryCard(List<Transaction> transactions) {
    final totalExpenses = _expenses.fold<double>(0, (sum, t) => sum + t.amount);
    final totalIncome = _income.fold<double>(0, (sum, t) => sum + t.amount);
    final totalSavings = _savings.fold<double>(0, (sum, t) => sum + t.amount);
    final totalSavingFromRounding = _allTransactions
        .where((t) => t.hasSaving)
        .fold<double>(0, (sum, t) => sum + (t.savingAmount ?? 0));

    return Container(
      margin: const EdgeInsets.all(16),
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
            'Resumen General',
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
                  'Gastos',
                  '\$${totalExpenses.toStringAsFixed(2)}',
                  Colors.white,
                  Icons.trending_down,
                  isExpense: true,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildSummaryItem(
                  'Ingresos',
                  '\$${totalIncome.toStringAsFixed(2)}',
                  Colors.white,
                  Icons.trending_up,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _buildSummaryItem(
                  'Ahorros',
                  '\$${totalSavings.toStringAsFixed(2)}',
                  Colors.white,
                  Icons.savings,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildSummaryItem(
                  'Redondeo',
                  '\$${totalSavingFromRounding.toStringAsFixed(2)}',
                  Colors.white,
                  Icons.auto_graph,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSummaryItem(String label, String value, Color textColor, IconData icon, {bool isExpense = false}) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.2),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          Icon(icon, color: Colors.white, size: 20),
          const SizedBox(width: 8),
          Flexible(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: GoogleFonts.poppins(
                    fontSize: 12,
                    color: Colors.white.withOpacity(0.9),
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
                Text(
                  value,
                  style: GoogleFonts.poppins(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTransactionCard(Transaction transaction) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
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
      ),
      child: Column(
        children: [
          Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: transaction.transactionTypeColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  _getTransactionIcon(transaction.transactionType),
                  color: transaction.transactionTypeColor,
                  size: 24,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      transaction.description,
                      style: GoogleFonts.poppins(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    if (transaction.merchantName != null && transaction.merchantName!.isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text(
                        transaction.merchantName!,
                        style: GoogleFonts.poppins(
                          fontSize: 14,
                          color: Colors.grey[600],
                        ),
                      ),
                    ],
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Text(
                          transaction.formattedShortDate,
                          style: GoogleFonts.poppins(
                            fontSize: 12,
                            color: Colors.grey[500],
                          ),
                        ),
                        const SizedBox(width: 12),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: transaction.statusColor.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: Text(
                            transaction.statusDisplay,
                            style: GoogleFonts.poppins(
                              fontSize: 10,
                              fontWeight: FontWeight.w500,
                              color: transaction.statusColor,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    transaction.formattedAmount,
                    style: GoogleFonts.poppins(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: transaction.transactionTypeColor,
                    ),
                  ),
                  if (transaction.hasSaving) ...[
                    const SizedBox(height: 4),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.green.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        '+${transaction.formattedSavingAmount}',
                        style: GoogleFonts.poppins(
                          fontSize: 10,
                          fontWeight: FontWeight.w600,
                          color: Colors.green,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ],
          ),
          if (transaction.originalAmount != null && transaction.roundedAmount != null) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.grey[50],
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Redondeo automático',
                    style: GoogleFonts.poppins(
                      fontSize: 12,
                      color: Colors.grey[600],
                    ),
                  ),
                  Row(
                    children: [
                      Text(
                        '\$${transaction.originalAmount!.toStringAsFixed(2)}',
                        style: GoogleFonts.poppins(
                          fontSize: 12,
                          color: Colors.grey[600],
                          decoration: TextDecoration.lineThrough,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        '\$${transaction.roundedAmount!.toStringAsFixed(2)}',
                        style: GoogleFonts.poppins(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: Theme.of(context).primaryColor,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  IconData _getTransactionIcon(TransactionType type) {
    switch (type) {
      case TransactionType.expense:
        return Icons.shopping_cart;
      case TransactionType.income:
        return Icons.attach_money;
      case TransactionType.saving:
        return Icons.savings;
      case TransactionType.fee:
        return Icons.receipt;
      case TransactionType.withdrawal:
        return Icons.account_balance_wallet_outlined;
    }
  }

  void _showFilterDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(
          'Filtrar transacciones',
          style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            'Todas',
            'Gastos',
            'Ingresos',
            'Ahorros',
          ].map((filter) => RadioListTile<String>(
            title: Text(filter),
            value: filter,
            groupValue: _selectedFilter,
            onChanged: (value) {
              setState(() {
                _selectedFilter = value!;
              });
              Navigator.of(context).pop();
            },
          )).toList(),
        ),
      ),
    );
  }

  /// Muestra el diálogo para añadir una nueva transacción.
  ///
  /// Abre [AddTransactionDialog] y, si la operación es exitosa (retorna `true`),
  /// recarga la lista de transacciones.
  void showAddTransactionDialog() {
    showDialog(
      context: context,
      builder: (context) => const AddTransactionDialog(),
    ).then((value) {
      if (value == true) {
        _loadTransactions();
      }
    });
  }
}

// --- DIÁLOGO DE AÑADIR TRANSACCIÓN ---
/// Diálogo modal que permite al usuario crear una nueva transacción.
class AddTransactionDialog extends StatefulWidget {
  const AddTransactionDialog({Key? key}) : super(key: key);

  @override
  State<AddTransactionDialog> createState() => _AddTransactionDialogState();
}

class _AddTransactionDialogState extends State<AddTransactionDialog> {
  final _formKey = GlobalKey<FormState>();
  final _amountController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _merchantController = TextEditingController();

  TransactionType _selectedType = TransactionType.expense;
  DateTime _selectedDate = DateTime.now();
  bool _isSaving = false;

  @override
  void dispose() {
    _amountController.dispose();
    _descriptionController.dispose();
    _merchantController.dispose();
    super.dispose();
  }

  Future<void> _selectDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
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
    if (picked != null && picked != _selectedDate) {
      setState(() {
        _selectedDate = picked;
      });
    }
  }

  /// Valida el formulario y envía la nueva transacción al servidor.
  ///
  /// Crea un objeto [Transaction] con los datos ingresados y realiza un POST
  /// mediante [ApiService.createTransaction].
  ///
  /// Muestra notificación de éxito y cierra el diálogo retornando `true`.
  Future<void> _submitForm() async {
    if (_formKey.currentState!.validate()) {
      setState(() {
        _isSaving = true;
      });

      try {
        final authService = Provider.of<AuthService>(context, listen: false);
        final apiService = Provider.of<ApiService>(context, listen: false);

        if (authService.user?.id == null) throw Exception("Usuario no identificado");

        final amount = double.parse(_amountController.text.replaceAll(',', '.'));

        final newTransaction = Transaction(
          userId: authService.user!.id!,
          amount: amount,
          description: _descriptionController.text,
          merchantName: _merchantController.text.isEmpty ? null : _merchantController.text,
          transactionType: _selectedType,
          transactionDate: _selectedDate,
          status: TransactionStatus.completed,
        );

        await apiService.createTransaction(newTransaction);

        if (mounted) {
          Navigator.of(context).pop(true);
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Transacción guardada exitosamente'),
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
              content: Text('Error al guardar: ${e.toString()}'),
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
        'Añadir Transacción',
        style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
      ),
      content: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Selector de Tipo
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: _buildTypeSegment(
                          'Gasto',
                          TransactionType.expense,
                          Colors.red
                      ),
                    ),
                    Expanded(
                      child: _buildTypeSegment(
                          'Ingreso',
                          TransactionType.income,
                          Colors.green
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // Campo de Monto
              TextFormField(
                controller: _amountController,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                style: GoogleFonts.poppins(
                  fontSize: 24,
                  fontWeight: FontWeight.w600,
                  color: _selectedType == TransactionType.expense ? Colors.red : Colors.green,
                ),
                decoration: InputDecoration(
                  labelText: 'Monto',
                  prefixText: '\$ ',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  prefixStyle: GoogleFonts.poppins(
                    fontSize: 24,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Ingresa un monto';
                  }
                  if (double.tryParse(value.replaceAll(',', '.')) == null) {
                    return 'Monto inválido';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),

              // Campo de Descripción
              TextFormField(
                controller: _descriptionController,
                decoration: InputDecoration(
                  labelText: 'Descripción',
                  hintText: 'Ej. Comida, Salario, Cine',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  prefixIcon: const Icon(Icons.description_outlined),
                ),
                textCapitalization: TextCapitalization.sentences,
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Ingresa una descripción';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),

              // Campo de Comercio
              if (_selectedType == TransactionType.expense) ...[
                TextFormField(
                  controller: _merchantController,
                  decoration: InputDecoration(
                    labelText: 'Comercio / Lugar (Opcional)',
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                    prefixIcon: const Icon(Icons.store_outlined),
                  ),
                  textCapitalization: TextCapitalization.words,
                ),
                const SizedBox(height: 16),
              ],

              // Selector de Fecha
              InkWell(
                onTap: () => _selectDate(context),
                borderRadius: BorderRadius.circular(12),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 16),
                  decoration: BoxDecoration(
                    border: Border.all(color: Colors.grey),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.calendar_today, color: Colors.grey),
                      const SizedBox(width: 12),
                      Text(
                        DateFormat('dd/MM/yyyy').format(_selectedDate),
                        style: GoogleFonts.poppins(fontSize: 16),
                      ),
                      const Spacer(),
                      const Icon(Icons.arrow_drop_down),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _isSaving ? null : () => Navigator.of(context).pop(),
          child: Text(
            'Cancelar',
            style: GoogleFonts.poppins(color: Colors.grey[600]),
          ),
        ),
        ElevatedButton(
          onPressed: _isSaving ? null : _submitForm,
          style: ElevatedButton.styleFrom(
            backgroundColor: Theme.of(context).primaryColor,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
          ),
          child: _isSaving
              ? const SizedBox(
            width: 20,
            height: 20,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              color: Colors.white,
            ),
          )
              : Text(
            'Guardar',
            style: GoogleFonts.poppins(
              fontWeight: FontWeight.w600,
              color: Colors.white,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildTypeSegment(String label, TransactionType type, Color activeColor) {
    final isSelected = _selectedType == type;
    return GestureDetector(
      onTap: () {
        setState(() {
          _selectedType = type;
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 10),
        decoration: BoxDecoration(
          color: isSelected ? Colors.white : Colors.transparent,
          borderRadius: BorderRadius.circular(10),
          boxShadow: isSelected
              ? [
            BoxShadow(
              color: Colors.black.withOpacity(0.1),
              blurRadius: 4,
              offset: const Offset(0, 2),
            ),
          ]
              : null,
        ),
        child: Center(
          child: Text(
            label,
            style: GoogleFonts.poppins(
              fontWeight: FontWeight.w600,
              color: isSelected ? activeColor : Colors.grey[600],
            ),
          ),
        ),
      ),
    );
  }
}