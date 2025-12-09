import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:logger/logger.dart';

import '../models/user.dart';
import '../models/transaction.dart';
import '../models/saving.dart';
import '../models/ai_recommendation.dart';

/// Servicio central encargado de gestionar todas las comunicaciones HTTP con el backend.
///
/// Esta clase actúa como una capa de abstracción sobre [Dio] para realizar peticiones REST.
/// Utiliza el mixin [ChangeNotifier] para notificar a los widgets escuchas (vía Provider)
/// sobre cambios en el estado de autenticación o actualizaciones del usuario actual.
///
/// Responsabilidades principales:
/// 1. Gestión de autenticación (Login, Logout, Refresh Token).
/// 2. Inyección automática del token JWT en las cabeceras mediante interceptores.
/// 3. Operaciones CRUD para [User], [Transaction] y [Saving].
/// 4. Comunicación con los endpoints de Inteligencia Artificial.
/// 5. Manejo centralizado de errores de red ([DioException]).
class ApiService with ChangeNotifier {
  /// URL base del servidor backend.
  static const String baseUrl = 'http://34.123.185.74:8080/api';

  final Dio _dio = Dio();
  final Logger _logger = Logger();

  String? _accessToken;
  User? _currentUser;

  /// Configura el cliente HTTP [Dio] con tiempos de espera e interceptores.
  ///
  /// Los interceptores se encargan de:
  /// - Agregar el encabezado `Authorization: Bearer ...` si existe un token.
  /// - Loguear las peticiones, respuestas y errores para depuración.
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

  /// Retorna `true` si el servicio tiene un token y un usuario cargado en memoria.
  bool get isAuthenticated => _accessToken != null && _currentUser != null;

  // --- AUTENTICACIÓN ---

  /// Autentica al usuario en el sistema.
  ///
  /// Realiza una petición `POST /auth/login`.
  ///
  /// [email]: Correo electrónico del usuario.
  /// [password]: Contraseña en texto plano.
  ///
  /// Retorna un [Future] con el mapa de respuesta que contiene el token.
  /// Notifica a los oyentes tras un inicio de sesión exitoso.
  Future<Map<String, dynamic>> login(String email, String password) async {
    try {
      final response = await _dio.post('/auth/login', data: {
        'email': email,
        'password': password,
      });

      final data = response.data;
      _accessToken = data['accessToken'];

      notifyListeners();
      return data;
    } on DioException catch (e) {
      _logger.e('Login error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  /// Cierra la sesión del usuario localmente.
  ///
  /// Limpia el [_accessToken] y el [_currentUser], y notifica a los oyentes
  /// para que la UI redirija a la pantalla de login.
  Future<void> logout() async {
    _accessToken = null;
    _currentUser = null;
    notifyListeners();
  }

  /// Renueva el token de acceso utilizando un token de refresco.
  ///
  /// Realiza una petición `POST /auth/refresh`.
  /// Actualiza el [_accessToken] interno y notifica a los oyentes.
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

  // --- USUARIOS ---

  /// Registra un nuevo usuario en la base de datos.
  ///
  /// Realiza una petición `POST /users`.
  ///
  /// [user]: Objeto [User] con los datos básicos.
  /// [password]: Contraseña deseada para la cuenta.
  ///
  /// Retorna el [User] creado y lo establece como el usuario actual.
  Future<User> createUser(User user, String password) async {
    try {
      final Map<String, dynamic> userMap = user.toJson();
      userMap['password'] = password;

      final response = await _dio.post('/users', data: userMap);

      final createdUser = User.fromJson(response.data);
      _currentUser = createdUser;

      notifyListeners();
      return createdUser;
    } on DioException catch (e) {
      _logger.e('Create user error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  /// Obtiene los detalles de un usuario específico.
  ///
  /// Realiza una petición `GET /users/$id`.
  Future<User> getUserById(int id) async {
    try {
      final response = await _dio.get('/users/$id');
      return User.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Get user error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  /// Actualiza la información básica del perfil de un usuario.
  ///
  /// Realiza una petición `PUT /users/$id`.
  /// Si el usuario actualizado es el actual, refresca el estado local.
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

  /// Actualiza la configuración de ahorro del usuario (tipo, redondeo, porcentajes).
  ///
  /// Realiza una petición `PUT /users/$id/saving-config`.
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

  /// Vincula los datos de una cuenta bancaria externa al perfil del usuario.
  ///
  /// Realiza una petición `PUT /users/$userId/bank-account`.
  Future<User> linkBankAccount(int userId, String bankAccount, String bankName) async {
    try {
      final response = await _dio.put('/users/$userId/bank-account', data: {
        'bankAccount': bankAccount,
        'bankName': bankName,
      });

      final updatedUser = User.fromJson(response.data);

      if (_currentUser?.id == userId) {
        _currentUser = updatedUser;
        notifyListeners();
      }

      return updatedUser;
    } on DioException catch (e) {
      _logger.e('Link bank account error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  // --- TRANSACCIONES ---

  /// Crea una nueva transacción (gasto o ingreso) manual.
  ///
  /// Realiza una petición `POST /transactions`.
  Future<Transaction> createTransaction(Transaction transaction) async {
    try {
      final response = await _dio.post('/transactions', data: transaction.toJson());
      return Transaction.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Create transaction error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  /// Realiza un depósito simulado para recargar el saldo de ahorros.
  ///
  /// Realiza una petición `POST /transactions/saving-deposit`.
  ///
  /// Efecto secundario: Actualiza los datos del usuario local (para refrescar el saldo total)
  /// llamando internamente a [getUserById].
  Future<Transaction> createSavingDeposit({
    required int userId,
    required double amount,
  }) async {
    try {
      final response = await _dio.post(
        '/transactions/saving-deposit',
        queryParameters: {
          'userId': userId,
          'amount': amount,
          'description': 'Recarga desde cuenta vinculada',
        },
      );

      final updatedUser = await getUserById(userId);
      setCurrentUser(updatedUser);

      return Transaction.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Deposit error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  /// Procesa el retiro de fondos de la cuenta de ahorros hacia la cuenta bancaria.
  ///
  /// Realiza una petición `POST /transactions/withdraw`.
  ///
  /// Efecto secundario: Actualiza el saldo del usuario localmente.
  Future<Transaction> withdrawFunds({
    required int userId,
    required double amount,
  }) async {
    try {
      final response = await _dio.post(
        '/transactions/withdraw',
        queryParameters: {
          'userId': userId,
          'amount': amount,
          'description': 'Retiro a cuenta bancaria',
        },
      );

      final updatedUser = await getUserById(userId);
      setCurrentUser(updatedUser);

      return Transaction.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Withdraw error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  /// Crea una transacción basada en datos parseados de notificaciones bancarias.
  ///
  /// Realiza una petición `POST /transactions/from-notification`.
  /// Útil para integraciones automáticas mediante lectura de SMS o Push.
  Future<Transaction> processTransactionFromNotification({
    required int userId,
    required double amount,
    required String description,
    String? merchantName,
    String? notificationSource,
    String? bankReference,
  }) async {
    try {
      final response = await _dio.post('/transactions/from-notification', queryParameters: {
        'userId': userId,
        'amount': amount,
        'description': description,
        if (merchantName != null) 'merchantName': merchantName,
        if (notificationSource != null) 'notificationSource': notificationSource,
        if (bankReference != null) 'bankReference': bankReference,
      });

      final updatedUser = await getUserById(userId);
      setCurrentUser(updatedUser);

      return Transaction.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Process notification error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  /// Obtiene el historial completo de transacciones de un usuario.
  ///
  /// Realiza una petición `GET /transactions/user/$userId`.
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

  /// Obtiene transacciones filtradas por tipo (Gasto, Ingreso, Ahorro).
  ///
  /// Realiza una petición `GET /transactions/user/$userId/type/$type`.
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

  // --- AHORROS (METAS) ---

  /// Crea una nueva meta de ahorro.
  ///
  /// Realiza una petición `POST /savings`.
  Future<Saving> createSavingGoal(Saving saving) async {
    try {
      final response = await _dio.post('/savings', data: saving.toJson());
      return Saving.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Create saving goal error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  /// Actualiza el progreso monetario de una meta específica.
  ///
  /// Realiza una petición `PUT /savings/$id/progress`.
  /// Se usa para añadir fondos manualmente a una meta.
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

  /// Obtiene todas las metas de ahorro de un usuario.
  ///
  /// Realiza una petición `GET /savings/user/$userId`.
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

  /// Obtiene únicamente las metas de ahorro que están activas (no completadas).
  ///
  /// Realiza una petición `GET /savings/user/$userId/active`.
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

  // --- IA / RECOMENDACIONES ---

  /// Obtiene las recomendaciones de IA activas para el usuario.
  ///
  /// Realiza una petición `GET /ai/user/$userId/active`.
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

  /// Solicita al backend generar recomendaciones basadas en patrones de gasto.
  ///
  /// Realiza una petición `POST /ai/generate/spending-patterns/$userId`.
  /// Esta operación puede ser costosa, por lo que el backend podría procesarla asíncronamente.
  Future<void> generateSpendingPatternRecommendations(int userId) async {
    try {
      await _dio.post('/ai/generate/spending-patterns/$userId');
    } on DioException catch (e) {
      _logger.e('Generate spending pattern recommendations error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  /// Solicita la generación de todo tipo de recomendaciones de IA.
  ///
  /// Realiza una petición `POST /ai/generate/all/$userId`.
  Future<void> generateAllRecommendations(int userId) async {
    try {
      await _dio.post('/ai/generate/all/$userId');
    } on DioException catch (e) {
      _logger.e('Generate recommendations error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  /// Marca una recomendación como "aplicada" o ejecutada por el usuario.
  ///
  /// Realiza una petición `PUT /ai/apply/$recommendationId`.
  /// Retorna el objeto [AIRecommendation] actualizado.
  Future<AIRecommendation> applyRecommendation(int recommendationId) async {
    try {
      final response = await _dio.put('/ai/apply/$recommendationId');
      return AIRecommendation.fromJson(response.data);
    } on DioException catch (e) {
      _logger.e('Apply recommendation error: ${e.response?.data}');
      throw _handleError(e);
    }
  }

  // --- UTILIDADES ---

  /// Actualiza manualmente el token de acceso y notifica a los oyentes.
  void setToken(String token) {
    _accessToken = token;
    notifyListeners();
  }

  /// Actualiza manualmente el usuario actual y notifica a los oyentes.
  void setCurrentUser(User user) {
    _currentUser = user;
    notifyListeners();
  }

  /// Transforma errores de [DioException] en mensajes de texto amigables para el usuario.
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
        } else if (error.response?.data is String) {
          return error.response!.data.toString();
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