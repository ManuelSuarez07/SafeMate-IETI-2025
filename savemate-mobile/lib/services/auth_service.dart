import 'dart:convert';
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

      // Guardar tokens
      await _prefs.setString('access_token', response['accessToken']);
      await _prefs.setString('refresh_token', response['refreshToken']);

      _apiService.setToken(response['accessToken']);

      // Usamos el usuario real que devuelve el backend
      if (response['user'] != null) {
        _user = User.fromJson(response['user']);
        await _saveUserToPrefs();
      } else {
        _user = User(
          id: 0,
          username: email.split('@')[0],
          email: email,
          firstName: 'Usuario',
          lastName: 'SaveMate',
        );
      }

      _setLoading(false);
      _logger.i('Login successful for user: ${_user?.email} (ID: ${_user?.id})');
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
      if (user.username == null || user.username!.isEmpty) {
        // Lógica opcional para username
      }

      final createdUser = await _apiService.createUser(user, password);

      final loginSuccess = await login(createdUser.email ?? user.email!, password);

      if (loginSuccess) {
        // Login maneja el guardado
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
      await _prefs.remove('access_token');
      await _prefs.remove('refresh_token');
      await _prefs.remove('user_data');

      await _apiService.logout();

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

      // CORRECCIÓN: Convertir a MAYÚSCULAS explícitamente para el Backend Java
      String savingTypeString = savingType == SavingType.rounding ? 'ROUNDING' : 'PERCENTAGE';

      String insufficientOptionString;
      switch (insufficientBalanceOption) {
        case InsufficientBalanceOption.noSaving:
          insufficientOptionString = 'NO_SAVING';
          break;
        case InsufficientBalanceOption.pending:
          insufficientOptionString = 'PENDING';
          break;
        case InsufficientBalanceOption.respectMinBalance:
          insufficientOptionString = 'RESPECT_MIN_BALANCE';
          break;
      }

      final user = await _apiService.updateSavingConfiguration(_user!.id!, {
        'savingType': savingTypeString,
        'roundingMultiple': roundingMultiple,
        'savingPercentage': savingPercentage,
        'minSafeBalance': minSafeBalance,
        'insufficientBalanceOption': insufficientOptionString,
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

  // --- MÉTODOS AGREGADOS (Corrección) ---

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

  // Este es el método que te faltaba:
  bool hasBankAccount() {
    return _user?.bankAccount != null && _user!.bankAccount!.isNotEmpty;
  }

  void updateProfileLocal(User updatedUser) {
    _user = updatedUser;
    notifyListeners();
    _saveUserToPrefs();
  }

  // --------------------------------------

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
        final Map<String, dynamic> userData = jsonDecode(userDataString);
        _user = User.fromJson(userData);
        notifyListeners();
        _logger.i('User loaded from preferences: ${_user?.email}');
      }
    } catch (e) {
      _logger.e('Error loading user from preferences: $e');
      logout();
    }
  }

  void clearError() {
    _clearError();
  }

  String get savingTypeDisplay {
    return _user?.savingTypeDisplay ?? 'Redondeo';
  }

  String get totalSavedDisplay {
    return '\$${(_user?.totalSaved ?? 0.0).toStringAsFixed(2)}';
  }
}