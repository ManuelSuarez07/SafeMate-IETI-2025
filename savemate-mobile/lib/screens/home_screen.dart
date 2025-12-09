import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';

import '../services/auth_service.dart';
import '../services/api_service.dart';
import '../models/user.dart';
import '../models/transaction.dart';
import '../models/saving.dart';
import '../models/ai_recommendation.dart';
import 'transactions_screen.dart';
import 'savings_screen.dart';
import 'ai_recommendations_screen.dart';
import 'profile_screen.dart';
import 'deposit_screen.dart';
import 'withdraw_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;
  late PageController _pageController;

  final GlobalKey<TransactionsScreenState> _transactionsKey = GlobalKey();
  final GlobalKey<SavingsScreenState> _savingsKey = GlobalKey();

  List<Transaction> _recentTransactions = [];
  List<Saving> _activeGoals = [];
  List<AIRecommendation> _recommendations = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _pageController = PageController();
    _loadData();
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    if (!mounted) return;
    setState(() {
      _isLoading = true;
    });

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final apiService = Provider.of<ApiService>(context, listen: false);

      if (authService.user?.id != null) {
        final updatedUser = await apiService.getUserById(authService.user!.id!);
        authService.updateProfileLocal(updatedUser);

        final transactions = await apiService.getTransactionsByUserId(authService.user!.id!);
        transactions.sort((a, b) {
          final dateA = a.transactionDate ?? a.createdAt ?? DateTime.now();
          final dateB = b.transactionDate ?? b.createdAt ?? DateTime.now();
          return dateB.compareTo(dateA);
        });

        if (mounted) {
          setState(() {
            _recentTransactions = transactions.take(5).toList();
          });
        }

        final goals = await apiService.getActiveSavingGoals(authService.user!.id!);
        if (mounted) {
          setState(() {
            _activeGoals = goals.take(3).toList();
          });
        }

        final recommendations = await apiService.getActiveRecommendations(authService.user!.id!);
        if (mounted) {
          setState(() {
            _recommendations = recommendations.take(3).toList();
          });
        }
      }
    } catch (e) {
      print('Error loading data: $e');
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _navigateToTab(int index, {VoidCallback? action}) {
    setState(() {
      _currentIndex = index;
    });

    _pageController.jumpToPage(index);

    if (index == 0) {
      _loadData();
    }

    if (action != null) {
      Future.delayed(const Duration(milliseconds: 100), action);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: PageView(
        controller: _pageController,
        onPageChanged: (index) {
          setState(() {
            _currentIndex = index;
          });
          if (index == 0) {
            _loadData();
          }
        },
        physics: const NeverScrollableScrollPhysics(),
        children: [
          _buildHomeTab(),
          TransactionsScreen(key: _transactionsKey),
          SavingsScreen(key: _savingsKey),
          const AIRecommendationsScreen(),
          const ProfileScreen(),
        ],
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) => _navigateToTab(index),
        type: BottomNavigationBarType.fixed,
        selectedItemColor: Theme.of(context).primaryColor,
        unselectedItemColor: Colors.grey,
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.home), label: 'Inicio'),
          BottomNavigationBarItem(icon: Icon(Icons.receipt_long), label: 'Transacciones'),
          BottomNavigationBarItem(icon: Icon(Icons.savings), label: 'Ahorros'),
          BottomNavigationBarItem(icon: Icon(Icons.lightbulb), label: 'IA'),
          BottomNavigationBarItem(icon: Icon(Icons.person), label: 'Perfil'),
        ],
      ),
    );
  }

  Widget _buildHomeTab() {
    return Consumer<AuthService>(
      builder: (context, authService, child) {
        return RefreshIndicator(
          onRefresh: _loadData,
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 20),
                _buildHeader(authService.user),
                const SizedBox(height: 24),
                _buildSavingsCard(authService.user),
                const SizedBox(height: 24),
                _buildQuickActions(),
                const SizedBox(height: 24),
                _buildActiveGoals(),
                const SizedBox(height: 24),
                _buildRecentTransactions(),
                const SizedBox(height: 24),
                _buildRecommendations(),
                const SizedBox(height: 100),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildHeader(User? user) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '¡Hola, ${user?.firstName ?? 'Usuario'}!',
              style: GoogleFonts.poppins(
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),
            Text(
              'Tu progreso de ahorro',
              style: GoogleFonts.poppins(
                fontSize: 16,
                color: Colors.grey[600],
              ),
            ),
          ],
        ),
        IconButton(
          onPressed: () {},
          icon: const Icon(Icons.notifications),
        ),
      ],
    );
  }

  Widget _buildSavingsCard(User? user) {
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
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Ahorro Total',
                style: GoogleFonts.poppins(
                  fontSize: 16,
                  color: Colors.white.withOpacity(0.9),
                ),
              ),
              const Icon(
                Icons.savings,
                color: Colors.white,
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            '\$${(user?.totalSaved ?? 0.0).toStringAsFixed(2)}',
            style: GoogleFonts.poppins(
              fontSize: 32,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const DepositScreen()),
                    ).then((_) => _loadData());
                  },
                  icon: const Icon(Icons.add, color: Color(0xFF2E7D32)),
                  label: const Text('Recargar', style: TextStyle(color: Color(0xFF2E7D32))),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.white,
                    foregroundColor: const Color(0xFF2E7D32),
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 12),

              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const WithdrawScreen()),
                    ).then((_) => _loadData()); // Recargar datos al volver
                  },
                  icon: const Icon(Icons.arrow_upward, color: Colors.white),
                  label: const Text('Retirar', style: TextStyle(color: Colors.white)),
                  style: OutlinedButton.styleFrom(
                    side: const BorderSide(color: Colors.white),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildQuickActions() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Acciones Rápidas',
          style: GoogleFonts.poppins(
            fontSize: 18,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: _buildActionButton(
                'Añadir\nTransacción',
                Icons.add_circle,
                Colors.blue,
                    () {
                  _navigateToTab(1, action: () {
                    if (_transactionsKey.currentState != null) {
                      _transactionsKey.currentState!.showAddTransactionDialog();
                    }
                  });
                },
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _buildActionButton(
                'Nueva\nMeta',
                Icons.flag,
                Colors.green,
                    () {
                  _navigateToTab(2, action: () {
                    if (_savingsKey.currentState != null) {
                      _savingsKey.currentState!.showAddGoalDialog();
                    }
                  });
                },
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _buildActionButton(
                'Ver\nEstadísticas',
                Icons.bar_chart,
                Colors.orange,
                    () {
                  _navigateToTab(2, action: () {
                    if (_savingsKey.currentState != null) {
                      _savingsKey.currentState!.showStatistics();
                    }
                  });
                },
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildActionButton(String title, IconData icon, Color color, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: color.withOpacity(0.3)),
        ),
        child: Column(
          children: [
            Icon(icon, color: color, size: 28),
            const SizedBox(height: 8),
            Text(
              title,
              textAlign: TextAlign.center,
              style: GoogleFonts.poppins(
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: color,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildActiveGoals() {
    if (_activeGoals.isEmpty) return Container();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('Metas Activas', style: GoogleFonts.poppins(fontSize: 18, fontWeight: FontWeight.w600)),
            TextButton(
              onPressed: () => _navigateToTab(2),
              child: Text('Ver todas', style: GoogleFonts.poppins(color: Theme.of(context).primaryColor)),
            ),
          ],
        ),
        const SizedBox(height: 16),
        ..._activeGoals.map((goal) => _buildGoalCard(goal)).toList(),
      ],
    );
  }

  Widget _buildGoalCard(Saving goal) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 2))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(goal.name, style: GoogleFonts.poppins(fontSize: 16, fontWeight: FontWeight.w600)),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(color: goal.priorityColor.withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
                child: Text(goal.priorityDisplay, style: GoogleFonts.poppins(fontSize: 10, fontWeight: FontWeight.w500, color: goal.priorityColor)),
              ),
            ],
          ),
          const SizedBox(height: 12),
          LinearProgressIndicator(value: goal.progressPercentage / 100, backgroundColor: Colors.grey[200], valueColor: AlwaysStoppedAnimation<Color>(goal.statusColor)),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(goal.formattedCurrentAmount, style: GoogleFonts.poppins(fontSize: 14, fontWeight: FontWeight.w500)),
              Text('${goal.progressPercentage.toStringAsFixed(1)}%', style: GoogleFonts.poppins(fontSize: 12, color: Colors.grey[600])),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildRecentTransactions() {
    if (_recentTransactions.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Text("No hay transacciones recientes", style: GoogleFonts.poppins(color: Colors.grey)),
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('Transacciones Recientes', style: GoogleFonts.poppins(fontSize: 18, fontWeight: FontWeight.w600)),
            TextButton(
              onPressed: () => _navigateToTab(1),
              child: Text('Ver todas', style: GoogleFonts.poppins(color: Theme.of(context).primaryColor)),
            ),
          ],
        ),
        const SizedBox(height: 16),
        ..._recentTransactions.map((transaction) => _buildTransactionCard(transaction)).toList(),
      ],
    );
  }

  Widget _buildTransactionCard(Transaction transaction) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 5, offset: const Offset(0, 1))],
      ),
      child: Row(
        children: [
          Container(
            width: 40, height: 40,
            decoration: BoxDecoration(color: transaction.transactionTypeColor.withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
            child: Icon(_getTransactionIcon(transaction.transactionType), color: transaction.transactionTypeColor, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(transaction.description, style: GoogleFonts.poppins(fontSize: 14, fontWeight: FontWeight.w500)),
                Text(transaction.formattedShortDate, style: GoogleFonts.poppins(fontSize: 12, color: Colors.grey[600])),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(transaction.formattedAmount, style: GoogleFonts.poppins(fontSize: 14, fontWeight: FontWeight.w600, color: transaction.transactionTypeColor)),
              if (transaction.hasSaving) Text('+${transaction.formattedSavingAmount}', style: GoogleFonts.poppins(fontSize: 10, color: Colors.green, fontWeight: FontWeight.w500)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildRecommendations() {
    if (_recommendations.isEmpty) return Container();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('Recomendaciones de IA', style: GoogleFonts.poppins(fontSize: 18, fontWeight: FontWeight.w600)),
            TextButton(
              onPressed: () => _navigateToTab(3),
              child: Text('Ver todas', style: GoogleFonts.poppins(color: Theme.of(context).primaryColor)),
            ),
          ],
        ),
        const SizedBox(height: 16),
        ..._recommendations.map((recommendation) => _buildRecommendationCard(recommendation)).toList(),
      ],
    );
  }

  Widget _buildRecommendationCard(AIRecommendation recommendation) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: recommendation.typeColor.withOpacity(0.1),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: recommendation.typeColor.withOpacity(0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.lightbulb, color: recommendation.typeColor, size: 20),
              const SizedBox(width: 8),
              Expanded(child: Text(recommendation.title, style: GoogleFonts.poppins(fontSize: 14, fontWeight: FontWeight.w600, color: recommendation.typeColor))),
              if (recommendation.hasHighConfidence)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(color: Colors.green.withOpacity(0.2), borderRadius: BorderRadius.circular(6)),
                  child: Text(recommendation.formattedConfidenceScore, style: GoogleFonts.poppins(fontSize: 10, fontWeight: FontWeight.w500, color: Colors.green)),
                ),
            ],
          ),
          const SizedBox(height: 8),
          Text(recommendation.description, style: GoogleFonts.poppins(fontSize: 12, color: Colors.grey[700])),
          if (recommendation.hasPotentialSavings) ...[
            const SizedBox(height: 8),
            Text('Ahorro potencial: ${recommendation.formattedPotentialSavings}', style: GoogleFonts.poppins(fontSize: 12, fontWeight: FontWeight.w600, color: Theme.of(context).primaryColor)),
          ],
        ],
      ),
    );
  }

  IconData _getTransactionIcon(TransactionType type) {
    switch (type) {
      case TransactionType.expense: return Icons.shopping_cart;
      case TransactionType.income: return Icons.attach_money;
      case TransactionType.saving: return Icons.savings;
      case TransactionType.fee: return Icons.receipt;
      case TransactionType.withdrawal: return Icons.arrow_circle_up;
    }
  }
}