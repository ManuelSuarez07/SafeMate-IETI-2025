import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:logger/logger.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth; // Alias para evitar conflicto con tu modelo User
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
    // CORRECCIÓN CRÍTICA: Usamos Future.microtask para evitar errores de construcción
    Future.microtask(() => _loadUserFromPrefs());
  }

  // Getters
  User? get user => _user;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  // Ahora la autenticación depende de que tengamos usuario local y sesión válida
  bool get isLoggedIn => _user != null && (_apiService.isAuthenticated || _firebaseAuth.currentUser != null);

  // --- LOGIN CON FIREBASE ---
// --- LOGIN CORREGIDO ---
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
        // Si falla (ej: usuario creado en Firebase manual pero no en BD),
        // lanzamos error para no dejar pasar usuarios "fantasmas" sin ID.
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

// --- LOGIN CON GOOGLE (CORREGIDO PARA VERSIÓN 7.x) ---
  Future<bool> loginWithGoogle() async {
    _setLoading(true);
    _clearError();

    try {
      // 1. CAMBIO IMPORTANTE: Usar la instancia singleton, no el constructor
      final GoogleSignIn googleSignIn = GoogleSignIn.instance;

      // 2. CAMBIO IMPORTANTE: Usar 'authenticate()' en lugar de 'signIn()'
      // Esto inicia el flujo interactivo de selección de cuenta
      final GoogleSignInAccount? googleUser = await googleSignIn.authenticate();

      if (googleUser == null) {
        _setLoading(false); // El usuario canceló el inicio de sesión
        return false;
      }

      // 3. Obtener credenciales de autenticación (tokens)
      final GoogleSignInAuthentication googleAuth = await googleUser.authentication;

      // 4. CAMBIO IMPORTANTE: Solo enviamos el idToken.
      // El accessToken ya no es necesario para Firebase en este flujo y la v7 lo eliminó.
      final firebase_auth.AuthCredential credential = firebase_auth.GoogleAuthProvider.credential(
        idToken: googleAuth.idToken,
        accessToken: null, // Pasamos null explícitamente ya que no se requiere
      );

      // 5. Iniciar sesión en Firebase con la credencial de Google
      final userCredential = await _firebaseAuth.signInWithCredential(credential);
      final firebaseUser = userCredential.user;
      final String? token = await firebaseUser?.getIdToken();

      // Validación crítica para asegurar que tenemos sesión válida
      if (token == null || firebaseUser == null) {
        throw Exception("Error al obtener token de Google/Firebase");
      }

      // 6. SINCRONIZACIÓN CON BACKEND MYSQL (Tu lógica personalizada)
      // Usamos el UID de Firebase como contraseña segura para la sincronización
      final String syncPassword = "Google_Auto_${firebaseUser.uid}";

      User? mysqlUser;

      try {
        // A) Intentamos LOGUEAR en el backend (por si el usuario ya existe en MySQL)
        _apiService.setToken(token); // Configuramos el token para la petición

        // ¡Importante! Usamos '!' porque ya validamos arriba que el email existe
        final loginResponse = await _apiService.login(firebaseUser.email!, syncPassword);

        if (loginResponse['user'] != null) {
          mysqlUser = User.fromJson(loginResponse['user']);
        }
      } catch (e) {
        // B) Si falla el login, intentamos REGISTRAR en el backend
        _logger.i("Usuario nuevo de Google, registrando en MySQL...");

        final newUser = User(
          username: firebaseUser.email!.split('@')[0],
          email: firebaseUser.email!, // Usamos '!' con seguridad
          firstName: firebaseUser.displayName?.split(' ').first ?? 'Google',
          lastName: firebaseUser.displayName?.split(' ').last ?? 'User',
          phoneNumber: firebaseUser.phoneNumber ?? '',
        );

        try {
          // Registramos el usuario en MySQL usando el UID como password
          final createdUser = await _apiService.createUser(newUser, syncPassword);
          mysqlUser = createdUser;
        } catch (regError) {
          _logger.e("Error fatal sincronizando Google con Backend: $regError");
          throw Exception("No se pudo vincular con el servidor. ¿Ya tienes cuenta con contraseña?");
        }
      }

      // 7. Finalizar sesión y guardar datos
      // Restauramos el token de Firebase (por si el login del backend lo cambió)
      _apiService.setToken(token);
      await _prefs.setString('access_token', token);
      await _prefs.setString('refresh_token', ''); // Firebase maneja el refresh internamente

      if (mysqlUser != null) {
        _user = mysqlUser;
        await _saveUserToPrefs();
      }

      _setLoading(false);
      return true;

    } catch (e) {
      // Manejo general de errores
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
        // Fallback: Usar datos locales si el backend falla momentáneamente
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

  // --- TOKEN REFRESH CORREGIDO ---
  Future<bool> refreshToken() async {
    try {
      final currentUser = _firebaseAuth.currentUser;
      if (currentUser == null) {
        await logout();
        return false;
      }

      // forceRefresh: true fuerza a pedir un nuevo token
      final token = await currentUser.getIdToken(true);

      // CORRECCIÓN: Verificamos si el token es nulo antes de usarlo
      if (token == null) {
        _logger.w('No se pudo refrescar el token (fue nulo)');
        await logout();
        return false;
      }

      // Ahora 'token' ya es tratado como String seguro
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