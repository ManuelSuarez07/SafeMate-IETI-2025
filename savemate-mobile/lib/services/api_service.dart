import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:logger/logger.dart';

import '../models/user.dart';
import '../models/transaction.dart';
import '../models/saving.dart';
import '../models/ai_recommendation.dart';

class ApiService with ChangeNotifier {
  static const String baseUrl = 'http://10.0.2.2:8080/api';

  final Dio _dio = Dio();
  final Logger _logger = Logger();
  
  String? _accessToken;
  User? _currentUser;
  
  ApiService() {
    _dio.options.baseUrl = baseUrl;
    _dio.options.connectTimeout = const Duration(seconds: 10);
    _dio.options.receiveTimeout = const Duration(seconds: 10);
    
    // Interceptor para agregar token
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        if (_accessToken != null) {
          options.headers['Authorization'] = 'Bearer $_accessToken';
        }
        _logger.d('Request: ${options.method} ${options.path}');
        handler.next(options);
      },
      onResponse: (response, handler) {
        _logger.d('Response: ${response.statusCode} ${response.requestOptions.path}');
        handler.next(response);
      },
      onError: (error, handler) {
        _logger.e('Error: ${error.message}');
        handler.next(error);
      },
    ));
  }

  // Getters
  String? get accessToken => _accessToken;
  User? get currentUser => _currentUser;
  bool get isAuthenticated => _accessToken != null && _currentUser != null;

  // Métodos de autenticación
  Future<Map<String, dynamic>> login(String email, String password) async {
    try {
      final response = await _dio.post('/auth/login', data: {
        'email': email,
        'password': password,
      });

      final data = response.data;
      _accessToken = data['accessToken'];
      
      // Guardar token en SharedPreferences
      // await _storage.write(key: 'access_token', value: _accessToken);
      
      notifyListeners();
      return data;
    } on DioException catch (e) {
      _logger.e('Login error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  Future<void> logout() async {
    _accessToken = null;
    _currentUser = null;
    
    // Limpiar SharedPreferences
    // await _storage.delete(key: 'access_token');
    
    notifyListeners();
  }

  Future<Map<String, dynamic>> refreshToken(String refreshToken) async {
    try {
      final response = await _dio.post('/auth/refresh', data: {
        'refreshToken': refreshToken,
      });

      final data = response.data;
      _accessToken = data['accessToken'];
      
      notifyListeners();
      return data;
    } on DioException catch (e) {
      _logger.e('Token refresh error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  // Métodos de usuarios
  Future<User> createUser(User user, String password) async {
    try {
      final response = await _dio.post('/users', data: {
        ...user.toJson(),
        'password': password,
      });

      final createdUser = User.fromJson(response.data);
      _currentUser = createdUser;
      
      notifyListeners();
      return createdUser;
    } on DioException catch (e) {
      _logger.e('Create user error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  Future<User> getUserById(int id) async {
    try {
      final response = await _dio.get('/users/$id');
      return User.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Get user error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  Future<User> updateUser(int id, User user) async {
    try {
      final response = await _dio.put('/users/$id', data: user.toJson());
      final updatedUser = User.fromJson(response.data);
      
      if (_currentUser?.id == id) {
        _currentUser = updatedUser;
        notifyListeners();
      }
      
      return updatedUser;
    } on DioException catch (e) {
      _logger.e('Update user error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  Future<User> updateSavingConfiguration(int id, Map<String, dynamic> config) async {
    try {
      final response = await _dio.put('/users/$id/saving-config', data: config);
      final updatedUser = User.fromJson(response.data);
      
      if (_currentUser?.id == id) {
        _currentUser = updatedUser;
        notifyListeners();
      }
      
      return updatedUser;
    } on DioException catch (e) {
      _logger.e('Update saving config error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  // Métodos de transacciones
  Future<Transaction> createTransaction(Transaction transaction) async {
    try {
      final response = await _dio.post('/transactions', data: transaction.toJson());
      return Transaction.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Create transaction error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  Future<Transaction> processTransactionFromNotification({
    required int userId,
    required double amount,
    required String description,
    String? merchantName,
    String? notificationSource,
    String? bankReference,
  }) async {
    try {
      final response = await _dio.post('/transactions/from-notification', data: {
        'userId': userId,
        'amount': amount,
        'description': description,
        if (merchantName != null) 'merchantName': merchantName,
        if (notificationSource != null) 'notificationSource': notificationSource,
        if (bankReference != null) 'bankReference': bankReference,
      });
      return Transaction.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Process notification error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  Future<List<Transaction>> getTransactionsByUserId(int userId) async {
    try {
      final response = await _dio.get('/transactions/user/$userId');
      final List<dynamic> data = response.data;
      return data.map((json) => Transaction.fromJson(json)).toList();
    } on DioException catch (e) {
      _logger.e('Get transactions error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  Future<List<Transaction>> getTransactionsByUserIdAndType(int userId, TransactionType type) async {
    try {
      final response = await _dio.get('/transactions/user/$userId/type/${type.name}');
      final List<dynamic> data = response.data;
      return data.map((json) => Transaction.fromJson(json)).toList();
    } on DioException catch (e) {
      _logger.e('Get transactions by type error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  // Métodos de ahorros
  Future<Saving> createSavingGoal(Saving saving) async {
    try {
      final response = await _dio.post('/savings', data: saving.toJson());
      return Saving.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Create saving goal error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  Future<Saving> updateSavingGoalProgress(int id, double amount) async {
    try {
      final response = await _dio.put('/savings/$id/progress', data: {
        'amount': amount,
      });
      return Saving.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Update saving progress error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  Future<List<Saving>> getSavingGoalsByUserId(int userId) async {
    try {
      final response = await _dio.get('/savings/user/$userId');
      final List<dynamic> data = response.data;
      return data.map((json) => Saving.fromJson(json)).toList();
    } on DioException catch (e) {
      _logger.e('Get saving goals error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  Future<List<Saving>> getActiveSavingGoals(int userId) async {
    try {
      final response = await _dio.get('/savings/user/$userId/active');
      final List<dynamic> data = response.data;
      return data.map((json) => Saving.fromJson(json)).toList();
    } on DioException catch (e) {
      _logger.e('Get active saving goals error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  // Métodos de IA
  Future<List<AIRecommendation>> getActiveRecommendations(int userId) async {
    try {
      final response = await _dio.get('/ai/user/$userId/active');
      final List<dynamic> data = response.data;
      return data.map((json) => AIRecommendation.fromJson(json)).toList();
    } on DioException catch (e) {
      _logger.e('Get active recommendations error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  Future<void> generateAllRecommendations(int userId) async {
    try {
      await _dio.post('/ai/generate/all/$userId');
    } on DioException catch (e) {
      _logger.e('Generate recommendations error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  Future<AIRecommendation> applyRecommendation(int recommendationId) async {
    try {
      final response = await _dio.put('/ai/apply/$recommendationId');
      return AIRecommendation.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Apply recommendation error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  // Métodos de utilidad
  void setToken(String token) {
    _accessToken = token;
    notifyListeners();
  }

  void setCurrentUser(User user) {
    _currentUser = user;
    notifyListeners();
  }

  String _handleError(DioException error) {
    switch (error.type) {
      case DioExceptionType.connectionTimeout:
        return 'Tiempo de conexión agotado';
      case DioExceptionType.sendTimeout:
        return 'Tiempo de envío agotado';
      case DioExceptionType.receiveTimeout:
        return 'Tiempo de respuesta agotado';
      case DioExceptionType.badResponse:
        if (error.response?.data is Map<String, dynamic>) {
          final data = error.response!.data as Map<String, dynamic>;
          return data['error'] ?? data['message'] ?? 'Error del servidor';
        }
        return 'Error del servidor: ${error.response?.statusCode}';
      case DioExceptionType.cancel:
        return 'Petición cancelada';
      case DioExceptionType.unknown:
        return 'Error de conexión: ${error.message}';
      default:
        return 'Error desconocido';
    }
  }
}