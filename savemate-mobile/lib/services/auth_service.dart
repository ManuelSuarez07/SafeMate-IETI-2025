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

  AuthService(this._prefs) {
    _loadUserFromPrefs();
  }

  // Getters
  User? get user => _user;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  bool get isLoggedIn => _user != null && _apiService.isAuthenticated;

  // Métodos de autenticación
  Future<bool> login(String email, String password) async {
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.login(email, password);
      
      // Guardar token
      await _prefs.setString('access_token', response['accessToken']);
      await _prefs.setString('refresh_token', response['refreshToken']);
      
      _apiService.setToken(response['accessToken']);
      
      // Obtener información del usuario
      // Nota: Esto requeriría un endpoint para obtener el usuario actual
      // Por ahora, creamos un usuario básico con el email
      _user = User(
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

  Future<bool> register(User user, String password) async {
    _setLoading(true);
    _clearError();

    try {
      final createdUser = await _apiService.createUser(user, password);
      
      // Auto-login después del registro
      final loginSuccess = await login(user.email, password);
      
      if (loginSuccess) {
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
        throw Exception('Usuario no autenticado');
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

      final user = await _apiService.updateSavingConfiguration(_user!.id!, {
        'savingType': savingType.name.toUpperCase(),
        'roundingMultiple': roundingMultiple,
        'savingPercentage': savingPercentage,
        'minSafeBalance': minSafeBalance,
        'insufficientBalanceOption': insufficientBalanceOption.name.toUpperCase(),
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

  // Métodos privados
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
      final userJson = _user!.toJson();
      await _prefs.setString('user_data', userJson.toString());
    }
  }

  void _loadUserFromPrefs() async {
    try {
      final token = _prefs.getString('access_token');
      final userData = _prefs.getString('user_data');

      if (token != null && userData != null) {
        _apiService.setToken(token);
        
        // Parse user data
        // Nota: Esto requeriría parsear el JSON manualmente o usar jsonDecode
        // Por ahora, creamos un usuario básico
        _user = User(
          email: 'user@savemate.com',
          firstName: 'Usuario',
          lastName: 'SaveMate',
        );
        
        notifyListeners();
        _logger.i('User loaded from preferences');
      }
    } catch (e) {
      _logger.e('Error loading user from preferences: $e');
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

import '../models/user.dart';