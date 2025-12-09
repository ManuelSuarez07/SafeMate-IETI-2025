import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import '../services/auth_service.dart';

/// Pantalla para la gestión y vinculación de la cuenta bancaria del usuario.
///
/// Esta clase es responsable de:
/// 1. Visualizar de forma segura la cuenta bancaria actual (enmascarada).
/// 2. Proveer un formulario para seleccionar una entidad financiera de una lista predefinida.
/// 3. Validar y enviar los datos de la nueva cuenta al backend a través de [AuthService].
/// 4. Alternar entre modo de visualización y modo de edición según el estado actual del usuario.
class BankAccountScreen extends StatefulWidget {
  const BankAccountScreen({Key? key}) : super(key: key);

  @override
  _BankAccountScreenState createState() => _BankAccountScreenState();
}

class _BankAccountScreenState extends State<BankAccountScreen> {
  final _formKey = GlobalKey<FormState>();
  final _accountController = TextEditingController();
  String? _selectedBank;

  /// Controla si el formulario está habilitado para escritura.
  /// Se inicializa en `false` si el usuario ya tiene datos guardados.
  bool _isEditing = true;

  /// Lista estática de bancos soportados para la selección en el dropdown.
  final List<String> _banks = [
    'Bancolombia',
    'Nequi',
    'Daviplata',
    'Davivienda',
    'Banco de Bogotá',
    'BBVA',
    'Scotiabank Colpatria',
    'Banco de Occidente',
    'Banco Popular'
  ];

  /// Inicializa el estado del widget y carga los datos existentes.
  ///
  /// Obtiene el usuario actual desde [AuthService]. Si el [User] ya posee
  /// una [bankAccount] vinculada:
  /// - Pre-llena el [_accountController].
  /// - Establece el [_selectedBank] (validando que exista en la lista [_banks]).
  /// - Deshabilita el modo de edición ([_isEditing] = false).
  @override
  void initState() {
    super.initState();
    final user = Provider.of<AuthService>(context, listen: false).user;

    // Si el usuario ya tiene cuenta, cargar datos
    if (user != null && user.bankAccount != null && user.bankAccount!.isNotEmpty) {
      _accountController.text = user.bankAccount!;

      // Verificar si el banco guardado está en la lista
      if (_banks.contains(user.bankName)) {
        _selectedBank = user.bankName;
      } else {
        _selectedBank = _banks.first;
      }

      _isEditing = false;
    } else {
      _selectedBank = 'Bancolombia'; // Default
    }
  }

  @override
  void dispose() {
    _accountController.dispose();
    super.dispose();
  }

  /// Valida el formulario y solicita la vinculación de la cuenta al servicio.
  ///
  /// Ejecuta las siguientes acciones:
  /// 1. Valida los campos del formulario usando [_formKey].
  /// 2. Invoca [AuthService.linkBankAccount] enviando el número de cuenta y el banco seleccionado.
  ///    (Esto realiza una petición HTTP al backend para actualizar el perfil del usuario).
  /// 3. Gestiona la respuesta:
  ///    - Si es exitosa: Muestra un feedback visual y cierra la pantalla.
  ///    - Si falla: Muestra un [SnackBar] con el error proveniente del servicio.
  ///
  /// Retorna un [Future<void>].
  Future<void> _saveAccount() async {
    if (!_formKey.currentState!.validate()) return;

    final authService = Provider.of<AuthService>(context, listen: false);

    // Ocultar teclado
    FocusScope.of(context).unfocus();

    final success = await authService.linkBankAccount(
      _accountController.text,
      _selectedBank!,
    );

    if (success) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Cuenta vinculada correctamente'),
            backgroundColor: Color(0xFF4CAF50),
          ),
        );
        setState(() {
          _isEditing = false;
        });
        Navigator.pop(context); // Regresar al perfil
      }
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(authService.errorMessage ?? 'Error al vincular cuenta'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final authService = Provider.of<AuthService>(context);
    final user = authService.user;
    final isLoading = authService.isLoading;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Cuenta Bancaria'),
        actions: [
          if (!_isEditing)
            IconButton(
              icon: const Icon(Icons.edit),
              onPressed: () => setState(() => _isEditing = true),
              tooltip: 'Editar cuenta',
            )
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Tarjeta Visual
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFF1B5E20), Color(0xFF4CAF50)],
                    begin: Alignment.bottomLeft,
                    end: Alignment.topRight,
                  ),
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.2),
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
                        const Icon(Icons.nfc, color: Colors.white54, size: 30),
                        Icon(Icons.account_balance, color: Colors.white.withOpacity(0.9), size: 30),
                      ],
                    ),
                    const SizedBox(height: 25),
                    Text(
                      user?.bankAccount != null && user!.bankAccount!.isNotEmpty
                          ? _formatAccountNumber(user.bankAccount!)
                          : '**** **** **** ****',
                      style: GoogleFonts.courierPrime(
                        color: Colors.white,
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                        letterSpacing: 2.0,
                      ),
                    ),
                    const SizedBox(height: 20),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'TITULAR',
                              style: GoogleFonts.poppins(color: Colors.white54, fontSize: 10),
                            ),
                            Text(
                              user?.fullName.toUpperCase() ?? 'USUARIO',
                              style: GoogleFonts.poppins(color: Colors.white, fontWeight: FontWeight.w600),
                            ),
                          ],
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(
                              'BANCO',
                              style: GoogleFonts.poppins(color: Colors.white54, fontSize: 10),
                            ),
                            Text(
                              user?.bankName ?? 'NO VINCULADO',
                              style: GoogleFonts.poppins(color: Colors.white, fontWeight: FontWeight.w600),
                            ),
                          ],
                        ),
                      ],
                    )
                  ],
                ),
              ),

              const SizedBox(height: 32),

              Text(
                'Configurar Cuenta',
                style: GoogleFonts.poppins(
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: Colors.black87,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'Selecciona tu banco y número de cuenta para procesar tus ahorros.',
                style: GoogleFonts.poppins(
                  fontSize: 14,
                  color: Colors.grey[600],
                ),
              ),

              const SizedBox(height: 24),

              // Selector de Banco
              DropdownButtonFormField<String>(
                value: _selectedBank,
                decoration: InputDecoration(
                  labelText: 'Banco',
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  prefixIcon: const Icon(Icons.account_balance_outlined),
                  filled: true,
                  fillColor: _isEditing ? Colors.white : Colors.grey[100],
                ),
                items: _banks.map((bank) {
                  return DropdownMenuItem(value: bank, child: Text(bank));
                }).toList(),
                onChanged: _isEditing
                    ? (value) => setState(() => _selectedBank = value)
                    : null,
              ),

              const SizedBox(height: 20),

              // Campo de Número de Cuenta
              TextFormField(
                controller: _accountController,
                enabled: _isEditing,
                decoration: InputDecoration(
                  labelText: 'Número de Cuenta / Celular',
                  hintText: 'Ej: 3001234567',
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  prefixIcon: const Icon(Icons.numbers),
                  filled: true,
                  fillColor: _isEditing ? Colors.white : Colors.grey[100],
                ),
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Por favor ingresa el número';
                  }
                  if (value.length < 10) {
                    return 'El número parece muy corto (mín. 10 dígitos)';
                  }
                  return null;
                },
              ),

              const SizedBox(height: 32),

              // Botón Guardar
              if (_isEditing)
                SizedBox(
                  width: double.infinity,
                  height: 55,
                  child: ElevatedButton(
                    onPressed: isLoading ? null : _saveAccount,
                    style: ElevatedButton.styleFrom(
                      elevation: 2,
                    ),
                    child: isLoading
                        ? const SizedBox(
                        width: 24,
                        height: 24,
                        child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2)
                    )
                        : Text(
                        'Vincular Cuenta',
                        style: GoogleFonts.poppins(fontSize: 16, fontWeight: FontWeight.w600)
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  /// Formatea el número de cuenta para mostrarlo en la tarjeta visual.
  ///
  /// Recibe el [number] completo y retorna una cadena enmascarada
  /// mostrando solo los últimos 4 dígitos (ej. "**** **** **** 1234")
  /// para proteger la privacidad del usuario en la interfaz.
  String _formatAccountNumber(String number) {
    if (number.length <= 4) return number;
    // Muestra solo los últimos 4 dígitos
    return '**** **** **** ${number.substring(number.length - 4)}';
  }
}