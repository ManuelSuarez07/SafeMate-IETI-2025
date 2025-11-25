import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import 'screens/login_screen.dart';
import 'screens/home_screen.dart';
import 'services/api_service.dart';
import 'services/notification_service.dart';
import 'services/auth_service.dart';
import 'models/user.dart';

final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
    FlutterLocalNotificationsPlugin();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Inicializar notificaciones
  await NotificationService.initialize(flutterLocalNotificationsPlugin);
  
  // Inicializar SharedPreferences
  final prefs = await SharedPreferences.getInstance();
  
  runApp(SaveMateApp(
    prefs: prefs,
  ));
}

class SaveMateApp extends StatelessWidget {
  final SharedPreferences prefs;
  
  const SaveMateApp({
    Key? key,
    required this.prefs,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => ApiService()),
        ChangeNotifierProvider(create: (context) => AuthService(prefs, Provider.of<ApiService>(context, listen: false))),
        ChangeNotifierProvider(create: (_) => NotificationService()),
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
        home: Consumer<AuthService>(
          builder: (context, authService, child) {
            return authService.isLoggedIn ? HomeScreen() : LoginScreen();
          },
        ),
      ),
    );
  }
}