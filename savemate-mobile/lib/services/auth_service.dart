import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:logger/logger.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:google_sign_in/google_sign_in.dart';
import '../models/user.dart';
import 'api_service.dart';

class AuthService with ChangeNotifier {
  final ApiService _apiService;
  final SharedPreferences _prefs;
  final Logger _logger = Logger();
  final firebase_auth.FirebaseAuth _firebaseAuth = firebase_auth.FirebaseAuth.instance;

  User? _user;
  bool _isLoading = false;
  String? _errorMessage;

  AuthService(this._prefs, this._apiService) {
    Future.microtask(() => _loadUserFromPrefs());
  }

  // Getters
  User? get user => _user;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  bool get isLoggedIn => _user != null && (_apiService.isAuthenticated || _firebaseAuth.currentUser != null);

  // --- LOGIN CON FIREBASE ---
// --- LOGIN ---
  Future<bool> login(String email, String password) async {
    _setLoading(true);
    _clearError();

    try {
      // PASO 1: Autenticar con Firebase (Garantiza seguridad Google)
      final firebase_auth.UserCredential userCredential =
      await _firebaseAuth.signInWithEmailAndPassword(
        email: email,
        password: password,
      );

      // Obtener el token de Firebase que usaremos para las peticiones
      final String? firebaseToken = await userCredential.user?.getIdToken();
      if (firebaseToken == null) throw Exception("Error obteniendo token de Firebase");

      // PASO 2: Obtener los datos reales del usuario desde MySQL
      // Usamos el login "antiguo" del backend. Aunque este endpoint devuelva
      // un token propio, lo que realmente nos interesa es el objeto 'user' que trae.
      User? mysqlUser;
      try {
        final backendResponse = await _apiService.login(email, password);

        if (backendResponse['user'] != null) {
          mysqlUser = User.fromJson(backendResponse['user']);
        }
      } catch (e) {
        _logger.w("Login en backend falló, posible desincronización: $e");
        throw Exception("Error de sincronización con el servidor. Contacte soporte.");
      }

      // PASO 3: Restaurar el Token de Firebase
      // IMPORTANTE: _apiService.login() sobrescribió el token internamente.
      // Debemos forzar el uso del token de Firebase, que es el que el backend valida ahora.
      _apiService.setToken(firebaseToken);

      // Guardar tokens en persistencia
      await _prefs.setString('access_token', firebaseToken);
      // El refresh token de Firebase se maneja interno, pero podemos guardar nulo o el mismo
      await _prefs.setString('refresh_token', '');

      // PASO 4: Guardar el usuario REAL (con ID de MySQL)
      if (mysqlUser != null) {
        _user = mysqlUser;
        await _saveUserToPrefs();
      } else {
        throw Exception("El servidor no devolvió datos del usuario");
      }

      _setLoading(false);
      _logger.i('Login exitoso y sincronizado. ID MySQL: ${_user?.id}');
      return true;

    } on firebase_auth.FirebaseAuthException catch (e) {
      String msg = 'Error de autenticación';
      if (e.code == 'user-not-found') msg = 'No existe usuario con este email';
      if (e.code == 'wrong-password') msg = 'Contraseña incorrecta';
      if (e.code == 'invalid-email') msg = 'Email inválido';

      _setError(msg);
      _setLoading(false);
      return false;
    } catch (e) {
      _setError(e.toString());
      _setLoading(false);
      _logger.e('Login error: $e');
      return false;
    }
  }

// --- LOGIN CON GOOGLE ---
  Future<bool> loginWithGoogle() async {
    _setLoading(true);
    _clearError();

    try {
      final GoogleSignIn googleSignIn = GoogleSignIn.instance;
      final GoogleSignInAccount? googleUser = await googleSignIn.authenticate();

      if (googleUser == null) {
        _setLoading(false);
        return false;
      }

      final GoogleSignInAuthentication googleAuth = await googleUser.authentication;

      final firebase_auth.AuthCredential credential = firebase_auth.GoogleAuthProvider.credential(
        idToken: googleAuth.idToken,
        accessToken: null,
      );

      final userCredential = await _firebaseAuth.signInWithCredential(credential);
      final firebaseUser = userCredential.user;
      final String? token = await firebaseUser?.getIdToken();

      if (token == null || firebaseUser == null) {
        throw Exception("Error al obtener token de Google/Firebase");
      }

      final String syncPassword = "Google_Auto_${firebaseUser.uid}";

      User? mysqlUser;

      try {
        _apiService.setToken(token);

        final loginResponse = await _apiService.login(firebaseUser.email!, syncPassword);

        if (loginResponse['user'] != null) {
          mysqlUser = User.fromJson(loginResponse['user']);
        }
      } catch (e) {
        _logger.i("Usuario nuevo de Google, registrando en MySQL...");

        final newUser = User(
          username: firebaseUser.email!.split('@')[0],
          email: firebaseUser.email!,
          firstName: firebaseUser.displayName?.split(' ').first ?? 'Google',
          lastName: firebaseUser.displayName?.split(' ').last ?? 'User',
          phoneNumber: firebaseUser.phoneNumber ?? '',
        );

        try {
          final createdUser = await _apiService.createUser(newUser, syncPassword);
          mysqlUser = createdUser;
        } catch (regError) {
          _logger.e("Error fatal sincronizando Google con Backend: $regError");
          throw Exception("No se pudo vincular con el servidor. ¿Ya tienes cuenta con contraseña?");
        }
      }

      _apiService.setToken(token);
      await _prefs.setString('access_token', token);
      await _prefs.setString('refresh_token', '');

      if (mysqlUser != null) {
        _user = mysqlUser;
        await _saveUserToPrefs();
      }

      _setLoading(false);
      return true;

    } catch (e) {
      _setError(e.toString());
      _setLoading(false);
      return false;
    }
  }

  // --- REGISTRO CON FIREBASE ---
  Future<bool> register(User user, String password) async {
    _setLoading(true);
    _clearError();

    try {
      // 1. Crear usuario en Firebase
      final firebase_auth.UserCredential userCredential =
      await _firebaseAuth.createUserWithEmailAndPassword(
        email: user.email!,
        password: password,
      );

      // 2. Obtener Token
      final String? token = await userCredential.user?.getIdToken();
      if (token != null) {
        _apiService.setToken(token);
        await _prefs.setString('access_token', token);
      }

      // 3. Crear usuario en tu Backend MySQL (Sincronización)
      // Enviamos los datos al backend para que guarde el registro en MySQL
      try {
        final createdUser = await _apiService.createUser(user, password);
        _user = createdUser; // Usamos el usuario que devuelve el backend con el ID correcto
      } catch (backendError) {
        _logger.w('Usuario creado en Firebase pero falló en Backend: $backendError');
        _user = user;
      }

      await _saveUserToPrefs();

      _setLoading(false);
      _logger.i('Registro exitoso: ${user.email}');
      return true;

    } on firebase_auth.FirebaseAuthException catch (e) {
      String msg = 'Error en registro';
      if (e.code == 'weak-password') msg = 'La contraseña es muy débil';
      if (e.code == 'email-already-in-use') msg = 'El email ya está registrado';

      _setError(msg);
      _setLoading(false);
      _logger.e('Firebase Register error: ${e.message}');
      return false;
    } catch (e) {
      _setError(e.toString());
      _setLoading(false);
      return false;
    }
  }

  // --- LOGOUT ---
  Future<void> logout() async {
    _setLoading(true);

    try {
      // Limpiar preferencias
      await _prefs.remove('access_token');
      await _prefs.remove('refresh_token');
      await _prefs.remove('user_data');

      // Logout de ApiService (limpia variables en memoria)
      await _apiService.logout();

      // Logout de Firebase
      await _firebaseAuth.signOut();

      _user = null;
      _clearError();

      _setLoading(false);
      _logger.i('Logout exitoso');
    } catch (e) {
      _logger.e('Logout error: $e');
      _setLoading(false);
    }
  }

  // --- TOKEN REFRESH ---
  Future<bool> refreshToken() async {
    try {
      final currentUser = _firebaseAuth.currentUser;
      if (currentUser == null) {
        await logout();
        return false;
      }

      // forceRefresh
      final token = await currentUser.getIdToken(true);

      if (token == null) {
        _logger.w('No se pudo refrescar el token (fue nulo)');
        await logout();
        return false;
      }

      await _prefs.setString('access_token', token);
      _apiService.setToken(token);

      _logger.i('Token de Firebase refrescado correctamente');
      return true;
    } catch (e) {
      _logger.e('Error refrescando token: $e');
      await logout();
      return false;
    }
  }

  // --- ACTUALIZAR PERFIL ---
  Future<bool> updateProfile(User updatedUser) async {
    _setLoading(true);
    _clearError();

    try {
      if (_user?.id == null) throw Exception('Usuario no autenticado');

      // Actualizar en Backend
      final user = await _apiService.updateUser(_user!.id!, updatedUser);
      _user = user;
      await _saveUserToPrefs();

      _setLoading(false);
      return true;
    } catch (e) {
      _setError(e.toString());
      _setLoading(false);
      return false;
    }
  }

  // --- CONFIGURACIÓN DE AHORRO ---
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
      if (_user?.id == null) throw Exception('Usuario no autenticado o sin ID');

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
      return true;
    } catch (e) {
      _setError(e.toString());
      _setLoading(false);
      return false;
    }
  }

  // --- VINCULAR CUENTA BANCARIA ---
  Future<bool> linkBankAccount(String bankAccount, String bankName) async {
    _setLoading(true);
    _clearError();

    try {
      if (_user?.id == null) throw Exception('Usuario no autenticado');

      final user = await _apiService.updateSavingConfiguration(_user!.id!, {
        'bankAccount': bankAccount,
        'bankName': bankName,
      });

      _user = user;
      await _saveUserToPrefs();
      _setLoading(false);
      return true;
    } catch (e) {
      _setError(e.toString());
      _setLoading(false);
      return false;
    }
  }

  bool hasBankAccount() {
    return _user?.bankAccount != null && _user!.bankAccount!.isNotEmpty;
  }

  void updateProfileLocal(User updatedUser) {
    _user = updatedUser;
    notifyListeners();
    _saveUserToPrefs();
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
        final userJsonString = jsonEncode(_user!.toJson());
        await _prefs.setString('user_data', userJsonString);
      } catch (e) {
        _logger.e("Error guardando usuario en prefs: $e");
      }
    }
  }

  // Cargar usuario persistido y verificar si la sesión de Firebase sigue activa
  void _loadUserFromPrefs() {
    try {
      final token = _prefs.getString('access_token');
      final userDataString = _prefs.getString('user_data');
      final firebaseUser = _firebaseAuth.currentUser;

      if (token != null && userDataString != null && firebaseUser != null) {
        // La sesión existe localmente y en Firebase
        _apiService.setToken(token);
        final Map<String, dynamic> userData = jsonDecode(userDataString);
        _user = User.fromJson(userData);
        notifyListeners();
        _logger.i('Usuario cargado de preferencias: ${_user?.email}');
      } else {
        // Si no hay usuario de Firebase, forzamos limpieza
        if (firebaseUser == null && token != null) {
          logout();
        }
      }
    } catch (e) {
      _logger.e('Error cargando usuario de preferencias: $e');
      logout();
    }
  }

  void clearError() {
    _clearError();
  }

  String get savingTypeDisplay => _user?.savingTypeDisplay ?? 'Redondeo';
  String get totalSavedDisplay => '\$${(_user?.totalSaved ?? 0.0).toStringAsFixed(2)}';
}