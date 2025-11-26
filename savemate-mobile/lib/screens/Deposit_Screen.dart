import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:lottie/lottie.dart'; // Opcional: Si tienes animaciones, sino usaremos Icons

import '../services/auth_service.dart';
import '../services/api_service.dart';
import 'bank_account_screen.dart';

class DepositScreen extends StatefulWidget {
  const DepositScreen({Key? key}) : super(key: key);

  @override
  _DepositScreenState createState() => _DepositScreenState();
}

class _DepositScreenState extends State<DepositScreen> {
  final _amountController = TextEditingController();
  bool _isSimulating = false;
  String _simulationStatus = '';

  // Estados de la simulación
  final List<String> _simulationSteps = [
    'Estableciendo conexión segura...',
    'Verificando credenciales bancarias...',
    'Autorizando transacción...',
    'Transfiriendo fondos a SaveMate...',
    'Finalizando operación...'
  ];

  @override
  void initState() {
    super.initState();
    // Verificar si tiene cuenta al abrir
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
        content: const Text('Para realizar transferencias necesitas vincular una cuenta bancaria primero.'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(ctx); // Cierra diálogo
              Navigator.pop(context); // Cierra pantalla depósito
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

  Future<void> _processDeposit() async {
    if (_amountController.text.isEmpty) return;

    final amount = double.tryParse(_amountController.text.replaceAll(',', ''));
    if (amount == null || amount <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Ingresa un monto válido'), backgroundColor: Colors.red),
      );
      return;
    }

    // Iniciar Simulación
    setState(() {
      _isSimulating = true;
    });

    // 1. Simulación Visual (Pasos falsos para dar sensación de seguridad/conexión)
    for (final step in _simulationSteps) {
      if (!mounted) return;
      setState(() => _simulationStatus = step);
      await Future.delayed(const Duration(milliseconds: 1500)); // 1.5 segundos por paso
    }

    // 2. Llamada Real al Backend
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final apiService = Provider.of<ApiService>(context, listen: false);

      await apiService.createSavingDeposit(
        userId: authService.user!.id!,
        amount: amount,
      );

      if (!mounted) return;

      // 3. Éxito
      setState(() {
        _isSimulating = false;
      });

      _showSuccessDialog(amount);

    } catch (e) {
      setState(() {
        _isSimulating = false;
      });
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error en la transferencia: $e'), backgroundColor: Colors.red),
      );
    }
  }

  void _showSuccessDialog(double amount) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.check_circle, color: Colors.green, size: 80),
            const SizedBox(height: 16),
            Text(
              '¡Transferencia Exitosa!',
              style: GoogleFonts.poppins(fontSize: 20, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              'Has recargado \$${amount.toStringAsFixed(0)} a tus ahorros.',
              style: GoogleFonts.poppins(color: Colors.grey),
              textAlign: TextAlign.center,
            ),
          ],
        ),
        actions: [
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green,
              minimumSize: const Size(double.infinity, 45),
            ),
            onPressed: () {
              Navigator.pop(ctx); // Cerrar diálogo
              Navigator.pop(context); // Regresar al home
            },
            child: const Text('Entendido'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final user = Provider.of<AuthService>(context).user;
    final bankName = user?.bankName ?? 'Banco';
    final accountNumber = user?.bankAccount != null && user!.bankAccount!.length > 4
        ? '**** ${user.bankAccount!.substring(user.bankAccount!.length - 4)}'
        : '****';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Recargar Saldo'),
        elevation: 0,
      ),
      body: _isSimulating
          ? _buildSimulationView(bankName)
          : _buildInputView(bankName, accountNumber),
    );
  }

  // Vista de Entrada de Datos
  Widget _buildInputView(String bankName, String accountNumber) {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Tarjeta de Origen
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.grey[100],
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: Colors.grey[300]!),
            ),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    shape: BoxShape.circle,
                    boxShadow: [BoxShadow(color: Colors.grey.withOpacity(0.2), blurRadius: 5)],
                  ),
                  child: const Icon(Icons.account_balance, color: Color(0xFF4CAF50)),
                ),
                const SizedBox(width: 16),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Desde tu cuenta:', style: GoogleFonts.poppins(fontSize: 12, color: Colors.grey[600])),
                    Text(bankName, style: GoogleFonts.poppins(fontWeight: FontWeight.bold, fontSize: 16)),
                    Text(accountNumber, style: GoogleFonts.poppins(color: Colors.grey[800])),
                  ],
                ),
              ],
            ),
          ),

          const SizedBox(height: 40),

          Center(
            child: Text(
              '¿Cuánto quieres transferir?',
              style: GoogleFonts.poppins(fontSize: 18, fontWeight: FontWeight.w500),
            ),
          ),

          const SizedBox(height: 20),

          // Input gigante de dinero
          TextField(
            controller: _amountController,
            keyboardType: TextInputType.number,
            textAlign: TextAlign.center,
            style: GoogleFonts.poppins(fontSize: 40, fontWeight: FontWeight.bold, color: const Color(0xFF4CAF50)),
            decoration: InputDecoration(
              hintText: '\$0',
              hintStyle: TextStyle(color: Colors.grey[300]),
              border: InputBorder.none,
              prefixText: '\$ ',
              prefixStyle: GoogleFonts.poppins(fontSize: 40, fontWeight: FontWeight.bold, color: Colors.grey[400]),
            ),
          ),

          const Spacer(),

          // Botones rápidos
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              _quickAmountButton(10000),
              _quickAmountButton(20000),
              _quickAmountButton(50000),
            ],
          ),

          const SizedBox(height: 24),

          SizedBox(
            width: double.infinity,
            height: 55,
            child: ElevatedButton(
              onPressed: _processDeposit,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF4CAF50),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.lock_outline, size: 20),
                  const SizedBox(width: 8),
                  Text(
                    'Transferir Segura',
                    style: GoogleFonts.poppins(fontSize: 18, fontWeight: FontWeight.w600),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _quickAmountButton(double amount) {
    return OutlinedButton(
      onPressed: () => _amountController.text = amount.toStringAsFixed(0),
      style: OutlinedButton.styleFrom(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      ),
      child: Text('\$${amount ~/ 1000}k'),
    );
  }

  // Vista de Simulación
  Widget _buildSimulationView(String bankName) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(40),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // Animación de pulso o carga
          Stack(
            alignment: Alignment.center,
            children: [
              SizedBox(
                width: 150,
                height: 150,
                child: CircularProgressIndicator(
                  strokeWidth: 8,
                  valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF4CAF50)),
                  backgroundColor: Colors.grey[200],
                ),
              ),
              const Icon(Icons.swap_horiz, size: 60, color: Color(0xFF4CAF50)),
            ],
          ),

          const SizedBox(height: 40),

          Text(
            'Conectando con $bankName',
            style: GoogleFonts.poppins(fontSize: 18, fontWeight: FontWeight.bold),
            textAlign: TextAlign.center,
          ),

          const SizedBox(height: 16),

          Text(
            _simulationStatus,
            style: GoogleFonts.poppins(fontSize: 14, color: Colors.grey[600]),
            textAlign: TextAlign.center,
          ),

          const SizedBox(height: 20),
          const LinearProgressIndicator(color: Color(0xFF4CAF50), backgroundColor: Colors.white),

          const SizedBox(height: 40),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.security, size: 16, color: Colors.grey),
              const SizedBox(width: 8),
              Text(
                'Encriptación de grado bancario 256-bit',
                style: GoogleFonts.poppins(fontSize: 10, color: Colors.grey),
              ),
            ],
          )
        ],
      ),
    );
  }
}