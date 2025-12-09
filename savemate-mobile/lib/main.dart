import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:firebase_core/firebase_core.dart';
import 'screens/login_screen.dart';
import 'screens/home_screen.dart';
import 'services/api_service.dart';
import 'services/notification_service.dart';
import 'services/auth_service.dart';

/// Instancia global del plugin de notificaciones para su inicialización en el punto de entrada.
final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
FlutterLocalNotificationsPlugin();

/// Punto de entrada principal de la aplicación.
///
/// Responsabilidades:
/// 1. Inicializar los bindings nativos de Flutter.
/// 2. Configurar servicios externos asíncronos: Firebase, Notificaciones Locales y [SharedPreferences].
/// 3. Lanzar el widget raíz [SaveMateApp] inyectando las preferencias compartidas.
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  await NotificationService.initialize(flutterLocalNotificationsPlugin);
  final prefs = await SharedPreferences.getInstance();

  runApp(SaveMateApp(
    prefs: prefs,
  ));
}

/// Widget raíz de la aplicación SaveMate.
///
/// Este widget es responsable de:
/// 1. Configurar la inyección de dependencias global mediante [MultiProvider].
/// 2. Establecer la relación de dependencia entre [ApiService] y [NotificationService] usando [ChangeNotifierProxyProvider].
/// 3. Definir el tema visual global (Colores, Tipografía Poppins).
/// 4. Gestionar el enrutamiento inicial basado en el estado de autenticación del usuario.
class SaveMateApp extends StatelessWidget {
  /// Instancia de preferencias compartidas inyectada desde el [main].
  final SharedPreferences prefs;

  const SaveMateApp({
    Key? key,
    required this.prefs,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        // 1. Creamos el ApiService
        // Proveedor base para comunicaciones HTTP.
        ChangeNotifierProvider(create: (_) => ApiService()),

        // 2. Creamos el AuthService (que depende de ApiService)
        // Inyecta ApiService para realizar el login contra el backend propio.
        ChangeNotifierProvider(create: (context) => AuthService(prefs, Provider.of<ApiService>(context, listen: false))),

        // 3. [MODIFICADO] Usamos ProxyProvider para inyectar ApiService en NotificationService
        // Permite que el servicio de notificaciones envíe transacciones detectadas al backend.
        ChangeNotifierProxyProvider<ApiService, NotificationService>(
          create: (_) => NotificationService(), // Obtiene la instancia Singleton
          update: (_, apiService, notificationService) {
            // Aquí ocurre la magia: le pasamos el ApiService al NotificationService
            if (notificationService != null) {
              notificationService.setApiService(apiService);
            }
            return notificationService!;
          },
        ),
      ],
      child: MaterialApp(
        title: 'SaveMate',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          primarySwatch: Colors.green,
          primaryColor: const Color(0xFF4CAF50),
          colorScheme: ColorScheme.fromSeed(
            seedColor: const Color(0xFF4CAF50),
            brightness: Brightness.light,
          ),
          useMaterial3: true,
          textTheme: GoogleFonts.poppinsTextTheme(),
          appBarTheme: AppBarTheme(
            backgroundColor: const Color(0xFF4CAF50),
            foregroundColor: Colors.white,
            elevation: 0,
            titleTextStyle: GoogleFonts.poppins(
              color: Colors.white,
              fontSize: 20,
              fontWeight: FontWeight.w600,
            ),
          ),
          elevatedButtonTheme: ElevatedButtonThemeData(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF4CAF50),
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
            ),
          ),
          cardTheme: CardThemeData(
            elevation: 4,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
        ),
        // Decide qué pantalla mostrar basado en si el usuario ya inició sesión
        home: Consumer<AuthService>(
          builder: (context, authService, child) {
            return authService.isLoggedIn ? HomeScreen() : LoginScreen();
          },
        ),
      ),
    );
  }
}