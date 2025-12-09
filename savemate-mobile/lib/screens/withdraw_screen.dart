import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';

import '../services/auth_service.dart';
import '../services/api_service.dart';
import 'bank_account_screen.dart';

class WithdrawScreen extends StatefulWidget {
  const WithdrawScreen({Key? key}) : super(key: key);

  @override
  _WithdrawScreenState createState() => _WithdrawScreenState();
}

class _WithdrawScreenState extends State<WithdrawScreen> {
  final _amountController = TextEditingController();
  bool _isSimulating = false;
  String _simulationStatus = '';

  final List<String> _simulationSteps = [
    'Verificando saldo disponible...',
    'Conectando con pasarela bancaria...',
    'Autorizando desembolso...',
    'Enviando fondos a tu cuenta...',
    'Confirmación exitosa.'
  ];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final user = Provider.of<AuthService>(context, listen: false).user;
      if (user?.bankAccount == null || user!.bankAccount!.isEmpty) {
        _showNoAccountDialog();
      }
    });
  }

  void _showNoAccountDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        title: const Text('Cuenta Requerida'),
        content: const Text('Necesitas una cuenta bancaria vinculada para recibir tus fondos.'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
              Navigator.pop(context);
            },
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(ctx);
              Navigator.pushReplacement(
                context,
                MaterialPageRoute(builder: (_) => const BankAccountScreen()),
              );
            },
            child: const Text('Vincular Ahora'),
          ),
        ],
      ),
    );
  }

  Future<void> _processWithdrawal() async {
    if (_amountController.text.isEmpty) return;

    // Limpieza básica del input
    final amount = double.tryParse(_amountController.text.replaceAll(',', '').replaceAll('\$', ''));
    if (amount == null || amount <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Ingresa un monto válido'), backgroundColor: Colors.red),
      );
      return;
    }

    final user = Provider.of<AuthService>(context, listen: false).user;
    if (user != null && (user.totalSaved ?? 0) < amount) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Fondos insuficientes para este retiro'), backgroundColor: Colors.red),
      );
      return;
    }

    // Iniciar Simulación UI
    setState(() {
      _isSimulating = true;
    });

    // Simulación Visual
    for (final step in _simulationSteps) {
      if (!mounted) return;
      setState(() => _simulationStatus = step);
      await Future.delayed(const Duration(milliseconds: 1200));
    }

    // Llamada Real al Backend
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final apiService = Provider.of<ApiService>(context, listen: false);

      await apiService.withdrawFunds(
        userId: authService.user!.id!,
        amount: amount,
      );

      if (!mounted) return;

      setState(() {
        _isSimulating = false;
      });

      _showSuccessDialog(amount, user?.bankName ?? 'tu cuenta');

    } catch (e) {
      setState(() {
        _isSimulating = false;
      });
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: $e'), backgroundColor: Colors.red),
      );
    }
  }

  void _showSuccessDialog(double amount, String bankName) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.check_circle, color: Colors.purple, size: 80),
            const SizedBox(height: 16),
            Text(
              '¡Retiro Exitoso!',
              style: GoogleFonts.poppins(fontSize: 20, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              'Se han enviado \$${amount.toStringAsFixed(0)} a $bankName.',
              style: GoogleFonts.poppins(color: Colors.grey),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              'La operación puede tardar unos minutos en reflejarse.',
              style: GoogleFonts.poppins(fontSize: 10, color: Colors.grey),
              textAlign: TextAlign.center,
            ),
          ],
        ),
        actions: [
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.purple,
              foregroundColor: Colors.white,
              minimumSize: const Size(double.infinity, 45),
            ),
            onPressed: () {
              Navigator.pop(ctx);
              Navigator.pop(context);
            },
            child: const Text('Finalizar'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final user = Provider.of<AuthService>(context).user;
    final bankName = user?.bankName ?? 'Banco';
    final totalSaved = user?.totalSaved ?? 0.0;
    final accountNumber = user?.bankAccount != null && user!.bankAccount!.length > 4
        ? '**** ${user.bankAccount!.substring(user.bankAccount!.length - 4)}'
        : '****';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Retirar Fondos'),
        elevation: 0,
      ),
      body: _isSimulating
          ? _buildSimulationView(bankName)
          : _buildInputView(bankName, accountNumber, totalSaved),
    );
  }

  Widget _buildInputView(String bankName, String accountNumber, double totalSaved) {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Saldo Disponible
          Center(
            child: Column(
              children: [
                Text('Saldo Disponible', style: GoogleFonts.poppins(color: Colors.grey[600])),
                Text(
                  '\$${totalSaved.toStringAsFixed(2)}',
                  style: GoogleFonts.poppins(
                      fontSize: 32,
                      fontWeight: FontWeight.bold,
                      color: Colors.black87
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(height: 30),

          // Tarjeta de Destino
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              boxShadow: [BoxShadow(color: Colors.grey.withOpacity(0.1), blurRadius: 10, offset: const Offset(0, 4))],
              border: Border.all(color: Colors.grey[200]!),
            ),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.purple[50],
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.account_balance, color: Colors.purple),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Enviar a:', style: GoogleFonts.poppins(fontSize: 12, color: Colors.grey[600])),
                      Text(bankName, style: GoogleFonts.poppins(fontWeight: FontWeight.bold, fontSize: 16)),
                      Text(accountNumber, style: GoogleFonts.poppins(color: Colors.grey[800])),
                    ],
                  ),
                ),
                const Icon(Icons.arrow_forward_ios, size: 16, color: Colors.grey),
              ],
            ),
          ),

          const SizedBox(height: 40),

          const Text('Monto a retirar'),
          const SizedBox(height: 10),

          TextField(
            controller: _amountController,
            keyboardType: TextInputType.number,
            style: GoogleFonts.poppins(fontSize: 24, fontWeight: FontWeight.w600),
            decoration: InputDecoration(
              prefixText: '\$ ',
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              hintText: '0.00',
            ),
          ),

          const SizedBox(height: 16),

          // Botón "Todo"
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              onPressed: () => _amountController.text = totalSaved.toStringAsFixed(0),
              child: const Text('Retirar todo el saldo', style: TextStyle(color: Colors.purple)),
            ),
          ),

          const Spacer(),

          SizedBox(
            width: double.infinity,
            height: 55,
            child: ElevatedButton(
              onPressed: _processWithdrawal,
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.purple,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              ),
              child: Text(
                'Confirmar Retiro',
                style: GoogleFonts.poppins(fontSize: 18, fontWeight: FontWeight.w600, color: Colors.white),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSimulationView(String bankName) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(40),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          SizedBox(
            width: 150,
            height: 150,
            child: CircularProgressIndicator(
              strokeWidth: 6,
              valueColor: const AlwaysStoppedAnimation<Color>(Colors.purple),
              backgroundColor: Colors.purple[50],
            ),
          ),

          const SizedBox(height: 40),

          Text(
            'Procesando Retiro',
            style: GoogleFonts.poppins(fontSize: 18, fontWeight: FontWeight.bold),
            textAlign: TextAlign.center,
          ),

          const SizedBox(height: 16),

          AnimatedSwitcher(
            duration: const Duration(milliseconds: 500),
            child: Text(
              _simulationStatus,
              key: ValueKey(_simulationStatus),
              style: GoogleFonts.poppins(fontSize: 14, color: Colors.grey[600]),
              textAlign: TextAlign.center,
            ),
          ),
        ],
      ),
    );
  }
}