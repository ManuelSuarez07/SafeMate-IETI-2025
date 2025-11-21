import 'dart:convert'; // <--- IMPORTANTE: Necesario para jsonEncode y jsonDecode
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:logger/logger.dart';
import '../models/user.dart';
import 'api_service.dart';

class AuthService with ChangeNotifier {
  final ApiService _apiService;
  final SharedPreferences _prefs;
  final Logger _logger = Logger();

  User? _user;
  bool _isLoading = false;
  String? _errorMessage;

  AuthService(this._prefs, this._apiService) {
    _loadUserFromPrefs();
  }

  // Getters
  User? get user => _user;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  bool get isLoggedIn => _user != null && _apiService.isAuthenticated;

  // --- LOGIN ---
  Future<bool> login(String email, String password) async {
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.login(email, password);

      // Guardar token
      await _prefs.setString('access_token', response['accessToken']);
      await _prefs.setString('refresh_token', response['refreshToken']);

      _apiService.setToken(response['accessToken']);

      // TODO: Lo ideal sería llamar a un endpoint '/api/users/me' aquí para obtener el usuario real.
      // Por ahora, creamos un usuario temporal con los datos que tenemos.
      _user = User(
        // Asumimos que User tiene un constructor que acepta estos parámetros
        username: email.split('@')[0],
        email: email,
        firstName: 'Usuario',
        lastName: 'SaveMate',
      );

      await _saveUserToPrefs();

      _setLoading(false);
      _logger.i('Login successful for user: $email');
      return true;
    } catch (e) {
      _setError(e.toString());
      _setLoading(false);
      _logger.e('Login failed: $e');
      return false;
    }
  }

  // --- REGISTRO ---
  Future<bool> register(User user, String password) async {
    _setLoading(true);
    _clearError();

    try {
      // FIX: Validación preventiva para evitar error 400 en el backend
      if (user.username == null || user.username!.isEmpty) {
        _logger.w("Username vacío, el backend podría rechazar esto. Asegúrate de enviarlo desde el formulario.");
        // Si tu modelo User no es 'final', podrías hacer:
        // user.username = user.email.split('@')[0];
      }

      final createdUser = await _apiService.createUser(user, password);

      // Auto-login después del registro
      // Usamos el email del usuario creado para asegurar consistencia
      final loginSuccess = await login(createdUser.email ?? user.email!, password);

      if (loginSuccess) {
        // Sobrescribimos el usuario local con el que devolvió el backend (que tiene el ID real)
        _user = createdUser;
        await _saveUserToPrefs();
      }

      _setLoading(false);
      _logger.i('Registration successful for user: ${user.email}');
      return true;
    } catch (e) {
      _setError(e.toString());
      _setLoading(false);
      _logger.e('Registration failed: $e');
      return false;
    }
  }

  Future<void> logout() async {
    _setLoading(true);

    try {
      // Limpiar preferencias
      await _prefs.remove('access_token');
      await _prefs.remove('refresh_token');
      await _prefs.remove('user_data');

      // Limpiar servicio API
      await _apiService.logout();

      // Limpiar estado local
      _user = null;
      _clearError();

      _setLoading(false);
      _logger.i('Logout successful');
    } catch (e) {
      _logger.e('Logout error: $e');
      _setLoading(false);
    }
  }

  Future<bool> refreshToken() async {
    try {
      final refreshToken = _prefs.getString('refresh_token');
      if (refreshToken == null) {
        await logout();
        return false;
      }

      final response = await _apiService.refreshToken(refreshToken);

      // Guardar nuevo token
      await _prefs.setString('access_token', response['accessToken']);
      await _prefs.setString('refresh_token', response['refreshToken']);

      _apiService.setToken(response['accessToken']);

      _logger.i('Token refreshed successfully');
      return true;
    } catch (e) {
      _logger.e('Token refresh failed: $e');
      await logout();
      return false;
    }
  }

  Future<bool> updateProfile(User updatedUser) async {
    _setLoading(true);
    _clearError();

    try {
      if (_user?.id == null) {
        throw Exception('Usuario no autenticado (ID nulo)');
      }

      final user = await _apiService.updateUser(_user!.id!, updatedUser);
      _user = user;

      await _saveUserToPrefs();

      _setLoading(false);
      _logger.i('Profile updated successfully');
      return true;
    } catch (e) {
      _setError(e.toString());
      _setLoading(false);
      _logger.e('Profile update failed: $e');
      return false;
    }
  }

  Future<bool> updateSavingConfiguration({
    required SavingType savingType,
    required int roundingMultiple,
    required double savingPercentage,
    required double minSafeBalance,
    required InsufficientBalanceOption insufficientBalanceOption,
  }) async {
    _setLoading(true);
    _clearError();

    try {
      if (_user?.id == null) {
        throw Exception('Usuario no autenticado');
      }

      // Nota: Asegúrate de que 'SavingType' tenga un método .name o toString() adecuado
      final user = await _apiService.updateSavingConfiguration(_user!.id!, {
        'savingType': savingType.toString().split('.').last, // FIX: Más seguro que .name en versiones viejas de Dart
        'roundingMultiple': roundingMultiple,
        'savingPercentage': savingPercentage,
        'minSafeBalance': minSafeBalance,
        'insufficientBalanceOption': insufficientBalanceOption.toString().split('.').last,
      });

      _user = user;
      await _saveUserToPrefs();

      _setLoading(false);
      _logger.i('Saving configuration updated successfully');
      return true;
    } catch (e) {
      _setError(e.toString());
      _setLoading(false);
      _logger.e('Saving configuration update failed: $e');
      return false;
    }
  }

  Future<bool> linkBankAccount(String bankAccount, String bankName) async {
    _setLoading(true);
    _clearError();

    try {
      if (_user?.id == null) {
        throw Exception('Usuario no autenticado');
      }

      final user = await _apiService.updateSavingConfiguration(_user!.id!, {
        'bankAccount': bankAccount,
        'bankName': bankName,
      });

      _user = user;
      await _saveUserToPrefs();

      _setLoading(false);
      _logger.i('Bank account linked successfully');
      return true;
    } catch (e) {
      _setError(e.toString());
      _setLoading(false);
      _logger.e('Bank account linking failed: $e');
      return false;
    }
  }

  // --- MÉTODOS PRIVADOS ---

  void _setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  void _setError(String error) {
    _errorMessage = error;
    notifyListeners();
  }

  void _clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  Future<void> _saveUserToPrefs() async {
    if (_user != null) {
      try {
        // FIX: Usar jsonEncode para crear un string JSON válido
        final userJsonString = jsonEncode(_user!.toJson());
        await _prefs.setString('user_data', userJsonString);
      } catch (e) {
        _logger.e("Error saving user to prefs: $e");
      }
    }
  }

  void _loadUserFromPrefs() {
    try {
      final token = _prefs.getString('access_token');
      final userDataString = _prefs.getString('user_data');

      if (token != null && userDataString != null) {
        _apiService.setToken(token);

        // FIX: Decodificar el JSON string correctamente y crear el objeto User real
        final Map<String, dynamic> userData = jsonDecode(userDataString);
        _user = User.fromJson(userData);

        notifyListeners();
        _logger.i('User loaded from preferences: ${_user?.email}');
      }
    } catch (e) {
      _logger.e('Error loading user from preferences: $e');
      // Si hay error en los datos guardados, es mejor limpiar para evitar crashes
      logout();
    }
  }

  // Utilidades
  void clearError() {
    _clearError();
  }

  bool hasBankAccount() {
    return _user?.bankAccount != null && _user!.bankAccount!.isNotEmpty;
  }

  String get savingTypeDisplay {
    return _user?.savingTypeDisplay ?? 'Redondeo';
  }

  String get totalSavedDisplay {
    return '\$${(_user?.totalSaved ?? 0.0).toStringAsFixed(2)}';
  }
}