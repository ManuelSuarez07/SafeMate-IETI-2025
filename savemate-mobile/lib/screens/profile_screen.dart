import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';

import '../services/auth_service.dart';
import '../models/user.dart';
import 'login_screen.dart';
import 'bank_account_screen.dart';

/// Pantalla de gestión de perfil y configuración de usuario.
///
/// Esta clase es responsable de:
/// 1. Visualizar la información personal del usuario y permitir su edición.
/// 2. Configurar las reglas de negocio para el ahorro automático (tipo de ahorro, redondeo, porcentajes).
/// 3. Gestionar la vinculación con la cuenta bancaria (navegación a [BankAccountScreen]).
/// 4. Proporcionar la funcionalidad de cierre de sesión.
class ProfileScreen extends StatefulWidget {
  const ProfileScreen({Key? key}) : super(key: key);

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  // Controladores
  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _minBalanceController = TextEditingController();

  // Configuración de ahorro
  SavingType _selectedSavingType = SavingType.rounding;
  int _selectedRoundingMultiple = 1000;
  double _savingPercentage = 10.0;
  InsufficientBalanceOption _insufficientOption = InsufficientBalanceOption.noSaving;

  bool _isLoading = false;
  bool _isEditingProfile = false;
  bool _isEditingConfig = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadUserData();
    });
  }

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _phoneController.dispose();
    _minBalanceController.dispose();
    super.dispose();
  }

  /// Sincroniza los controladores locales con los datos del [User] actual.
  ///
  /// Obtiene la instancia de [User] desde [AuthService] y puebla los campos
  /// de texto y las variables de estado de configuración (tipo de ahorro, redondeo, etc.)
  /// para reflejar el estado actual en la interfaz.
  void _loadUserData() {
    final authService = Provider.of<AuthService>(context, listen: false);
    final user = authService.user;

    if (user != null) {
      _firstNameController.text = user.firstName;
      _lastNameController.text = user.lastName;
      _phoneController.text = user.phoneNumber ?? '';

      setState(() {
        _selectedSavingType = user.savingType;
        _selectedRoundingMultiple = user.roundingMultiple;
        _savingPercentage = user.savingPercentage;
        _minBalanceController.text = user.minSafeBalance.toStringAsFixed(0);
        _insufficientOption = user.insufficientBalanceOption;
      });
    }
  }

  /// Guarda los cambios realizados en la información personal del usuario.
  ///
  /// Crea una nueva instancia de [User] con los datos modificados en los controladores
  /// y realiza una llamada HTTP mediante [AuthService.updateProfile].
  ///
  /// Muestra feedback visual (SnackBars) de éxito o error y actualiza el estado de la UI.
  /// Retorna un [Future<void>].
  Future<void> _saveProfile() async {
    setState(() => _isLoading = true);
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final currentUser = authService.user!;

      final updatedUser = User(
        id: currentUser.id,
        username: currentUser.username,
        email: currentUser.email,
        firstName: _firstNameController.text,
        lastName: _lastNameController.text,
        phoneNumber: _phoneController.text,
        bankAccount: currentUser.bankAccount,
        bankName: currentUser.bankName,
        savingType: currentUser.savingType,
        roundingMultiple: currentUser.roundingMultiple,
        savingPercentage: currentUser.savingPercentage,
        minSafeBalance: currentUser.minSafeBalance,
        insufficientBalanceOption: currentUser.insufficientBalanceOption,
        totalSaved: currentUser.totalSaved,
      );

      await authService.updateProfile(updatedUser);

      setState(() {
        _isEditingProfile = false;
        _isLoading = false;
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Perfil actualizado correctamente'), backgroundColor: Colors.green),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  /// Actualiza la configuración de las reglas de ahorro automático.
  ///
  /// Envía los parámetros seleccionados (tipo, porcentaje, saldo mínimo) al backend
  /// mediante [AuthService.updateSavingConfiguration].
  ///
  /// Retorna un [Future<void>].
  Future<void> _saveSavingConfig() async {
    setState(() => _isLoading = true);
    try {
      final authService = Provider.of<AuthService>(context, listen: false);

      await authService.updateSavingConfiguration(
        savingType: _selectedSavingType,
        roundingMultiple: _selectedRoundingMultiple,
        savingPercentage: _savingPercentage,
        minSafeBalance: double.tryParse(_minBalanceController.text) ?? 0.0,
        insufficientBalanceOption: _insufficientOption,
      );

      setState(() {
        _isEditingConfig = false;
        _isLoading = false;
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Configuración de ahorro guardada'), backgroundColor: Colors.green),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error al guardar: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final authService = Provider.of<AuthService>(context);
    final user = authService.user;

    if (user == null) return const Center(child: CircularProgressIndicator());

    return Scaffold(
      appBar: AppBar(
        title: Text(
          'Mi Perfil',
          style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
        ),
        actions: [
          IconButton(
            onPressed: _logout,
            icon: const Icon(Icons.logout, color: Colors.white),
          ),
        ],
      ),
      body: _isLoading && !_isEditingConfig && !_isEditingProfile
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildSectionHeader(
                'Datos Personales',
                Icons.person,
                _isEditingProfile,
                    () => setState(() => _isEditingProfile = !_isEditingProfile)
            ),
            const SizedBox(height: 16),
            _isEditingProfile
                ? _buildProfileForm()
                : _buildProfileInfo(user),

            const SizedBox(height: 32),

            _buildSectionHeader(
                'Configuración de Ahorro',
                Icons.savings,
                _isEditingConfig,
                    () => setState(() => _isEditingConfig = !_isEditingConfig)
            ),
            const SizedBox(height: 16),
            _isEditingConfig
                ? _buildSavingConfigForm()
                : _buildSavingConfigInfo(user),

            const SizedBox(height: 32),

            Text(
              'Cuenta Vinculada',
              style: GoogleFonts.poppins(
                  fontSize: 18,
                  fontWeight: FontWeight.w600
              ),
            ),
            const SizedBox(height: 16),
            _buildBankAccountCard(user),
            const SizedBox(height: 50),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title, IconData icon, bool isEditing, VoidCallback onToggle) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Row(
          children: [
            Icon(icon, color: Theme.of(context).primaryColor),
            const SizedBox(width: 8),
            Text(
              title,
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
        if (!isEditing)
          IconButton(
            onPressed: onToggle,
            icon: const Icon(Icons.edit, size: 20),
            tooltip: 'Editar',
          ),
      ],
    );
  }

  Widget _buildProfileInfo(User user) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            _buildInfoRow('Nombre', '${user.firstName} ${user.lastName}'),
            const Divider(),
            _buildInfoRow('Email', user.email),
            const Divider(),
            _buildInfoRow('Teléfono', user.phoneNumber ?? 'No registrado'),
          ],
        ),
      ),
    );
  }

  Widget _buildProfileForm() {
    return Column(
      children: [
        TextFormField(
          controller: _firstNameController,
          decoration: const InputDecoration(labelText: 'Nombre', border: OutlineInputBorder()),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _lastNameController,
          decoration: const InputDecoration(labelText: 'Apellido', border: OutlineInputBorder()),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _phoneController,
          decoration: const InputDecoration(labelText: 'Teléfono', border: OutlineInputBorder()),
          keyboardType: TextInputType.phone,
        ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            TextButton(
              onPressed: () {
                setState(() => _isEditingProfile = false);
                _loadUserData();
              },
              child: const Text('Cancelar'),
            ),
            const SizedBox(width: 12),
            ElevatedButton(
              onPressed: _isLoading ? null : _saveProfile,
              child: const Text('Guardar Cambios'),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildSavingConfigInfo(User user) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            _buildInfoRow('Método de Ahorro', user.savingType == SavingType.rounding ? 'Redondeo Automático' : 'Porcentaje de Gasto'),
            const Divider(),
            if (user.savingType == SavingType.rounding)
              _buildInfoRow('Redondear a', 'Múltiplos de \$${user.roundingMultiple}'),
            if (user.savingType == SavingType.percentage)
              _buildInfoRow('Porcentaje', '${user.savingPercentage}% de cada gasto'),
            const Divider(),
            _buildInfoRow('Saldo Mínimo Seguro', '\$${user.minSafeBalance.toStringAsFixed(2)}'),
            const Divider(),
            _buildInfoRow('Si saldo insuficiente', _getInsufficientOptionText(user.insufficientBalanceOption)),
          ],
        ),
      ),
    );
  }

  Widget _buildSavingConfigForm() {
    return Card(
      elevation: 0,
      color: Colors.grey[50],
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16), side: BorderSide(color: Colors.grey[300]!)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Método de Ahorro', style: GoogleFonts.poppins(fontWeight: FontWeight.w500)),
            Row(
              children: [
                Expanded(
                  child: RadioListTile<SavingType>(
                    title: const Text('Redondeo'),
                    value: SavingType.rounding,
                    groupValue: _selectedSavingType,
                    onChanged: (val) => setState(() => _selectedSavingType = val!),
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
                Expanded(
                  child: RadioListTile<SavingType>(
                    title: const Text('Porcentaje'),
                    value: SavingType.percentage,
                    groupValue: _selectedSavingType,
                    onChanged: (val) => setState(() => _selectedSavingType = val!),
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
              ],
            ),

            if (_selectedSavingType == SavingType.rounding) ...[
              const SizedBox(height: 8),
              DropdownButtonFormField<int>(
                value: _selectedRoundingMultiple,
                decoration: const InputDecoration(
                  labelText: 'Redondear al siguiente...',
                  border: OutlineInputBorder(),
                  helperText: 'Ej: Gasto \$4.500 -> Ahorras \$500',
                ),
                items: const [
                  DropdownMenuItem(value: 100, child: Text('\$100 pesos')),
                  DropdownMenuItem(value: 500, child: Text('\$500 pesos')),
                  DropdownMenuItem(value: 1000, child: Text('\$1.000 pesos')),
                  DropdownMenuItem(value: 5000, child: Text('\$5.000 pesos')),
                  DropdownMenuItem(value: 10000, child: Text('\$10.000 pesos')),
                ],
                onChanged: (val) => setState(() => _selectedRoundingMultiple = val!),
              ),
            ] else ...[
              const SizedBox(height: 8),
              Text('Porcentaje a ahorrar: ${_savingPercentage.round()}%', style: GoogleFonts.poppins()),
              Slider(
                value: _savingPercentage,
                min: 1,
                max: 50,
                divisions: 49,
                label: '${_savingPercentage.round()}%',
                onChanged: (val) => setState(() => _savingPercentage = val),
              ),
            ],

            const SizedBox(height: 16),
            TextFormField(
              controller: _minBalanceController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Saldo Mínimo de Seguridad',
                prefixText: '\$ ',
                border: OutlineInputBorder(),
                helperText: 'No ahorrar si el saldo baja de este monto',
              ),
            ),

            const SizedBox(height: 16),
            DropdownButtonFormField<InsufficientBalanceOption>(
              value: _insufficientOption,
              decoration: const InputDecoration(
                labelText: 'Si no hay saldo suficiente...',
                border: OutlineInputBorder(),
              ),
              items: [
                DropdownMenuItem(
                  value: InsufficientBalanceOption.noSaving,
                  child: Text(
                    'No ahorrar',
                    style: GoogleFonts.poppins(fontSize: 14),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                DropdownMenuItem(
                  value: InsufficientBalanceOption.pending,
                  child: Text(
                    'Dejar pendiente',
                    style: GoogleFonts.poppins(fontSize: 14),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                DropdownMenuItem(
                  value: InsufficientBalanceOption.respectMinBalance,
                  child: Text(
                    'Ahorrar lo que se pueda',
                    style: GoogleFonts.poppins(fontSize: 14),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
              onChanged: (val) => setState(() => _insufficientOption = val!),
              isExpanded: true,
            ),

            const SizedBox(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                  onPressed: () {
                    setState(() => _isEditingConfig = false);
                    _loadUserData();
                  },
                  child: const Text('Cancelar'),
                ),
                const SizedBox(width: 12),
                ElevatedButton(
                  onPressed: _isLoading ? null : _saveSavingConfig,
                  child: const Text('Guardar Configuración'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBankAccountCard(User user) {
    bool hasAccount = user.bankAccount != null && user.bankAccount!.isNotEmpty;

    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: InkWell(
        onTap: () async {
          await Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const BankAccountScreen()),
          );
          _loadUserData();
        },
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: hasAccount ? Colors.green.withOpacity(0.1) : Colors.grey.withOpacity(0.1),
                child: Icon(
                    Icons.account_balance,
                    color: hasAccount ? Colors.green : Colors.grey
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      hasAccount ? (user.bankName ?? 'Banco') : 'Vincular Cuenta',
                      style: GoogleFonts.poppins(fontWeight: FontWeight.w600, fontSize: 16),
                    ),
                    Text(
                      hasAccount
                          ? '•••• ${user.bankAccount!.substring(user.bankAccount!.length - 4)}'
                          : 'Configura tu cuenta para los débitos',
                      style: GoogleFonts.poppins(fontSize: 13, color: Colors.grey[600]),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: Colors.grey),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: GoogleFonts.poppins(color: Colors.grey[600])),
          Flexible(
            child: Text(
              value,
              style: GoogleFonts.poppins(fontWeight: FontWeight.w500),
              textAlign: TextAlign.right,
            ),
          ),
        ],
      ),
    );
  }

  String _getInsufficientOptionText(InsufficientBalanceOption option) {
    switch (option) {
      case InsufficientBalanceOption.noSaving: return 'No realizar ahorro';
      case InsufficientBalanceOption.pending: return 'Dejar como pendiente';
      case InsufficientBalanceOption.respectMinBalance: return 'Ahorrar parcial';
    }
  }

  /// Cierra la sesión del usuario actual.
  ///
  /// Muestra un diálogo de confirmación y, si el usuario confirma,
  /// llama a [AuthService.logout] y redirige a [LoginScreen],
  /// eliminando todo el historial de navegación anterior.
  void _logout() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Cerrar Sesión'),
        content: const Text('¿Estás seguro de que quieres salir?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            onPressed: () async {
              Navigator.pop(context);
              await Provider.of<AuthService>(context, listen: false).logout();
              if (mounted) {
                Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (_) => const LoginScreen()),
                      (route) => false,
                );
              }
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
            child: const Text('Cerrar Sesión'),
          ),
        ],
      ),
    );
  }
}